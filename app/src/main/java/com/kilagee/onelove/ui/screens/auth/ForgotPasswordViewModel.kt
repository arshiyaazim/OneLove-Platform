package com.kilagee.onelove.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kilagee.onelove.domain.repository.AuthRepository
import com.kilagee.onelove.domain.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the forgot password screen
 */
@HiltViewModel
class ForgotPasswordViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    
    // UI state
    private val _uiState = MutableStateFlow<ForgotPasswordUiState>(ForgotPasswordUiState.Initial)
    val uiState: StateFlow<ForgotPasswordUiState> = _uiState.asStateFlow()
    
    // One-time events
    private val _events = MutableSharedFlow<ForgotPasswordEvent>()
    val events: SharedFlow<ForgotPasswordEvent> = _events.asSharedFlow()
    
    // Form state
    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email.asStateFlow()
    
    private val _isEmailValid = MutableStateFlow(true)
    val isEmailValid: StateFlow<Boolean> = _isEmailValid.asStateFlow()
    
    /**
     * Update email field
     */
    fun updateEmail(email: String) {
        _email.value = email
        validateEmail()
    }
    
    /**
     * Validate email format
     */
    private fun validateEmail() {
        _isEmailValid.value = android.util.Patterns.EMAIL_ADDRESS.matcher(_email.value).matches()
    }
    
    /**
     * Send password reset email
     */
    fun resetPassword() {
        validateEmail()
        
        if (!_isEmailValid.value) {
            viewModelScope.launch {
                _events.emit(ForgotPasswordEvent.ValidationError("Please enter a valid email address"))
            }
            return
        }
        
        _uiState.value = ForgotPasswordUiState.Loading
        
        viewModelScope.launch {
            val result = authRepository.sendPasswordResetEmail(_email.value)
            
            when (result) {
                is Result.Success -> {
                    _uiState.value = ForgotPasswordUiState.Success
                    _events.emit(ForgotPasswordEvent.ResetEmailSent)
                }
                is Result.Error -> {
                    _uiState.value = ForgotPasswordUiState.Error(
                        result.message ?: "Failed to send password reset email"
                    )
                }
                else -> {
                    _uiState.value = ForgotPasswordUiState.Error("Unknown error")
                }
            }
        }
    }
    
    /**
     * Clear errors
     */
    fun clearErrors() {
        if (_uiState.value is ForgotPasswordUiState.Error) {
            _uiState.value = ForgotPasswordUiState.Initial
        }
    }
}

/**
 * UI state for the forgot password screen
 */
sealed class ForgotPasswordUiState {
    object Initial : ForgotPasswordUiState()
    object Loading : ForgotPasswordUiState()
    object Success : ForgotPasswordUiState()
    data class Error(val message: String) : ForgotPasswordUiState()
}

/**
 * Events emitted by the forgot password screen
 */
sealed class ForgotPasswordEvent {
    object ResetEmailSent : ForgotPasswordEvent()
    object NavigateBack : ForgotPasswordEvent()
    data class ValidationError(val message: String) : ForgotPasswordEvent()
}