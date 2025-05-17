package com.kilagee.onelove.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kilagee.onelove.data.model.Match
import com.kilagee.onelove.data.model.MatchRequest
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
 * Match view model
 */
@HiltViewModel
class MatchViewModel @Inject constructor(
    private val matchRepository: MatchRepository
) : ViewModel() {
    
    private val _matches = MutableStateFlow<Result<List<Match>>>(Result.Loading)
    val matches: StateFlow<Result<List<Match>>> = _matches
    
    private val _receivedRequests = MutableStateFlow<Result<List<MatchRequest>>>(Result.Loading)
    val receivedRequests: StateFlow<Result<List<MatchRequest>>> = _receivedRequests
    
    private val _sentRequests = MutableStateFlow<Result<List<MatchRequest>>>(Result.Loading)
    val sentRequests: StateFlow<Result<List<MatchRequest>>> = _sentRequests
    
    private val _usersWhoLikedMe = MutableStateFlow<Result<List<User>>>(Result.Loading)
    val usersWhoLikedMe: StateFlow<Result<List<User>>> = _usersWhoLikedMe
    
    private val _recentMatches = MutableStateFlow<Result<List<Match>>>(Result.Loading)
    val recentMatches: StateFlow<Result<List<Match>>> = _recentMatches
    
    private val _unreadRequestCount = MutableStateFlow(0)
    val unreadRequestCount: StateFlow<Int> = _unreadRequestCount
    
    private val _matchActionState = MutableStateFlow<MatchActionState>(MatchActionState.Idle)
    val matchActionState: StateFlow<MatchActionState> = _matchActionState
    
    init {
        loadMatches()
        loadReceivedRequests()
        loadSentRequests()
        loadUsersWhoLikedMe()
        loadRecentMatches()
        observeUnreadRequestCount()
    }
    
    /**
     * Load all matches for the current user
     */
    fun loadMatches() {
        viewModelScope.launch {
            matchRepository.getMatches()
                .catch { e ->
                    Timber.e(e, "Error loading matches")
                    _matches.value = Result.Error("Failed to load matches: ${e.message}")
                }
                .collect { result ->
                    _matches.value = result
                }
        }
    }
    
    /**
     * Load match requests received by the current user
     */
    fun loadReceivedRequests() {
        viewModelScope.launch {
            matchRepository.getReceivedMatchRequests()
                .catch { e ->
                    Timber.e(e, "Error loading received match requests")
                    _receivedRequests.value = Result.Error("Failed to load match requests: ${e.message}")
                }
                .collect { result ->
                    _receivedRequests.value = result
                }
        }
    }
    
    /**
     * Load match requests sent by the current user
     */
    fun loadSentRequests() {
        viewModelScope.launch {
            matchRepository.getSentMatchRequests()
                .catch { e ->
                    Timber.e(e, "Error loading sent match requests")
                    _sentRequests.value = Result.Error("Failed to load match requests: ${e.message}")
                }
                .collect { result ->
                    _sentRequests.value = result
                }
        }
    }
    
    /**
     * Load users who have liked the current user
     */
    fun loadUsersWhoLikedMe() {
        viewModelScope.launch {
            matchRepository.getUsersWhoLikedMe()
                .catch { e ->
                    Timber.e(e, "Error loading users who liked me")
                    _usersWhoLikedMe.value = Result.Error("Failed to load users: ${e.message}")
                }
                .collect { result ->
                    _usersWhoLikedMe.value = result
                }
        }
    }
    
    /**
     * Load recent matches
     */
    fun loadRecentMatches() {
        viewModelScope.launch {
            matchRepository.getRecentMatches()
                .catch { e ->
                    Timber.e(e, "Error loading recent matches")
                    _recentMatches.value = Result.Error("Failed to load recent matches: ${e.message}")
                }
                .collect { result ->
                    _recentMatches.value = result
                }
        }
    }
    
    /**
     * Observe the count of unread match requests
     */
    private fun observeUnreadRequestCount() {
        viewModelScope.launch {
            matchRepository.getUnreadMatchRequestCount()
                .catch { e ->
                    Timber.e(e, "Error getting unread request count")
                    _unreadRequestCount.value = 0
                }
                .collect { count ->
                    _unreadRequestCount.value = count
                }
        }
    }
    
    /**
     * Send a match request
     * 
     * @param recipientId ID of the recipient
     * @param message Optional message to include with the request
     */
    fun sendMatchRequest(recipientId: String, message: String? = null) {
        viewModelScope.launch {
            try {
                _matchActionState.value = MatchActionState.Loading
                val result = matchRepository.sendMatchRequest(recipientId, message)
                
                when (result) {
                    is Result.Success -> {
                        _matchActionState.value = MatchActionState.RequestSent(result.data)
                        // Refresh sent requests
                        loadSentRequests()
                    }
                    is Result.Error -> {
                        _matchActionState.value = MatchActionState.Error(result.message)
                    }
                    is Result.Loading -> {
                        _matchActionState.value = MatchActionState.Loading
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error sending match request")
                _matchActionState.value = MatchActionState.Error(e.message ?: "Failed to send request")
            } finally {
                // Reset state after a delay
                kotlinx.coroutines.delay(2000)
                _matchActionState.value = MatchActionState.Idle
            }
        }
    }
    
    /**
     * Accept a match request
     * 
     * @param requestId ID of the request
     */
    fun acceptMatchRequest(requestId: String) {
        viewModelScope.launch {
            try {
                _matchActionState.value = MatchActionState.Loading
                val result = matchRepository.acceptMatchRequest(requestId)
                
                when (result) {
                    is Result.Success -> {
                        _matchActionState.value = MatchActionState.RequestAccepted(result.data)
                        // Refresh matches and requests
                        loadMatches()
                        loadReceivedRequests()
                        loadRecentMatches()
                    }
                    is Result.Error -> {
                        _matchActionState.value = MatchActionState.Error(result.message)
                    }
                    is Result.Loading -> {
                        _matchActionState.value = MatchActionState.Loading
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error accepting match request")
                _matchActionState.value = MatchActionState.Error(e.message ?: "Failed to accept request")
            } finally {
                // Reset state after a delay
                kotlinx.coroutines.delay(2000)
                _matchActionState.value = MatchActionState.Idle
            }
        }
    }
    
    /**
     * Decline a match request
     * 
     * @param requestId ID of the request
     */
    fun declineMatchRequest(requestId: String) {
        viewModelScope.launch {
            try {
                _matchActionState.value = MatchActionState.Loading
                val result = matchRepository.declineMatchRequest(requestId)
                
                when (result) {
                    is Result.Success -> {
                        _matchActionState.value = MatchActionState.RequestDeclined
                        // Refresh received requests
                        loadReceivedRequests()
                    }
                    is Result.Error -> {
                        _matchActionState.value = MatchActionState.Error(result.message)
                    }
                    is Result.Loading -> {
                        _matchActionState.value = MatchActionState.Loading
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error declining match request")
                _matchActionState.value = MatchActionState.Error(e.message ?: "Failed to decline request")
            } finally {
                // Reset state after a delay
                kotlinx.coroutines.delay(2000)
                _matchActionState.value = MatchActionState.Idle
            }
        }
    }
    
    /**
     * Mark a match request as viewed
     * 
     * @param requestId ID of the request
     */
    fun markMatchRequestAsViewed(requestId: String) {
        viewModelScope.launch {
            try {
                matchRepository.markMatchRequestAsViewed(requestId)
            } catch (e: Exception) {
                Timber.e(e, "Error marking match request as viewed")
            }
        }
    }
    
    /**
     * Cancel a match
     * 
     * @param matchId ID of the match
     */
    fun cancelMatch(matchId: String) {
        viewModelScope.launch {
            try {
                _matchActionState.value = MatchActionState.Loading
                val result = matchRepository.cancelMatch(matchId)
                
                when (result) {
                    is Result.Success -> {
                        _matchActionState.value = MatchActionState.MatchCancelled
                        // Refresh matches
                        loadMatches()
                    }
                    is Result.Error -> {
                        _matchActionState.value = MatchActionState.Error(result.message)
                    }
                    is Result.Loading -> {
                        _matchActionState.value = MatchActionState.Loading
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error cancelling match")
                _matchActionState.value = MatchActionState.Error(e.message ?: "Failed to cancel match")
            } finally {
                // Reset state after a delay
                kotlinx.coroutines.delay(2000)
                _matchActionState.value = MatchActionState.Idle
            }
        }
    }
    
    /**
     * Like a user
     * 
     * @param userId ID of the user to like
     */
    fun likeUser(userId: String) {
        viewModelScope.launch {
            try {
                _matchActionState.value = MatchActionState.Loading
                val result = matchRepository.likeUser(userId)
                
                when (result) {
                    is Result.Success -> {
                        if (result.data != null) {
                            // A match was created (mutual like)
                            _matchActionState.value = MatchActionState.MatchCreated(result.data)
                            // Refresh matches
                            loadMatches()
                            loadRecentMatches()
                        } else {
                            _matchActionState.value = MatchActionState.UserLiked
                        }
                        // Refresh users who liked me
                        loadUsersWhoLikedMe()
                    }
                    is Result.Error -> {
                        _matchActionState.value = MatchActionState.Error(result.message)
                    }
                    is Result.Loading -> {
                        _matchActionState.value = MatchActionState.Loading
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error liking user")
                _matchActionState.value = MatchActionState.Error(e.message ?: "Failed to like user")
            } finally {
                // Reset state after a delay
                kotlinx.coroutines.delay(2000)
                _matchActionState.value = MatchActionState.Idle
            }
        }
    }
    
    /**
     * Unlike a user
     * 
     * @param userId ID of the user to unlike
     */
    fun unlikeUser(userId: String) {
        viewModelScope.launch {
            try {
                _matchActionState.value = MatchActionState.Loading
                val result = matchRepository.unlikeUser(userId)
                
                when (result) {
                    is Result.Success -> {
                        _matchActionState.value = MatchActionState.UserUnliked
                        // Refresh users who liked me
                        loadUsersWhoLikedMe()
                    }
                    is Result.Error -> {
                        _matchActionState.value = MatchActionState.Error(result.message)
                    }
                    is Result.Loading -> {
                        _matchActionState.value = MatchActionState.Loading
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error unliking user")
                _matchActionState.value = MatchActionState.Error(e.message ?: "Failed to unlike user")
            } finally {
                // Reset state after a delay
                kotlinx.coroutines.delay(2000)
                _matchActionState.value = MatchActionState.Idle
            }
        }
    }
}

/**
 * Match action state
 */
sealed class MatchActionState {
    object Idle : MatchActionState()
    object Loading : MatchActionState()
    data class RequestSent(val request: MatchRequest) : MatchActionState()
    data class RequestAccepted(val match: Match) : MatchActionState()
    object RequestDeclined : MatchActionState()
    object MatchCancelled : MatchActionState()
    object UserLiked : MatchActionState()
    object UserUnliked : MatchActionState()
    data class MatchCreated(val match: Match) : MatchActionState()
    data class Error(val message: String) : MatchActionState()
}