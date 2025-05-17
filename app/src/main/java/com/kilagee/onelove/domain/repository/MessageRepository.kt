package com.kilagee.onelove.domain.repository

import com.kilagee.onelove.data.model.LocationData
import com.kilagee.onelove.data.model.Message
import com.kilagee.onelove.domain.util.Result
import kotlinx.coroutines.flow.Flow
import java.io.File

/**
 * Repository interface for message operations
 */
interface MessageRepository {
    
    /**
     * Send a text message
     */
    suspend fun sendTextMessage(
        matchId: String,
        senderId: String,
        receiverId: String,
        content: String,
        replyToMessageId: String = ""
    ): Result<Message>
    
    /**
     * Send an image message
     */
    suspend fun sendImageMessage(
        matchId: String,
        senderId: String,
        receiverId: String,
        imageFile: File,
        caption: String = ""
    ): Result<Message>
    
    /**
     * Send a video message
     */
    suspend fun sendVideoMessage(
        matchId: String,
        senderId: String,
        receiverId: String,
        videoFile: File,
        caption: String = ""
    ): Result<Message>
    
    /**
     * Send an audio message
     */
    suspend fun sendAudioMessage(
        matchId: String,
        senderId: String,
        receiverId: String,
        audioFile: File,
        caption: String = ""
    ): Result<Message>
    
    /**
     * Send a location message
     */
    suspend fun sendLocationMessage(
        matchId: String,
        senderId: String,
        receiverId: String,
        locationData: LocationData,
        caption: String = ""
    ): Result<Message>
    
    /**
     * Get messages for a match
     */
    suspend fun getMessages(matchId: String, limit: Int = 50): Result<List<Message>>
    
    /**
     * Get messages for a match as a flow for real-time updates
     */
    fun getMessagesFlow(matchId: String, limit: Int = 50): Flow<Result<List<Message>>>
    
    /**
     * Mark messages as read
     */
    suspend fun markMessagesAsRead(matchId: String, userId: String): Result<Unit>
    
    /**
     * Delete a message
     */
    suspend fun deleteMessage(messageId: String, userId: String): Result<Unit>
    
    /**
     * Add a reaction to a message
     */
    suspend fun addReaction(messageId: String, userId: String, reaction: String): Result<Unit>
    
    /**
     * Remove a reaction from a message
     */
    suspend fun removeReaction(messageId: String, userId: String): Result<Unit>
    
    /**
     * Get unread message count
     */
    suspend fun getUnreadMessageCount(userId: String): Result<Int>
    
    /**
     * Get unread message count as a flow
     */
    fun getUnreadMessageCountFlow(userId: String): Flow<Result<Int>>
    
    /**
     * Get a specific message by ID
     */
    suspend fun getMessageById(messageId: String): Result<Message>
}