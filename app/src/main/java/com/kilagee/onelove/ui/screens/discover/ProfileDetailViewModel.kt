package com.kilagee.onelove.ui.screens.discover

import androidx.lifecycle.SavedStateHandle
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
 * ViewModel for the profile detail screen
 */
@HiltViewModel
class ProfileDetailViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val userId: String = checkNotNull(savedStateHandle["userId"])
    
    private val _profileState = MutableStateFlow<ProfileDetailState>(ProfileDetailState.Loading)
    val profileState: StateFlow<ProfileDetailState> = _profileState.asStateFlow()
    
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()
    
    /**
     * Initialize and load profile data
     */
    init {
        loadProfile()
        loadCurrentUser()
    }
    
    /**
     * Load profile details
     */
    private fun loadProfile() {
        viewModelScope.launch {
            _profileState.value = ProfileDetailState.Loading
            
            try {
                userRepository.getUserById(userId).collectLatest { result ->
                    when (result) {
                        is Result.Success -> {
                            _profileState.value = ProfileDetailState.Success(result.data)
                        }
                        is Result.Error -> {
                            Timber.e("Error loading profile: ${result.message}")
                            _profileState.value = ProfileDetailState.Error(result.message)
                        }
                        is Result.Loading -> {
                            _profileState.value = ProfileDetailState.Loading
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Exception loading profile")
                _profileState.value = ProfileDetailState.Error(e.message ?: "Unknown error occurred")
            }
        }
    }
    
    /**
     * Load current user details
     */
    private fun loadCurrentUser() {
        viewModelScope.launch {
            authRepository.getCurrentUser().collectLatest { result ->
                if (result is Result.Success) {
                    _currentUser.value = result.data
                }
            }
        }
    }
    
    /**
     * Block a user
     */
    fun blockUser() {
        viewModelScope.launch {
            val currentUserId = authRepository.getCurrentUserId()
            if (currentUserId != null) {
                try {
                    val result = userRepository.blockUser(currentUserId, userId)
                    if (result is Result.Error) {
                        Timber.e("Error blocking user: ${result.message}")
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Exception blocking user")
                }
            }
        }
    }
    
    /**
     * Report a user
     */
    fun reportUser(reason: String) {
        viewModelScope.launch {
            val currentUserId = authRepository.getCurrentUserId()
            if (currentUserId != null) {
                try {
                    val result = userRepository.reportUser(currentUserId, userId, reason)
                    if (result is Result.Error) {
                        Timber.e("Error reporting user: ${result.message}")
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Exception reporting user")
                }
            }
        }
    }
}

/**
 * Profile detail state sealed class
 */
sealed class ProfileDetailState {
    object Loading : ProfileDetailState()
    data class Success(val user: User) : ProfileDetailState()
    data class Error(val message: String) : ProfileDetailState()
}