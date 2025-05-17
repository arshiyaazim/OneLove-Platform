package com.kilagee.onelove.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.kilagee.onelove.data.model.Chat
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Chat-related operations
 */
@Dao
interface ChatDao {
    
    @Query("SELECT * FROM chats WHERE id = :chatId")
    fun getChatById(chatId: String): Flow<Chat?>
    
    @Query("SELECT * FROM chats WHERE :userId IN (participantIds) ORDER BY updatedAt DESC")
    fun getChatsForUser(userId: String): Flow<List<Chat>>
    
    @Query("SELECT * FROM chats WHERE :userId IN (participantIds) AND isPinned = 1 ORDER BY updatedAt DESC")
    fun getPinnedChatsForUser(userId: String): Flow<List<Chat>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChat(chat: Chat)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChats(chats: List<Chat>)
    
    @Update
    suspend fun updateChat(chat: Chat)
    
    @Delete
    suspend fun deleteChat(chat: Chat)
    
    @Query("DELETE FROM chats WHERE id = :chatId")
    suspend fun deleteChatById(chatId: String)
    
    @Query("DELETE FROM chats")
    suspend fun deleteAllChats()
    
    @Query("""
        UPDATE chats 
        SET unreadCounts = json_set(unreadCounts, '$.' || :userId, 0) 
        WHERE id = :chatId
    """)
    suspend fun markChatAsRead(chatId: String, userId: String)
    
    @Query("""
        UPDATE chats 
        SET unreadCounts = json_set(unreadCounts, '$.' || :userId, 
            CASE 
                WHEN json_extract(unreadCounts, '$.' || :userId) IS NULL THEN 1
                ELSE json_extract(unreadCounts, '$.' || :userId) + 1
            END
        ) 
        WHERE id = :chatId
    """)
    suspend fun incrementUnreadCount(chatId: String, userId: String)
    
    @Query("""
        UPDATE chats 
        SET typingStatus = json_set(typingStatus, '$.' || :userId, :isTyping) 
        WHERE id = :chatId
    """)
    suspend fun updateTypingStatus(chatId: String, userId: String, isTyping: Boolean)
    
    @Query("""
        SELECT SUM(CASE WHEN json_extract(unreadCounts, '$.' || :userId) IS NULL 
                  THEN 0 
                  ELSE json_extract(unreadCounts, '$.' || :userId) 
                  END) 
        FROM chats 
        WHERE :userId IN (participantIds)
    """)
    fun getTotalUnreadMessageCount(userId: String): Flow<Int>
    
    @Transaction
    @Query("""
        SELECT * FROM chats c
        WHERE :userId IN (participantIds)
        AND EXISTS (
            SELECT 1 FROM messages m
            WHERE m.chatId = c.id
            AND m.content LIKE '%' || :query || '%'
        )
        ORDER BY c.updatedAt DESC
    """)
    fun searchChats(userId: String, query: String): Flow<List<Chat>>
    
    @Query("UPDATE chats SET isPinned = :isPinned WHERE id = :chatId")
    suspend fun updatePinStatus(chatId: String, isPinned: Boolean)
    
    @Query("UPDATE chats SET isMuted = :isMuted WHERE id = :chatId")
    suspend fun updateMuteStatus(chatId: String, isMuted: Boolean)
}