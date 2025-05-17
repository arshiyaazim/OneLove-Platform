package com.kilagee.onelove.ui.matching

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kilagee.onelove.data.model.User
import com.kilagee.onelove.domain.matching.MatchEngine
import com.kilagee.onelove.domain.model.Resource
import com.kilagee.onelove.domain.repository.MatchRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MatchViewModel @Inject constructor(
    private val matchRepository: MatchRepository
) : ViewModel() {
    
    // State for potential matches (users who can be liked/disliked)
    private val _potentialMatchesState = MutableStateFlow<Resource<List<MatchEngine.MatchResult>>>(Resource.Loading)
    val potentialMatchesState: StateFlow<Resource<List<MatchEngine.MatchResult>>> = _potentialMatchesState
    
    // State for matched users (mutual likes)
    private val _matchesState = MutableStateFlow<Resource<List<User>>>(Resource.Loading)
    val matchesState: StateFlow<Resource<List<User>>> = _matchesState
    
    // State for like action
    private val _likeState = MutableStateFlow<Resource<Boolean>?>(null)
    val likeState: StateFlow<Resource<Boolean>?> = _likeState
    
    // Current position in potential matches
    private var currentMatchIndex = 0
    
    init {
        loadPotentialMatches()
        loadMatches()
    }
    
    /**
     * Load potential matches for the user
     */
    fun loadPotentialMatches(minMatchPercentage: Int = 50) {
        viewModelScope.launch {
            matchRepository.getPotentialMatches(minMatchPercentage)
                .onEach { resource ->
                    _potentialMatchesState.value = resource
                    currentMatchIndex = 0
                }
                .catch { e ->
                    _potentialMatchesState.value = Resource.error("Failed to load matches: ${e.message}")
                }
                .launchIn(viewModelScope)
        }
    }
    
    /**
     * Load matches (mutual likes)
     */
    fun loadMatches() {
        viewModelScope.launch {
            matchRepository.getMatches()
                .onEach { resource ->
                    _matchesState.value = resource
                }
                .catch { e ->
                    _matchesState.value = Resource.error("Failed to load matches: ${e.message}")
                }
                .launchIn(viewModelScope)
        }
    }
    
    /**
     * Like the current user in the stack
     */
    fun likeCurrentUser() {
        viewModelScope.launch {
            val matches = (_potentialMatchesState.value as? Resource.Success)?.data ?: return@launch
            if (currentMatchIndex >= matches.size) return@launch
            
            val currentMatch = matches[currentMatchIndex]
            val userId = currentMatch.user.id
            
            _likeState.value = Resource.Loading
            
            matchRepository.likeUser(userId)
                .onEach { resource ->
                    _likeState.value = resource
                    
                    // Move to next potential match
                    if (resource is Resource.Success) {
                        currentMatchIndex++
                        
                        // If it's a match, refresh the matches list
                        if (resource.data) {
                            loadMatches()
                        }
                    }
                }
                .catch { e ->
                    _likeState.value = Resource.error("Failed to like user: ${e.message}")
                }
                .launchIn(viewModelScope)
        }
    }
    
    /**
     * Dislike/Skip the current user in the stack
     */
    fun skipCurrentUser() {
        viewModelScope.launch {
            val matches = (_potentialMatchesState.value as? Resource.Success)?.data ?: return@launch
            if (currentMatchIndex >= matches.size) return@launch
            
            val currentMatch = matches[currentMatchIndex]
            val userId = currentMatch.user.id
            
            matchRepository.rejectUser(userId)
                .onEach { resource ->
                    if (resource is Resource.Success) {
                        // Move to next potential match
                        currentMatchIndex++
                    }
                }
                .catch { e ->
                    // Just log error, don't update UI for skip failures
                    println("Failed to skip user: ${e.message}")
                }
                .launchIn(viewModelScope)
        }
    }
    
    /**
     * Unmatch with a user (remove from matches)
     */
    fun unmatchUser(userId: String) {
        viewModelScope.launch {
            matchRepository.unmatchUser(userId)
                .onEach { resource ->
                    if (resource is Resource.Success) {
                        // Refresh matches list
                        loadMatches()
                    }
                }
                .catch { e ->
                    println("Failed to unmatch user: ${e.message}")
                }
                .launchIn(viewModelScope)
        }
    }
    
    /**
     * Get the current user to display
     */
    fun getCurrentPotentialMatch(): MatchEngine.MatchResult? {
        val matches = (_potentialMatchesState.value as? Resource.Success)?.data ?: return null
        if (currentMatchIndex >= matches.size) return null
        return matches[currentMatchIndex]
    }
    
    /**
     * Reset like state
     */
    fun clearLikeState() {
        _likeState.value = null
    }
}