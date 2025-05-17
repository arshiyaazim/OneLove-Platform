package com.kilagee.onelove.ui.screens.profile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kilagee.onelove.data.model.User
import com.kilagee.onelove.data.model.UserPreferences
import com.kilagee.onelove.domain.repository.AuthRepository
import com.kilagee.onelove.domain.repository.ProfileRepository
import com.kilagee.onelove.domain.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

/**
 * ViewModel for user profile screens
 */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val authRepository: AuthRepository
) : ViewModel() {
    
    // UI state
    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()
    
    // One-time events
    private val _events = MutableSharedFlow<ProfileEvent>()
    val events: SharedFlow<ProfileEvent> = _events.asSharedFlow()
    
    // Current user
    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user.asStateFlow()
    
    // User preferences
    private val _preferences = MutableStateFlow<UserPreferences?>(null)
    val preferences: StateFlow<UserPreferences?> = _preferences.asStateFlow()
    
    // Edit mode
    private val _isEditMode = MutableStateFlow(false)
    val isEditMode: StateFlow<Boolean> = _isEditMode.asStateFlow()
    
    // Edited fields
    private val _editedName = MutableStateFlow("")
    val editedName: StateFlow<String> = _editedName.asStateFlow()
    
    private val _editedBio = MutableStateFlow("")
    val editedBio: StateFlow<String> = _editedBio.asStateFlow()
    
    private val _editedOccupation = MutableStateFlow("")
    val editedOccupation: StateFlow<String> = _editedOccupation.asStateFlow()
    
    private val _editedEducation = MutableStateFlow("")
    val editedEducation: StateFlow<String> = _editedEducation.asStateFlow()
    
    private val _editedInterests = MutableStateFlow<List<String>>(emptyList())
    val editedInterests: StateFlow<List<String>> = _editedInterests.asStateFlow()
    
    private val _editedHeight = MutableStateFlow<Int?>(null)
    val editedHeight: StateFlow<Int?> = _editedHeight.asStateFlow()
    
    // Is uploading image
    private val _isUploadingImage = MutableStateFlow(false)
    val isUploadingImage: StateFlow<Boolean> = _isUploadingImage.asStateFlow()
    
    // Is deleting account
    private val _isDeletingAccount = MutableStateFlow(false)
    val isDeletingAccount: StateFlow<Boolean> = _isDeletingAccount.asStateFlow()
    
    // Active jobs
    private var profileJob: Job? = null
    
    init {
        loadUserProfile()
        loadUserPreferences()
    }
    
    /**
     * Load user profile
     */
    private fun loadUserProfile() {
        // Cancel any existing job
        profileJob?.cancel()
        
        // Start new job
        profileJob = viewModelScope.launch {
            _uiState.value = ProfileUiState.Loading
            
            profileRepository.getCurrentUserProfileFlow().collectLatest { result ->
                when (result) {
                    is Result.Success -> {
                        _user.value = result.data
                        
                        // Update edited fields when not in edit mode
                        if (!_isEditMode.value) {
                            _editedName.value = result.data.name
                            _editedBio.value = result.data.bio ?: ""
                            _editedOccupation.value = result.data.occupation ?: ""
                            _editedEducation.value = result.data.education ?: ""
                            _editedInterests.value = result.data.interests
                            _editedHeight.value = result.data.height
                        }
                        
                        _uiState.value = ProfileUiState.Success
                    }
                    is Result.Error -> {
                        _events.emit(ProfileEvent.Error(result.message ?: "Failed to load profile"))
                        _uiState.value = ProfileUiState.Error(result.message ?: "Failed to load profile")
                    }
                    is Result.Loading -> {
                        _uiState.value = ProfileUiState.Loading
                    }
                }
            }
        }
    }
    
    /**
     * Load user preferences
     */
    private fun loadUserPreferences() {
        viewModelScope.launch {
            val result = profileRepository.getUserPreferences()
            
            if (result is Result.Success) {
                _preferences.value = result.data
            }
        }
    }
    
    /**
     * Toggle edit mode
     */
    fun toggleEditMode() {
        if (_isEditMode.value) {
            // Exiting edit mode, discard changes
            _user.value?.let { user ->
                _editedName.value = user.name
                _editedBio.value = user.bio ?: ""
                _editedOccupation.value = user.occupation ?: ""
                _editedEducation.value = user.education ?: ""
                _editedInterests.value = user.interests
                _editedHeight.value = user.height
            }
        }
        
        _isEditMode.value = !_isEditMode.value
    }
    
    /**
     * Update edited name
     */
    fun updateName(name: String) {
        _editedName.value = name
    }
    
    /**
     * Update edited bio
     */
    fun updateBio(bio: String) {
        _editedBio.value = bio
    }
    
    /**
     * Update edited occupation
     */
    fun updateOccupation(occupation: String) {
        _editedOccupation.value = occupation
    }
    
    /**
     * Update edited education
     */
    fun updateEducation(education: String) {
        _editedEducation.value = education
    }
    
    /**
     * Add interest
     */
    fun addInterest(interest: String) {
        if (interest.isNotBlank() && !_editedInterests.value.contains(interest)) {
            _editedInterests.value = _editedInterests.value + interest
        }
    }
    
    /**
     * Remove interest
     */
    fun removeInterest(interest: String) {
        _editedInterests.value = _editedInterests.value.filter { it != interest }
    }
    
    /**
     * Update edited height
     */
    fun updateHeight(height: Int?) {
        _editedHeight.value = height
    }
    
    /**
     * Save profile changes
     */
    fun saveProfileChanges() {
        viewModelScope.launch {
            _uiState.value = ProfileUiState.Loading
            
            val result = profileRepository.updateProfile(
                name = _editedName.value,
                bio = _editedBio.value.takeIf { it.isNotBlank() },
                occupation = _editedOccupation.value.takeIf { it.isNotBlank() },
                education = _editedEducation.value.takeIf { it.isNotBlank() },
                interests = _editedInterests.value,
                height = _editedHeight.value
            )
            
            when (result) {
                is Result.Success -> {
                    _user.value = result.data
                    _isEditMode.value = false
                    _events.emit(ProfileEvent.ProfileUpdated)
                    _uiState.value = ProfileUiState.Success
                }
                is Result.Error -> {
                    _events.emit(ProfileEvent.Error(result.message ?: "Failed to update profile"))
                    _uiState.value = ProfileUiState.Error(result.message ?: "Failed to update profile")
                }
                is Result.Loading -> {
                    // Keep loading state
                }
            }
        }
    }
    
    /**
     * Upload profile image
     */
    fun uploadProfileImage(imageUri: Uri, isPrimary: Boolean = false) {
        viewModelScope.launch {
            _isUploadingImage.value = true
            
            val result = profileRepository.uploadProfileImageFromUri(imageUri, isPrimary)
            
            _isUploadingImage.value = false
            
            when (result) {
                is Result.Success -> {
                    _events.emit(ProfileEvent.ImageUploaded)
                }
                is Result.Error -> {
                    _events.emit(ProfileEvent.Error(result.message ?: "Failed to upload image"))
                }
                is Result.Loading -> {
                    // Do nothing for loading state
                }
            }
        }
    }
    
    /**
     * Delete profile image
     */
    fun deleteProfileImage(imageUrl: String) {
        viewModelScope.launch {
            val result = profileRepository.deleteProfileImage(imageUrl)
            
            if (result is Result.Error) {
                _events.emit(ProfileEvent.Error(result.message ?: "Failed to delete image"))
            } else if (result is Result.Success) {
                _events.emit(ProfileEvent.ImageDeleted)
            }
        }
    }
    
    /**
     * Reorder profile images
     */
    fun reorderProfileImages(imageUrls: List<String>) {
        viewModelScope.launch {
            val result = profileRepository.reorderProfileImages(imageUrls)
            
            if (result is Result.Error) {
                _events.emit(ProfileEvent.Error(result.message ?: "Failed to reorder images"))
            }
        }
    }
    
    /**
     * Update user preferences
     */
    fun updateUserPreferences(
        minAge: Int? = null,
        maxAge: Int? = null,
        maxDistance: Int? = null,
        genderPreferences: List<String>? = null,
        showMe: Boolean? = null,
        autoPlayVideos: Boolean? = null,
        showOnlineStatus: Boolean? = null,
        showLastActive: Boolean? = null
    ) {
        viewModelScope.launch {
            _uiState.value = ProfileUiState.Loading
            
            val result = profileRepository.updateUserPreferences(
                minAge = minAge,
                maxAge = maxAge,
                maxDistance = maxDistance,
                genderPreferences = genderPreferences,
                showMe = showMe,
                autoPlayVideos = autoPlayVideos,
                showOnlineStatus = showOnlineStatus,
                showLastActive = showLastActive
            )
            
            when (result) {
                is Result.Success -> {
                    _preferences.value = result.data
                    _events.emit(ProfileEvent.PreferencesUpdated)
                    _uiState.value = ProfileUiState.Success
                }
                is Result.Error -> {
                    _events.emit(ProfileEvent.Error(result.message ?: "Failed to update preferences"))
                    _uiState.value = ProfileUiState.Success
                }
                is Result.Loading -> {
                    // Keep loading state
                }
            }
        }
    }
    
    /**
     * Start email verification
     */
    fun startEmailVerification() {
        viewModelScope.launch {
            _uiState.value = ProfileUiState.Loading
            
            val result = profileRepository.startEmailVerification()
            
            when (result) {
                is Result.Success -> {
                    _events.emit(ProfileEvent.VerificationEmailSent)
                    _uiState.value = ProfileUiState.Success
                }
                is Result.Error -> {
                    _events.emit(ProfileEvent.Error(result.message ?: "Failed to send verification email"))
                    _uiState.value = ProfileUiState.Success
                }
                is Result.Loading -> {
                    // Keep loading state
                }
            }
        }
    }
    
    /**
     * Start phone verification
     */
    fun startPhoneVerification(phoneNumber: String) {
        viewModelScope.launch {
            _uiState.value = ProfileUiState.Loading
            
            val result = profileRepository.startPhoneVerification(phoneNumber)
            
            when (result) {
                is Result.Success -> {
                    _events.emit(ProfileEvent.VerificationSmsSent)
                    _uiState.value = ProfileUiState.Success
                }
                is Result.Error -> {
                    _events.emit(ProfileEvent.Error(result.message ?: "Failed to send verification SMS"))
                    _uiState.value = ProfileUiState.Success
                }
                is Result.Loading -> {
                    // Keep loading state
                }
            }
        }
    }
    
    /**
     * Start ID verification
     */
    fun startIdVerification(
        idType: String,
        idFrontFile: File,
        idBackFile: File? = null,
        selfieFile: File
    ) {
        viewModelScope.launch {
            _uiState.value = ProfileUiState.Loading
            
            val result = profileRepository.startIdVerification(
                idType = idType,
                idFrontFile = idFrontFile,
                idBackFile = idBackFile,
                selfieFile = selfieFile
            )
            
            when (result) {
                is Result.Success -> {
                    _events.emit(ProfileEvent.IdVerificationStarted(result.data))
                    _uiState.value = ProfileUiState.Success
                }
                is Result.Error -> {
                    _events.emit(ProfileEvent.Error(result.message ?: "Failed to start ID verification"))
                    _uiState.value = ProfileUiState.Success
                }
                is Result.Loading -> {
                    // Keep loading state
                }
            }
        }
    }
    
    /**
     * Check ID verification status
     */
    fun checkIdVerificationStatus(verificationRequestId: String) {
        viewModelScope.launch {
            val result = profileRepository.checkIdVerificationStatus(verificationRequestId)
            
            if (result is Result.Success) {
                _events.emit(ProfileEvent.IdVerificationStatus(result.data))
            } else if (result is Result.Error) {
                _events.emit(ProfileEvent.Error(result.message ?: "Failed to check verification status"))
            }
        }
    }
    
    /**
     * Update location
     */
    fun updateLocation(
        latitude: Double,
        longitude: Double,
        city: String? = null,
        country: String? = null
    ) {
        viewModelScope.launch {
            val result = profileRepository.updateLocation(
                latitude = latitude,
                longitude = longitude,
                city = city,
                country = country
            )
            
            if (result is Result.Error) {
                _events.emit(ProfileEvent.Error(result.message ?: "Failed to update location"))
            } else if (result is Result.Success) {
                _events.emit(ProfileEvent.LocationUpdated)
            }
        }
    }
    
    /**
     * Deactivate account
     */
    fun deactivateAccount(reason: String? = null) {
        viewModelScope.launch {
            _uiState.value = ProfileUiState.Loading
            
            val result = profileRepository.deactivateAccount(reason)
            
            when (result) {
                is Result.Success -> {
                    _events.emit(ProfileEvent.AccountDeactivated)
                    _uiState.value = ProfileUiState.Success
                }
                is Result.Error -> {
                    _events.emit(ProfileEvent.Error(result.message ?: "Failed to deactivate account"))
                    _uiState.value = ProfileUiState.Success
                }
                is Result.Loading -> {
                    // Keep loading state
                }
            }
        }
    }
    
    /**
     * Delete account
     */
    fun deleteAccount(reason: String? = null, feedback: String? = null) {
        viewModelScope.launch {
            _isDeletingAccount.value = true
            
            val result = profileRepository.deleteAccount(reason, feedback)
            
            _isDeletingAccount.value = false
            
            when (result) {
                is Result.Success -> {
                    // Sign out after account deletion
                    authRepository.signOut()
                    _events.emit(ProfileEvent.AccountDeleted)
                }
                is Result.Error -> {
                    _events.emit(ProfileEvent.Error(result.message ?: "Failed to delete account"))
                }
                is Result.Loading -> {
                    // Do nothing for loading state
                }
            }
        }
    }
    
    /**
     * Sign out
     */
    fun signOut() {
        viewModelScope.launch {
            val result = authRepository.signOut()
            
            when (result) {
                is Result.Success -> {
                    _events.emit(ProfileEvent.SignedOut)
                }
                is Result.Error -> {
                    _events.emit(ProfileEvent.Error(result.message ?: "Failed to sign out"))
                }
                is Result.Loading -> {
                    // Do nothing for loading state
                }
            }
        }
    }
    
    /**
     * Clear errors
     */
    fun clearErrors() {
        if (_uiState.value is ProfileUiState.Error) {
            _uiState.value = ProfileUiState.Success
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        profileJob?.cancel()
    }
}

/**
 * UI state for the profile screen
 */
sealed class ProfileUiState {
    object Loading : ProfileUiState()
    object Success : ProfileUiState()
    data class Error(val message: String) : ProfileUiState()
}

/**
 * Events emitted by the profile screen
 */
sealed class ProfileEvent {
    object ProfileUpdated : ProfileEvent()
    object ImageUploaded : ProfileEvent()
    object ImageDeleted : ProfileEvent()
    object PreferencesUpdated : ProfileEvent()
    object VerificationEmailSent : ProfileEvent()
    object VerificationSmsSent : ProfileEvent()
    data class IdVerificationStarted(val requestId: String) : ProfileEvent()
    data class IdVerificationStatus(val status: String) : ProfileEvent()
    object LocationUpdated : ProfileEvent()
    object AccountDeactivated : ProfileEvent()
    object AccountDeleted : ProfileEvent()
    object SignedOut : ProfileEvent()
    data class Error(val message: String) : ProfileEvent()
}