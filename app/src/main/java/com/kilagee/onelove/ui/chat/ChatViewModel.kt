package com.kilagee.onelove.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kilagee.onelove.data.model.Chat
import com.kilagee.onelove.domain.model.Resource
import com.kilagee.onelove.domain.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository
) : ViewModel() {
    
    private val _chatsState = MutableStateFlow<Resource<List<Chat>>>(Resource.Loading)
    val chatsState: StateFlow<Resource<List<Chat>>> = _chatsState
    
    private var allChats: List<Chat> = emptyList()
    
    init {
        loadChats()
    }
    
    fun loadChats() {
        viewModelScope.launch {
            chatRepository.getChatsForCurrentUser()
                .onEach { resource ->
                    _chatsState.value = resource
                    if (resource is Resource.Success) {
                        allChats = resource.data
                    }
                }
                .catch { e ->
                    _chatsState.value = Resource.error("Failed to load chats: ${e.message}")
                }
                .launchIn(viewModelScope)
        }
    }
    
    fun searchChats(query: String) {
        if (query.isBlank()) {
            _chatsState.value = Resource.success(allChats)
            return
        }
        
        // Filter chats by username or last message text
        val filteredChats = allChats.filter { chat ->
            val username = chat.username?.lowercase() ?: ""
            val lastMessage = chat.lastMessageText?.lowercase() ?: ""
            
            username.contains(query.lowercase()) || lastMessage.contains(query.lowercase())
        }
        
        _chatsState.value = Resource.success(filteredChats)
    }
    
    fun markChatAsRead(chatId: String) {
        viewModelScope.launch {
            chatRepository.resetUnreadCount(chatId)
        }
    }
}