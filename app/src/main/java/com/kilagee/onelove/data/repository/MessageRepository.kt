package com.kilagee.onelove.data.repository

import android.net.Uri
import com.kilagee.onelove.data.model.Message
import com.kilagee.onelove.data.model.MessageContent
import com.kilagee.onelove.data.model.Result
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for message operations
 */
interface MessageRepository {
    /**
     * Get message by ID
     */
    suspend fun getMessageById(messageId: String): Result<Message>
    
    /**
     * Get messages for chat
     */
    suspend fun getMessagesForChat(chatId: String, limit: Int = 50): Result<List<Message>>
    
    /**
     * Get messages for chat as Flow
     */
    fun getMessagesForChatFlow(chatId: String): Flow<List<Message>>
    
    /**
     * Send text message
     */
    suspend fun sendTextMessage(
        chatId: String,
        senderId: String,
        receiverId: String,
        content: String,
        replyToMessageId: String? = null
    ): Result<Message>
    
    /**
     * Send message with typed content
     */
    suspend fun sendMessage(
        chatId: String,
        senderId: String,
        receiverId: String,
        content: MessageContent,
        replyToMessageId: String? = null
    ): Result<Message>
    
    /**
     * Send image message
     */
    suspend fun sendImageMessage(
        chatId: String,
        senderId: String,
        receiverId: String,
        imageUri: Uri,
        caption: String? = null,
        replyToMessageId: String? = null,
        onProgress: ((Float) -> Unit)? = null
    ): Result<Message>
    
    /**
     * Send video message
     */
    suspend fun sendVideoMessage(
        chatId: String,
        senderId: String,
        receiverId: String,
        videoUri: Uri,
        caption: String? = null,
        replyToMessageId: String? = null,
        onProgress: ((Float) -> Unit)? = null
    ): Result<Message>
    
    /**
     * Send audio message
     */
    suspend fun sendAudioMessage(
        chatId: String,
        senderId: String,
        receiverId: String,
        audioUri: Uri,
        caption: String? = null,
        replyToMessageId: String? = null,
        onProgress: ((Float) -> Unit)? = null
    ): Result<Message>
    
    /**
     * Send file message
     */
    suspend fun sendFileMessage(
        chatId: String,
        senderId: String,
        receiverId: String,
        fileUri: Uri,
        fileName: String,
        fileType: String,
        replyToMessageId: String? = null,
        onProgress: ((Float) -> Unit)? = null
    ): Result<Message>
    
    /**
     * Send location message
     */
    suspend fun sendLocationMessage(
        chatId: String,
        senderId: String,
        receiverId: String,
        latitude: Double,
        longitude: Double,
        address: String? = null,
        name: String? = null,
        replyToMessageId: String? = null
    ): Result<Message>
    
    /**
     * Mark message as read
     */
    suspend fun markMessageAsRead(messageId: String): Result<Unit>
    
    /**
     * Mark all messages in chat as read
     */
    suspend fun markChatMessagesAsRead(chatId: String, userId: String): Result<Unit>
    
    /**
     * Delete message
     */
    suspend fun deleteMessage(messageId: String): Result<Unit>
    
    /**
     * Delete messages in chat
     */
    suspend fun deleteMessagesInChat(chatId: String): Result<Unit>
    
    /**
     * Get unread message count for user
     */
    suspend fun getUnreadMessageCount(userId: String): Result<Int>
    
    /**
     * Get unread message count for chat
     */
    suspend fun getUnreadMessageCountForChat(chatId: String, userId: String): Result<Int>
    
    /**
     * Get unread messages for user
     */
    suspend fun getUnreadMessagesForUser(userId: String): Result<List<Message>>
    
    /**
     * Send AI-generated message (for chat suggestions)
     */
    suspend fun sendAIMessage(
        chatId: String,
        aiProfileId: String,
        receiverId: String,
        content: String,
        replyToMessageId: String? = null
    ): Result<Message>
    
    /**
     * Generate AI message suggestions based on chat context
     */
    suspend fun generateAIMessageSuggestions(
        chatId: String,
        userId: String,
        aiProfileId: String? = null,
        contextMessageCount: Int = 5
    ): Result<List<String>>
    
    /**
     * Add reaction to message
     */
    suspend fun addReactionToMessage(
        messageId: String,
        userId: String,
        reaction: String
    ): Result<Unit>
    
    /**
     * Remove reaction from message
     */
    suspend fun removeReactionFromMessage(
        messageId: String,
        userId: String
    ): Result<Unit>
    
    /**
     * Forward message to another chat
     */
    suspend fun forwardMessage(
        messageId: String,
        targetChatId: String,
        targetReceiverId: String
    ): Result<Message>
    
    /**
     * Get message translations
     */
    suspend fun getMessageTranslation(
        messageId: String,
        targetLanguage: String
    ): Result<String>
    
    /**
     * Pin message in chat
     */
    suspend fun pinMessage(messageId: String): Result<Unit>
    
    /**
     * Unpin message in chat
     */
    suspend fun unpinMessage(messageId: String): Result<Unit>
    
    /**
     * Get pinned messages in chat
     */
    suspend fun getPinnedMessages(chatId: String): Result<List<Message>>
}