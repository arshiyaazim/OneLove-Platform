package com.kilagee.onelove.ui.screens.profile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.kilagee.onelove.data.model.User
import com.kilagee.onelove.domain.repository.AuthRepository
import com.kilagee.onelove.domain.repository.UserRepository
import com.kilagee.onelove.domain.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.util.Date
import java.util.UUID
import javax.inject.Inject

/**
 * EditProfileViewModel for handling profile editing operations
 */
@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
) : ViewModel() {

    private val _uiState = MutableStateFlow<EditProfileUiState>(EditProfileUiState.Loading)
    val uiState: StateFlow<EditProfileUiState> = _uiState.asStateFlow()
    
    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user.asStateFlow()
    
    // Profile fields that can be edited
    private val _nameInput = MutableStateFlow("")
    val nameInput: StateFlow<String> = _nameInput.asStateFlow()
    
    private val _bioInput = MutableStateFlow("")
    val bioInput: StateFlow<String> = _bioInput.asStateFlow()
    
    private val _ageInput = MutableStateFlow("")
    val ageInput: StateFlow<String> = _ageInput.asStateFlow()
    
    private val _genderInput = MutableStateFlow("")
    val genderInput: StateFlow<String> = _genderInput.asStateFlow()
    
    private val _locationInput = MutableStateFlow("")
    val locationInput: StateFlow<String> = _locationInput.asStateFlow()
    
    private val _interestsInput = MutableStateFlow<List<String>>(emptyList())
    val interestsInput: StateFlow<List<String>> = _interestsInput.asStateFlow()
    
    private val _lookingForInput = MutableStateFlow<List<String>>(emptyList())
    val lookingForInput: StateFlow<List<String>> = _lookingForInput.asStateFlow()
    
    private val _minAgePreferenceInput = MutableStateFlow(18)
    val minAgePreferenceInput: StateFlow<Int> = _minAgePreferenceInput.asStateFlow()
    
    private val _maxAgePreferenceInput = MutableStateFlow(65)
    val maxAgePreferenceInput: StateFlow<Int> = _maxAgePreferenceInput.asStateFlow()
    
    private val _maxDistanceInput = MutableStateFlow(50)
    val maxDistanceInput: StateFlow<Int> = _maxDistanceInput.asStateFlow()
    
    private val _selectedPhotoUris = MutableStateFlow<List<Uri>>(emptyList())
    val selectedPhotoUris: StateFlow<List<Uri>> = _selectedPhotoUris.asStateFlow()
    
    private val _currentPhotos = MutableStateFlow<List<String>>(emptyList())
    val currentPhotos: StateFlow<List<String>> = _currentPhotos.asStateFlow()
    
    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()
    
    /**
     * Initialize the view model and load user data
     */
    init {
        loadUserProfile()
    }
    
    /**
     * Load the current user profile
     */
    private fun loadUserProfile() {
        viewModelScope.launch {
            _uiState.value = EditProfileUiState.Loading
            
            try {
                val userId = authRepository.getCurrentUserId()
                if (userId == null) {
                    _uiState.value = EditProfileUiState.Error("User not authenticated")
                    return@launch
                }
                
                userRepository.getUserById(userId).collectLatest { result ->
                    when (result) {
                        is Result.Success -> {
                            val user = result.data
                            _user.value = user
                            
                            // Populate input fields with current values
                            _nameInput.value = user.name
                            _bioInput.value = user.bio ?: ""
                            _ageInput.value = user.age?.toString() ?: ""
                            _genderInput.value = user.gender ?: ""
                            _locationInput.value = user.location ?: ""
                            _interestsInput.value = user.interests ?: emptyList()
                            _lookingForInput.value = user.lookingFor ?: emptyList()
                            _minAgePreferenceInput.value = user.minAgePreference ?: 18
                            _maxAgePreferenceInput.value = user.maxAgePreference ?: 65
                            _maxDistanceInput.value = user.maxDistance ?: 50
                            _currentPhotos.value = user.photos ?: emptyList()
                            
                            _uiState.value = EditProfileUiState.Success
                        }
                        is Result.Error -> {
                            _uiState.value = EditProfileUiState.Error(result.message)
                        }
                        is Result.Loading -> {
                            _uiState.value = EditProfileUiState.Loading
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading user profile")
                _uiState.value = EditProfileUiState.Error("Failed to load profile: ${e.message}")
            }
        }
    }
    
    /**
     * Update name input
     */
    fun onNameChanged(name: String) {
        _nameInput.value = name
    }
    
    /**
     * Update bio input
     */
    fun onBioChanged(bio: String) {
        _bioInput.value = bio
    }
    
    /**
     * Update age input
     */
    fun onAgeChanged(age: String) {
        // Only allow numeric input
        if (age.isEmpty() || age.all { it.isDigit() }) {
            _ageInput.value = age
        }
    }
    
    /**
     * Update gender input
     */
    fun onGenderChanged(gender: String) {
        _genderInput.value = gender
    }
    
    /**
     * Update location input
     */
    fun onLocationChanged(location: String) {
        _locationInput.value = location
    }
    
    /**
     * Add interest to list
     */
    fun addInterest(interest: String) {
        if (interest.isNotBlank() && !_interestsInput.value.contains(interest)) {
            _interestsInput.value = _interestsInput.value + interest
        }
    }
    
    /**
     * Remove interest from list
     */
    fun removeInterest(interest: String) {
        _interestsInput.value = _interestsInput.value.filter { it != interest }
    }
    
    /**
     * Add looking for option
     */
    fun addLookingFor(lookingFor: String) {
        if (lookingFor.isNotBlank() && !_lookingForInput.value.contains(lookingFor)) {
            _lookingForInput.value = _lookingForInput.value + lookingFor
        }
    }
    
    /**
     * Remove looking for option
     */
    fun removeLookingFor(lookingFor: String) {
        _lookingForInput.value = _lookingForInput.value.filter { it != lookingFor }
    }
    
    /**
     * Update min age preference
     */
    fun onMinAgePreferenceChanged(age: Int) {
        if (age in 18..(_maxAgePreferenceInput.value - 1)) {
            _minAgePreferenceInput.value = age
        }
    }
    
    /**
     * Update max age preference
     */
    fun onMaxAgePreferenceChanged(age: Int) {
        if (age in (_minAgePreferenceInput.value + 1)..100) {
            _maxAgePreferenceInput.value = age
        }
    }
    
    /**
     * Update max distance preference
     */
    fun onMaxDistanceChanged(distance: Int) {
        if (distance > 0) {
            _maxDistanceInput.value = distance
        }
    }
    
    /**
     * Add photo URI to list
     */
    fun addPhotoUri(uri: Uri) {
        if (!_selectedPhotoUris.value.contains(uri)) {
            _selectedPhotoUris.value = _selectedPhotoUris.value + uri
        }
    }
    
    /**
     * Remove photo URI from list
     */
    fun removePhotoUri(uri: Uri) {
        _selectedPhotoUris.value = _selectedPhotoUris.value.filter { it != uri }
    }
    
    /**
     * Remove photo URL from current photos
     */
    fun removePhoto(url: String) {
        _currentPhotos.value = _currentPhotos.value.filter { it != url }
    }
    
    /**
     * Save profile changes
     */
    fun saveProfile(onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                _isSaving.value = true
                _uiState.value = EditProfileUiState.Loading
                
                val currentUser = _user.value ?: run {
                    _uiState.value = EditProfileUiState.Error("User data not available")
                    _isSaving.value = false
                    return@launch
                }
                
                // Upload any new photos
                val photoUrls = uploadPhotos()
                
                // Combine existing (not removed) photos with new ones
                val allPhotos = _currentPhotos.value + photoUrls
                
                // Check if at least name and age are provided
                if (_nameInput.value.isBlank()) {
                    _uiState.value = EditProfileUiState.Error("Name cannot be empty")
                    _isSaving.value = false
                    return@launch
                }
                
                val ageValue = _ageInput.value.toIntOrNull()
                if (ageValue == null || ageValue < 18) {
                    _uiState.value = EditProfileUiState.Error("Valid age (18+) is required")
                    _isSaving.value = false
                    return@launch
                }
                
                // Build updated user object
                val updatedUser = currentUser.copy(
                    name = _nameInput.value,
                    bio = _bioInput.value.takeIf { it.isNotBlank() },
                    age = ageValue,
                    gender = _genderInput.value.takeIf { it.isNotBlank() },
                    location = _locationInput.value.takeIf { it.isNotBlank() },
                    interests = _interestsInput.value.takeIf { it.isNotEmpty() },
                    lookingFor = _lookingForInput.value.takeIf { it.isNotEmpty() },
                    minAgePreference = _minAgePreferenceInput.value,
                    maxAgePreference = _maxAgePreferenceInput.value,
                    maxDistance = _maxDistanceInput.value,
                    photos = allPhotos.takeIf { it.isNotEmpty() },
                    profilePictureUrl = allPhotos.firstOrNull() ?: currentUser.profilePictureUrl,
                    isProfileComplete = true,
                    updatedAt = Date()
                )
                
                // Save the user to Firestore
                val result = userRepository.updateUser(updatedUser)
                
                when (result) {
                    is Result.Success -> {
                        _uiState.value = EditProfileUiState.Success
                        onSuccess()
                    }
                    is Result.Error -> {
                        _uiState.value = EditProfileUiState.Error(result.message)
                    }
                    is Result.Loading -> {
                        // Already handled
                    }
                }
                
                _isSaving.value = false
                
            } catch (e: Exception) {
                Timber.e(e, "Error saving profile")
                _uiState.value = EditProfileUiState.Error("Failed to save profile: ${e.message}")
                _isSaving.value = false
            }
        }
    }
    
    /**
     * Upload photos to Firebase Storage
     */
    private suspend fun uploadPhotos(): List<String> {
        val uploadedUrls = mutableListOf<String>()
        
        for (uri in _selectedPhotoUris.value) {
            try {
                val userId = authRepository.getCurrentUserId() ?: continue
                val filename = "users/$userId/photos/${UUID.randomUUID()}.jpg"
                val photoRef = storage.reference.child(filename)
                
                // Upload photo
                photoRef.putFile(uri).await()
                
                // Get download URL
                val downloadUrl = photoRef.downloadUrl.await().toString()
                uploadedUrls.add(downloadUrl)
                
            } catch (e: Exception) {
                Timber.e(e, "Error uploading photo")
                // Continue with other photos even if one fails
            }
        }
        
        return uploadedUrls
    }
}

/**
 * UI state for the edit profile screen
 */
sealed class EditProfileUiState {
    object Loading : EditProfileUiState()
    object Success : EditProfileUiState()
    data class Error(val message: String) : EditProfileUiState()
}