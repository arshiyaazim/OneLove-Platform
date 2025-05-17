package com.kilagee.onelove.ui.screens.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kilagee.onelove.data.model.Match
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
import javax.inject.Inject

/**
 * ViewModel for the chat list screen
 */
@HiltViewModel
class ChatListViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _chatListState = MutableStateFlow<ChatListState>(ChatListState.Loading)
    val chatListState: StateFlow<ChatListState> = _chatListState.asStateFlow()
    
    private val _chatMatches = MutableStateFlow<List<ChatWithPartner>>(emptyList())
    val chatMatches: StateFlow<List<ChatWithPartner>> = _chatMatches.asStateFlow()
    
    /**
     * Initialize the chat list
     */
    init {
        loadChatList()
    }
    
    /**
     * Load all chat matches for the current user
     */
    private fun loadChatList() {
        viewModelScope.launch {
            _chatListState.value = ChatListState.Loading
            
            try {
                val currentUserId = authRepository.getCurrentUserId()
                
                if (currentUserId == null) {
                    _chatListState.value = ChatListState.Error("User not authenticated")
                    return@launch
                }
                
                // Load the matches
                chatRepository.getChats(currentUserId).collectLatest { result ->
                    when (result) {
                        is Result.Success -> {
                            val matches = result.data
                            if (matches.isEmpty()) {
                                _chatListState.value = ChatListState.Empty
                                return@collectLatest
                            }
                            
                            // For each match, get the chat partner details
                            val chatsWithPartners = mutableListOf<ChatWithPartner>()
                            
                            for (match in matches) {
                                val chatPartnerId = if (match.userId == currentUserId) {
                                    match.matchedUserId
                                } else {
                                    match.userId
                                }
                                
                                // Get the chat partner details
                                val partnerResult = userRepository.getUserById(chatPartnerId).collectLatest { userResult ->
                                    if (userResult is Result.Success) {
                                        val partner = userResult.data
                                        chatsWithPartners.add(ChatWithPartner(match, partner))
                                        
                                        // Sort by last message time, most recent first
                                        chatsWithPartners.sortByDescending { it.match.lastMessageTimestamp }
                                        
                                        _chatMatches.value = chatsWithPartners
                                        _chatListState.value = ChatListState.Success
                                    }
                                }
                            }
                        }
                        is Result.Error -> {
                            Timber.e("Error loading chats: ${result.message}")
                            _chatListState.value = ChatListState.Error(result.message)
                        }
                        is Result.Loading -> {
                            _chatListState.value = ChatListState.Loading
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading chat list")
                _chatListState.value = ChatListState.Error(e.message ?: "Unknown error")
            }
        }
    }
    
    /**
     * Refresh the chat list
     */
    fun refreshChatList() {
        loadChatList()
    }
}

/**
 * Data class for chat with partner
 */
data class ChatWithPartner(
    val match: Match,
    val partner: User
)

/**
 * Chat list state sealed class
 */
sealed class ChatListState {
    object Loading : ChatListState()
    object Success : ChatListState()
    object Empty : ChatListState()
    data class Error(val message: String) : ChatListState()
}