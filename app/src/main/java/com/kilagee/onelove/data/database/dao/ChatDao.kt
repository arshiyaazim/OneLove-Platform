package com.kilagee.onelove.data.database.dao

import androidx.room.*
import com.kilagee.onelove.data.model.Chat
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface ChatDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChat(chat: Chat)
    
    @Update
    suspend fun updateChat(chat: Chat)
    
    @Delete
    suspend fun deleteChat(chat: Chat)
    
    @Query("SELECT * FROM chats WHERE id = :chatId")
    fun getChatById(chatId: String): Flow<Chat?>
    
    @Query("SELECT * FROM chats WHERE user1_id = :userId OR user2_id = :userId ORDER BY last_active_time DESC")
    fun getChatsByUserId(userId: String): Flow<List<Chat>>
    
    @Query("SELECT * FROM chats WHERE (user1_id = :user1Id AND user2_id = :user2Id) OR (user1_id = :user2Id AND user2_id = :user1Id) LIMIT 1")
    fun getChatBetweenUsers(user1Id: String, user2Id: String): Flow<Chat?>
    
    @Query("UPDATE chats SET last_message_id = :messageId, last_active_time = :lastActiveTime WHERE id = :chatId")
    suspend fun updateLastMessage(chatId: String, messageId: String, lastActiveTime: Date)
    
    @Query("UPDATE chats SET unread_count = unread_count + 1 WHERE id = :chatId")
    suspend fun incrementUnreadCount(chatId: String)
    
    @Query("UPDATE chats SET unread_count = 0 WHERE id = :chatId")
    suspend fun resetUnreadCount(chatId: String)
    
    @Query("UPDATE chats SET is_blocked = :isBlocked WHERE id = :chatId")
    suspend fun setBlockStatus(chatId: String, isBlocked: Boolean)
    
    @Query("UPDATE chats SET is_muted = :isMuted WHERE id = :chatId")
    suspend fun setMuteStatus(chatId: String, isMuted: Boolean)
}