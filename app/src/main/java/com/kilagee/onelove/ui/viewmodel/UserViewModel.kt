package com.kilagee.onelove.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kilagee.onelove.data.model.GeoLocation
import com.kilagee.onelove.data.model.RegionalSettings
import com.kilagee.onelove.data.model.Result
import com.kilagee.onelove.data.model.User
import com.kilagee.onelove.data.model.UserGender
import com.kilagee.onelove.data.model.UserPreference
import com.kilagee.onelove.data.model.VerificationStatus
import com.kilagee.onelove.data.repository.AuthRepository
import com.kilagee.onelove.data.repository.StorageRepository
import com.kilagee.onelove.data.repository.UserRepository
import com.kilagee.onelove.util.AppError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Date
import javax.inject.Inject

/**
 * Sealed class representing profile UI state
 */
sealed class ProfileUiState {
    object Initial : ProfileUiState()
    object Loading : ProfileUiState()
    data class Error(val error: AppError, val message: String) : ProfileUiState()
    data class Success<T>(val data: T) : ProfileUiState()
}

/**
 * User profile operation types
 */
enum class ProfileOperation {
    NONE,
    FETCH_PROFILE,
    UPDATE_PROFILE,
    UPDATE_PHOTO,
    UPDATE_LOCATION,
    ADD_GALLERY_PHOTO,
    REMOVE_GALLERY_PHOTO,
    BLOCK_USER,
    REPORT_USER,
    VERIFY_USER,
    SEARCH_USERS,
    GET_NEARBY_USERS,
    GET_RECOMMENDED_MATCHES
}

/**
 * ViewModel for user profile operations
 */
@HiltViewModel
class UserViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
    private val storageRepository: StorageRepository
) : ViewModel() {
    
    // Current profile UI state
    private val _profileUiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Initial)
    val profileUiState: StateFlow<ProfileUiState> = _profileUiState.asStateFlow()
    
    // Current profile operation
    private val _currentOperation = MutableStateFlow(ProfileOperation.NONE)
    val currentOperation: StateFlow<ProfileOperation> = _currentOperation.asStateFlow()
    
    // Current user data
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()
    
    // Selected user (when viewing other profiles)
    private val _selectedUser = MutableStateFlow<User?>(null)
    val selectedUser: StateFlow<User?> = _selectedUser.asStateFlow()
    
    // Recommended users
    private val _recommendedUsers = MutableStateFlow<List<User>>(emptyList())
    val recommendedUsers: StateFlow<List<User>> = _recommendedUsers.asStateFlow()
    
    // Nearby users
    private val _nearbyUsers = MutableStateFlow<List<User>>(emptyList())
    val nearbyUsers: StateFlow<List<User>> = _nearbyUsers.asStateFlow()
    
    // Search results
    private val _searchResults = MutableStateFlow<List<User>>(emptyList())
    val searchResults: StateFlow<List<User>> = _searchResults.asStateFlow()
    
    // Photo upload progress
    private val _uploadProgress = MutableStateFlow(0f)
    val uploadProgress: StateFlow<Float> = _uploadProgress.asStateFlow()
    
    // Verification status
    private val _verificationStatus = MutableStateFlow<VerificationStatus?>(null)
    val verificationStatus: StateFlow<VerificationStatus?> = _verificationStatus.asStateFlow()
    
    init {
        // Listen for current user changes
        viewModelScope.launch {
            userRepository.getCurrentUserFlow().collectLatest { user ->
                _currentUser.value = user
                user?.let {
                    _verificationStatus.value = it.verificationStatus
                }
            }
        }
        
        // Fetch current user data initially
        fetchCurrentUserProfile()
    }
    
    /**
     * Fetch current user profile
     */
    fun fetchCurrentUserProfile() {
        viewModelScope.launch {
            _currentOperation.value = ProfileOperation.FETCH_PROFILE
            _profileUiState.value = ProfileUiState.Loading
            
            when (val result = userRepository.getCurrentUser()) {
                is Result.Success -> {
                    _currentUser.value = result.data
                    _verificationStatus.value = result.data.verificationStatus
                    _profileUiState.value = ProfileUiState.Success(result.data)
                }
                is Result.Error -> {
                    _profileUiState.value = ProfileUiState.Error(result.error, result.error.message)
                }
            }
            
            _currentOperation.value = ProfileOperation.NONE
        }
    }
    
    /**
     * Get user by ID
     */
    fun getUserById(userId: String) {
        viewModelScope.launch {
            _currentOperation.value = ProfileOperation.FETCH_PROFILE
            _profileUiState.value = ProfileUiState.Loading
            
            when (val result = userRepository.getUserById(userId)) {
                is Result.Success -> {
                    _selectedUser.value = result.data
                    _profileUiState.value = ProfileUiState.Success(result.data)
                }
                is Result.Error -> {
                    _profileUiState.value = ProfileUiState.Error(result.error, result.error.message)
                }
            }
            
            _currentOperation.value = ProfileOperation.NONE
        }
    }
    
    /**
     * Update user profile
     */
    fun updateUserProfile(
        displayName: String? = null,
        bio: String? = null,
        gender: UserGender? = null,
        genderPreferences: List<UserGender>? = null,
        birthDate: Date? = null,
        interests: List<String>? = null,
        languages: List<String>? = null,
        occupation: String? = null,
        education: String? = null
    ) {
        viewModelScope.launch {
            _currentOperation.value = ProfileOperation.UPDATE_PROFILE
            _profileUiState.value = ProfileUiState.Loading
            
            val userId = authRepository.getCurrentUserId() ?: return@launch
            val currentUser = _currentUser.value ?: return@launch
            
            // Update individual fields
            var updatedUser = currentUser
            
            // Create an updated user with the given fields
            try {
                // Build an updated user
                if (displayName != null) {
                    when (val result = userRepository.updateDisplayName(userId, displayName)) {
                        is Result.Success -> updatedUser = updatedUser.copy(displayName = displayName)
                        is Result.Error -> {
                            _profileUiState.value = ProfileUiState.Error(result.error, "Failed to update display name")
                            _currentOperation.value = ProfileOperation.NONE
                            return@launch
                        }
                    }
                }
                
                if (bio != null) {
                    when (val result = userRepository.updateBio(userId, bio)) {
                        is Result.Success -> updatedUser = updatedUser.copy(bio = bio)
                        is Result.Error -> {
                            _profileUiState.value = ProfileUiState.Error(result.error, "Failed to update bio")
                            _currentOperation.value = ProfileOperation.NONE
                            return@launch
                        }
                    }
                }
                
                if (gender != null) {
                    when (val result = userRepository.updateGender(userId, gender)) {
                        is Result.Success -> updatedUser = updatedUser.copy(gender = gender)
                        is Result.Error -> {
                            _profileUiState.value = ProfileUiState.Error(result.error, "Failed to update gender")
                            _currentOperation.value = ProfileOperation.NONE
                            return@launch
                        }
                    }
                }
                
                if (genderPreferences != null) {
                    when (val result = userRepository.updateGenderPreferences(userId, genderPreferences)) {
                        is Result.Success -> updatedUser = updatedUser.copy(genderPreference = genderPreferences)
                        is Result.Error -> {
                            _profileUiState.value = ProfileUiState.Error(result.error, "Failed to update gender preferences")
                            _currentOperation.value = ProfileOperation.NONE
                            return@launch
                        }
                    }
                }
                
                if (birthDate != null) {
                    when (val result = userRepository.updateBirthDate(userId, birthDate)) {
                        is Result.Success -> updatedUser = updatedUser.copy(birthDate = birthDate)
                        is Result.Error -> {
                            _profileUiState.value = ProfileUiState.Error(result.error, "Failed to update birth date")
                            _currentOperation.value = ProfileOperation.NONE
                            return@launch
                        }
                    }
                }
                
                if (interests != null) {
                    when (val result = userRepository.updateInterests(userId, interests)) {
                        is Result.Success -> updatedUser = updatedUser.copy(interests = interests)
                        is Result.Error -> {
                            _profileUiState.value = ProfileUiState.Error(result.error, "Failed to update interests")
                            _currentOperation.value = ProfileOperation.NONE
                            return@launch
                        }
                    }
                }
                
                if (languages != null) {
                    when (val result = userRepository.updateUserLanguages(userId, languages)) {
                        is Result.Success -> updatedUser = updatedUser.copy(languages = languages)
                        is Result.Error -> {
                            _profileUiState.value = ProfileUiState.Error(result.error, "Failed to update languages")
                            _currentOperation.value = ProfileOperation.NONE
                            return@launch
                        }
                    }
                }
                
                if (occupation != null) {
                    val extraData = currentUser.extraData.toMutableMap()
                    extraData["occupation"] = occupation
                    updatedUser = updatedUser.copy(occupation = occupation)
                }
                
                if (education != null) {
                    val extraData = currentUser.extraData.toMutableMap()
                    extraData["education"] = education
                    updatedUser = updatedUser.copy(education = education)
                }
                
                // Update the current user
                _currentUser.value = updatedUser
                _profileUiState.value = ProfileUiState.Success(updatedUser)
                
            } catch (e: Exception) {
                Timber.e(e, "Error updating user profile")
                _profileUiState.value = ProfileUiState.Error(
                    AppError.DataError.UpdateFailed("Failed to update profile: ${e.message}", e),
                    "Failed to update profile"
                )
            }
            
            _currentOperation.value = ProfileOperation.NONE
        }
    }
    
    /**
     * Update user profile photo
     */
    fun updateProfilePhoto(photoUri: Uri) {
        viewModelScope.launch {
            _currentOperation.value = ProfileOperation.UPDATE_PHOTO
            _profileUiState.value = ProfileUiState.Loading
            _uploadProgress.value = 0f
            
            val userId = authRepository.getCurrentUserId() ?: return@launch
            
            when (val result = userRepository.updateProfilePhoto(userId, photoUri)) {
                is Result.Success -> {
                    val updatedUrl = result.data
                    _currentUser.value = _currentUser.value?.copy(profilePhotoUrl = updatedUrl)
                    _uploadProgress.value = 1f
                    _profileUiState.value = ProfileUiState.Success(updatedUrl)
                }
                is Result.Error -> {
                    _profileUiState.value = ProfileUiState.Error(result.error, "Failed to update profile photo")
                }
            }
            
            _currentOperation.value = ProfileOperation.NONE
        }
    }
    
    /**
     * Update user cover photo
     */
    fun updateCoverPhoto(photoUri: Uri) {
        viewModelScope.launch {
            _currentOperation.value = ProfileOperation.UPDATE_PHOTO
            _profileUiState.value = ProfileUiState.Loading
            _uploadProgress.value = 0f
            
            val userId = authRepository.getCurrentUserId() ?: return@launch
            
            when (val result = userRepository.updateCoverPhoto(userId, photoUri)) {
                is Result.Success -> {
                    val updatedUrl = result.data
                    _currentUser.value = _currentUser.value?.copy(coverPhotoUrl = updatedUrl)
                    _uploadProgress.value = 1f
                    _profileUiState.value = ProfileUiState.Success(updatedUrl)
                }
                is Result.Error -> {
                    _profileUiState.value = ProfileUiState.Error(result.error, "Failed to update cover photo")
                }
            }
            
            _currentOperation.value = ProfileOperation.NONE
        }
    }
    
    /**
     * Add photo to gallery
     */
    fun addPhotoToGallery(photoUri: Uri) {
        viewModelScope.launch {
            _currentOperation.value = ProfileOperation.ADD_GALLERY_PHOTO
            _profileUiState.value = ProfileUiState.Loading
            _uploadProgress.value = 0f
            
            val userId = authRepository.getCurrentUserId() ?: return@launch
            val currentUser = _currentUser.value ?: return@launch
            
            when (val result = userRepository.addPhotoToGallery(userId, photoUri)) {
                is Result.Success -> {
                    val photoUrl = result.data
                    val updatedPhotos = currentUser.photos.toMutableList().apply {
                        add(photoUrl)
                    }
                    _currentUser.value = currentUser.copy(photos = updatedPhotos)
                    _uploadProgress.value = 1f
                    _profileUiState.value = ProfileUiState.Success(photoUrl)
                }
                is Result.Error -> {
                    _profileUiState.value = ProfileUiState.Error(result.error, "Failed to add photo to gallery")
                }
            }
            
            _currentOperation.value = ProfileOperation.NONE
        }
    }
    
    /**
     * Remove photo from gallery
     */
    fun removePhotoFromGallery(photoUrl: String) {
        viewModelScope.launch {
            _currentOperation.value = ProfileOperation.REMOVE_GALLERY_PHOTO
            _profileUiState.value = ProfileUiState.Loading
            
            val userId = authRepository.getCurrentUserId() ?: return@launch
            val currentUser = _currentUser.value ?: return@launch
            
            when (val result = userRepository.removePhotoFromGallery(userId, photoUrl)) {
                is Result.Success -> {
                    val updatedPhotos = currentUser.photos.toMutableList().apply {
                        remove(photoUrl)
                    }
                    _currentUser.value = currentUser.copy(photos = updatedPhotos)
                    _profileUiState.value = ProfileUiState.Success(Unit)
                }
                is Result.Error -> {
                    _profileUiState.value = ProfileUiState.Error(result.error, "Failed to remove photo from gallery")
                }
            }
            
            _currentOperation.value = ProfileOperation.NONE
        }
    }
    
    /**
     * Update user location
     */
    fun updateLocation(latitude: Double, longitude: Double, locationName: String? = null) {
        viewModelScope.launch {
            _currentOperation.value = ProfileOperation.UPDATE_LOCATION
            _profileUiState.value = ProfileUiState.Loading
            
            val userId = authRepository.getCurrentUserId() ?: return@launch
            val currentUser = _currentUser.value ?: return@launch
            
            when (val result = userRepository.updateLocation(userId, latitude, longitude, locationName)) {
                is Result.Success -> {
                    val updatedLocation = GeoLocation(latitude, longitude, locationName)
                    _currentUser.value = currentUser.copy(location = updatedLocation)
                    _profileUiState.value = ProfileUiState.Success(updatedLocation)
                }
                is Result.Error -> {
                    _profileUiState.value = ProfileUiState.Error(result.error, "Failed to update location")
                }
            }
            
            _currentOperation.value = ProfileOperation.NONE
        }
    }
    
    /**
     * Update user settings
     */
    fun updateUserSettings(
        showLocation: Boolean? = null,
        showOnlineStatus: Boolean? = null,
        showLastActive: Boolean? = null,
        notificationEnabled: Boolean? = null,
        emailNotificationEnabled: Boolean? = null,
        profileVisibility: Boolean? = null,
        maxDistanceInKm: Int? = null,
        minAgePreference: Int? = null,
        maxAgePreference: Int? = null,
        language: String? = null,
        regionalSettings: RegionalSettings? = null
    ) {
        viewModelScope.launch {
            _currentOperation.value = ProfileOperation.UPDATE_PROFILE
            _profileUiState.value = ProfileUiState.Loading
            
            val userId = authRepository.getCurrentUserId() ?: return@launch
            val currentUser = _currentUser.value ?: return@launch
            
            try {
                // Update the settings through the repository
                val result = userRepository.updateUserSettings(
                    userId = userId,
                    showLocation = showLocation,
                    showOnlineStatus = showOnlineStatus,
                    notificationEnabled = notificationEnabled,
                    emailNotificationEnabled = emailNotificationEnabled,
                    profileVisibility = profileVisibility,
                    maxDistanceInKm = maxDistanceInKm,
                    minAgePreference = minAgePreference,
                    maxAgePreference = maxAgePreference,
                    language = language
                )
                
                when (result) {
                    is Result.Success -> {
                        // Update local copy of user
                        var updatedUser = currentUser
                        
                        if (showLocation != null) updatedUser = updatedUser.copy(showLocation = showLocation)
                        if (showOnlineStatus != null) updatedUser = updatedUser.copy(showOnlineStatus = showOnlineStatus)
                        if (showLastActive != null) updatedUser = updatedUser.copy(showLastActive = showLastActive)
                        if (notificationEnabled != null) updatedUser = updatedUser.copy(notificationEnabled = notificationEnabled)
                        if (emailNotificationEnabled != null) updatedUser = updatedUser.copy(emailNotificationEnabled = emailNotificationEnabled)
                        if (profileVisibility != null) updatedUser = updatedUser.copy(profileVisibility = profileVisibility)
                        if (maxDistanceInKm != null) updatedUser = updatedUser.copy(maxDistanceInKm = maxDistanceInKm)
                        if (minAgePreference != null) updatedUser = updatedUser.copy(minAgePreference = minAgePreference)
                        if (maxAgePreference != null) updatedUser = updatedUser.copy(maxAgePreference = maxAgePreference)
                        if (language != null) updatedUser = updatedUser.copy(language = language)
                        if (regionalSettings != null) updatedUser = updatedUser.copy(regionalSettings = regionalSettings)
                        
                        _currentUser.value = updatedUser
                        _profileUiState.value = ProfileUiState.Success(updatedUser)
                    }
                    is Result.Error -> {
                        _profileUiState.value = ProfileUiState.Error(result.error, "Failed to update settings")
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error updating user settings")
                _profileUiState.value = ProfileUiState.Error(
                    AppError.DataError.UpdateFailed("Failed to update settings: ${e.message}", e),
                    "Failed to update settings"
                )
            }
            
            _currentOperation.value = ProfileOperation.NONE
        }
    }
    
    /**
     * Update user preference
     */
    fun updateUserPreference(preference: UserPreference) {
        viewModelScope.launch {
            _currentOperation.value = ProfileOperation.UPDATE_PROFILE
            _profileUiState.value = ProfileUiState.Loading
            
            val userId = authRepository.getCurrentUserId() ?: return@launch
            
            when (val result = userRepository.updateUserPreference(userId, preference)) {
                is Result.Success -> {
                    // Refresh user data to get updated preferences
                    fetchCurrentUserProfile()
                    _profileUiState.value = ProfileUiState.Success(preference)
                }
                is Result.Error -> {
                    _profileUiState.value = ProfileUiState.Error(result.error, "Failed to update preference")
                }
            }
            
            _currentOperation.value = ProfileOperation.NONE
        }
    }
    
    /**
     * Block a user
     */
    fun blockUser(blockedUserId: String) {
        viewModelScope.launch {
            _currentOperation.value = ProfileOperation.BLOCK_USER
            _profileUiState.value = ProfileUiState.Loading
            
            val userId = authRepository.getCurrentUserId() ?: return@launch
            val currentUser = _currentUser.value ?: return@launch
            
            when (val result = userRepository.blockUser(userId, blockedUserId)) {
                is Result.Success -> {
                    // Update blocked users list
                    val updatedBlockedUsers = currentUser.blockedUsers.toMutableList().apply {
                        if (!contains(blockedUserId)) {
                            add(blockedUserId)
                        }
                    }
                    _currentUser.value = currentUser.copy(blockedUsers = updatedBlockedUsers)
                    _profileUiState.value = ProfileUiState.Success(Unit)
                }
                is Result.Error -> {
                    _profileUiState.value = ProfileUiState.Error(result.error, "Failed to block user")
                }
            }
            
            _currentOperation.value = ProfileOperation.NONE
        }
    }
    
    /**
     * Unblock a user
     */
    fun unblockUser(blockedUserId: String) {
        viewModelScope.launch {
            _currentOperation.value = ProfileOperation.BLOCK_USER
            _profileUiState.value = ProfileUiState.Loading
            
            val userId = authRepository.getCurrentUserId() ?: return@launch
            val currentUser = _currentUser.value ?: return@launch
            
            when (val result = userRepository.unblockUser(userId, blockedUserId)) {
                is Result.Success -> {
                    // Update blocked users list
                    val updatedBlockedUsers = currentUser.blockedUsers.toMutableList().apply {
                        remove(blockedUserId)
                    }
                    _currentUser.value = currentUser.copy(blockedUsers = updatedBlockedUsers)
                    _profileUiState.value = ProfileUiState.Success(Unit)
                }
                is Result.Error -> {
                    _profileUiState.value = ProfileUiState.Error(result.error, "Failed to unblock user")
                }
            }
            
            _currentOperation.value = ProfileOperation.NONE
        }
    }
    
    /**
     * Report a user
     */
    fun reportUser(reportedUserId: String, reason: String, details: String? = null) {
        viewModelScope.launch {
            _currentOperation.value = ProfileOperation.REPORT_USER
            _profileUiState.value = ProfileUiState.Loading
            
            val userId = authRepository.getCurrentUserId() ?: return@launch
            
            when (val result = userRepository.reportUser(userId, reportedUserId, reason, details)) {
                is Result.Success -> {
                    _profileUiState.value = ProfileUiState.Success(Unit)
                }
                is Result.Error -> {
                    _profileUiState.value = ProfileUiState.Error(result.error, "Failed to report user")
                }
            }
            
            _currentOperation.value = ProfileOperation.NONE
        }
    }
    
    /**
     * Submit verification document
     */
    fun submitVerificationDocument(documentType: String, documentUri: Uri) {
        viewModelScope.launch {
            _currentOperation.value = ProfileOperation.VERIFY_USER
            _profileUiState.value = ProfileUiState.Loading
            _uploadProgress.value = 0f
            
            val userId = authRepository.getCurrentUserId() ?: return@launch
            
            when (val result = userRepository.submitVerificationDocuments(userId, documentType, documentUri)) {
                is Result.Success -> {
                    _uploadProgress.value = 1f
                    _profileUiState.value = ProfileUiState.Success(result.data)
                    checkVerificationStatus()
                }
                is Result.Error -> {
                    _profileUiState.value = ProfileUiState.Error(result.error, "Failed to submit verification document")
                }
            }
            
            _currentOperation.value = ProfileOperation.NONE
        }
    }
    
    /**
     * Check verification status
     */
    fun checkVerificationStatus() {
        viewModelScope.launch {
            val userId = authRepository.getCurrentUserId() ?: return@launch
            
            when (val result = userRepository.checkVerificationStatus(userId)) {
                is Result.Success -> {
                    _verificationStatus.value = result.data
                }
                is Result.Error -> {
                    Timber.e(result.error.throwable, "Error checking verification status")
                }
            }
        }
    }
    
    /**
     * Search users
     */
    fun searchUsers(query: String, limit: Int = 20) {
        viewModelScope.launch {
            _currentOperation.value = ProfileOperation.SEARCH_USERS
            _profileUiState.value = ProfileUiState.Loading
            
            when (val result = userRepository.searchUsers(query, limit)) {
                is Result.Success -> {
                    _searchResults.value = result.data
                    _profileUiState.value = ProfileUiState.Success(result.data)
                }
                is Result.Error -> {
                    _profileUiState.value = ProfileUiState.Error(result.error, "Search failed")
                }
            }
            
            _currentOperation.value = ProfileOperation.NONE
        }
    }
    
    /**
     * Get nearby users
     */
    fun getNearbyUsers(latitude: Double, longitude: Double, radius: Double, limit: Int = 20) {
        viewModelScope.launch {
            _currentOperation.value = ProfileOperation.GET_NEARBY_USERS
            _profileUiState.value = ProfileUiState.Loading
            
            when (val result = userRepository.getNearbyUsers(latitude, longitude, radius, limit)) {
                is Result.Success -> {
                    _nearbyUsers.value = result.data
                    _profileUiState.value = ProfileUiState.Success(result.data)
                }
                is Result.Error -> {
                    _profileUiState.value = ProfileUiState.Error(result.error, "Failed to get nearby users")
                }
            }
            
            _currentOperation.value = ProfileOperation.NONE
        }
    }
    
    /**
     * Get recommended matches
     */
    fun getRecommendedMatches(limit: Int = 20) {
        viewModelScope.launch {
            _currentOperation.value = ProfileOperation.GET_RECOMMENDED_MATCHES
            _profileUiState.value = ProfileUiState.Loading
            
            val userId = authRepository.getCurrentUserId() ?: return@launch
            
            when (val result = userRepository.getRecommendedMatches(userId, limit)) {
                is Result.Success -> {
                    _recommendedUsers.value = result.data
                    _profileUiState.value = ProfileUiState.Success(result.data)
                }
                is Result.Error -> {
                    _profileUiState.value = ProfileUiState.Error(result.error, "Failed to get recommended matches")
                }
            }
            
            _currentOperation.value = ProfileOperation.NONE
        }
    }
    
    /**
     * Update user online status
     */
    fun updateOnlineStatus(isOnline: Boolean) {
        viewModelScope.launch {
            val userId = authRepository.getCurrentUserId() ?: return@launch
            
            userRepository.updateUserOnlineStatus(userId, isOnline)
            
            // Update last active timestamp
            if (isOnline) {
                userRepository.updateUserLastActive(userId)
            }
        }
    }
    
    /**
     * Reset profile UI state
     */
    fun resetProfileState() {
        _profileUiState.value = ProfileUiState.Initial
        _currentOperation.value = ProfileOperation.NONE
    }
}