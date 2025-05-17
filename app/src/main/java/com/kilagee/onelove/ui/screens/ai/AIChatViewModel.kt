package com.kilagee.onelove.ui.screens.ai

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kilagee.onelove.data.model.AIMessage
import com.kilagee.onelove.data.model.AIProfile
import com.kilagee.onelove.data.model.ReactionType
import com.kilagee.onelove.domain.repository.AIRepository
import com.kilagee.onelove.domain.repository.SubscriptionRepository
import com.kilagee.onelove.domain.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Date
import java.util.UUID
import javax.inject.Inject

/**
 * ViewModel for AI Chat screen
 */
@HiltViewModel
class AIChatViewModel @Inject constructor(
    private val aiRepository: AIRepository,
    private val subscriptionRepository: SubscriptionRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    
    // Get parameters from navigation
    private val profileId: String = savedStateHandle.get<String>("profileId") ?: ""
    private val interactionId: String? = savedStateHandle.get<String>("interactionId")
    
    // UI state
    private val _uiState = MutableStateFlow<AIChatUIState>(AIChatUIState.Loading)
    val uiState: StateFlow<AIChatUIState> = _uiState.asStateFlow()
    
    // Messages
    private val _messages = MutableStateFlow<List<AIMessage>>(emptyList())
    val messages: StateFlow<List<AIMessage>> = _messages.asStateFlow()
    
    // AI profile
    private val _aiProfile = MutableStateFlow<AIProfile?>(null)
    val aiProfile: StateFlow<AIProfile?> = _aiProfile.asStateFlow()
    
    // Is generating response
    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()
    
    // Subscription status
    private val _isSubscribed = MutableStateFlow(false)
    val isSubscribed: StateFlow<Boolean> = _isSubscribed.asStateFlow()
    
    // Interaction count and limit
    private val _interactionCount = MutableStateFlow(0)
    val interactionCount: StateFlow<Int> = _interactionCount.asStateFlow()
    
    private val _interactionLimit = MutableStateFlow(10) // Default free limit
    val interactionLimit: StateFlow<Int> = _interactionLimit.asStateFlow()
    
    // Events
    private val _events = MutableSharedFlow<AIChatEvent>()
    val events: SharedFlow<AIChatEvent> = _events.asSharedFlow()
    
    // Active jobs
    private var messagesJob: Job? = null
    
    init {
        loadAIProfile()
        checkSubscriptionStatus()
        loadConversation()
    }
    
    /**
     * Load AI profile
     */
    private fun loadAIProfile() {
        viewModelScope.launch {
            val result = aiRepository.getAIProfile(profileId)
            
            when (result) {
                is Result.Success -> {
                    _aiProfile.value = result.data
                }
                is Result.Error -> {
                    _events.emit(AIChatEvent.Error(result.message ?: "Failed to load AI profile"))
                    _uiState.value = AIChatUIState.Error(result.message ?: "Failed to load AI profile")
                }
                is Result.Loading -> {
                    // Already set to loading in init
                }
            }
        }
    }
    
    /**
     * Check subscription status
     */
    private fun checkSubscriptionStatus() {
        viewModelScope.launch {
            val result = subscriptionRepository.checkActiveSubscription()
            
            if (result is Result.Success) {
                _isSubscribed.value = result.data
            }
            
            // Get interaction limit
            val limitResult = aiRepository.getFreeInteractionLimit()
            if (limitResult is Result.Success) {
                _interactionLimit.value = limitResult.data
            }
            
            // Get current interaction count for this AI
            val countResult = aiRepository.getInteractionCount(profileId)
            if (countResult is Result.Success) {
                _interactionCount.value = countResult.data
            }
        }
    }
    
    /**
     * Load conversation
     */
    fun loadConversation() {
        // Cancel any existing job
        messagesJob?.cancel()
        
        // Start new job
        messagesJob = viewModelScope.launch {
            _uiState.value = AIChatUIState.Loading
            
            val interactionIdToUse = interactionId ?: aiRepository.createInteraction(profileId)
                .let { result ->
                    if (result is Result.Success) result.data else null
                }
            
            if (interactionIdToUse == null) {
                _events.emit(AIChatEvent.Error("Failed to create or load interaction"))
                _uiState.value = AIChatUIState.Error("Failed to create or load interaction")
                return@launch
            }
            
            // Get messages flow
            aiRepository.getMessagesFlow(profileId, interactionIdToUse).collectLatest { result ->
                when (result) {
                    is Result.Success -> {
                        val messageList = result.data
                        
                        if (messageList.isEmpty()) {
                            _uiState.value = AIChatUIState.Empty
                        } else {
                            _uiState.value = AIChatUIState.Success
                        }
                        
                        _messages.value = messageList
                    }
                    is Result.Error -> {
                        _events.emit(AIChatEvent.Error(result.message ?: "Failed to load messages"))
                        _uiState.value = AIChatUIState.Error(result.message ?: "Failed to load messages")
                    }
                    is Result.Loading -> {
                        // Keep current state if already loaded
                        if (_uiState.value !is AIChatUIState.Success && _uiState.value !is AIChatUIState.Empty) {
                            _uiState.value = AIChatUIState.Loading
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Send a message to the AI
     */
    fun sendMessage(content: String) {
        viewModelScope.launch {
            // Check if we've reached the interaction limit for free users
            if (!_isSubscribed.value && _interactionCount.value >= _interactionLimit.value) {
                _events.emit(AIChatEvent.LimitReached)
                return@launch
            }
            
            // Create user message
            val messageId = UUID.randomUUID().toString()
            
            val message = AIMessage(
                id = messageId,
                profileId = profileId,
                interactionId = interactionId ?: "",
                content = content,
                timestamp = Date(),
                isFromUser = true
            )
            
            // Add message to UI immediately
            _messages.value = _messages.value + message
            
            // Set generating flag
            _isGenerating.value = true
            
            // Send message to repository
            val result = aiRepository.sendMessage(message)
            
            when (result) {
                is Result.Success -> {
                    _events.emit(AIChatEvent.MessageSent)
                    
                    // Increment interaction count for free users
                    if (!_isSubscribed.value) {
                        _interactionCount.value += 1
                    }
                    
                    // AI will respond asynchronously via the messages flow
                    // But for better UX, we can simulate typing (in a real app this would be based on server events)
                    simulateAITyping()
                }
                is Result.Error -> {
                    _events.emit(AIChatEvent.Error(result.message ?: "Failed to send message"))
                    _isGenerating.value = false
                }
                is Result.Loading -> {
                    // Already set generating flag above
                }
            }
        }
    }
    
    /**
     * Simulate AI typing for better UX
     * In a real app, this would be based on server events or WebSockets
     */
    private fun simulateAITyping() {
        viewModelScope.launch {
            // Give the AI time to "think"
            delay(1000)
            
            // Create a simulated AI response
            val aiProfile = _aiProfile.value
            val responseContent = when {
                aiProfile == null -> "I'm here to chat with you. What would you like to talk about?"
                else -> {
                    val lastUserMessage = _messages.value.lastOrNull { it.isFromUser }?.content ?: ""
                    generateAIResponse(lastUserMessage, aiProfile)
                }
            }
            
            // Create AI message
            val aiMessage = AIMessage(
                id = UUID.randomUUID().toString(),
                profileId = profileId,
                interactionId = interactionId ?: "",
                content = responseContent,
                timestamp = Date(),
                isFromUser = false
            )
            
            // In a real app, this message would come from the server via the messages flow
            // Here we're adding it manually for demonstration
            _messages.value = _messages.value + aiMessage
            
            // Reset generating flag
            _isGenerating.value = false
        }
    }
    
    /**
     * Generate a simple AI response based on personality
     * In a real app, this would be handled by a backend service with actual AI
     */
    private fun generateAIResponse(userMessage: String, aiProfile: AIProfile): String {
        // This is just a simple placeholder - in a real app, this would be handled by the backend
        return when {
            userMessage.contains("hello", ignoreCase = true) || 
            userMessage.contains("hi", ignoreCase = true) -> 
                "Hello! I'm ${aiProfile.name}, your ${aiProfile.personalityType} companion. How can I assist you today?"
            
            userMessage.contains("how are you", ignoreCase = true) -> 
                "I'm doing well, thank you for asking! How about you?"
            
            userMessage.contains("name", ignoreCase = true) -> 
                "My name is ${aiProfile.name}. I'm an AI companion with a ${aiProfile.personalityType} personality."
            
            userMessage.contains("help", ignoreCase = true) -> 
                "I'd be happy to help! What do you need assistance with?"
            
            userMessage.length < 10 -> 
                "I'd love to hear more about that. Could you tell me more?"
            
            else -> 
                "That's interesting! I'd love to continue this conversation. Is there anything specific you'd like to know about me or discuss?"
        }
    }
    
    /**
     * React to a message
     */
    fun reactToMessage(messageId: String, reaction: ReactionType) {
        viewModelScope.launch {
            val result = aiRepository.reactToMessage(messageId, reaction)
            
            if (result is Result.Error) {
                _events.emit(AIChatEvent.Error(result.message ?: "Failed to add reaction"))
            }
        }
    }
    
    /**
     * Remove reaction from a message
     */
    fun removeReaction(messageId: String) {
        viewModelScope.launch {
            val result = aiRepository.removeReaction(messageId)
            
            if (result is Result.Error) {
                _events.emit(AIChatEvent.Error(result.message ?: "Failed to remove reaction"))
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        messagesJob?.cancel()
    }
}

/**
 * UI state for the AI chat screen
 */
sealed class AIChatUIState {
    object Loading : AIChatUIState()
    object Success : AIChatUIState()
    object Empty : AIChatUIState()
    data class Error(val message: String) : AIChatUIState()
}

/**
 * Events emitted by the AI chat screen
 */
sealed class AIChatEvent {
    object MessageSent : AIChatEvent()
    object LimitReached : AIChatEvent()
    data class Error(val message: String) : AIChatEvent()
}