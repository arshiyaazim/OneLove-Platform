package com.kilagee.onelove.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.kilagee.onelove.data.model.Message
import kotlinx.coroutines.flow.Flow

/**
 * Data access object for Message
 */
@Dao
interface MessageDao {
    
    /**
     * Insert a message
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: Message)
    
    /**
     * Insert multiple messages
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<Message>)
    
    /**
     * Update a message
     */
    @Update
    suspend fun updateMessage(message: Message)
    
    /**
     * Get a message by ID
     */
    @Query("SELECT * FROM messages WHERE id = :messageId")
    suspend fun getMessageById(messageId: String): Message?
    
    /**
     * Get messages for a match
     */
    @Query("SELECT * FROM messages WHERE matchId = :matchId AND isDeleted = 0 ORDER BY timestamp ASC")
    suspend fun getMessagesByMatchId(matchId: String): List<Message>
    
    /**
     * Get messages for a match as Flow
     */
    @Query("SELECT * FROM messages WHERE matchId = :matchId AND isDeleted = 0 ORDER BY timestamp ASC")
    fun getMessagesByMatchIdFlow(matchId: String): Flow<List<Message>>
    
    /**
     * Get unread messages for a user in a match
     */
    @Query("SELECT * FROM messages WHERE matchId = :matchId AND receiverId = :userId AND isRead = 0 AND isDeleted = 0")
    suspend fun getUnreadMessagesForUser(matchId: String, userId: String): List<Message>
    
    /**
     * Get unread message count for a user in a match
     */
    @Query("SELECT COUNT(*) FROM messages WHERE matchId = :matchId AND receiverId = :userId AND isRead = 0 AND isDeleted = 0")
    suspend fun getUnreadMessageCount(matchId: String, userId: String): Int
    
    /**
     * Get unread message count for a user across all matches
     */
    @Query("SELECT COUNT(*) FROM messages WHERE receiverId = :userId AND isRead = 0 AND isDeleted = 0")
    suspend fun getTotalUnreadMessageCount(userId: String): Int
    
    /**
     * Get total unread message count as Flow
     */
    @Query("SELECT COUNT(*) FROM messages WHERE receiverId = :userId AND isRead = 0 AND isDeleted = 0")
    fun getTotalUnreadMessageCountFlow(userId: String): Flow<Int>
    
    /**
     * Mark all messages in a match as read for a user
     */
    @Query("UPDATE messages SET isRead = 1 WHERE matchId = :matchId AND receiverId = :userId")
    suspend fun markAllMessagesAsRead(matchId: String, userId: String)
    
    /**
     * Delete messages for a match
     */
    @Query("DELETE FROM messages WHERE matchId = :matchId")
    suspend fun deleteMessagesForMatch(matchId: String)
    
    /**
     * Delete all messages
     */
    @Query("DELETE FROM messages")
    suspend fun deleteAllMessages()
}