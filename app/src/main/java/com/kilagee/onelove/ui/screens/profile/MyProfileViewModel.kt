package com.kilagee.onelove.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for the user's profile screen
 */
@HiltViewModel
class MyProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()
    
    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user.asStateFlow()
    
    private val _isSubscribed = MutableStateFlow(false)
    val isSubscribed: StateFlow<Boolean> = _isSubscribed.asStateFlow()
    
    private val _subscriptionType = MutableStateFlow<String?>(null)
    val subscriptionType: StateFlow<String?> = _subscriptionType.asStateFlow()
    
    private val _subscriptionExpiryDate = MutableStateFlow<String?>(null)
    val subscriptionExpiryDate: StateFlow<String?> = _subscriptionExpiryDate.asStateFlow()
    
    private val _verificationLevel = MutableStateFlow(0)
    val verificationLevel: StateFlow<Int> = _verificationLevel.asStateFlow()
    
    private val _profileCompletionPercentage = MutableStateFlow(0)
    val profileCompletionPercentage: StateFlow<Int> = _profileCompletionPercentage.asStateFlow()
    
    private val _points = MutableStateFlow(0)
    val points: StateFlow<Int> = _points.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    /**
     * Initialize the view model
     */
    init {
        loadUserProfile()
    }
    
    /**
     * Load the user profile
     */
    fun loadUserProfile() {
        viewModelScope.launch {
            _uiState.value = ProfileUiState.Loading
            
            try {
                val userId = authRepository.getCurrentUserId()
                if (userId == null) {
                    _uiState.value = ProfileUiState.Error("User not authenticated")
                    return@launch
                }
                
                userRepository.getUserById(userId).collectLatest { result ->
                    when (result) {
                        is Result.Success -> {
                            val user = result.data
                            _user.value = user
                            
                            // Process user data
                            _isSubscribed.value = user.isPremium
                            _subscriptionType.value = user.subscriptionType
                            
                            // Format the subscription expiry date
                            _subscriptionExpiryDate.value = user.subscriptionExpiryDate?.let {
                                val formatter = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
                                formatter.format(it)
                            }
                            
                            _verificationLevel.value = user.verificationLevel
                            _points.value = user.points
                            
                            // Calculate profile completion percentage
                            calculateProfileCompletion(user)
                            
                            _uiState.value = ProfileUiState.Success
                        }
                        is Result.Error -> {
                            _uiState.value = ProfileUiState.Error(result.message)
                            _errorMessage.value = result.message
                        }
                        is Result.Loading -> {
                            _uiState.value = ProfileUiState.Loading
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading user profile")
                _uiState.value = ProfileUiState.Error("Failed to load profile: ${e.message}")
                _errorMessage.value = "Failed to load profile: ${e.message}"
            }
        }
    }
    
    /**
     * Calculate profile completion percentage
     */
    private fun calculateProfileCompletion(user: User) {
        val fields = listOf(
            !user.name.isNullOrBlank(),
            !user.bio.isNullOrBlank(),
            user.age != null,
            !user.gender.isNullOrBlank(),
            !user.location.isNullOrBlank(),
            !user.interests.isNullOrEmpty(),
            !user.lookingFor.isNullOrEmpty(),
            !user.photos.isNullOrEmpty(),
            user.profilePictureUrl != null,
            user.minAgePreference != null,
            user.maxAgePreference != null,
            user.maxDistance != null
        )
        
        val completedFields = fields.count { it }
        val totalFields = fields.size
        
        _profileCompletionPercentage.value = ((completedFields.toFloat() / totalFields) * 100).toInt()
    }
    
    /**
     * Logout the user
     */
    fun logout(onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                val result = authRepository.logout()
                if (result is Result.Success) {
                    onSuccess()
                } else if (result is Result.Error) {
                    _errorMessage.value = result.message
                }
            } catch (e: Exception) {
                Timber.e(e, "Error logging out")
                _errorMessage.value = "Failed to logout: ${e.message}"
            }
        }
    }
    
    /**
     * Clear error message
     */
    fun clearErrorMessage() {
        _errorMessage.value = null
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