package com.kilagee.onelove.ui

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kilagee.onelove.domain.repository.AuthRepository
import com.kilagee.onelove.domain.repository.PreferencesRepository
import com.kilagee.onelove.ui.navigation.NavDestinations
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for MainActivity
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {
    
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    // Deep linking intent
    private val _deepLinkIntent = MutableStateFlow<Intent?>(null)
    val deepLinkIntent: StateFlow<Intent?> = _deepLinkIntent.asStateFlow()
    
    // Theme preference
    val isDarkTheme = preferencesRepository.getThemePreference()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )
    
    // The start destination depends on authentication state
    // If the user is authenticated, start with Home
    // Otherwise, start with Login
    val startDestination: String
        get() = if (authRepository.isUserLoggedIn()) {
            NavDestinations.Home.route
        } else {
            NavDestinations.Login.route
        }
    
    init {
        // Check auth state and finish loading
        viewModelScope.launch {
            checkAuthState()
            _isLoading.value = false
        }
    }
    
    /**
     * Check authentication state
     */
    private suspend fun checkAuthState() {
        // Refresh auth token if needed
        authRepository.refreshToken()
    }
    
    /**
     * Update theme preference
     */
    fun updateTheme(isDark: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setThemePreference(isDark)
        }
    }
    
    /**
     * Handle deep link
     */
    fun handleDeepLink(intent: Intent) {
        _deepLinkIntent.value = intent
    }
}