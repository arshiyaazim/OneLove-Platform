package com.kilagee.onelove.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kilagee.onelove.data.model.User
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
 * ViewModel for the registration screen
 */
@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    
    // UI state
    private val _uiState = MutableStateFlow<RegisterUiState>(RegisterUiState.Initial)
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()
    
    // One-time events
    private val _events = MutableSharedFlow<RegisterEvent>()
    val events: SharedFlow<RegisterEvent> = _events.asSharedFlow()
    
    // Form state
    private val _name = MutableStateFlow("")
    val name: StateFlow<String> = _name.asStateFlow()
    
    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email.asStateFlow()
    
    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()
    
    private val _confirmPassword = MutableStateFlow("")
    val confirmPassword: StateFlow<String> = _confirmPassword.asStateFlow()
    
    private val _phoneNumber = MutableStateFlow("")
    val phoneNumber: StateFlow<String> = _phoneNumber.asStateFlow()
    
    private val _acceptedTerms = MutableStateFlow(false)
    val acceptedTerms: StateFlow<Boolean> = _acceptedTerms.asStateFlow()
    
    // Validation state
    private val _isNameValid = MutableStateFlow(true)
    val isNameValid: StateFlow<Boolean> = _isNameValid.asStateFlow()
    
    private val _isEmailValid = MutableStateFlow(true)
    val isEmailValid: StateFlow<Boolean> = _isEmailValid.asStateFlow()
    
    private val _isPasswordValid = MutableStateFlow(true)
    val isPasswordValid: StateFlow<Boolean> = _isPasswordValid.asStateFlow()
    
    private val _isConfirmPasswordValid = MutableStateFlow(true)
    val isConfirmPasswordValid: StateFlow<Boolean> = _isConfirmPasswordValid.asStateFlow()
    
    private val _isPhoneNumberValid = MutableStateFlow(true)
    val isPhoneNumberValid: StateFlow<Boolean> = _isPhoneNumberValid.asStateFlow()
    
    /**
     * Update name field
     */
    fun updateName(name: String) {
        _name.value = name
        validateName()
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
        validateConfirmPassword()
    }
    
    /**
     * Update confirm password field
     */
    fun updateConfirmPassword(confirmPassword: String) {
        _confirmPassword.value = confirmPassword
        validateConfirmPassword()
    }
    
    /**
     * Update phone number field
     */
    fun updatePhoneNumber(phoneNumber: String) {
        _phoneNumber.value = phoneNumber
        validatePhoneNumber()
    }
    
    /**
     * Toggle terms acceptance
     */
    fun toggleTermsAcceptance() {
        _acceptedTerms.value = !_acceptedTerms.value
    }
    
    /**
     * Validate name format
     */
    private fun validateName() {
        _isNameValid.value = _name.value.length >= 2
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
        // Password should be at least 8 characters with at least one letter and one number
        val passwordPattern = "^(?=.*[0-9])(?=.*[a-zA-Z]).{8,}$".toRegex()
        _isPasswordValid.value = passwordPattern.matches(_password.value)
    }
    
    /**
     * Validate confirm password matches
     */
    private fun validateConfirmPassword() {
        _isConfirmPasswordValid.value = _password.value == _confirmPassword.value
    }
    
    /**
     * Validate phone number format
     */
    private fun validatePhoneNumber() {
        // Simple validation, can be enhanced with country-specific validation
        _isPhoneNumberValid.value = _phoneNumber.value.isEmpty() || 
                android.util.Patterns.PHONE.matcher(_phoneNumber.value).matches()
    }
    
    /**
     * Validate all fields
     */
    private fun validateAllFields(): Boolean {
        validateName()
        validateEmail()
        validatePassword()
        validateConfirmPassword()
        validatePhoneNumber()
        
        return _isNameValid.value && 
                _isEmailValid.value && 
                _isPasswordValid.value && 
                _isConfirmPasswordValid.value && 
                _isPhoneNumberValid.value &&
                _acceptedTerms.value
    }
    
    /**
     * Register user
     */
    fun register() {
        if (!validateAllFields()) {
            viewModelScope.launch {
                _events.emit(RegisterEvent.ValidationError("Please correct the errors in the form"))
            }
            return
        }
        
        _uiState.value = RegisterUiState.Loading
        
        viewModelScope.launch {
            val result = authRepository.signUpWithEmailAndPassword(
                email = _email.value,
                password = _password.value,
                displayName = _name.value,
                phoneNumber = if (_phoneNumber.value.isNotEmpty()) _phoneNumber.value else null
            )
            
            when (result) {
                is Result.Success -> {
                    // Send email verification
                    authRepository.sendEmailVerification()
                    
                    _uiState.value = RegisterUiState.Success(result.data)
                    _events.emit(RegisterEvent.NavigateToHome)
                }
                is Result.Error -> {
                    _uiState.value = RegisterUiState.Error(result.message ?: "Registration failed")
                }
                else -> {
                    _uiState.value = RegisterUiState.Error("Unknown error")
                }
            }
        }
    }
    
    /**
     * Register with Google
     */
    fun registerWithGoogle(idToken: String) {
        _uiState.value = RegisterUiState.Loading
        
        viewModelScope.launch {
            val result = authRepository.signInWithGoogle(idToken)
            
            when (result) {
                is Result.Success -> {
                    _uiState.value = RegisterUiState.Success(result.data)
                    _events.emit(RegisterEvent.NavigateToHome)
                }
                is Result.Error -> {
                    _uiState.value = RegisterUiState.Error(result.message ?: "Google registration failed")
                }
                else -> {
                    _uiState.value = RegisterUiState.Error("Unknown error")
                }
            }
        }
    }
    
    /**
     * Register with Facebook
     */
    fun registerWithFacebook(accessToken: String) {
        _uiState.value = RegisterUiState.Loading
        
        viewModelScope.launch {
            val result = authRepository.signInWithFacebook(accessToken)
            
            when (result) {
                is Result.Success -> {
                    _uiState.value = RegisterUiState.Success(result.data)
                    _events.emit(RegisterEvent.NavigateToHome)
                }
                is Result.Error -> {
                    _uiState.value = RegisterUiState.Error(result.message ?: "Facebook registration failed")
                }
                else -> {
                    _uiState.value = RegisterUiState.Error("Unknown error")
                }
            }
        }
    }
    
    /**
     * Clear errors
     */
    fun clearErrors() {
        if (_uiState.value is RegisterUiState.Error) {
            _uiState.value = RegisterUiState.Initial
        }
    }
}

/**
 * UI state for the registration screen
 */
sealed class RegisterUiState {
    object Initial : RegisterUiState()
    object Loading : RegisterUiState()
    data class Success(val user: User) : RegisterUiState()
    data class Error(val message: String) : RegisterUiState()
}

/**
 * Events emitted by the registration screen
 */
sealed class RegisterEvent {
    object NavigateToHome : RegisterEvent()
    object NavigateToLogin : RegisterEvent()
    data class ValidationError(val message: String) : RegisterEvent()
}