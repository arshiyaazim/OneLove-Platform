package com.kilagee.onelove.data.database.dao

import androidx.room.*
import com.kilagee.onelove.data.model.Message
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface MessageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: Message)
    
    @Update
    suspend fun updateMessage(message: Message)
    
    @Delete
    suspend fun deleteMessage(message: Message)
    
    @Query("SELECT * FROM messages WHERE id = :messageId")
    fun getMessageById(messageId: String): Flow<Message?>
    
    @Query("SELECT * FROM messages WHERE chat_id = :chatId ORDER BY sent_at ASC")
    fun getMessagesByChatId(chatId: String): Flow<List<Message>>
    
    @Query("SELECT * FROM messages WHERE sender_id = :senderId AND receiver_id = :receiverId OR sender_id = :receiverId AND receiver_id = :senderId ORDER BY sent_at ASC")
    fun getMessagesBetweenUsers(senderId: String, receiverId: String): Flow<List<Message>>
    
    @Query("UPDATE messages SET is_deleted = 1 WHERE id = :messageId")
    suspend fun markMessageAsDeleted(messageId: String)
    
    @Query("UPDATE messages SET read_at = :readTime WHERE id = :messageId AND read_at IS NULL")
    suspend fun markMessageAsRead(messageId: String, readTime: Date)
    
    @Query("SELECT COUNT(*) FROM messages WHERE receiver_id = :userId AND read_at IS NULL")
    fun getUnreadMessagesCount(userId: String): Flow<Int>
    
    @Query("SELECT * FROM messages WHERE receiver_id = :userId AND read_at IS NULL ORDER BY sent_at DESC")
    fun getUnreadMessages(userId: String): Flow<List<Message>>
}