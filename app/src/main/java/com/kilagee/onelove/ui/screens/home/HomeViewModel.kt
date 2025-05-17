package com.kilagee.onelove.ui.screens.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kilagee.onelove.domain.model.UserDomain
import com.kilagee.onelove.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Home screen
 * Handles discovery and profile swiping
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    // UI state
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
    // Properties
    var isLoading by mutableStateOf(false)
        private set
    
    var errorMessage by mutableStateOf<String?>(null)
        private set
    
    init {
        loadDiscoveryProfiles()
    }
    
    /**
     * Load profiles for discovery
     */
    fun loadDiscoveryProfiles() {
        viewModelScope.launch {
            try {
                isLoading = true
                errorMessage = null
                
                // Fetch profiles based on user preferences
                val profiles = userRepository.getDiscoveryProfiles()
                
                _uiState.value = HomeUiState(
                    profiles = profiles
                )
            } catch (e: Exception) {
                errorMessage = e.message
            } finally {
                isLoading = false
            }
        }
    }
    
    /**
     * Like a profile
     */
    fun likeProfile(profileId: String, isSuperLike: Boolean = false) {
        viewModelScope.launch {
            try {
                userRepository.likeProfile(profileId, isSuperLike)
                
                // Remove the liked profile from the list
                val updatedProfiles = _uiState.value.profiles.filterNot { it.id == profileId }
                _uiState.value = _uiState.value.copy(profiles = updatedProfiles)
                
                // Check if this created a match
                checkForMatch(profileId)
            } catch (e: Exception) {
                errorMessage = e.message
            }
        }
    }
    
    /**
     * Skip a profile
     */
    fun skipProfile(profileId: String, reason: String? = null) {
        viewModelScope.launch {
            try {
                userRepository.skipProfile(profileId, reason)
                
                // Remove the skipped profile from the list
                val updatedProfiles = _uiState.value.profiles.filterNot { it.id == profileId }
                _uiState.value = _uiState.value.copy(profiles = updatedProfiles)
            } catch (e: Exception) {
                errorMessage = e.message
            }
        }
    }
    
    /**
     * Check if there is a match after liking a profile
     */
    private suspend fun checkForMatch(profileId: String) {
        try {
            val isMatch = userRepository.checkForMatch(profileId)
            
            if (isMatch) {
                _uiState.value = _uiState.value.copy(
                    matchedProfileId = profileId,
                    showMatchAnimation = true
                )
            }
        } catch (e: Exception) {
            errorMessage = e.message
        }
    }
    
    /**
     * Reset match animation state
     */
    fun resetMatchAnimation() {
        _uiState.value = _uiState.value.copy(
            matchedProfileId = null,
            showMatchAnimation = false
        )
    }
}

/**
 * UI state for the Home screen
 */
data class HomeUiState(
    val profiles: List<UserDomain> = emptyList(),
    val matchedProfileId: String? = null,
    val showMatchAnimation: Boolean = false
)