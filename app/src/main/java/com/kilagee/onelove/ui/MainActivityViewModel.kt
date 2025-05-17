package com.kilagee.onelove.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseUser
import com.kilagee.onelove.domain.repository.AuthRepository
import com.kilagee.onelove.domain.repository.ChatRepository
import com.kilagee.onelove.domain.repository.NotificationRepository
import com.kilagee.onelove.domain.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Authentication state for the app
 */
sealed class AuthState {
    data object Loading : AuthState()
    data class Authenticated(val user: FirebaseUser) : AuthState()
    data object Unauthenticated : AuthState()
    data class Error(val message: String) : AuthState()
}

/**
 * ViewModel for the MainActivity
 * 
 * Handles:
 * - Authentication state
 * - Unread message counts
 * - Notification handling
 */
@HiltViewModel
class MainActivityViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val chatRepository: ChatRepository,
    private val notificationRepository: NotificationRepository
) : ViewModel() {

    // Authentication state
    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()
    
    // Unread message count
    private val _unreadMessageCount = MutableStateFlow(0)
    val unreadMessageCount: StateFlow<Int> = _unreadMessageCount.asStateFlow()
    
    init {
        observeAuthState()
        observeUnreadMessages()
        registerDeviceForNotifications()
    }
    
    /**
     * Observe Firebase Auth state changes
     */
    private fun observeAuthState() {
        viewModelScope.launch {
            authRepository.getCurrentUser().collectLatest { result ->
                when (result) {
                    is Result.Loading -> {
                        _authState.update { AuthState.Loading }
                    }
                    is Result.Success -> {
                        val user = result.data
                        if (user != null) {
                            _authState.update { AuthState.Authenticated(user) }
                        } else {
                            _authState.update { AuthState.Unauthenticated }
                        }
                    }
                    is Result.Error -> {
                        Timber.e("Auth error: ${result.message}")
                        _authState.update { AuthState.Error(result.message ?: "Authentication error") }
                    }
                }
            }
        }
    }
    
    /**
     * Observe unread message count
     */
    private fun observeUnreadMessages() {
        viewModelScope.launch {
            chatRepository.getUnreadMessageCount().collectLatest { result ->
                when (result) {
                    is Result.Success -> {
                        _unreadMessageCount.value = result.data
                    }
                    is Result.Error -> {
                        Timber.e("Error getting unread message count: ${result.message}")
                    }
                    is Result.Loading -> {
                        // Do nothing while loading
                    }
                }
            }
        }
    }
    
    /**
     * Register device for push notifications
     */
    private fun registerDeviceForNotifications() {
        viewModelScope.launch {
            when (val result = notificationRepository.registerDevice()) {
                is Result.Success -> {
                    Timber.d("Device registered for notifications")
                }
                is Result.Error -> {
                    Timber.e("Error registering device for notifications: ${result.message}")
                }
                is Result.Loading -> {
                    // Do nothing while loading
                }
            }
        }
    }
    
    /**
     * Sign out the current user
     */
    fun signOut() {
        viewModelScope.launch {
            when (val result = authRepository.signOut()) {
                is Result.Success -> {
                    _authState.update { AuthState.Unauthenticated }
                }
                is Result.Error -> {
                    Timber.e("Error signing out: ${result.message}")
                }
                is Result.Loading -> {
                    // Do nothing while loading
                }
            }
        }
    }
}