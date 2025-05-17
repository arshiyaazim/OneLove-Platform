package com.kilagee.onelove.ui.ai

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kilagee.onelove.domain.model.AIMessage
import com.kilagee.onelove.domain.model.AIProfile
import com.kilagee.onelove.domain.model.AIResponseType
import com.kilagee.onelove.domain.repository.AIProfileRepository
import com.kilagee.onelove.util.PremiumAccessManager
import com.kilagee.onelove.util.PremiumFeature
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * ViewModel for handling AI chat interactions
 */
@HiltViewModel
class AIChatViewModel @Inject constructor(
    private val repository: AIProfileRepository,
    private val premiumAccessManager: PremiumAccessManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(AIChatUIState())
    val uiState: StateFlow<AIChatUIState> = _uiState.asStateFlow()
    
    private val _events = MutableSharedFlow<AIChatEvent>()
    val events: SharedFlow<AIChatEvent> = _events.asSharedFlow()
    
    private val conversationId: String = savedStateHandle.get<String>("conversationId") ?: ""
    private val aiProfileId: String = savedStateHandle.get<String>("aiProfileId") ?: ""
    
    val hasPremiumAccess: StateFlow<Boolean> = premiumAccessManager
        .checkFeatureAccess(PremiumFeature.AI_CHAT)
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            false
        )
    
    init {
        if (conversationId.isNotEmpty()) {
            loadConversation()
        } else if (aiProfileId.isNotEmpty()) {
            loadAIProfile()
        }
    }
    
    /**
     * Load AI profile and conversation messages
     */
    private fun loadConversation() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            // Load AI profile
            repository.getAIProfileById(aiProfileId).collectLatest { profile ->
                profile?.let { aiProfile ->
                    _uiState.value = _uiState.value.copy(
                        aiProfile = aiProfile,
                        isLoading = false
                    )
                    
                    // Load messages
                    loadMessages()
                }
            }
        }
    }
    
    /**
     * Load AI profile by ID
     */
    private fun loadAIProfile() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            repository.getAIProfileById(aiProfileId).collectLatest { profile ->
                profile?.let { aiProfile ->
                    _uiState.value = _uiState.value.copy(
                        aiProfile = aiProfile,
                        conversationId = aiProfile.conversationId,
                        isLoading = false
                    )
                    
                    // Load messages
                    loadMessages()
                }
            }
        }
    }
    
    /**
     * Load conversation messages
     */
    private fun loadMessages() {
        val conversationIdToUse = if (conversationId.isNotEmpty()) {
            conversationId
        } else {
            _uiState.value.aiProfile?.conversationId ?: return
        }
        
        viewModelScope.launch {
            repository.getConversationMessages(conversationIdToUse).collectLatest { messages ->
                _uiState.value = _uiState.value.copy(
                    messages = messages,
                    isLoading = false
                )
            }
        }
    }
    
    /**
     * Send a message to the AI
     */
    fun sendMessage(message: String) {
        if (message.trim().isEmpty()) return
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSending = true)
            
            // Check premium access
            val hasPremium = premiumAccessManager.checkFeatureAccess(PremiumFeature.AI_CHAT).stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                false
            ).value
            
            if (!hasPremium) {
                _events.emit(AIChatEvent.PremiumRequired)
                _uiState.value = _uiState.value.copy(isSending = false)
                return@launch
            }
            
            val conversationIdToUse = if (conversationId.isNotEmpty()) {
                conversationId
            } else {
                _uiState.value.aiProfile?.conversationId ?: return@launch
            }
            
            // Select a response type based on the AI's personality
            val responseType = chooseResponseType(_uiState.value.aiProfile?.personality ?: "")
            
            val result = repository.sendMessageToAI(
                conversationId = conversationIdToUse,
                message = message,
                preferredResponseType = responseType
            )
            
            if (result.isSuccess) {
                // Messages will be updated automatically via flow collection
                _uiState.value = _uiState.value.copy(isSending = false)
            } else {
                _events.emit(AIChatEvent.MessageSendFailed(result.exceptionOrNull()?.message ?: "Unknown error"))
                _uiState.value = _uiState.value.copy(isSending = false)
            }
        }
    }
    
    /**
     * Mark all messages in the conversation as read
     */
    fun markMessagesAsRead() {
        val conversationIdToUse = if (conversationId.isNotEmpty()) {
            conversationId
        } else {
            _uiState.value.aiProfile?.conversationId ?: return
        }
        
        viewModelScope.launch {
            repository.markConversationAsRead(conversationIdToUse)
        }
    }
    
    /**
     * Choose a response type based on AI personality
     */
    private fun chooseResponseType(personality: String): AIResponseType {
        val personalityLower = personality.lowercase()
        
        return when {
            personalityLower.contains("romantic") -> AIResponseType.ROMANTIC
            personalityLower.contains("humorous") || personalityLower.contains("funny") -> AIResponseType.FUNNY
            personalityLower.contains("intellectual") || personalityLower.contains("analytical") -> AIResponseType.DEEP
            personalityLower.contains("adventurous") || personalityLower.contains("spontaneous") -> AIResponseType.EXCITED
            personalityLower.contains("caring") || personalityLower.contains("thoughtful") -> AIResponseType.SUPPORTIVE
            personalityLower.contains("mysterious") -> AIResponseType.DEEP
            personalityLower.contains("playful") || personalityLower.contains("flirty") -> AIResponseType.FLIRTY
            personalityLower.contains("curious") -> AIResponseType.CURIOUS
            else -> AIResponseType.values().random()
        }
    }
}

/**
 * UI state for AI chat screen
 */
data class AIChatUIState(
    val aiProfile: AIProfile? = null,
    val messages: List<AIMessage> = emptyList(),
    val conversationId: String = "",
    val isLoading: Boolean = false,
    val isSending: Boolean = false
)

/**
 * Events emitted by the AI chat view model
 */
sealed class AIChatEvent {
    data class MessageSendFailed(val error: String) : AIChatEvent()
    object PremiumRequired : AIChatEvent()
}