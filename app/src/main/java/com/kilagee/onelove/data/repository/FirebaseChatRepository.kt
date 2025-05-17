package com.kilagee.onelove.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.kilagee.onelove.data.database.dao.ChatDao
import com.kilagee.onelove.data.database.dao.MessageDao
import com.kilagee.onelove.data.model.Chat
import com.kilagee.onelove.data.model.Message
import com.kilagee.onelove.domain.model.Resource
import com.kilagee.onelove.domain.repository.ChatRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseChatRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val chatDao: ChatDao,
    private val messageDao: MessageDao
) : ChatRepository {
    
    private val chatsCollection = firestore.collection("chats")
    private val messagesCollection = firestore.collection("messages")
    private val usersCollection = firestore.collection("users")
    
    override fun getChatsForCurrentUser(): Flow<Resource<List<Chat>>> = callbackFlow {
        trySend(Resource.Loading)
        
        val currentUserId = getCurrentUserId() ?: run {
            trySend(Resource.error("User not authenticated"))
            awaitClose()
            return@callbackFlow
        }
        
        // Query chats where current user is either user1 or user2
        val query = chatsCollection
            .whereArrayContains("participants", currentUserId)
            .orderBy("lastActiveTime", Query.Direction.DESCENDING)
        
        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(Resource.error("Failed to load chats: ${error.message}"))
                return@addSnapshotListener
            }
            
            if (snapshot != null) {
                val chats = snapshot.documents.mapNotNull { document ->
                    document.toObject(Chat::class.java)?.apply {
                        id = document.id
                    }
                }
                
                // For each chat, get the other user's details
                val enhancedChats = mutableListOf<Chat>()
                
                chats.forEach { chat ->
                    val otherUserId = if (chat.user1Id == currentUserId) chat.user2Id else chat.user1Id
                    
                    try {
                        val userDoc = usersCollection.document(otherUserId!!).get().await()
                        if (userDoc.exists()) {
                            val username = userDoc.getString("username")
                            val profileImageUrl = userDoc.getString("profileImageUrl")
                            val lastActive = userDoc.getDate("lastActive")
                            
                            // Current time minus 5 minutes - if user was active in last 5 mins, consider them online
                            val fiveMinutesAgo = Calendar.getInstance().apply {
                                add(Calendar.MINUTE, -5)
                            }.time
                            
                            val isOnline = lastActive != null && lastActive.after(fiveMinutesAgo)
                            
                            enhancedChats.add(
                                chat.copy(
                                    username = username,
                                    profileImageUrl = profileImageUrl,
                                    isOnline = isOnline
                                )
                            )
                        } else {
                            enhancedChats.add(chat)
                        }
                    } catch (e: Exception) {
                        // If we can't get user details, just add the chat without enhancement
                        enhancedChats.add(chat)
                    }
                }
                
                // Save chats to local database for offline access
                enhancedChats.forEach { chat ->
                    chatDao.insertChat(chat)
                }
                
                trySend(Resource.success(enhancedChats))
            }
        }
        
        awaitClose {
            listener.remove()
        }
    }
    
    override fun getChatById(chatId: String): Flow<Resource<Chat>> = callbackFlow {
        trySend(Resource.Loading)
        
        val currentUserId = getCurrentUserId() ?: run {
            trySend(Resource.error("User not authenticated"))
            awaitClose()
            return@callbackFlow
        }
        
        val listener = chatsCollection.document(chatId).addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(Resource.error("Failed to load chat: ${error.message}"))
                return@addSnapshotListener
            }
            
            if (snapshot != null && snapshot.exists()) {
                val chat = snapshot.toObject(Chat::class.java)?.apply {
                    id = snapshot.id
                }
                
                if (chat != null) {
                    // Get other user's details
                    val otherUserId = if (chat.user1Id == currentUserId) chat.user2Id else chat.user1Id
                    
                    try {
                        val userDoc = usersCollection.document(otherUserId!!).get().await()
                        if (userDoc.exists()) {
                            val username = userDoc.getString("username")
                            val profileImageUrl = userDoc.getString("profileImageUrl")
                            val lastActive = userDoc.getDate("lastActive")
                            
                            // Current time minus 5 minutes
                            val fiveMinutesAgo = Calendar.getInstance().apply {
                                add(Calendar.MINUTE, -5)
                            }.time
                            
                            val isOnline = lastActive != null && lastActive.after(fiveMinutesAgo)
                            
                            val enhancedChat = chat.copy(
                                username = username,
                                profileImageUrl = profileImageUrl,
                                isOnline = isOnline
                            )
                            
                            // Save to local database
                            chatDao.insertChat(enhancedChat)
                            
                            trySend(Resource.success(enhancedChat))
                        } else {
                            // Save to local database
                            chatDao.insertChat(chat)
                            
                            trySend(Resource.success(chat))
                        }
                    } catch (e: Exception) {
                        // If we can't get user details, just return the chat without enhancement
                        // Save to local database
                        chatDao.insertChat(chat)
                        
                        trySend(Resource.success(chat))
                    }
                } else {
                    trySend(Resource.error("Chat not found"))
                }
            } else {
                trySend(Resource.error("Chat not found"))
            }
        }
        
        awaitClose {
            listener.remove()
        }
    }
    
    override fun getChatBetweenUsers(user1Id: String, user2Id: String): Flow<Resource<Chat?>> = callbackFlow {
        trySend(Resource.Loading)
        
        // Try to find a chat where either (user1 = user1Id and user2 = user2Id) or (user1 = user2Id and user2 = user1Id)
        val query = chatsCollection
            .whereArrayContains("participants", user1Id)
            .whereEqualTo("participants", listOf(user1Id, user2Id))
        
        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(Resource.error("Failed to find chat: ${error.message}"))
                return@addSnapshotListener
            }
            
            if (snapshot != null && !snapshot.isEmpty) {
                // Chat exists
                val document = snapshot.documents.first()
                val chat = document.toObject(Chat::class.java)?.apply {
                    id = document.id
                }
                
                if (chat != null) {
                    // Get other user's details (from user2's perspective)
                    try {
                        val userDoc = usersCollection.document(user2Id).get().await()
                        if (userDoc.exists()) {
                            val username = userDoc.getString("username")
                            val profileImageUrl = userDoc.getString("profileImageUrl")
                            val lastActive = userDoc.getDate("lastActive")
                            
                            // Current time minus 5 minutes
                            val fiveMinutesAgo = Calendar.getInstance().apply {
                                add(Calendar.MINUTE, -5)
                            }.time
                            
                            val isOnline = lastActive != null && lastActive.after(fiveMinutesAgo)
                            
                            val enhancedChat = chat.copy(
                                username = username,
                                profileImageUrl = profileImageUrl,
                                isOnline = isOnline
                            )
                            
                            // Save to local database
                            chatDao.insertChat(enhancedChat)
                            
                            trySend(Resource.success(enhancedChat))
                        } else {
                            // Save to local database
                            chatDao.insertChat(chat)
                            
                            trySend(Resource.success(chat))
                        }
                    } catch (e: Exception) {
                        // If we can't get user details, just return the chat without enhancement
                        // Save to local database
                        chatDao.insertChat(chat)
                        
                        trySend(Resource.success(chat))
                    }
                } else {
                    trySend(Resource.success(null))
                }
            } else {
                // No chat found
                trySend(Resource.success(null))
            }
        }
        
        awaitClose {
            listener.remove()
        }
    }
    
    override fun createChat(user1Id: String, user2Id: String): Flow<Resource<Chat>> = flow {
        emit(Resource.Loading)
        
        try {
            // First check if a chat already exists
            val existingChat = getChatBetweenUsers(user1Id, user2Id).collect { resource ->
                if (resource is Resource.Success && resource.data != null) {
                    emit(Resource.success(resource.data))
                    return@collect
                }
            }
            
            // If we got here, we need to create a new chat
            val currentTime = Date()
            val newChat = Chat(
                id = null,
                user1Id = user1Id,
                user2Id = user2Id,
                participants = listOf(user1Id, user2Id),
                lastMessageId = null,
                lastMessageText = null,
                lastActiveTime = currentTime,
                unreadCount = 0,
                isBlocked = false,
                isMuted = false,
                username = null, // Will be set on retrieval
                profileImageUrl = null, // Will be set on retrieval
                isOnline = false // Will be set on retrieval
            )
            
            // Add to Firestore
            val documentRef = chatsCollection.add(newChat).await()
            
            // Get the new chat with ID
            val snapshot = documentRef.get().await()
            val createdChat = snapshot.toObject(Chat::class.java)?.apply {
                id = snapshot.id
            }
            
            if (createdChat != null) {
                // Save to local database
                chatDao.insertChat(createdChat)
                
                emit(Resource.success(createdChat))
            } else {
                emit(Resource.error("Failed to create chat"))
            }
        } catch (e: Exception) {
            emit(Resource.error("Failed to create chat: ${e.message}"))
        }
    }
    
    override fun updateLastMessage(chatId: String, messageId: String, lastMessageText: String): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading)
        
        try {
            val currentTime = Date()
            
            // Update chat in Firestore
            chatsCollection.document(chatId).update(
                mapOf(
                    "lastMessageId" to messageId,
                    "lastMessageText" to lastMessageText,
                    "lastActiveTime" to currentTime,
                    "unreadCount" to 1 // Reset to 1 since this is a new message
                )
            ).await()
            
            // Update local database
            chatDao.updateLastMessage(chatId, messageId, currentTime)
            
            emit(Resource.success(Unit))
        } catch (e: Exception) {
            emit(Resource.error("Failed to update last message: ${e.message}"))
        }
    }
    
    override fun resetUnreadCount(chatId: String): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading)
        
        try {
            // Update chat in Firestore
            chatsCollection.document(chatId).update("unreadCount", 0).await()
            
            // Update local database
            chatDao.resetUnreadCount(chatId)
            
            emit(Resource.success(Unit))
        } catch (e: Exception) {
            emit(Resource.error("Failed to reset unread count: ${e.message}"))
        }
    }
    
    override fun blockChat(chatId: String, isBlocked: Boolean): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading)
        
        try {
            // Update chat in Firestore
            chatsCollection.document(chatId).update("isBlocked", isBlocked).await()
            
            // Update local database
            chatDao.setBlockStatus(chatId, isBlocked)
            
            emit(Resource.success(Unit))
        } catch (e: Exception) {
            emit(Resource.error("Failed to update block status: ${e.message}"))
        }
    }
    
    override fun muteChat(chatId: String, isMuted: Boolean): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading)
        
        try {
            // Update chat in Firestore
            chatsCollection.document(chatId).update("isMuted", isMuted).await()
            
            // Update local database
            chatDao.setMuteStatus(chatId, isMuted)
            
            emit(Resource.success(Unit))
        } catch (e: Exception) {
            emit(Resource.error("Failed to update mute status: ${e.message}"))
        }
    }
    
    override fun getMessagesByChatId(chatId: String): Flow<Resource<List<Message>>> = callbackFlow {
        trySend(Resource.Loading)
        
        val query = messagesCollection
            .whereEqualTo("chatId", chatId)
            .whereEqualTo("isDeleted", false)
            .orderBy("sentAt", Query.Direction.ASCENDING)
        
        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(Resource.error("Failed to load messages: ${error.message}"))
                return@addSnapshotListener
            }
            
            if (snapshot != null) {
                val messages = snapshot.documents.mapNotNull { document ->
                    document.toObject(Message::class.java)?.apply {
                        id = document.id
                    }
                }
                
                // Save messages to local database for offline access
                messages.forEach { message ->
                    messageDao.insertMessage(message)
                }
                
                trySend(Resource.success(messages))
            }
        }
        
        awaitClose {
            listener.remove()
        }
    }
    
    override fun getMessageById(messageId: String): Flow<Resource<Message>> = flow {
        emit(Resource.Loading)
        
        try {
            // Try local database first
            val localMessage = messageDao.getMessageById(messageId).value
            if (localMessage != null) {
                emit(Resource.success(localMessage))
                return@flow
            }
            
            // If not in local database, try Firestore
            val snapshot = messagesCollection.document(messageId).get().await()
            if (snapshot.exists()) {
                val message = snapshot.toObject(Message::class.java)?.apply {
                    id = snapshot.id
                }
                
                if (message != null) {
                    // Save to local database
                    messageDao.insertMessage(message)
                    
                    emit(Resource.success(message))
                } else {
                    emit(Resource.error("Failed to parse message"))
                }
            } else {
                emit(Resource.error("Message not found"))
            }
        } catch (e: Exception) {
            emit(Resource.error("Failed to get message: ${e.message}"))
        }
    }
    
    override fun sendMessage(message: Message): Flow<Resource<Message>> = flow {
        emit(Resource.Loading)
        
        try {
            // Add message to Firestore
            val documentRef = messagesCollection.add(message).await()
            
            // Get the message with ID
            val snapshot = documentRef.get().await()
            val sentMessage = snapshot.toObject(Message::class.java)?.apply {
                id = snapshot.id
            }
            
            if (sentMessage != null) {
                // Save to local database
                messageDao.insertMessage(sentMessage)
                
                // Increment unread count for the chat (this will be done on the receiver's side)
                sentMessage.chatId?.let { chatId ->
                    chatDao.incrementUnreadCount(chatId)
                }
                
                emit(Resource.success(sentMessage))
            } else {
                emit(Resource.error("Failed to send message"))
            }
        } catch (e: Exception) {
            emit(Resource.error("Failed to send message: ${e.message}"))
        }
    }
    
    override fun markMessageAsRead(messageId: String): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading)
        
        try {
            val currentTime = Date()
            
            // Update message in Firestore
            messagesCollection.document(messageId).update("readAt", currentTime).await()
            
            // Update local database
            messageDao.markMessageAsRead(messageId, currentTime)
            
            emit(Resource.success(Unit))
        } catch (e: Exception) {
            emit(Resource.error("Failed to mark message as read: ${e.message}"))
        }
    }
    
    override fun markMessageAsDelivered(messageId: String): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading)
        
        try {
            val currentTime = Date()
            
            // Update message in Firestore
            messagesCollection.document(messageId).update("deliveredAt", currentTime).await()
            
            emit(Resource.success(Unit))
        } catch (e: Exception) {
            emit(Resource.error("Failed to mark message as delivered: ${e.message}"))
        }
    }
    
    override fun deleteMessage(messageId: String): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading)
        
        try {
            // Soft delete - update isDeleted flag
            messagesCollection.document(messageId).update("isDeleted", true).await()
            
            // Update local database
            messageDao.markMessageAsDeleted(messageId)
            
            emit(Resource.success(Unit))
        } catch (e: Exception) {
            emit(Resource.error("Failed to delete message: ${e.message}"))
        }
    }
    
    override fun getUnreadMessagesCount(): Flow<Resource<Int>> = flow {
        emit(Resource.Loading)
        
        val currentUserId = getCurrentUserId() ?: run {
            emit(Resource.error("User not authenticated"))
            return@flow
        }
        
        try {
            // Get unread messages count from local database
            val count = messageDao.getUnreadMessagesCount(currentUserId).value
            emit(Resource.success(count))
        } catch (e: Exception) {
            emit(Resource.error("Failed to get unread messages count: ${e.message}"))
        }
    }
    
    private fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }
}