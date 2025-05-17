package com.kilagee.onelove.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.kilagee.onelove.data.local.MatchDao
import com.kilagee.onelove.data.local.MessageDao
import com.kilagee.onelove.data.model.Match
import com.kilagee.onelove.data.model.MatchStatus
import com.kilagee.onelove.data.model.Message
import com.kilagee.onelove.data.model.MessageReaction
import com.kilagee.onelove.domain.repository.ChatRepository
import com.kilagee.onelove.domain.util.Result
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.util.Date
import javax.inject.Inject

/**
 * Chat repository implementation
 */
class ChatRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val messageDao: MessageDao,
    private val matchDao: MatchDao
) : ChatRepository {

    private val matchesCollection = firestore.collection("matches")
    private val messagesCollection = firestore.collection("messages")
    
    /**
     * Get chats (matches with messages) for current user
     */
    override fun getChats(userId: String): Flow<Result<List<Match>>> = callbackFlow {
        trySend(Result.Loading)
        
        try {
            // Try to get cached matches first
            val cachedMatches = matchDao.getActiveMatchesForUser(userId)
            if (cachedMatches.isNotEmpty()) {
                trySend(Result.Success(cachedMatches))
            }
            
            // Get matches where user is either the initiator or the matched
            val query = matchesCollection
                .whereEqualTo("status", MatchStatus.ACTIVE.name)
                .where(
                    com.google.firebase.firestore.Filter.or(
                        com.google.firebase.firestore.Filter.equalTo("userId", userId),
                        com.google.firebase.firestore.Filter.equalTo("matchedUserId", userId)
                    )
                )
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                
            val listener = query.addSnapshotListener { snapshot, exception ->
                if (exception != null) {
                    trySend(Result.Error("Failed to listen for chats: ${exception.message}"))
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    val matches = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Match::class.java)?.copy(id = doc.id)
                    }
                    
                    // Cache matches locally
                    matchDao.insertMatches(matches)
                    
                    trySend(Result.Success(matches))
                }
            }
            
            awaitClose { listener.remove() }
        } catch (e: Exception) {
            Timber.e(e, "Error getting chats")
            trySend(Result.Error("Failed to get chats: ${e.message}"))
            close()
        }
    }.catch { e ->
        Timber.e(e, "Exception in getChats flow")
        emit(Result.Error("Exception: ${e.message}"))
    }
    
    /**
     * Get match by ID
     */
    override fun getMatchById(matchId: String): Flow<Result<Match>> = callbackFlow {
        trySend(Result.Loading)
        
        try {
            // Try to get cached match first
            val cachedMatch = matchDao.getMatchByIdFlow(matchId)
            cachedMatch.collect { match ->
                if (match != null) {
                    trySend(Result.Success(match))
                }
            }
            
            val listener = matchesCollection.document(matchId)
                .addSnapshotListener { snapshot, exception ->
                    if (exception != null) {
                        trySend(Result.Error("Failed to listen for match: ${exception.message}"))
                        return@addSnapshotListener
                    }
                    
                    if (snapshot != null && snapshot.exists()) {
                        val match = snapshot.toObject(Match::class.java)?.copy(id = snapshot.id)
                        if (match != null) {
                            // Cache match locally
                            matchDao.insertMatch(match)
                            
                            trySend(Result.Success(match))
                        } else {
                            trySend(Result.Error("Failed to parse match data"))
                        }
                    } else {
                        trySend(Result.Error("Match not found"))
                    }
                }
            
            awaitClose { listener.remove() }
        } catch (e: Exception) {
            Timber.e(e, "Error getting match")
            trySend(Result.Error("Failed to get match: ${e.message}"))
            close()
        }
    }.catch { e ->
        Timber.e(e, "Exception in getMatchById flow")
        emit(Result.Error("Exception: ${e.message}"))
    }
    
    /**
     * Send a message
     */
    override suspend fun sendMessage(message: Message): Result<Message> {
        return try {
            // Save message to Firestore
            messagesCollection.document(message.id).set(message).await()
            
            // Update match with last message
            val matchUpdate = hashMapOf<String, Any>(
                "lastMessageText" to message.text,
                "lastMessageTimestamp" to message.timestamp,
                "updatedAt" to Date()
            )
            
            // Increment unread count for the receiver
            val match = matchDao.getMatchById(message.matchId)
            if (match != null) {
                // Only increment if the message is to the other user
                if (message.receiverId != message.senderId) {
                    matchUpdate["unreadCount"] = (match.unreadCount ?: 0) + 1
                    matchDao.incrementUnreadCount(message.matchId)
                }
            }
            
            matchesCollection.document(message.matchId).update(matchUpdate).await()
            
            // Cache message locally
            messageDao.insertMessage(message)
            
            Result.Success(message)
        } catch (e: Exception) {
            Timber.e(e, "Error sending message")
            Result.Error("Failed to send message: ${e.message}")
        }
    }
    
    /**
     * Get messages for a match
     */
    override fun getMessages(matchId: String): Flow<Result<List<Message>>> = callbackFlow {
        trySend(Result.Loading)
        
        try {
            // Try to get cached messages first
            val cachedMessages = messageDao.getMessagesByMatchId(matchId)
            if (cachedMessages.isNotEmpty()) {
                trySend(Result.Success(cachedMessages))
            }
            
            // Get messages from Firestore
            val query = messagesCollection
                .whereEqualTo("matchId", matchId)
                .whereEqualTo("isDeleted", false)
                .orderBy("timestamp", Query.Direction.ASCENDING)
                
            val listener = query.addSnapshotListener { snapshot, exception ->
                if (exception != null) {
                    trySend(Result.Error("Failed to listen for messages: ${exception.message}"))
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    val messages = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Message::class.java)?.copy(id = doc.id)
                    }
                    
                    // Cache messages locally
                    messageDao.insertMessages(messages)
                    
                    trySend(Result.Success(messages))
                }
            }
            
            awaitClose { listener.remove() }
        } catch (e: Exception) {
            Timber.e(e, "Error getting messages")
            trySend(Result.Error("Failed to get messages: ${e.message}"))
            close()
        }
    }.catch { e ->
        Timber.e(e, "Exception in getMessages flow")
        emit(Result.Error("Exception: ${e.message}"))
    }
    
    /**
     * Mark message as read
     */
    override suspend fun markMessageAsRead(messageId: String): Result<Unit> {
        return try {
            messagesCollection.document(messageId).update("isRead", true).await()
            
            // Update local cache
            val cachedMessage = messageDao.getMessageById(messageId)
            if (cachedMessage != null) {
                messageDao.updateMessage(cachedMessage.copy(isRead = true))
            }
            
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error marking message as read")
            Result.Error("Failed to mark message as read: ${e.message}")
        }
    }
    
    /**
     * Mark all messages in a match as read
     */
    override suspend fun markAllMessagesAsRead(matchId: String, userId: String): Result<Unit> {
        return try {
            // Get all unread messages sent to this user in this match
            val query = messagesCollection
                .whereEqualTo("matchId", matchId)
                .whereEqualTo("receiverId", userId)
                .whereEqualTo("isRead", false)
                .get()
                .await()
            
            // Batch update
            val batch = firestore.batch()
            query.documents.forEach { doc ->
                batch.update(doc.reference, "isRead", true)
            }
            batch.commit().await()
            
            // Update match unread count
            matchesCollection.document(matchId).update("unreadCount", 0).await()
            
            // Update local cache
            matchDao.resetUnreadCount(matchId)
            messageDao.markAllMessagesAsRead(matchId, userId)
            
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error marking all messages as read")
            Result.Error("Failed to mark all messages as read: ${e.message}")
        }
    }
    
    /**
     * Delete a message
     */
    override suspend fun deleteMessage(messageId: String): Result<Unit> {
        return try {
            // We don't actually delete, just mark as deleted
            messagesCollection.document(messageId).update("isDeleted", true).await()
            
            // Update local cache
            val cachedMessage = messageDao.getMessageById(messageId)
            if (cachedMessage != null) {
                messageDao.updateMessage(cachedMessage.copy(isDeleted = true))
            }
            
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error deleting message")
            Result.Error("Failed to delete message: ${e.message}")
        }
    }
    
    /**
     * Add reaction to a message
     */
    override suspend fun addReactionToMessage(messageId: String, reaction: MessageReaction): Result<Unit> {
        return try {
            messagesCollection.document(messageId).update("reaction", reaction.name).await()
            
            // Update local cache
            val cachedMessage = messageDao.getMessageById(messageId)
            if (cachedMessage != null) {
                messageDao.updateMessage(cachedMessage.copy(reaction = reaction))
            }
            
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error adding reaction to message")
            Result.Error("Failed to add reaction: ${e.message}")
        }
    }
    
    /**
     * Remove reaction from a message
     */
    override suspend fun removeReactionFromMessage(messageId: String): Result<Unit> {
        return try {
            messagesCollection.document(messageId).update("reaction", null).await()
            
            // Update local cache
            val cachedMessage = messageDao.getMessageById(messageId)
            if (cachedMessage != null) {
                messageDao.updateMessage(cachedMessage.copy(reaction = null))
            }
            
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error removing reaction from message")
            Result.Error("Failed to remove reaction: ${e.message}")
        }
    }
}