package com.kilagee.onelove.ui.screens.login

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kilagee.onelove.domain.repository.AuthRepository
import com.kilagee.onelove.domain.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Login screen
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    // UI state
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()
    
    // Form fields
    var email by mutableStateOf("")
        private set
    
    var password by mutableStateOf("")
        private set
    
    var isLoading by mutableStateOf(false)
        private set
    
    var errorMessage by mutableStateOf<String?>(null)
        private set
    
    var isAuthenticated by mutableStateOf(false)
        private set
    
    /**
     * Update email field
     */
    fun updateEmail(newEmail: String) {
        email = newEmail
        validateForm()
    }
    
    /**
     * Update password field
     */
    fun updatePassword(newPassword: String) {
        password = newPassword
        validateForm()
    }
    
    /**
     * Validate the login form
     */
    private fun validateForm() {
        _uiState.value = _uiState.value.copy(
            isEmailValid = isEmailValid(email),
            isPasswordValid = isPasswordValid(password),
            isFormValid = isEmailValid(email) && isPasswordValid(password)
        )
    }
    
    /**
     * Check if email is valid
     */
    private fun isEmailValid(email: String): Boolean {
        return email.isNotBlank() && email.contains("@") && email.contains(".")
    }
    
    /**
     * Check if password is valid
     */
    private fun isPasswordValid(password: String): Boolean {
        return password.length >= 6
    }
    
    /**
     * Sign in with email and password
     */
    fun signInWithEmailPassword() {
        viewModelScope.launch {
            try {
                isLoading = true
                errorMessage = null
                
                val result = authRepository.signInWithEmailPassword(email, password)
                
                when (result) {
                    is Result.Success -> {
                        isAuthenticated = true
                        _uiState.value = _uiState.value.copy(
                            isLoginSuccessful = true,
                            navigateToHome = true
                        )
                    }
                    is Result.Error -> {
                        errorMessage = result.message
                        _uiState.value = _uiState.value.copy(
                            isLoginSuccessful = false,
                            errorMessage = result.message
                        )
                    }
                    is Result.Loading -> {
                        // Handle loading state if needed
                    }
                }
            } catch (e: Exception) {
                errorMessage = e.message
                _uiState.value = _uiState.value.copy(
                    isLoginSuccessful = false,
                    errorMessage = e.message ?: "An unknown error occurred"
                )
            } finally {
                isLoading = false
            }
        }
    }
    
    /**
     * Sign in with Google
     */
    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            try {
                isLoading = true
                errorMessage = null
                
                val result = authRepository.signInWithGoogle(idToken)
                
                when (result) {
                    is Result.Success -> {
                        isAuthenticated = true
                        _uiState.value = _uiState.value.copy(
                            isLoginSuccessful = true,
                            navigateToHome = true
                        )
                    }
                    is Result.Error -> {
                        errorMessage = result.message
                        _uiState.value = _uiState.value.copy(
                            isLoginSuccessful = false,
                            errorMessage = result.message
                        )
                    }
                    is Result.Loading -> {
                        // Handle loading state if needed
                    }
                }
            } catch (e: Exception) {
                errorMessage = e.message
                _uiState.value = _uiState.value.copy(
                    isLoginSuccessful = false,
                    errorMessage = e.message ?: "An unknown error occurred"
                )
            } finally {
                isLoading = false
            }
        }
    }
    
    /**
     * Send password reset email
     */
    fun resetPassword(email: String) {
        viewModelScope.launch {
            try {
                isLoading = true
                errorMessage = null
                
                val result = authRepository.resetPassword(email)
                
                when (result) {
                    is Result.Success -> {
                        _uiState.value = _uiState.value.copy(
                            isPasswordResetEmailSent = true,
                            errorMessage = null
                        )
                    }
                    is Result.Error -> {
                        errorMessage = result.message
                        _uiState.value = _uiState.value.copy(
                            isPasswordResetEmailSent = false,
                            errorMessage = result.message
                        )
                    }
                    is Result.Loading -> {
                        // Handle loading state if needed
                    }
                }
            } catch (e: Exception) {
                errorMessage = e.message
                _uiState.value = _uiState.value.copy(
                    isPasswordResetEmailSent = false,
                    errorMessage = e.message ?: "An unknown error occurred"
                )
            } finally {
                isLoading = false
            }
        }
    }
    
    /**
     * Reset navigation flags
     */
    fun resetNavigation() {
        _uiState.value = _uiState.value.copy(
            navigateToHome = false
        )
    }
    
    /**
     * Reset error message
     */
    fun resetErrorMessage() {
        errorMessage = null
        _uiState.value = _uiState.value.copy(
            errorMessage = null
        )
    }
}

/**
 * UI state for the Login screen
 */
data class LoginUiState(
    val isEmailValid: Boolean = false,
    val isPasswordValid: Boolean = false,
    val isFormValid: Boolean = false,
    val isLoginSuccessful: Boolean = false,
    val isPasswordResetEmailSent: Boolean = false,
    val navigateToHome: Boolean = false,
    val errorMessage: String? = null
)