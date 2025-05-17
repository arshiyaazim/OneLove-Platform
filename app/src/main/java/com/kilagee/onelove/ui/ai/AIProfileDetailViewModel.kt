package com.kilagee.onelove.ui.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.kilagee.onelove.data.model.AIProfile
import com.kilagee.onelove.domain.model.Resource
import com.kilagee.onelove.domain.repository.AIProfileRepository
import com.kilagee.onelove.util.UIEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AIProfileDetailViewModel @Inject constructor(
    private val aiRepository: AIProfileRepository,
    private val auth: FirebaseAuth
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(AIProfileDetailUIState())
    val uiState: StateFlow<AIProfileDetailUIState> = _uiState
    
    private val _uiEvent = MutableSharedFlow<UIEvent>()
    val uiEvent: SharedFlow<UIEvent> = _uiEvent.asSharedFlow()
    
    fun loadProfile(profileId: String) {
        _uiState.update { it.copy(isLoading = true, error = null) }
        
        aiRepository.getAIProfileById(profileId)
            .onEach { resource ->
                when (resource) {
                    is Resource.Success -> {
                        _uiState.update { it.copy(
                            profile = resource.data,
                            isLoading = false,
                            error = null
                        )}
                    }
                    is Resource.Error -> {
                        _uiState.update { it.copy(
                            isLoading = false,
                            error = resource.message
                        )}
                        _uiEvent.emit(UIEvent.ShowToast(resource.message))
                    }
                    is Resource.Loading -> {
                        _uiState.update { it.copy(isLoading = true) }
                    }
                }
            }
            .catch { e ->
                _uiState.update { it.copy(
                    isLoading = false,
                    error = e.message ?: "Unknown error occurred"
                )}
                _uiEvent.emit(UIEvent.ShowToast(e.message ?: "Unknown error occurred"))
            }
            .launchIn(viewModelScope)
    }
    
    fun startConversation() {
        val profile = _uiState.value.profile ?: return
        val currentUser = auth.currentUser
        
        if (currentUser == null) {
            viewModelScope.launch {
                _uiEvent.emit(UIEvent.ShowToast("You must be logged in to chat"))
            }
            return
        }
        
        _uiState.update { it.copy(isCreatingConversation = true) }
        
        viewModelScope.launch {
            aiRepository.createConversation(currentUser.uid, profile.id)
                .catch { e ->
                    _uiState.update { it.copy(isCreatingConversation = false) }
                    _uiEvent.emit(UIEvent.ShowToast(e.message ?: "Failed to start conversation"))
                }
                .collectLatest { resource ->
                    when (resource) {
                        is Resource.Success -> {
                            _uiState.update { it.copy(isCreatingConversation = false) }
                            _uiEvent.emit(UIEvent.Navigate("ai_chat/${resource.data.id}"))
                        }
                        is Resource.Error -> {
                            _uiState.update { it.copy(isCreatingConversation = false) }
                            _uiEvent.emit(UIEvent.ShowToast(resource.message))
                        }
                        is Resource.Loading -> {
                            _uiState.update { it.copy(isCreatingConversation = true) }
                        }
                    }
                }
        }
    }
}

data class AIProfileDetailUIState(
    val profile: AIProfile? = null,
    val isLoading: Boolean = false,
    val isCreatingConversation: Boolean = false,
    val error: String? = null
)