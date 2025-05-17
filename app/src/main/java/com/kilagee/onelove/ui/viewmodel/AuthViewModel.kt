package com.kilagee.onelove.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.AuthCredential
import com.kilagee.onelove.data.model.User
import com.kilagee.onelove.domain.repository.AuthRepository
import com.kilagee.onelove.domain.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Authentication view model
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    
    private val _authState = MutableStateFlow<AuthState>(AuthState.Initial)
    val authState: StateFlow<AuthState> = _authState
    
    private val _userProfile = MutableStateFlow<Result<User>>(Result.Loading)
    val userProfile: StateFlow<Result<User>> = _userProfile
    
    private val _isAuthenticated = MutableLiveData<Boolean>()
    val isAuthenticated: LiveData<Boolean> = _isAuthenticated
    
    init {
        checkAuthState()
        fetchUserProfile()
    }
    
    /**
     * Check the current authentication state
     */
    private fun checkAuthState() {
        viewModelScope.launch {
            authRepository.getCurrentUser()
                .catch { e ->
                    Timber.e(e, "Error checking auth state")
                    _authState.value = AuthState.Error(e.message ?: "Unknown error")
                    _isAuthenticated.value = false
                }
                .collect { user ->
                    if (user != null) {
                        _authState.value = AuthState.Authenticated(user.uid)
                        _isAuthenticated.value = true
                    } else {
                        _authState.value = AuthState.Unauthenticated
                        _isAuthenticated.value = false
                    }
                }
        }
    }
    
    /**
     * Fetch the current user's profile
     */
    private fun fetchUserProfile() {
        viewModelScope.launch {
            authRepository.getUserProfile()
                .catch { e ->
                    Timber.e(e, "Error fetching user profile")
                    _userProfile.value = Result.Error("Failed to load profile: ${e.message}")
                }
                .collect { result ->
                    _userProfile.value = result
                }
        }
    }
    
    /**
     * Sign in with email and password
     * 
     * @param email User's email
     * @param password User's password
     */
    fun signInWithEmailPassword(email: String, password: String) {
        viewModelScope.launch {
            try {
                _authState.value = AuthState.Loading
                val result = authRepository.signInWithEmailPassword(email, password)
                handleAuthResult(result)
            } catch (e: Exception) {
                Timber.e(e, "Error during sign in")
                _authState.value = AuthState.Error(e.message ?: "Sign in failed")
            }
        }
    }
    
    /**
     * Sign in with credential (e.g., Google, Facebook)
     * 
     * @param credential Auth credential
     */
    fun signInWithCredential(credential: AuthCredential) {
        viewModelScope.launch {
            try {
                _authState.value = AuthState.Loading
                val result = authRepository.signInWithCredential(credential)
                handleAuthResult(result)
            } catch (e: Exception) {
                Timber.e(e, "Error during sign in with credential")
                _authState.value = AuthState.Error(e.message ?: "Sign in failed")
            }
        }
    }
    
    /**
     * Sign up with email and password
     * 
     * @param email User's email
     * @param password User's password
     * @param name User's display name
     */
    fun signUpWithEmailPassword(email: String, password: String, name: String) {
        viewModelScope.launch {
            try {
                _authState.value = AuthState.Loading
                val result = authRepository.signUpWithEmailPassword(email, password, name)
                handleAuthResult(result)
            } catch (e: Exception) {
                Timber.e(e, "Error during sign up")
                _authState.value = AuthState.Error(e.message ?: "Sign up failed")
            }
        }
    }
    
    /**
     * Send password reset email
     * 
     * @param email User's email
     */
    fun sendPasswordResetEmail(email: String) {
        viewModelScope.launch {
            try {
                _authState.value = AuthState.Loading
                val result = authRepository.sendPasswordResetEmail(email)
                when (result) {
                    is Result.Success -> {
                        _authState.value = AuthState.PasswordResetEmailSent
                    }
                    is Result.Error -> {
                        _authState.value = AuthState.Error(result.message)
                    }
                    is Result.Loading -> {
                        // Do nothing, already in loading state
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error sending password reset email")
                _authState.value = AuthState.Error(e.message ?: "Failed to send password reset email")
            }
        }
    }
    
    /**
     * Sign out the current user
     */
    fun signOut() {
        viewModelScope.launch {
            try {
                val result = authRepository.signOut()
                when (result) {
                    is Result.Success -> {
                        _authState.value = AuthState.Unauthenticated
                        _isAuthenticated.value = false
                    }
                    is Result.Error -> {
                        _authState.value = AuthState.Error(result.message)
                    }
                    is Result.Loading -> {
                        _authState.value = AuthState.Loading
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error during sign out")
                _authState.value = AuthState.Error(e.message ?: "Sign out failed")
            }
        }
    }
    
    /**
     * Update user profile
     * 
     * @param user Updated user profile
     */
    fun updateUserProfile(user: User) {
        viewModelScope.launch {
            try {
                _userProfile.value = Result.Loading
                val result = authRepository.updateUserProfile(user)
                _userProfile.value = result
            } catch (e: Exception) {
                Timber.e(e, "Error updating user profile")
                _userProfile.value = Result.Error("Failed to update profile: ${e.message}")
            }
        }
    }
    
    /**
     * Delete the current user's account
     */
    fun deleteAccount() {
        viewModelScope.launch {
            try {
                _authState.value = AuthState.Loading
                val result = authRepository.deleteAccount()
                when (result) {
                    is Result.Success -> {
                        _authState.value = AuthState.Unauthenticated
                        _isAuthenticated.value = false
                    }
                    is Result.Error -> {
                        _authState.value = AuthState.Error(result.message)
                    }
                    is Result.Loading -> {
                        // Do nothing, already in loading state
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error deleting account")
                _authState.value = AuthState.Error(e.message ?: "Failed to delete account")
            }
        }
    }
    
    /**
     * Handle authentication result
     */
    private fun handleAuthResult(result: Result<com.google.firebase.auth.FirebaseUser>) {
        when (result) {
            is Result.Success -> {
                _authState.value = AuthState.Authenticated(result.data.uid)
                _isAuthenticated.value = true
                fetchUserProfile()
            }
            is Result.Error -> {
                _authState.value = AuthState.Error(result.message)
                _isAuthenticated.value = false
            }
            is Result.Loading -> {
                _authState.value = AuthState.Loading
            }
        }
    }
}

/**
 * Authentication state
 */
sealed class AuthState {
    object Initial : AuthState()
    object Loading : AuthState()
    data class Authenticated(val userId: String) : AuthState()
    object Unauthenticated : AuthState()
    object PasswordResetEmailSent : AuthState()
    data class Error(val message: String) : AuthState()
}