package com.kilagee.onelove.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kilagee.onelove.data.model.User
import com.kilagee.onelove.data.model.VerificationRequest
import com.kilagee.onelove.domain.repository.AuthRepository
import com.kilagee.onelove.domain.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import javax.inject.Inject

/**
 * Profile view model
 */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    
    private val _currentUserProfile = MutableStateFlow<Result<User>>(Result.Loading)
    val currentUserProfile: StateFlow<Result<User>> = _currentUserProfile
    
    private val _viewedUserProfile = MutableStateFlow<Result<User?>>(Result.Loading)
    val viewedUserProfile: StateFlow<Result<User?>> = _viewedUserProfile
    
    private val _profileUpdateState = MutableStateFlow<ProfileUpdateState>(ProfileUpdateState.Idle)
    val profileUpdateState: StateFlow<ProfileUpdateState> = _profileUpdateState
    
    private val _verificationState = MutableStateFlow<VerificationState>(VerificationState.Idle)
    val verificationState: StateFlow<VerificationState> = _verificationState
    
    private val _profilePictureUploadState = MutableStateFlow<ProfilePictureUploadState>(ProfilePictureUploadState.Idle)
    val profilePictureUploadState: StateFlow<ProfilePictureUploadState> = _profilePictureUploadState
    
    private val _viewedUserId = MutableStateFlow<String?>(null)
    
    init {
        loadCurrentUserProfile()
    }
    
    /**
     * Load current user profile
     */
    fun loadCurrentUserProfile() {
        viewModelScope.launch {
            authRepository.getUserProfile()
                .catch { e ->
                    Timber.e(e, "Error loading current user profile")
                    _currentUserProfile.value = Result.Error("Failed to load profile: ${e.message}")
                }
                .collect { result ->
                    _currentUserProfile.value = result
                }
        }
    }
    
    /**
     * Load profile for a specific user
     * 
     * @param userId ID of the user
     */
    fun loadUserProfile(userId: String) {
        _viewedUserId.value = userId
        
        viewModelScope.launch {
            authRepository.getUserProfile(userId)
                .catch { e ->
                    Timber.e(e, "Error loading user profile")
                    _viewedUserProfile.value = Result.Error("Failed to load profile: ${e.message}")
                }
                .collect { result ->
                    _viewedUserProfile.value = result
                }
        }
    }
    
    /**
     * Clear viewed user profile
     */
    fun clearViewedUserProfile() {
        _viewedUserId.value = null
        _viewedUserProfile.value = Result.Success(null)
    }
    
    /**
     * Update user profile
     * 
     * @param user Updated user profile
     */
    fun updateUserProfile(user: User) {
        viewModelScope.launch {
            try {
                _profileUpdateState.value = ProfileUpdateState.Updating
                val result = authRepository.updateUserProfile(user)
                
                when (result) {
                    is Result.Success -> {
                        _profileUpdateState.value = ProfileUpdateState.Updated
                        // Update current user profile
                        _currentUserProfile.value = Result.Success(result.data)
                    }
                    is Result.Error -> {
                        _profileUpdateState.value = ProfileUpdateState.Error(result.message)
                    }
                    is Result.Loading -> {
                        _profileUpdateState.value = ProfileUpdateState.Updating
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error updating user profile")
                _profileUpdateState.value = ProfileUpdateState.Error(e.message ?: "Failed to update profile")
            } finally {
                // Reset state after a delay
                kotlinx.coroutines.delay(2000)
                _profileUpdateState.value = ProfileUpdateState.Idle
            }
        }
    }
    
    /**
     * Submit a verification request
     * 
     * @param request Verification request
     */
    fun submitVerificationRequest(request: VerificationRequest) {
        viewModelScope.launch {
            try {
                _verificationState.value = VerificationState.Submitting
                
                // In a real implementation, there would be a verification repository
                // For now, we just pretend to submit the request
                kotlinx.coroutines.delay(1000)
                
                _verificationState.value = VerificationState.Submitted
                
                // Update user profile
                loadCurrentUserProfile()
            } catch (e: Exception) {
                Timber.e(e, "Error submitting verification request")
                _verificationState.value = VerificationState.Error(e.message ?: "Failed to submit verification request")
            } finally {
                // Reset state after a delay
                kotlinx.coroutines.delay(2000)
                _verificationState.value = VerificationState.Idle
            }
        }
    }
    
    /**
     * Upload a profile picture
     * 
     * @param file Image file
     */
    fun uploadProfilePicture(file: File) {
        viewModelScope.launch {
            try {
                _profilePictureUploadState.value = ProfilePictureUploadState.Uploading(0f)
                
                // In a real implementation, there would be a file upload repository
                // For now, we just pretend to upload the file with artificial progress
                for (i in 1..10) {
                    kotlinx.coroutines.delay(300)
                    _profilePictureUploadState.value = ProfilePictureUploadState.Uploading(i / 10f)
                }
                
                _profilePictureUploadState.value = ProfilePictureUploadState.Success("https://example.com/profile.jpg")
                
                // Update current user profile to include the new profile picture URL
                _currentUserProfile.value.getOrNull()?.let { currentUser ->
                    val updatedUser = currentUser.copy(
                        profilePictureUrl = "https://example.com/profile.jpg"
                    )
                    updateUserProfile(updatedUser)
                }
            } catch (e: Exception) {
                Timber.e(e, "Error uploading profile picture")
                _profilePictureUploadState.value = ProfilePictureUploadState.Error(e.message ?: "Failed to upload profile picture")
            } finally {
                // Reset state after a delay
                kotlinx.coroutines.delay(2000)
                _profilePictureUploadState.value = ProfilePictureUploadState.Idle
            }
        }
    }
    
    /**
     * Update user location
     * 
     * @param latitude Latitude
     * @param longitude Longitude
     * @param locationName Location name
     */
    fun updateLocation(latitude: Double, longitude: Double, locationName: String) {
        _currentUserProfile.value.getOrNull()?.let { currentUser ->
            val updatedUser = currentUser.copy(
                latitude = latitude,
                longitude = longitude,
                location = locationName
            )
            updateUserProfile(updatedUser)
        }
    }
    
    /**
     * Update user interests
     * 
     * @param interests List of interests
     */
    fun updateInterests(interests: List<String>) {
        _currentUserProfile.value.getOrNull()?.let { currentUser ->
            val updatedUser = currentUser.copy(
                interests = interests
            )
            updateUserProfile(updatedUser)
        }
    }
    
    /**
     * Update user preferences
     * 
     * @param minAge Minimum age preference
     * @param maxAge Maximum age preference
     * @param maxDistance Maximum distance preference
     */
    fun updatePreferences(minAge: Int?, maxAge: Int?, maxDistance: Int?) {
        _currentUserProfile.value.getOrNull()?.let { currentUser ->
            val updatedUser = currentUser.copy(
                minAgePreference = minAge,
                maxAgePreference = maxAge,
                maxDistance = maxDistance
            )
            updateUserProfile(updatedUser)
        }
    }
}

/**
 * Profile update state
 */
sealed class ProfileUpdateState {
    object Idle : ProfileUpdateState()
    object Updating : ProfileUpdateState()
    object Updated : ProfileUpdateState()
    data class Error(val message: String) : ProfileUpdateState()
}

/**
 * Verification state
 */
sealed class VerificationState {
    object Idle : VerificationState()
    object Submitting : VerificationState()
    object Submitted : VerificationState()
    data class Error(val message: String) : VerificationState()
}

/**
 * Profile picture upload state
 */
sealed class ProfilePictureUploadState {
    object Idle : ProfilePictureUploadState()
    data class Uploading(val progress: Float) : ProfilePictureUploadState()
    data class Success(val url: String) : ProfilePictureUploadState()
    data class Error(val message: String) : ProfilePictureUploadState()
}