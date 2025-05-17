package com.kilagee.onelove.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kilagee.onelove.data.model.Message
import com.kilagee.onelove.data.model.MessageContent
import com.kilagee.onelove.data.model.Result
import com.kilagee.onelove.data.repository.AuthRepository
import com.kilagee.onelove.data.repository.MessageRepository
import com.kilagee.onelove.data.repository.UserRepository
import com.kilagee.onelove.util.AppError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Sealed class representing message UI state
 */
sealed class MessageUiState {
    object Initial : MessageUiState()
    object Loading : MessageUiState()
    data class Error(val error: AppError, val message: String) : MessageUiState()
    data class Success<T>(val data: T) : MessageUiState()
}

/**
 * Message operation types
 */
enum class MessageOperation {
    NONE,
    SEND_MESSAGE,
    SEND_MEDIA,
    MARK_READ,
    DELETE_MESSAGE,
    ADD_REACTION,
    FORWARD_MESSAGE,
    TRANSLATE_MESSAGE,
    PIN_MESSAGE,
    GENERATE_AI_SUGGESTIONS
}

/**
 * ViewModel for message operations
 */
@HiltViewModel
class MessageViewModel @Inject constructor(
    private val messageRepository: MessageRepository,
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) : ViewModel() {
    
    // Current message UI state
    private val _messageUiState = MutableStateFlow<MessageUiState>(MessageUiState.Initial)
    val messageUiState: StateFlow<MessageUiState> = _messageUiState.asStateFlow()
    
    // Current message operation
    private val _currentOperation = MutableStateFlow(MessageOperation.NONE)
    val currentOperation: StateFlow<MessageOperation> = _currentOperation.asStateFlow()
    
    // Current chat ID
    private val _currentChatId = MutableStateFlow<String?>(null)
    val currentChatId: StateFlow<String?> = _currentChatId.asStateFlow()
    
    // Current receiver ID
    private val _currentReceiverId = MutableStateFlow<String?>(null)
    val currentReceiverId: StateFlow<String?> = _currentReceiverId.asStateFlow()
    
    // Messages for current chat
    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()
    
    // Media upload progress
    private val _uploadProgress = MutableStateFlow(0f)
    val uploadProgress: StateFlow<Float> = _uploadProgress.asStateFlow()
    
    // Unread message count
    private val _unreadMessageCount = MutableStateFlow(0)
    val unreadMessageCount: StateFlow<Int> = _unreadMessageCount.asStateFlow()
    
    // AI message suggestions
    private val _aiSuggestions = MutableStateFlow<List<String>>(emptyList())
    val aiSuggestions: StateFlow<List<String>> = _aiSuggestions.asStateFlow()
    
    // Pinned messages
    private val _pinnedMessages = MutableStateFlow<List<Message>>(emptyList())
    val pinnedMessages: StateFlow<List<Message>> = _pinnedMessages.asStateFlow()
    
    /**
     * Set current chat
     */
    fun setCurrentChat(chatId: String, receiverId: String) {
        _currentChatId.value = chatId
        _currentReceiverId.value = receiverId
        
        // Load messages for the chat
        loadMessagesForChat(chatId)
        
        // Load pinned messages
        loadPinnedMessages(chatId)
        
        // Mark messages as read
        val userId = authRepository.getCurrentUserId()
        if (userId != null) {
            markChatAsRead(chatId, userId)
        }
    }
    
    /**
     * Load messages for chat
     */
    private fun loadMessagesForChat(chatId: String) {
        viewModelScope.launch {
            _currentOperation.value = MessageOperation.NONE
            _messageUiState.value = MessageUiState.Loading
            
            // Subscribe to message updates
            messageRepository.getMessagesForChatFlow(chatId)
                .catch { e ->
                    Timber.e(e, "Error getting messages for chat")
                    _messageUiState.value = MessageUiState.Error(
                        AppError.DataError.FetchFailed("Failed to load messages: ${e.message}", e),
                        "Failed to load messages"
                    )
                }
                .collect { messageList ->
                    _messages.value = messageList
                    _messageUiState.value = MessageUiState.Success(messageList)
                }
        }
    }
    
    /**
     * Load pinned messages
     */
    private fun loadPinnedMessages(chatId: String) {
        viewModelScope.launch {
            when (val result = messageRepository.getPinnedMessages(chatId)) {
                is Result.Success -> {
                    _pinnedMessages.value = result.data
                }
                is Result.Error -> {
                    Timber.e(result.error.throwable, "Error loading pinned messages")
                }
            }
        }
    }
    
    /**
     * Send text message
     */
    fun sendTextMessage(content: String, replyToMessageId: String? = null) {
        viewModelScope.launch {
            _currentOperation.value = MessageOperation.SEND_MESSAGE
            _messageUiState.value = MessageUiState.Loading
            
            val chatId = _currentChatId.value ?: return@launch
            val receiverId = _currentReceiverId.value ?: return@launch
            val senderId = authRepository.getCurrentUserId() ?: return@launch
            
            when (val result = messageRepository.sendTextMessage(chatId, senderId, receiverId, content, replyToMessageId)) {
                is Result.Success -> {
                    // Message will be updated through the flow
                    _messageUiState.value = MessageUiState.Success(result.data)
                }
                is Result.Error -> {
                    _messageUiState.value = MessageUiState.Error(result.error, "Failed to send message")
                }
            }
            
            _currentOperation.value = MessageOperation.NONE
        }
    }
    
    /**
     * Send message with typed content
     */
    fun sendMessage(content: MessageContent, replyToMessageId: String? = null) {
        viewModelScope.launch {
            _currentOperation.value = MessageOperation.SEND_MESSAGE
            _messageUiState.value = MessageUiState.Loading
            
            val chatId = _currentChatId.value ?: return@launch
            val receiverId = _currentReceiverId.value ?: return@launch
            val senderId = authRepository.getCurrentUserId() ?: return@launch
            
            when (val result = messageRepository.sendMessage(chatId, senderId, receiverId, content, replyToMessageId)) {
                is Result.Success -> {
                    // Message will be updated through the flow
                    _messageUiState.value = MessageUiState.Success(result.data)
                }
                is Result.Error -> {
                    _messageUiState.value = MessageUiState.Error(result.error, "Failed to send message")
                }
            }
            
            _currentOperation.value = MessageOperation.NONE
        }
    }
    
    /**
     * Send image message
     */
    fun sendImageMessage(imageUri: Uri, caption: String? = null, replyToMessageId: String? = null) {
        viewModelScope.launch {
            _currentOperation.value = MessageOperation.SEND_MEDIA
            _messageUiState.value = MessageUiState.Loading
            _uploadProgress.value = 0f
            
            val chatId = _currentChatId.value ?: return@launch
            val receiverId = _currentReceiverId.value ?: return@launch
            val senderId = authRepository.getCurrentUserId() ?: return@launch
            
            when (val result = messageRepository.sendImageMessage(
                chatId, senderId, receiverId, imageUri, caption, replyToMessageId
            ) { progress ->
                _uploadProgress.value = progress
            }) {
                is Result.Success -> {
                    _messageUiState.value = MessageUiState.Success(result.data)
                }
                is Result.Error -> {
                    _messageUiState.value = MessageUiState.Error(result.error, "Failed to send image")
                }
            }
            
            _currentOperation.value = MessageOperation.NONE
        }
    }
    
    /**
     * Send video message
     */
    fun sendVideoMessage(videoUri: Uri, caption: String? = null, replyToMessageId: String? = null) {
        viewModelScope.launch {
            _currentOperation.value = MessageOperation.SEND_MEDIA
            _messageUiState.value = MessageUiState.Loading
            _uploadProgress.value = 0f
            
            val chatId = _currentChatId.value ?: return@launch
            val receiverId = _currentReceiverId.value ?: return@launch
            val senderId = authRepository.getCurrentUserId() ?: return@launch
            
            when (val result = messageRepository.sendVideoMessage(
                chatId, senderId, receiverId, videoUri, caption, replyToMessageId
            ) { progress ->
                _uploadProgress.value = progress
            }) {
                is Result.Success -> {
                    _messageUiState.value = MessageUiState.Success(result.data)
                }
                is Result.Error -> {
                    _messageUiState.value = MessageUiState.Error(result.error, "Failed to send video")
                }
            }
            
            _currentOperation.value = MessageOperation.NONE
        }
    }
    
    /**
     * Send audio message
     */
    fun sendAudioMessage(audioUri: Uri, caption: String? = null, replyToMessageId: String? = null) {
        viewModelScope.launch {
            _currentOperation.value = MessageOperation.SEND_MEDIA
            _messageUiState.value = MessageUiState.Loading
            _uploadProgress.value = 0f
            
            val chatId = _currentChatId.value ?: return@launch
            val receiverId = _currentReceiverId.value ?: return@launch
            val senderId = authRepository.getCurrentUserId() ?: return@launch
            
            when (val result = messageRepository.sendAudioMessage(
                chatId, senderId, receiverId, audioUri, caption, replyToMessageId
            ) { progress ->
                _uploadProgress.value = progress
            }) {
                is Result.Success -> {
                    _messageUiState.value = MessageUiState.Success(result.data)
                }
                is Result.Error -> {
                    _messageUiState.value = MessageUiState.Error(result.error, "Failed to send audio")
                }
            }
            
            _currentOperation.value = MessageOperation.NONE
        }
    }
    
    /**
     * Send file message
     */
    fun sendFileMessage(
        fileUri: Uri, 
        fileName: String, 
        fileType: String, 
        replyToMessageId: String? = null
    ) {
        viewModelScope.launch {
            _currentOperation.value = MessageOperation.SEND_MEDIA
            _messageUiState.value = MessageUiState.Loading
            _uploadProgress.value = 0f
            
            val chatId = _currentChatId.value ?: return@launch
            val receiverId = _currentReceiverId.value ?: return@launch
            val senderId = authRepository.getCurrentUserId() ?: return@launch
            
            when (val result = messageRepository.sendFileMessage(
                chatId, senderId, receiverId, fileUri, fileName, fileType, replyToMessageId
            ) { progress ->
                _uploadProgress.value = progress
            }) {
                is Result.Success -> {
                    _messageUiState.value = MessageUiState.Success(result.data)
                }
                is Result.Error -> {
                    _messageUiState.value = MessageUiState.Error(result.error, "Failed to send file")
                }
            }
            
            _currentOperation.value = MessageOperation.NONE
        }
    }
    
    /**
     * Send location message
     */
    fun sendLocationMessage(
        latitude: Double,
        longitude: Double,
        address: String? = null,
        name: String? = null,
        replyToMessageId: String? = null
    ) {
        viewModelScope.launch {
            _currentOperation.value = MessageOperation.SEND_MESSAGE
            _messageUiState.value = MessageUiState.Loading
            
            val chatId = _currentChatId.value ?: return@launch
            val receiverId = _currentReceiverId.value ?: return@launch
            val senderId = authRepository.getCurrentUserId() ?: return@launch
            
            when (val result = messageRepository.sendLocationMessage(
                chatId, senderId, receiverId, latitude, longitude, address, name, replyToMessageId
            )) {
                is Result.Success -> {
                    _messageUiState.value = MessageUiState.Success(result.data)
                }
                is Result.Error -> {
                    _messageUiState.value = MessageUiState.Error(result.error, "Failed to send location")
                }
            }
            
            _currentOperation.value = MessageOperation.NONE
        }
    }
    
    /**
     * Mark message as read
     */
    fun markMessageAsRead(messageId: String) {
        viewModelScope.launch {
            _currentOperation.value = MessageOperation.MARK_READ
            
            messageRepository.markMessageAsRead(messageId)
            
            _currentOperation.value = MessageOperation.NONE
        }
    }
    
    /**
     * Mark chat as read
     */
    fun markChatAsRead(chatId: String, userId: String) {
        viewModelScope.launch {
            _currentOperation.value = MessageOperation.MARK_READ
            
            messageRepository.markChatMessagesAsRead(chatId, userId)
            
            _currentOperation.value = MessageOperation.NONE
        }
    }
    
    /**
     * Delete message
     */
    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            _currentOperation.value = MessageOperation.DELETE_MESSAGE
            _messageUiState.value = MessageUiState.Loading
            
            when (val result = messageRepository.deleteMessage(messageId)) {
                is Result.Success -> {
                    _messageUiState.value = MessageUiState.Success(Unit)
                }
                is Result.Error -> {
                    _messageUiState.value = MessageUiState.Error(result.error, "Failed to delete message")
                }
            }
            
            _currentOperation.value = MessageOperation.NONE
        }
    }
    
    /**
     * Add reaction to message
     */
    fun addReactionToMessage(messageId: String, reaction: String) {
        viewModelScope.launch {
            _currentOperation.value = MessageOperation.ADD_REACTION
            _messageUiState.value = MessageUiState.Loading
            
            val userId = authRepository.getCurrentUserId() ?: return@launch
            
            when (val result = messageRepository.addReactionToMessage(messageId, userId, reaction)) {
                is Result.Success -> {
                    _messageUiState.value = MessageUiState.Success(Unit)
                }
                is Result.Error -> {
                    _messageUiState.value = MessageUiState.Error(result.error, "Failed to add reaction")
                }
            }
            
            _currentOperation.value = MessageOperation.NONE
        }
    }
    
    /**
     * Remove reaction from message
     */
    fun removeReactionFromMessage(messageId: String) {
        viewModelScope.launch {
            _currentOperation.value = MessageOperation.ADD_REACTION
            _messageUiState.value = MessageUiState.Loading
            
            val userId = authRepository.getCurrentUserId() ?: return@launch
            
            when (val result = messageRepository.removeReactionFromMessage(messageId, userId)) {
                is Result.Success -> {
                    _messageUiState.value = MessageUiState.Success(Unit)
                }
                is Result.Error -> {
                    _messageUiState.value = MessageUiState.Error(result.error, "Failed to remove reaction")
                }
            }
            
            _currentOperation.value = MessageOperation.NONE
        }
    }
    
    /**
     * Forward message to another chat
     */
    fun forwardMessage(messageId: String, targetChatId: String, targetReceiverId: String) {
        viewModelScope.launch {
            _currentOperation.value = MessageOperation.FORWARD_MESSAGE
            _messageUiState.value = MessageUiState.Loading
            
            when (val result = messageRepository.forwardMessage(messageId, targetChatId, targetReceiverId)) {
                is Result.Success -> {
                    _messageUiState.value = MessageUiState.Success(result.data)
                }
                is Result.Error -> {
                    _messageUiState.value = MessageUiState.Error(result.error, "Failed to forward message")
                }
            }
            
            _currentOperation.value = MessageOperation.NONE
        }
    }
    
    /**
     * Get message translation
     */
    fun getMessageTranslation(messageId: String, targetLanguage: String) {
        viewModelScope.launch {
            _currentOperation.value = MessageOperation.TRANSLATE_MESSAGE
            _messageUiState.value = MessageUiState.Loading
            
            when (val result = messageRepository.getMessageTranslation(messageId, targetLanguage)) {
                is Result.Success -> {
                    _messageUiState.value = MessageUiState.Success(result.data)
                }
                is Result.Error -> {
                    _messageUiState.value = MessageUiState.Error(result.error, "Failed to translate message")
                }
            }
            
            _currentOperation.value = MessageOperation.NONE
        }
    }
    
    /**
     * Pin message
     */
    fun pinMessage(messageId: String) {
        viewModelScope.launch {
            _currentOperation.value = MessageOperation.PIN_MESSAGE
            _messageUiState.value = MessageUiState.Loading
            
            when (val result = messageRepository.pinMessage(messageId)) {
                is Result.Success -> {
                    _messageUiState.value = MessageUiState.Success(Unit)
                    
                    // Refresh pinned messages
                    val chatId = _currentChatId.value
                    if (chatId != null) {
                        loadPinnedMessages(chatId)
                    }
                }
                is Result.Error -> {
                    _messageUiState.value = MessageUiState.Error(result.error, "Failed to pin message")
                }
            }
            
            _currentOperation.value = MessageOperation.NONE
        }
    }
    
    /**
     * Unpin message
     */
    fun unpinMessage(messageId: String) {
        viewModelScope.launch {
            _currentOperation.value = MessageOperation.PIN_MESSAGE
            _messageUiState.value = MessageUiState.Loading
            
            when (val result = messageRepository.unpinMessage(messageId)) {
                is Result.Success -> {
                    _messageUiState.value = MessageUiState.Success(Unit)
                    
                    // Refresh pinned messages
                    val chatId = _currentChatId.value
                    if (chatId != null) {
                        loadPinnedMessages(chatId)
                    }
                }
                is Result.Error -> {
                    _messageUiState.value = MessageUiState.Error(result.error, "Failed to unpin message")
                }
            }
            
            _currentOperation.value = MessageOperation.NONE
        }
    }
    
    /**
     * Send AI-generated message (for chat suggestions)
     */
    fun sendAIMessage(aiProfileId: String, content: String, replyToMessageId: String? = null) {
        viewModelScope.launch {
            _currentOperation.value = MessageOperation.SEND_MESSAGE
            _messageUiState.value = MessageUiState.Loading
            
            val chatId = _currentChatId.value ?: return@launch
            val receiverId = _currentReceiverId.value ?: return@launch
            
            when (val result = messageRepository.sendAIMessage(chatId, aiProfileId, receiverId, content, replyToMessageId)) {
                is Result.Success -> {
                    _messageUiState.value = MessageUiState.Success(result.data)
                }
                is Result.Error -> {
                    _messageUiState.value = MessageUiState.Error(result.error, "Failed to send AI message")
                }
            }
            
            _currentOperation.value = MessageOperation.NONE
        }
    }
    
    /**
     * Generate AI message suggestions
     */
    fun generateAIMessageSuggestions(contextMessageCount: Int = 5) {
        viewModelScope.launch {
            _currentOperation.value = MessageOperation.GENERATE_AI_SUGGESTIONS
            _messageUiState.value = MessageUiState.Loading
            
            val chatId = _currentChatId.value ?: return@launch
            val userId = authRepository.getCurrentUserId() ?: return@launch
            
            when (val result = messageRepository.generateAIMessageSuggestions(chatId, userId, null, contextMessageCount)) {
                is Result.Success -> {
                    _aiSuggestions.value = result.data
                    _messageUiState.value = MessageUiState.Success(result.data)
                }
                is Result.Error -> {
                    _messageUiState.value = MessageUiState.Error(result.error, "Failed to generate message suggestions")
                }
            }
            
            _currentOperation.value = MessageOperation.NONE
        }
    }
    
    /**
     * Get unread message count for current user
     */
    fun getUnreadMessageCount() {
        viewModelScope.launch {
            val userId = authRepository.getCurrentUserId() ?: return@launch
            
            when (val result = messageRepository.getUnreadMessageCount(userId)) {
                is Result.Success -> {
                    _unreadMessageCount.value = result.data
                }
                is Result.Error -> {
                    Timber.e(result.error.throwable, "Error getting unread message count")
                }
            }
        }
    }
    
    /**
     * Reset message UI state
     */
    fun resetMessageState() {
        _messageUiState.value = MessageUiState.Initial
        _currentOperation.value = MessageOperation.NONE
    }
}