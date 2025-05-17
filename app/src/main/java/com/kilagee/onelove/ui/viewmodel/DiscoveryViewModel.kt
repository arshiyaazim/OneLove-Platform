package com.kilagee.onelove.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kilagee.onelove.data.model.User
import com.kilagee.onelove.domain.repository.MatchRepository
import com.kilagee.onelove.domain.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Discovery view model for finding and suggesting users
 */
@HiltViewModel
class DiscoveryViewModel @Inject constructor(
    private val matchRepository: MatchRepository
) : ViewModel() {
    
    private val _suggestions = MutableStateFlow<Result<List<User>>>(Result.Loading)
    val suggestions: StateFlow<Result<List<User>>> = _suggestions
    
    private val _searchResults = MutableStateFlow<Result<List<User>>>(Result.Loading)
    val searchResults: StateFlow<Result<List<User>>> = _searchResults
    
    private val _currentFilters = MutableStateFlow(DiscoveryFilters())
    val currentFilters: StateFlow<DiscoveryFilters> = _currentFilters
    
    private val _actionState = MutableStateFlow<DiscoveryActionState>(DiscoveryActionState.Idle)
    val actionState: StateFlow<DiscoveryActionState> = _actionState
    
    init {
        loadSuggestions()
    }
    
    /**
     * Load user suggestions
     */
    fun loadSuggestions() {
        viewModelScope.launch {
            matchRepository.getMatchSuggestions()
                .catch { e ->
                    Timber.e(e, "Error loading suggestions")
                    _suggestions.value = Result.Error("Failed to load suggestions: ${e.message}")
                }
                .collect { result ->
                    _suggestions.value = result
                }
        }
    }
    
    /**
     * Search for users based on filters
     */
    fun searchUsers() {
        viewModelScope.launch {
            _searchResults.value = Result.Loading
            
            val filters = _currentFilters.value
            matchRepository.findUsers(
                minAge = filters.minAge,
                maxAge = filters.maxAge,
                distance = filters.maxDistance,
                interests = filters.interests
            )
                .catch { e ->
                    Timber.e(e, "Error searching users")
                    _searchResults.value = Result.Error("Failed to search users: ${e.message}")
                }
                .collect { result ->
                    _searchResults.value = result
                }
        }
    }
    
    /**
     * Update discovery filters
     * 
     * @param filters New discovery filters
     */
    fun updateFilters(filters: DiscoveryFilters) {
        _currentFilters.value = filters
        // Search with new filters
        searchUsers()
    }
    
    /**
     * Update minimum age filter
     * 
     * @param minAge Minimum age
     */
    fun updateMinAge(minAge: Int?) {
        _currentFilters.value = _currentFilters.value.copy(minAge = minAge)
    }
    
    /**
     * Update maximum age filter
     * 
     * @param maxAge Maximum age
     */
    fun updateMaxAge(maxAge: Int?) {
        _currentFilters.value = _currentFilters.value.copy(maxAge = maxAge)
    }
    
    /**
     * Update maximum distance filter
     * 
     * @param maxDistance Maximum distance in kilometers
     */
    fun updateMaxDistance(maxDistance: Int?) {
        _currentFilters.value = _currentFilters.value.copy(maxDistance = maxDistance)
    }
    
    /**
     * Update interests filter
     * 
     * @param interests List of interests
     */
    fun updateInterests(interests: List<String>?) {
        _currentFilters.value = _currentFilters.value.copy(interests = interests)
    }
    
    /**
     * Like a user
     * 
     * @param userId ID of the user to like
     */
    fun likeUser(userId: String) {
        viewModelScope.launch {
            try {
                _actionState.value = DiscoveryActionState.Loading
                val result = matchRepository.likeUser(userId)
                
                when (result) {
                    is Result.Success -> {
                        if (result.data != null) {
                            // A match was created (mutual like)
                            _actionState.value = DiscoveryActionState.MatchCreated(result.data)
                        } else {
                            _actionState.value = DiscoveryActionState.UserLiked
                        }
                        // Refresh suggestions
                        loadSuggestions()
                    }
                    is Result.Error -> {
                        _actionState.value = DiscoveryActionState.Error(result.message)
                    }
                    is Result.Loading -> {
                        _actionState.value = DiscoveryActionState.Loading
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error liking user")
                _actionState.value = DiscoveryActionState.Error(e.message ?: "Failed to like user")
            } finally {
                // Reset state after a delay
                kotlinx.coroutines.delay(2000)
                _actionState.value = DiscoveryActionState.Idle
            }
        }
    }
    
    /**
     * Skip a user (don't show again in suggestions)
     * 
     * @param userId ID of the user to skip
     */
    fun skipUser(userId: String) {
        // In a real app, this would update a "skipped users" list in the backend
        // For now, we just refresh suggestions
        loadSuggestions()
    }
    
    /**
     * Send a direct match request to a user
     * 
     * @param userId ID of the user
     * @param message Optional message to include
     */
    fun sendMatchRequest(userId: String, message: String? = null) {
        viewModelScope.launch {
            try {
                _actionState.value = DiscoveryActionState.Loading
                val result = matchRepository.sendMatchRequest(userId, message)
                
                when (result) {
                    is Result.Success -> {
                        _actionState.value = DiscoveryActionState.RequestSent
                        // Refresh suggestions
                        loadSuggestions()
                    }
                    is Result.Error -> {
                        _actionState.value = DiscoveryActionState.Error(result.message)
                    }
                    is Result.Loading -> {
                        _actionState.value = DiscoveryActionState.Loading
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error sending match request")
                _actionState.value = DiscoveryActionState.Error(e.message ?: "Failed to send request")
            } finally {
                // Reset state after a delay
                kotlinx.coroutines.delay(2000)
                _actionState.value = DiscoveryActionState.Idle
            }
        }
    }
}

/**
 * Discovery filters for searching users
 */
data class DiscoveryFilters(
    val minAge: Int? = null,
    val maxAge: Int? = null,
    val maxDistance: Int? = null,
    val interests: List<String>? = null
)

/**
 * Discovery action state
 */
sealed class DiscoveryActionState {
    object Idle : DiscoveryActionState()
    object Loading : DiscoveryActionState()
    object UserLiked : DiscoveryActionState()
    object RequestSent : DiscoveryActionState()
    data class MatchCreated(val match: com.kilagee.onelove.data.model.Match) : DiscoveryActionState()
    data class Error(val message: String) : DiscoveryActionState()
}