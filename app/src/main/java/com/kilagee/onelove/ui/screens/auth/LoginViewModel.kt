package com.kilagee.onelove.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kilagee.onelove.data.model.User
import com.kilagee.onelove.domain.repository.AuthRepository
import com.kilagee.onelove.domain.repository.PreferencesRepository
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
 * ViewModel for the login screen
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {
    
    // UI state
    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Initial)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()
    
    // One-time events
    private val _events = MutableSharedFlow<LoginEvent>()
    val events: SharedFlow<LoginEvent> = _events.asSharedFlow()
    
    // Form state
    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email.asStateFlow()
    
    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()
    
    private val _rememberMe = MutableStateFlow(false)
    val rememberMe: StateFlow<Boolean> = _rememberMe.asStateFlow()
    
    private val _isEmailValid = MutableStateFlow(true)
    val isEmailValid: StateFlow<Boolean> = _isEmailValid.asStateFlow()
    
    private val _isPasswordValid = MutableStateFlow(true)
    val isPasswordValid: StateFlow<Boolean> = _isPasswordValid.asStateFlow()
    
    init {
        // Check if we have saved credentials
        viewModelScope.launch {
            val credentials = preferencesRepository.getSavedLoginCredentials()
            if (credentials != null) {
                _email.value = credentials.email
                _password.value = credentials.password
                _rememberMe.value = true
            }
        }
    }
    
    /**
     * Update email field
     */
    fun updateEmail(email: String) {
        _email.value = email
        validateEmail()
    }
    
    /**
     * Update password field
     */
    fun updatePassword(password: String) {
        _password.value = password
        validatePassword()
    }
    
    /**
     * Toggle remember me
     */
    fun toggleRememberMe() {
        _rememberMe.value = !_rememberMe.value
    }
    
    /**
     * Validate email format
     */
    private fun validateEmail() {
        _isEmailValid.value = android.util.Patterns.EMAIL_ADDRESS.matcher(_email.value).matches()
    }
    
    /**
     * Validate password format
     */
    private fun validatePassword() {
        _isPasswordValid.value = _password.value.length >= 6
    }
    
    /**
     * Login with email and password
     */
    fun login() {
        validateEmail()
        validatePassword()
        
        if (!_isEmailValid.value || !_isPasswordValid.value) {
            viewModelScope.launch {
                _events.emit(LoginEvent.ValidationError("Please correct the errors in the form"))
            }
            return
        }
        
        _uiState.value = LoginUiState.Loading
        
        viewModelScope.launch {
            val result = authRepository.signInWithEmailAndPassword(_email.value, _password.value)
            
            when (result) {
                is Result.Success -> {
                    // Save credentials if remember me is checked
                    if (_rememberMe.value) {
                        preferencesRepository.saveLoginCredentials(
                            com.kilagee.onelove.domain.repository.LoginCredentials(
                                email = _email.value,
                                password = _password.value
                            )
                        )
                    } else {
                        preferencesRepository.clearLoginCredentials()
                    }
                    
                    _uiState.value = LoginUiState.Success(result.data)
                    _events.emit(LoginEvent.NavigateToHome)
                }
                is Result.Error -> {
                    _uiState.value = LoginUiState.Error(result.message ?: "Authentication failed")
                }
                else -> {
                    _uiState.value = LoginUiState.Error("Unknown error")
                }
            }
        }
    }
    
    /**
     * Login with Google
     */
    fun loginWithGoogle(idToken: String) {
        _uiState.value = LoginUiState.Loading
        
        viewModelScope.launch {
            val result = authRepository.signInWithGoogle(idToken)
            
            when (result) {
                is Result.Success -> {
                    _uiState.value = LoginUiState.Success(result.data)
                    _events.emit(LoginEvent.NavigateToHome)
                }
                is Result.Error -> {
                    _uiState.value = LoginUiState.Error(result.message ?: "Google authentication failed")
                }
                else -> {
                    _uiState.value = LoginUiState.Error("Unknown error")
                }
            }
        }
    }
    
    /**
     * Login with Facebook
     */
    fun loginWithFacebook(accessToken: String) {
        _uiState.value = LoginUiState.Loading
        
        viewModelScope.launch {
            val result = authRepository.signInWithFacebook(accessToken)
            
            when (result) {
                is Result.Success -> {
                    _uiState.value = LoginUiState.Success(result.data)
                    _events.emit(LoginEvent.NavigateToHome)
                }
                is Result.Error -> {
                    _uiState.value = LoginUiState.Error(result.message ?: "Facebook authentication failed")
                }
                else -> {
                    _uiState.value = LoginUiState.Error("Unknown error")
                }
            }
        }
    }
    
    /**
     * Login as guest
     */
    fun loginAsGuest() {
        _uiState.value = LoginUiState.Loading
        
        viewModelScope.launch {
            val result = authRepository.signInAnonymously()
            
            when (result) {
                is Result.Success -> {
                    _uiState.value = LoginUiState.Success(result.data)
                    _events.emit(LoginEvent.NavigateToHome)
                }
                is Result.Error -> {
                    _uiState.value = LoginUiState.Error(result.message ?: "Guest login failed")
                }
                else -> {
                    _uiState.value = LoginUiState.Error("Unknown error")
                }
            }
        }
    }
    
    /**
     * Clear errors
     */
    fun clearErrors() {
        if (_uiState.value is LoginUiState.Error) {
            _uiState.value = LoginUiState.Initial
        }
    }
}

/**
 * UI state for the login screen
 */
sealed class LoginUiState {
    object Initial : LoginUiState()
    object Loading : LoginUiState()
    data class Success(val user: User) : LoginUiState()
    data class Error(val message: String) : LoginUiState()
}

/**
 * Events emitted by the login screen
 */
sealed class LoginEvent {
    object NavigateToHome : LoginEvent()
    object NavigateToRegister : LoginEvent()
    object NavigateToForgotPassword : LoginEvent()
    data class ValidationError(val message: String) : LoginEvent()
}