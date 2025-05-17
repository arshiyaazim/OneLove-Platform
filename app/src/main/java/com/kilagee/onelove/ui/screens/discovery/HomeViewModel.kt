package com.kilagee.onelove.ui.screens.discovery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kilagee.onelove.data.model.MatchRecommendation
import com.kilagee.onelove.data.model.User
import com.kilagee.onelove.domain.repository.AuthRepository
import com.kilagee.onelove.domain.repository.DiscoveryRepository
import com.kilagee.onelove.domain.repository.PreferencesRepository
import com.kilagee.onelove.domain.repository.UserRepository
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
import javax.inject.Inject

/**
 * ViewModel for the home discovery screen
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val discoveryRepository: DiscoveryRepository,
    private val userRepository: UserRepository,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {
    
    // UI state
    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
    // One-time events
    private val _events = MutableSharedFlow<HomeEvent>()
    val events: SharedFlow<HomeEvent> = _events.asSharedFlow()
    
    // Current user
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()
    
    // Discover cards
    private val _discoverCards = MutableStateFlow<List<DiscoverCard>>(emptyList())
    val discoverCards: StateFlow<List<DiscoverCard>> = _discoverCards.asStateFlow()
    
    // Top picks
    private val _topPicks = MutableStateFlow<List<DiscoverCard>>(emptyList())
    val topPicks: StateFlow<List<DiscoverCard>> = _topPicks.asStateFlow()
    
    // Users who liked you
    private val _likesYou = MutableStateFlow<List<User>>(emptyList())
    val likesYou: StateFlow<List<User>> = _likesYou.asStateFlow()
    
    // Recently active users
    private val _recentlyActive = MutableStateFlow<List<User>>(emptyList())
    val recentlyActive: StateFlow<List<User>> = _recentlyActive.asStateFlow()
    
    // Remaining likes count
    private val _remainingLikes = MutableStateFlow(0)
    val remainingLikes: StateFlow<Int> = _remainingLikes.asStateFlow()
    
    // Remaining super likes count
    private val _remainingSuperLikes = MutableStateFlow(0)
    val remainingSuperLikes: StateFlow<Int> = _remainingSuperLikes.asStateFlow()
    
    // Active jobs
    private var discoveryJob: Job? = null
    
    init {
        loadCurrentUser()
        startDiscoveryUpdates()
        loadPremiumContent()
        loadLimits()
    }
    
    /**
     * Load current user data
     */
    private fun loadCurrentUser() {
        viewModelScope.launch {
            val result = userRepository.getCurrentUser()
            
            when (result) {
                is Result.Success -> {
                    _currentUser.value = result.data
                }
                is Result.Error -> {
                    _events.emit(HomeEvent.Error(result.message ?: "Failed to load user data"))
                }
                else -> {
                    // Do nothing for Loading state
                }
            }
        }
    }
    
    /**
     * Start discovery updates
     */
    private fun startDiscoveryUpdates() {
        // Cancel any existing job
        discoveryJob?.cancel()
        
        // Start new job
        discoveryJob = viewModelScope.launch {
            discoveryRepository.getRecommendationsFlow().collectLatest { result ->
                when (result) {
                    is Result.Success -> {
                        val cards = result.data.map { recommendation ->
                            DiscoverCard(
                                recommendation = recommendation,
                                user = recommendation.recommendedUser
                            )
                        }
                        _discoverCards.value = cards
                        _uiState.value = if (cards.isEmpty()) {
                            HomeUiState.Empty
                        } else {
                            HomeUiState.Content
                        }
                    }
                    is Result.Error -> {
                        _events.emit(HomeEvent.Error(result.message ?: "Failed to load recommendations"))
                        _uiState.value = HomeUiState.Error(result.message ?: "Failed to load recommendations")
                    }
                    is Result.Loading -> {
                        _uiState.value = HomeUiState.Loading
                    }
                }
            }
        }
    }
    
    /**
     * Load premium content
     */
    private fun loadPremiumContent() {
        viewModelScope.launch {
            // Load top picks
            val topPicksResult = discoveryRepository.getTopPicks()
            if (topPicksResult is Result.Success) {
                val cards = topPicksResult.data.map { recommendation ->
                    DiscoverCard(
                        recommendation = recommendation,
                        user = recommendation.recommendedUser
                    )
                }
                _topPicks.value = cards
            }
            
            // Load users who liked you
            val likesYouResult = discoveryRepository.getLikesYou()
            if (likesYouResult is Result.Success) {
                _likesYou.value = likesYouResult.data
            }
            
            // Load recently active users
            val recentlyActiveResult = discoveryRepository.getRecentlyActiveUsers()
            if (recentlyActiveResult is Result.Success) {
                _recentlyActive.value = recentlyActiveResult.data
            }
        }
    }
    
    /**
     * Load limits for likes, super likes, etc.
     */
    private fun loadLimits() {
        viewModelScope.launch {
            // Load remaining likes
            val remainingLikesResult = discoveryRepository.getRemainingLikesCount()
            if (remainingLikesResult is Result.Success) {
                _remainingLikes.value = remainingLikesResult.data
            }
            
            // Load remaining super likes
            val remainingSuperLikesResult = discoveryRepository.getRemainingSuperLikesCount()
            if (remainingSuperLikesResult is Result.Success) {
                _remainingSuperLikes.value = remainingSuperLikesResult.data
            }
        }
    }
    
    /**
     * Like a user
     */
    fun likeUser(userId: String) {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading
            
            val result = discoveryRepository.likeUser(userId)
            
            when (result) {
                is Result.Success -> {
                    // If result.data is not null, it's a match
                    val matchId = result.data
                    if (matchId != null) {
                        _events.emit(HomeEvent.Match(matchId))
                    }
                    
                    // Reload limits
                    loadLimits()
                    
                    // Move to next card
                    removeCard(userId)
                }
                is Result.Error -> {
                    _events.emit(HomeEvent.Error(result.message ?: "Failed to like user"))
                }
                else -> {
                    // Do nothing for Loading state
                }
            }
        }
    }
    
    /**
     * Super like a user
     */
    fun superLikeUser(userId: String) {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading
            
            val result = discoveryRepository.superLikeUser(userId)
            
            when (result) {
                is Result.Success -> {
                    // If result.data is not null, it's a match
                    val matchId = result.data
                    if (matchId != null) {
                        _events.emit(HomeEvent.Match(matchId))
                    } else {
                        _events.emit(HomeEvent.SuperLiked)
                    }
                    
                    // Reload limits
                    loadLimits()
                    
                    // Move to next card
                    removeCard(userId)
                }
                is Result.Error -> {
                    _events.emit(HomeEvent.Error(result.message ?: "Failed to super like user"))
                }
                else -> {
                    // Do nothing for Loading state
                }
            }
        }
    }
    
    /**
     * Pass on a user
     */
    fun passUser(userId: String) {
        viewModelScope.launch {
            val result = discoveryRepository.passUser(userId)
            
            if (result is Result.Success) {
                // Move to next card
                removeCard(userId)
            } else if (result is Result.Error) {
                _events.emit(HomeEvent.Error(result.message ?: "Failed to pass user"))
            }
        }
    }
    
    /**
     * Boost visibility
     */
    fun boostVisibility() {
        viewModelScope.launch {
            val result = discoveryRepository.boostVisibility()
            
            when (result) {
                is Result.Success -> {
                    _events.emit(HomeEvent.BoostActivated(result.data))
                }
                is Result.Error -> {
                    _events.emit(HomeEvent.Error(result.message ?: "Failed to activate boost"))
                }
                else -> {
                    // Do nothing for Loading state
                }
            }
        }
    }
    
    /**
     * Undo last action
     */
    fun undoLastAction() {
        viewModelScope.launch {
            val result = discoveryRepository.undoLastAction()
            
            when (result) {
                is Result.Success -> {
                    _events.emit(HomeEvent.ActionUndone(result.data))
                    // Reload discovery to get back the undone card
                    loadDiscoveryCards()
                }
                is Result.Error -> {
                    _events.emit(HomeEvent.Error(result.message ?: "Failed to undo action"))
                }
                else -> {
                    // Do nothing for Loading state
                }
            }
        }
    }
    
    /**
     * Block a user
     */
    fun blockUser(userId: String, reason: String? = null) {
        viewModelScope.launch {
            val result = discoveryRepository.blockUser(userId, reason)
            
            when (result) {
                is Result.Success -> {
                    _events.emit(HomeEvent.UserBlocked)
                    // Move to next card
                    removeCard(userId)
                }
                is Result.Error -> {
                    _events.emit(HomeEvent.Error(result.message ?: "Failed to block user"))
                }
                else -> {
                    // Do nothing for Loading state
                }
            }
        }
    }
    
    /**
     * Report a user
     */
    fun reportUser(userId: String, reason: String, details: String? = null) {
        viewModelScope.launch {
            val result = discoveryRepository.reportUser(userId, reason, details)
            
            when (result) {
                is Result.Success -> {
                    _events.emit(HomeEvent.UserReported)
                    // Move to next card
                    removeCard(userId)
                }
                is Result.Error -> {
                    _events.emit(HomeEvent.Error(result.message ?: "Failed to report user"))
                }
                else -> {
                    // Do nothing for Loading state
                }
            }
        }
    }
    
    /**
     * Reload discovery cards
     */
    fun loadDiscoveryCards() {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading
            
            val result = discoveryRepository.getRecommendations()
            
            when (result) {
                is Result.Success -> {
                    val cards = result.data.map { recommendation ->
                        DiscoverCard(
                            recommendation = recommendation,
                            user = recommendation.recommendedUser
                        )
                    }
                    _discoverCards.value = cards
                    _uiState.value = if (cards.isEmpty()) {
                        HomeUiState.Empty
                    } else {
                        HomeUiState.Content
                    }
                }
                is Result.Error -> {
                    _events.emit(HomeEvent.Error(result.message ?: "Failed to load recommendations"))
                    _uiState.value = HomeUiState.Error(result.message ?: "Failed to load recommendations")
                }
                else -> {
                    // Loading state is already set
                }
            }
        }
    }
    
    /**
     * Remove a card from the list
     */
    private fun removeCard(userId: String) {
        _discoverCards.value = _discoverCards.value.filter { 
            it.user.id != userId 
        }
        
        // Check if we need to load more cards
        if (_discoverCards.value.isEmpty()) {
            _uiState.value = HomeUiState.Empty
            loadDiscoveryCards()
        } else {
            _uiState.value = HomeUiState.Content
        }
    }
    
    /**
     * Clear errors
     */
    fun clearErrors() {
        if (_uiState.value is HomeUiState.Error) {
            _uiState.value = if (_discoverCards.value.isEmpty()) {
                HomeUiState.Empty
            } else {
                HomeUiState.Content
            }
        }
    }
    
    /**
     * Refresh all data
     */
    fun refreshAll() {
        loadCurrentUser()
        loadDiscoveryCards()
        loadPremiumContent()
        loadLimits()
    }
    
    override fun onCleared() {
        super.onCleared()
        discoveryJob?.cancel()
    }
}

/**
 * UI state for the home screen
 */
sealed class HomeUiState {
    object Loading : HomeUiState()
    object Content : HomeUiState()
    object Empty : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}

/**
 * Events emitted by the home screen
 */
sealed class HomeEvent {
    data class Match(val matchId: String) : HomeEvent()
    object SuperLiked : HomeEvent()
    data class BoostActivated(val expiryTime: Long) : HomeEvent()
    data class ActionUndone(val userId: String) : HomeEvent()
    object UserBlocked : HomeEvent()
    object UserReported : HomeEvent()
    data class Error(val message: String) : HomeEvent()
}

/**
 * Discover card data class
 */
data class DiscoverCard(
    val recommendation: MatchRecommendation,
    val user: User
)