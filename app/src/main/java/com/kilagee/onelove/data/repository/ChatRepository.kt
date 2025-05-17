package com.kilagee.onelove.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.kilagee.onelove.data.model.Chat
import com.kilagee.onelove.data.model.MediaType
import com.kilagee.onelove.data.model.Message
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val userRepository: UserRepository
) {
    private val chatsCollection = firestore.collection("chats")
    private val messagesCollection = firestore.collection("messages")
    
    suspend fun createChat(participants: List<String>, isAIChat: Boolean = false): Result<String> {
        return try {
            // Check if a chat with these participants already exists
            val query = chatsCollection
                .whereEqualTo("participants", participants.sorted())
                .limit(1)
                .get()
                .await()
                
            if (!query.isEmpty) {
                // Chat already exists, return its ID
                return Result.success(query.documents[0].id)
            }
            
            // Create a new chat
            val chat = Chat(
                participants = participants.sorted(),
                lastMessageTimestamp = Timestamp.now(),
                unreadCount = participants.associateWith { 0 },
                containsAI = isAIChat
            )
            
            val result = chatsCollection.add(chat.toMap()).await()
            Result.success(result.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getUserChats(userId: String): Flow<Result<List<Chat>>> = flow {
        try {
            val query = chatsCollection
                .whereArrayContains("participants", userId)
                .orderBy("lastMessageTimestamp", Query.Direction.DESCENDING)
                .get()
                .await()
                
            val chats = query.documents.mapNotNull { doc ->
                doc.toObject(Chat::class.java)?.copy(id = doc.id)
            }
            
            emit(Result.success(chats))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
    
    suspend fun sendMessage(
        chatId: String,
        senderId: String,
        receiverId: String,
        content: String,
        mediaUrl: String = "",
        mediaType: MediaType = MediaType.NONE,
        isAIGenerated: Boolean = false
    ): Result<String> {
        return try {
            val message = Message(
                id = UUID.randomUUID().toString(),
                chatId = chatId,
                senderId = senderId,
                receiverId = receiverId,
                content = content,
                mediaUrl = mediaUrl,
                mediaType = mediaType,
                timestamp = Timestamp.now(),
                isAIGenerated = isAIGenerated
            )
            
            // Add message to messages collection
            messagesCollection.add(message.toMap()).await()
            
            // Update the chat document
            val chatDoc = chatsCollection.document(chatId).get().await()
            val unreadCount = chatDoc.get("unreadCount") as? Map<String, Int> ?: emptyMap()
            val updatedUnreadCount = unreadCount.toMutableMap()
            
            // Increment unread count for the receiver
            updatedUnreadCount[receiverId] = (updatedUnreadCount[receiverId] ?: 0) + 1
            
            chatsCollection.document(chatId).update(
                mapOf(
                    "lastMessage" to content,
                    "lastMessageTimestamp" to message.timestamp,
                    "unreadCount" to updatedUnreadCount
                )
            ).await()
            
            // Award points for messaging (only for human interaction)
            if (!isAIGenerated) {
                userRepository.addPoints(senderId, 1)
            }
            
            Result.success(message.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getMessages(
        chatId: String,
        limit: Int = 50,
        lastVisibleTimestamp: Timestamp? = null
    ): Flow<Result<List<Message>>> = flow {
        try {
            var query = messagesCollection
                .whereEqualTo("chatId", chatId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                
            if (lastVisibleTimestamp != null) {
                query = query.startAfter(lastVisibleTimestamp)
            }
            
            val result = query.get().await()
            val messages = result.documents.mapNotNull { doc ->
                doc.toObject(Message::class.java)?.copy(id = doc.id)
            }
            
            emit(Result.success(messages))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
    
    suspend fun markChatAsRead(chatId: String, userId: String): Result<Unit> {
        return try {
            val chatDoc = chatsCollection.document(chatId).get().await()
            val unreadCount = chatDoc.get("unreadCount") as? Map<String, Int> ?: emptyMap()
            val updatedUnreadCount = unreadCount.toMutableMap()
            
            // Reset unread count for this user
            updatedUnreadCount[userId] = 0
            
            chatsCollection.document(chatId)
                .update("unreadCount", updatedUnreadCount)
                .await()
                
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun deleteChat(chatId: String): Result<Unit> {
        return try {
            // Delete the chat document
            chatsCollection.document(chatId).delete().await()
            
            // Delete all messages in the chat
            val messagesQuery = messagesCollection
                .whereEqualTo("chatId", chatId)
                .get()
                .await()
                
            val batch = firestore.batch()
            messagesQuery.documents.forEach { doc ->
                batch.delete(doc.reference)
            }
            
            batch.commit().await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun generateAIResponse(userMessage: String): String {
        // This would normally call an OpenAI or Dialogflow API
        // For now, implement simple responses
        val responses = listOf(
            "That's interesting! Tell me more about yourself.",
            "I'd love to chat more with you. What do you enjoy doing in your free time?",
            "That sounds wonderful! What else interests you?",
            "I'm enjoying our conversation. How has your day been?",
            "That's a great point! I'm curious to hear more of your thoughts.",
            "You seem like such an interesting person. What are you passionate about?",
            "I'm glad we're connecting. What are you looking for in a relationship?",
            "Thanks for sharing that with me! I appreciate getting to know you better.",
            "That's fascinating! I'd love to learn more about that.",
            "You really have a way with words. What else would you like to talk about?"
        )
        
        return responses.random()
    }
}
