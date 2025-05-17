package com.kilagee.onelove.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.kilagee.onelove.data.model.Chat
import com.kilagee.onelove.data.model.Message
import com.kilagee.onelove.data.model.MessageType
import com.kilagee.onelove.domain.repository.ChatRepository
import com.kilagee.onelove.domain.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import javax.inject.Inject

/**
 * Chat view model
 */
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository
) : ViewModel() {
    
    private val _chats = MutableStateFlow<Result<List<Chat>>>(Result.Loading)
    val chats: StateFlow<Result<List<Chat>>> = _chats
    
    private val _selectedChatId = MutableStateFlow<String?>(null)
    val selectedChatId: StateFlow<String?> = _selectedChatId
    
    private val _selectedChat = MutableStateFlow<Result<Chat?>>(Result.Loading)
    val selectedChat: StateFlow<Result<Chat?>> = _selectedChat
    
    private val _recentMessages = MutableStateFlow<Result<List<Message>>>(Result.Loading)
    val recentMessages: StateFlow<Result<List<Message>>> = _recentMessages
    
    private val _messageSendState = MutableStateFlow<MessageSendState>(MessageSendState.Idle)
    val messageSendState: StateFlow<MessageSendState> = _messageSendState
    
    private val _totalUnreadCount = MutableStateFlow(0)
    val totalUnreadCount: StateFlow<Int> = _totalUnreadCount
    
    init {
        loadChats()
        observeTotalUnreadCount()
    }
    
    /**
     * Load all chats for the current user
     */
    private fun loadChats() {
        viewModelScope.launch {
            chatRepository.getChats()
                .catch { e ->
                    Timber.e(e, "Error loading chats")
                    _chats.value = Result.Error("Failed to load chats: ${e.message}")
                }
                .collect { result ->
                    _chats.value = result
                }
        }
    }
    
    /**
     * Observe the total unread message count
     */
    private fun observeTotalUnreadCount() {
        viewModelScope.launch {
            chatRepository.getTotalUnreadCount()
                .catch { e ->
                    Timber.e(e, "Error getting total unread count")
                    _totalUnreadCount.value = 0
                }
                .collect { count ->
                    _totalUnreadCount.value = count
                }
        }
    }
    
    /**
     * Select a chat by ID
     * 
     * @param chatId ID of the chat to select
     */
    fun selectChat(chatId: String) {
        _selectedChatId.value = chatId
        loadSelectedChat(chatId)
        loadRecentMessages(chatId)
        markChatAsRead(chatId)
    }
    
    /**
     * Clear the selected chat
     */
    fun clearSelectedChat() {
        _selectedChatId.value = null
        _selectedChat.value = Result.Success(null)
        _recentMessages.value = Result.Success(emptyList())
    }
    
    /**
     * Load the selected chat
     * 
     * @param chatId ID of the chat to load
     */
    private fun loadSelectedChat(chatId: String) {
        viewModelScope.launch {
            chatRepository.getChatById(chatId)
                .catch { e ->
                    Timber.e(e, "Error loading selected chat")
                    _selectedChat.value = Result.Error("Failed to load chat: ${e.message}")
                }
                .collect { result ->
                    _selectedChat.value = result.map { it }
                }
        }
    }
    
    /**
     * Load recent messages for the selected chat
     * 
     * @param chatId ID of the chat
     * @param limit Maximum number of messages to load
     */
    private fun loadRecentMessages(chatId: String, limit: Int = 20) {
        viewModelScope.launch {
            chatRepository.getRecentMessages(chatId, limit)
                .catch { e ->
                    Timber.e(e, "Error loading recent messages")
                    _recentMessages.value = Result.Error("Failed to load messages: ${e.message}")
                }
                .collect { result ->
                    _recentMessages.value = result
                }
        }
    }
    
    /**
     * Mark a chat as read
     * 
     * @param chatId ID of the chat
     */
    fun markChatAsRead(chatId: String) {
        viewModelScope.launch {
            try {
                chatRepository.markChatAsRead(chatId)
            } catch (e: Exception) {
                Timber.e(e, "Error marking chat as read")
            }
        }
    }
    
    /**
     * Send a text message
     * 
     * @param content Text content of the message
     */
    fun sendTextMessage(content: String) {
        val chatId = _selectedChatId.value ?: return
        
        if (content.isBlank()) {
            return
        }
        
        viewModelScope.launch {
            try {
                _messageSendState.value = MessageSendState.Sending
                val result = chatRepository.sendTextMessage(chatId, content)
                
                when (result) {
                    is Result.Success -> {
                        _messageSendState.value = MessageSendState.Success(result.data)
                        // Refresh recent messages
                        loadRecentMessages(chatId)
                    }
                    is Result.Error -> {
                        _messageSendState.value = MessageSendState.Error(result.message)
                    }
                    is Result.Loading -> {
                        _messageSendState.value = MessageSendState.Sending
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error sending text message")
                _messageSendState.value = MessageSendState.Error(e.message ?: "Failed to send message")
            } finally {
                // Reset state after a delay
                kotlinx.coroutines.delay(2000)
                _messageSendState.value = MessageSendState.Idle
            }
        }
    }
    
    /**
     * Send a media message
     * 
     * @param file Media file to send
     * @param type Type of media message
     * @param caption Optional caption for the media
     */
    fun sendMediaMessage(file: File, type: MessageType, caption: String? = null) {
        val chatId = _selectedChatId.value ?: return
        
        viewModelScope.launch {
            try {
                _messageSendState.value = MessageSendState.Sending
                val result = chatRepository.sendMediaMessage(chatId, file, type, caption)
                
                when (result) {
                    is Result.Success -> {
                        _messageSendState.value = MessageSendState.Success(result.data)
                        // Refresh recent messages
                        loadRecentMessages(chatId)
                    }
                    is Result.Error -> {
                        _messageSendState.value = MessageSendState.Error(result.message)
                    }
                    is Result.Loading -> {
                        _messageSendState.value = MessageSendState.Sending
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error sending media message")
                _messageSendState.value = MessageSendState.Error(e.message ?: "Failed to send message")
            } finally {
                // Reset state after a delay
                kotlinx.coroutines.delay(2000)
                _messageSendState.value = MessageSendState.Idle
            }
        }
    }
    
    /**
     * Update typing status
     * 
     * @param isTyping Whether the user is typing
     */
    fun updateTypingStatus(isTyping: Boolean) {
        val chatId = _selectedChatId.value ?: return
        
        viewModelScope.launch {
            try {
                chatRepository.updateTypingStatus(chatId, isTyping)
            } catch (e: Exception) {
                Timber.e(e, "Error updating typing status")
            }
        }
    }
    
    /**
     * Get paged messages for the selected chat
     * 
     * @return Flow of paged messages
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun getPagedMessages(): Flow<PagingData<Message>> {
        return _selectedChatId
            .flatMapLatest { chatId ->
                chatId?.let {
                    chatRepository.getMessages(it)
                        .catch { e ->
                            Timber.e(e, "Error loading paged messages")
                            emit(PagingData.empty())
                        }
                } ?: flowOf(PagingData.empty())
            }
            .cachedIn(viewModelScope)
    }
    
    /**
     * Create a new chat
     * 
     * @param userIds List of user IDs to include in the chat
     * @param isGroupChat Whether this is a group chat
     * @param name Name of the group chat (only for group chats)
     */
    fun createChat(userIds: List<String>, isGroupChat: Boolean = false, name: String? = null) {
        viewModelScope.launch {
            try {
                val result = chatRepository.createChat(userIds, isGroupChat, name)
                if (result is Result.Success) {
                    // Refresh chat list
                    loadChats()
                    // Select the new chat
                    selectChat(result.data.id)
                }
            } catch (e: Exception) {
                Timber.e(e, "Error creating chat")
            }
        }
    }
    
    /**
     * Set chat pin status
     * 
     * @param chatId ID of the chat
     * @param isPinned Whether the chat should be pinned
     */
    fun setPinStatus(chatId: String, isPinned: Boolean) {
        viewModelScope.launch {
            try {
                chatRepository.setPinStatus(chatId, isPinned)
                // Refresh chat list
                loadChats()
            } catch (e: Exception) {
                Timber.e(e, "Error setting pin status")
            }
        }
    }
    
    /**
     * Set chat mute status
     * 
     * @param chatId ID of the chat
     * @param isMuted Whether the chat should be muted
     */
    fun setMuteStatus(chatId: String, isMuted: Boolean) {
        viewModelScope.launch {
            try {
                chatRepository.setMuteStatus(chatId, isMuted)
                // Refresh chat list
                loadChats()
            } catch (e: Exception) {
                Timber.e(e, "Error setting mute status")
            }
        }
    }
    
    /**
     * Add a reaction to a message
     * 
     * @param messageId ID of the message
     * @param reaction Reaction to add (emoji)
     */
    fun addReaction(messageId: String, reaction: String) {
        viewModelScope.launch {
            try {
                chatRepository.addReaction(messageId, reaction)
                // Refresh recent messages
                _selectedChatId.value?.let { chatId ->
                    loadRecentMessages(chatId)
                }
            } catch (e: Exception) {
                Timber.e(e, "Error adding reaction")
            }
        }
    }
    
    /**
     * Remove a reaction from a message
     * 
     * @param messageId ID of the message
     */
    fun removeReaction(messageId: String) {
        viewModelScope.launch {
            try {
                chatRepository.removeReaction(messageId)
                // Refresh recent messages
                _selectedChatId.value?.let { chatId ->
                    loadRecentMessages(chatId)
                }
            } catch (e: Exception) {
                Timber.e(e, "Error removing reaction")
            }
        }
    }
    
    /**
     * Search for messages by content
     * 
     * @param query Search query
     * @return Flow of Result containing a list of matching messages
     */
    fun searchMessages(query: String): Flow<Result<List<Message>>> {
        return chatRepository.searchMessages(query, _selectedChatId.value)
            .catch { e ->
                Timber.e(e, "Error searching messages")
                emit(Result.Error("Failed to search messages: ${e.message}"))
            }
    }
    
    /**
     * Delete a message
     * 
     * @param messageId ID of the message to delete
     * @param deleteForEveryone Whether to delete the message for all users
     */
    fun deleteMessage(messageId: String, deleteForEveryone: Boolean = false) {
        viewModelScope.launch {
            try {
                chatRepository.deleteMessage(messageId, deleteForEveryone)
                // Refresh recent messages
                _selectedChatId.value?.let { chatId ->
                    loadRecentMessages(chatId)
                }
            } catch (e: Exception) {
                Timber.e(e, "Error deleting message")
            }
        }
    }
}

/**
 * Message send state
 */
sealed class MessageSendState {
    object Idle : MessageSendState()
    object Sending : MessageSendState()
    data class Success(val message: Message) : MessageSendState()
    data class Error(val message: String) : MessageSendState()
}