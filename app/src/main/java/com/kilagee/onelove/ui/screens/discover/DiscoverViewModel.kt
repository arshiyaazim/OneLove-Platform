package com.kilagee.onelove.ui.screens.discover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kilagee.onelove.data.model.Match
import com.kilagee.onelove.data.model.MatchStatus
import com.kilagee.onelove.data.model.User
import com.kilagee.onelove.domain.repository.AuthRepository
import com.kilagee.onelove.domain.repository.DiscoverRepository
import com.kilagee.onelove.domain.repository.UserRepository
import com.kilagee.onelove.domain.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Date
import java.util.UUID
import javax.inject.Inject

/**
 * ViewModel for the discover screen
 */
@HiltViewModel
class DiscoverViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val discoverRepository: DiscoverRepository
) : ViewModel() {
    
    private val _discoverState = MutableStateFlow<DiscoverState>(DiscoverState.Loading)
    val discoverState: StateFlow<DiscoverState> = _discoverState.asStateFlow()
    
    private val _currentUserProfile = MutableStateFlow<User?>(null)
    val currentUserProfile: StateFlow<User?> = _currentUserProfile.asStateFlow()
    
    private val _potentialMatches = MutableStateFlow<List<User>>(emptyList())
    val potentialMatches: StateFlow<List<User>> = _potentialMatches.asStateFlow()
    
    /**
     * Initialize the discover screen
     */
    init {
        loadCurrentUser()
        loadPotentialMatches()
    }
    
    /**
     * Load the current user profile
     */
    private fun loadCurrentUser() {
        viewModelScope.launch {
            authRepository.getCurrentUser().collect { result ->
                when (result) {
                    is Result.Success -> {
                        _currentUserProfile.value = result.data
                    }
                    is Result.Error -> {
                        Timber.e("Error loading current user: ${result.message}")
                        _discoverState.value = DiscoverState.Error(result.message)
                    }
                    is Result.Loading -> {
                        _discoverState.value = DiscoverState.Loading
                    }
                }
            }
        }
    }
    
    /**
     * Load potential matches for the user
     */
    private fun loadPotentialMatches() {
        viewModelScope.launch {
            _discoverState.value = DiscoverState.Loading
            
            try {
                discoverRepository.getPotentialMatches().collectLatest { result ->
                    when (result) {
                        is Result.Success -> {
                            _potentialMatches.value = result.data
                            if (result.data.isEmpty()) {
                                _discoverState.value = DiscoverState.Empty
                            } else {
                                _discoverState.value = DiscoverState.Success
                            }
                        }
                        is Result.Error -> {
                            Timber.e("Error loading potential matches: ${result.message}")
                            _discoverState.value = DiscoverState.Error(result.message)
                        }
                        is Result.Loading -> {
                            _discoverState.value = DiscoverState.Loading
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Exception while loading potential matches")
                _discoverState.value = DiscoverState.Error(e.message ?: "Unknown error occurred")
            }
        }
    }
    
    /**
     * Like a user - attempt to create a match
     */
    fun likeUser(user: User) {
        viewModelScope.launch {
            try {
                val currentUserId = authRepository.getCurrentUserId()
                
                if (currentUserId != null) {
                    val match = Match(
                        id = UUID.randomUUID().toString(),
                        userId = currentUserId,
                        matchedUserId = user.id,
                        status = MatchStatus.PENDING,
                        createdAt = Date(),
                        updatedAt = Date()
                    )
                    
                    val result = discoverRepository.createMatch(match)
                    if (result is Result.Error) {
                        Timber.e("Error creating match: ${result.message}")
                    }

                    // Check if this is a mutual match
                    discoverRepository.checkForMutualMatch(currentUserId, user.id)
                    
                    // Remove the user from potential matches
                    val updatedMatches = _potentialMatches.value.toMutableList()
                    updatedMatches.remove(user)
                    _potentialMatches.value = updatedMatches
                    
                    if (updatedMatches.isEmpty()) {
                        _discoverState.value = DiscoverState.Empty
                    }
                } else {
                    Timber.e("Current user ID is null")
                }
            } catch (e: Exception) {
                Timber.e(e, "Exception while liking user")
            }
        }
    }
    
    /**
     * Dislike a user - skip this potential match
     */
    fun dislikeUser(user: User) {
        viewModelScope.launch {
            try {
                val currentUserId = authRepository.getCurrentUserId()
                
                if (currentUserId != null) {
                    // Save the dislike to prevent showing this user again
                    discoverRepository.skipUser(currentUserId, user.id)
                    
                    // Remove the user from potential matches
                    val updatedMatches = _potentialMatches.value.toMutableList()
                    updatedMatches.remove(user)
                    _potentialMatches.value = updatedMatches
                    
                    if (updatedMatches.isEmpty()) {
                        _discoverState.value = DiscoverState.Empty
                    }
                } else {
                    Timber.e("Current user ID is null")
                }
            } catch (e: Exception) {
                Timber.e(e, "Exception while disliking user")
            }
        }
    }
    
    /**
     * Refresh potential matches
     */
    fun refreshMatches() {
        loadPotentialMatches()
    }
}

/**
 * Discover state sealed class
 */
sealed class DiscoverState {
    object Loading : DiscoverState()
    object Success : DiscoverState()
    object Empty : DiscoverState()
    data class Error(val message: String) : DiscoverState()
}