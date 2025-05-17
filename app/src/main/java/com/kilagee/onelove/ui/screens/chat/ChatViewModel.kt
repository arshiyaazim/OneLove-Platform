package com.kilagee.onelove.ui.screens.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kilagee.onelove.data.model.Message
import com.kilagee.onelove.data.model.MessageReaction
import com.kilagee.onelove.data.model.User
import com.kilagee.onelove.domain.repository.AuthRepository
import com.kilagee.onelove.domain.repository.ChatRepository
import com.kilagee.onelove.domain.repository.UserRepository
import com.kilagee.onelove.domain.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject

/**
 * ViewModel for the chat screen
 */
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // Extract matchId from SavedStateHandle (navigation arguments)
    private val chatId: String = checkNotNull(savedStateHandle["chatId"])
    
    private val _chatState = MutableStateFlow<ChatState>(ChatState.Loading)
    val chatState: StateFlow<ChatState> = _chatState.asStateFlow()
    
    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()
    
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()
    
    private val _chatPartner = MutableStateFlow<User?>(null)
    val chatPartner: StateFlow<User?> = _chatPartner.asStateFlow()
    
    /**
     * Initialize the chat
     */
    init {
        loadMatch()
        loadMessages()
        loadCurrentUser()
        markMessagesAsRead()
    }
    
    /**
     * Load the match details
     */
    private fun loadMatch() {
        viewModelScope.launch {
            try {
                chatRepository.getMatchById(chatId).collectLatest { result ->
                    when (result) {
                        is Result.Success -> {
                            val match = result.data
                            val currentUserId = authRepository.getCurrentUserId()
                            
                            if (currentUserId != null) {
                                // Determine the chat partner ID
                                val chatPartnerId = if (match.userId == currentUserId) {
                                    match.matchedUserId
                                } else {
                                    match.userId
                                }
                                
                                // Load the chat partner details
                                userRepository.getUserById(chatPartnerId).collectLatest { userResult ->
                                    if (userResult is Result.Success) {
                                        _chatPartner.value = userResult.data
                                        _chatState.value = ChatState.Success
                                    }
                                }
                            }
                        }
                        is Result.Error -> {
                            Timber.e("Error loading match: ${result.message}")
                            _chatState.value = ChatState.Error(result.message)
                        }
                        is Result.Loading -> {
                            _chatState.value = ChatState.Loading
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading match")
                _chatState.value = ChatState.Error(e.message ?: "Unknown error")
            }
        }
    }
    
    /**
     * Load messages for the chat
     */
    private fun loadMessages() {
        viewModelScope.launch {
            try {
                chatRepository.getMessages(chatId).collectLatest { result ->
                    when (result) {
                        is Result.Success -> {
                            _messages.value = result.data
                        }
                        is Result.Error -> {
                            Timber.e("Error loading messages: ${result.message}")
                        }
                        is Result.Loading -> {
                            // Already handled by the chat state
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading messages")
            }
        }
    }
    
    /**
     * Load current user details
     */
    private fun loadCurrentUser() {
        viewModelScope.launch {
            authRepository.getCurrentUser().collectLatest { result ->
                if (result is Result.Success) {
                    _currentUser.value = result.data
                }
            }
        }
    }
    
    /**
     * Mark all messages as read
     */
    private fun markMessagesAsRead() {
        viewModelScope.launch {
            val currentUserId = authRepository.getCurrentUserId()
            if (currentUserId != null) {
                try {
                    chatRepository.markAllMessagesAsRead(chatId, currentUserId)
                } catch (e: Exception) {
                    Timber.e(e, "Error marking messages as read")
                }
            }
        }
    }
    
    /**
     * Send a message
     */
    fun sendMessage(text: String) {
        viewModelScope.launch {
            val currentUserId = authRepository.getCurrentUserId()
            val chatPartnerId = _chatPartner.value?.id
            
            if (currentUserId != null && chatPartnerId != null && text.isNotBlank()) {
                val messageId = UUID.randomUUID().toString()
                val message = Message(
                    id = messageId,
                    matchId = chatId,
                    senderId = currentUserId,
                    receiverId = chatPartnerId,
                    text = text
                )
                
                try {
                    val result = chatRepository.sendMessage(message)
                    if (result is Result.Error) {
                        Timber.e("Error sending message: ${result.message}")
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Error sending message")
                }
            }
        }
    }
    
    /**
     * Add reaction to a message
     */
    fun addReaction(messageId: String, reaction: MessageReaction) {
        viewModelScope.launch {
            try {
                val result = chatRepository.addReactionToMessage(messageId, reaction)
                if (result is Result.Error) {
                    Timber.e("Error adding reaction: ${result.message}")
                }
            } catch (e: Exception) {
                Timber.e(e, "Error adding reaction")
            }
        }
    }
    
    /**
     * Remove reaction from a message
     */
    fun removeReaction(messageId: String) {
        viewModelScope.launch {
            try {
                val result = chatRepository.removeReactionFromMessage(messageId)
                if (result is Result.Error) {
                    Timber.e("Error removing reaction: ${result.message}")
                }
            } catch (e: Exception) {
                Timber.e(e, "Error removing reaction")
            }
        }
    }
    
    /**
     * Delete a message
     */
    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            try {
                val result = chatRepository.deleteMessage(messageId)
                if (result is Result.Error) {
                    Timber.e("Error deleting message: ${result.message}")
                }
            } catch (e: Exception) {
                Timber.e(e, "Error deleting message")
            }
        }
    }
}

/**
 * Chat state sealed class
 */
sealed class ChatState {
    object Loading : ChatState()
    object Success : ChatState()
    data class Error(val message: String) : ChatState()
}