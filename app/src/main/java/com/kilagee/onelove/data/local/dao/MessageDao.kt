package com.kilagee.onelove.data.local.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.kilagee.onelove.data.model.Message
import com.kilagee.onelove.data.model.MessageStatus
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Message-related operations
 */
@Dao
interface MessageDao {
    
    @Query("SELECT * FROM messages WHERE id = :messageId")
    fun getMessageById(messageId: String): Flow<Message?>
    
    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY createdAt DESC LIMIT 1")
    fun getLatestMessage(chatId: String): Flow<Message?>
    
    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY createdAt DESC")
    fun getPagingMessages(chatId: String): PagingSource<Int, Message>
    
    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY createdAt DESC LIMIT :limit")
    fun getRecentMessages(chatId: String, limit: Int): Flow<List<Message>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: Message)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<Message>)
    
    @Update
    suspend fun updateMessage(message: Message)
    
    @Query("UPDATE messages SET status = :status WHERE id = :messageId")
    suspend fun updateMessageStatus(messageId: String, status: MessageStatus)
    
    @Query("DELETE FROM messages WHERE id = :messageId")
    suspend fun deleteMessageById(messageId: String)
    
    @Query("DELETE FROM messages WHERE chatId = :chatId")
    suspend fun deleteMessagesByChatId(chatId: String)
    
    @Query("SELECT * FROM messages WHERE chatId = :chatId AND content LIKE '%' || :query || '%' ORDER BY createdAt DESC")
    fun searchMessages(chatId: String, query: String): Flow<List<Message>>
    
    @Query("""
        UPDATE messages 
        SET reactions = json_set(
            CASE WHEN reactions IS NULL THEN '{}' ELSE reactions END, 
            '$.' || :userId, 
            :reaction
        ) 
        WHERE id = :messageId
    """)
    suspend fun addReaction(messageId: String, userId: String, reaction: String)
    
    @Query("""
        UPDATE messages 
        SET reactions = json_remove(reactions, '$.' || :userId) 
        WHERE id = :messageId
    """)
    suspend fun removeReaction(messageId: String, userId: String)
    
    @Transaction
    @Query("""
        SELECT m.* FROM messages m
        JOIN chats c ON m.chatId = c.id
        WHERE c.id IN (SELECT id FROM chats WHERE :userId IN (participantIds))
        AND m.content LIKE '%' || :query || '%'
        ORDER BY m.createdAt DESC
        LIMIT :limit
    """)
    fun searchMessagesAcrossChats(userId: String, query: String, limit: Int): Flow<List<Message>>
}