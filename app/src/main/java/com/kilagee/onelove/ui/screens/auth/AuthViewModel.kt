package com.kilagee.onelove.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kilagee.onelove.data.model.User
import com.kilagee.onelove.domain.repository.AuthRepository
import com.kilagee.onelove.domain.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for the Auth screens (Login/Register)
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    
    private val _loginState = MutableStateFlow<LoginState>(LoginState.Initial)
    val loginState: StateFlow<LoginState> = _loginState.asStateFlow()
    
    private val _registerState = MutableStateFlow<RegisterState>(RegisterState.Initial)
    val registerState: StateFlow<RegisterState> = _registerState.asStateFlow()
    
    /**
     * Login with email and password
     */
    fun login(email: String, password: String) {
        _loginState.value = LoginState.Loading
        
        viewModelScope.launch {
            try {
                when (val result = authRepository.signInWithEmailPassword(email, password)) {
                    is Result.Success -> {
                        _loginState.value = LoginState.Success(result.data)
                    }
                    is Result.Error -> {
                        _loginState.value = LoginState.Error(result.message)
                    }
                    is Result.Loading -> {
                        _loginState.value = LoginState.Loading
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Login error")
                _loginState.value = LoginState.Error(e.message ?: "Unknown error")
            }
        }
    }
    
    /**
     * Login with Google
     */
    fun loginWithGoogle(idToken: String) {
        _loginState.value = LoginState.Loading
        
        viewModelScope.launch {
            try {
                when (val result = authRepository.signInWithGoogle(idToken)) {
                    is Result.Success -> {
                        _loginState.value = LoginState.Success(result.data)
                    }
                    is Result.Error -> {
                        _loginState.value = LoginState.Error(result.message)
                    }
                    is Result.Loading -> {
                        _loginState.value = LoginState.Loading
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Google login error")
                _loginState.value = LoginState.Error(e.message ?: "Unknown error")
            }
        }
    }
    
    /**
     * Register with email, password, and name
     */
    fun register(name: String, email: String, password: String) {
        _registerState.value = RegisterState.Loading
        
        viewModelScope.launch {
            try {
                when (val result = authRepository.signUpWithEmailPassword(email, password, name)) {
                    is Result.Success -> {
                        _registerState.value = RegisterState.Success(result.data)
                    }
                    is Result.Error -> {
                        _registerState.value = RegisterState.Error(result.message)
                    }
                    is Result.Loading -> {
                        _registerState.value = RegisterState.Loading
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Register error")
                _registerState.value = RegisterState.Error(e.message ?: "Unknown error")
            }
        }
    }
    
    /**
     * Reset password
     */
    fun resetPassword(email: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                when (val result = authRepository.resetPassword(email)) {
                    is Result.Success -> {
                        onSuccess()
                    }
                    is Result.Error -> {
                        onError(result.message)
                    }
                    is Result.Loading -> {
                        // Do nothing
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Reset password error")
                onError(e.message ?: "Unknown error")
            }
        }
    }
    
    /**
     * Reset login state
     */
    fun resetLoginState() {
        _loginState.value = LoginState.Initial
    }
    
    /**
     * Reset register state
     */
    fun resetRegisterState() {
        _registerState.value = RegisterState.Initial
    }
}

/**
 * Login state sealed class
 */
sealed class LoginState {
    object Initial : LoginState()
    object Loading : LoginState()
    data class Success(val user: User) : LoginState()
    data class Error(val message: String) : LoginState()
}

/**
 * Register state sealed class
 */
sealed class RegisterState {
    object Initial : RegisterState()
    object Loading : RegisterState()
    data class Success(val user: User) : RegisterState()
    data class Error(val message: String) : RegisterState()
}