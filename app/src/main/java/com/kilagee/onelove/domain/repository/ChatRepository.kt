package com.kilagee.onelove.domain.repository

import com.kilagee.onelove.data.model.Match
import com.kilagee.onelove.data.model.Message
import com.kilagee.onelove.data.model.MessageReaction
import com.kilagee.onelove.domain.util.Result
import kotlinx.coroutines.flow.Flow

/**
 * Chat repository interface
 */
interface ChatRepository {
    
    /**
     * Get chats (matches with messages) for current user
     */
    fun getChats(userId: String): Flow<Result<List<Match>>>
    
    /**
     * Get match by ID
     */
    fun getMatchById(matchId: String): Flow<Result<Match>>
    
    /**
     * Send a message
     */
    suspend fun sendMessage(message: Message): Result<Message>
    
    /**
     * Get messages for a match
     */
    fun getMessages(matchId: String): Flow<Result<List<Message>>>
    
    /**
     * Mark message as read
     */
    suspend fun markMessageAsRead(messageId: String): Result<Unit>
    
    /**
     * Mark all messages in a match as read
     */
    suspend fun markAllMessagesAsRead(matchId: String, userId: String): Result<Unit>
    
    /**
     * Delete a message
     */
    suspend fun deleteMessage(messageId: String): Result<Unit>
    
    /**
     * Add reaction to a message
     */
    suspend fun addReactionToMessage(messageId: String, reaction: MessageReaction): Result<Unit>
    
    /**
     * Remove reaction from a message
     */
    suspend fun removeReactionFromMessage(messageId: String): Result<Unit>
}