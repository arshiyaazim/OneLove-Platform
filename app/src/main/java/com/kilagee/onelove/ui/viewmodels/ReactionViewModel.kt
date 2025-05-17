package com.kilagee.onelove.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kilagee.onelove.data.model.Reaction
import com.kilagee.onelove.data.model.ReactionSummary
import com.kilagee.onelove.data.model.ReactionTargetType
import com.kilagee.onelove.data.model.ReactionType
import com.kilagee.onelove.domain.repository.ReactionRepository
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
 * ViewModel for managing reactions to content
 */
@HiltViewModel
class ReactionViewModel @Inject constructor(
    private val reactionRepository: ReactionRepository
) : ViewModel() {
    
    // Current target
    private val _currentTargetId = MutableStateFlow<String?>(null)
    private val _currentTargetType = MutableStateFlow<ReactionTargetType?>(null)
    
    // Reaction summary
    private val _reactionSummary = MutableStateFlow<ReactionSummary?>(null)
    val reactionSummary: StateFlow<ReactionSummary?> = _reactionSummary.asStateFlow()
    
    // User's current reaction
    private val _userReaction = MutableStateFlow<ReactionType?>(null)
    val userReaction: StateFlow<ReactionType?> = _userReaction.asStateFlow()
    
    // Popular emojis for quick selection
    private val _popularEmojis = MutableStateFlow<List<ReactionType>>(emptyList())
    val popularEmojis: StateFlow<List<ReactionType>> = _popularEmojis.asStateFlow()
    
    // Loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    // Error events
    private val _errorEvents = MutableSharedFlow<String>()
    val errorEvents: SharedFlow<String> = _errorEvents.asSharedFlow()
    
    // Active jobs
    private var summaryJob: Job? = null
    
    init {
        loadPopularEmojis()
    }
    
    /**
     * Set the current target for reactions
     */
    fun setTarget(targetId: String, targetType: ReactionTargetType) {
        // Only update if different from current target
        if (_currentTargetId.value == targetId && _currentTargetType.value == targetType) {
            return
        }
        
        _currentTargetId.value = targetId
        _currentTargetType.value = targetType
        
        // Load reaction data for new target
        loadReactionSummary()
        loadUserReaction()
    }
    
    /**
     * Load reaction summary
     */
    private fun loadReactionSummary() {
        val targetId = _currentTargetId.value ?: return
        val targetType = _currentTargetType.value ?: return
        
        // Cancel any existing job
        summaryJob?.cancel()
        
        // Start new job
        summaryJob = viewModelScope.launch {
            reactionRepository.getReactionSummaryFlow(targetId, targetType).collectLatest { result ->
                when (result) {
                    is Result.Success -> {
                        _reactionSummary.value = result.data
                    }
                    is Result.Error -> {
                        _errorEvents.emit(result.message ?: "Failed to load reactions")
                    }
                    is Result.Loading -> {
                        _isLoading.value = true
                    }
                }
                
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Load user's current reaction
     */
    private fun loadUserReaction() {
        val targetId = _currentTargetId.value ?: return
        val targetType = _currentTargetType.value ?: return
        
        viewModelScope.launch {
            _isLoading.value = true
            
            val result = reactionRepository.getUserReaction(targetId, targetType)
            
            when (result) {
                is Result.Success -> {
                    _userReaction.value = result.data?.type
                }
                is Result.Error -> {
                    _errorEvents.emit(result.message ?: "Failed to load your reaction")
                }
                is Result.Loading -> {
                    // Already handled above
                }
            }
            
            _isLoading.value = false
        }
    }
    
    /**
     * Add or update a reaction
     */
    fun addReaction(reactionType: ReactionType) {
        val targetId = _currentTargetId.value ?: return
        val targetType = _currentTargetType.value ?: return
        
        viewModelScope.launch {
            _isLoading.value = true
            
            val result = reactionRepository.addReaction(
                targetId = targetId,
                targetType = targetType,
                reactionType = reactionType
            )
            
            when (result) {
                is Result.Success -> {
                    _userReaction.value = reactionType
                    
                    // Update popular emojis if this is a new emoji for the user
                    loadPopularEmojis()
                }
                is Result.Error -> {
                    _errorEvents.emit(result.message ?: "Failed to add reaction")
                }
                is Result.Loading -> {
                    // Already handled above
                }
            }
            
            _isLoading.value = false
        }
    }
    
    /**
     * Remove a reaction
     */
    fun removeReaction() {
        val targetId = _currentTargetId.value ?: return
        val targetType = _currentTargetType.value ?: return
        
        viewModelScope.launch {
            _isLoading.value = true
            
            val result = reactionRepository.removeReaction(targetId, targetType)
            
            when (result) {
                is Result.Success -> {
                    _userReaction.value = null
                }
                is Result.Error -> {
                    _errorEvents.emit(result.message ?: "Failed to remove reaction")
                }
                is Result.Loading -> {
                    // Already handled above
                }
            }
            
            _isLoading.value = false
        }
    }
    
    /**
     * Toggle a reaction (add if not present, remove if already selected)
     */
    fun toggleReaction(reactionType: ReactionType) {
        if (_userReaction.value == reactionType) {
            removeReaction()
        } else {
            addReaction(reactionType)
        }
    }
    
    /**
     * Load popular emojis for the current user
     */
    private fun loadPopularEmojis() {
        viewModelScope.launch {
            val result = reactionRepository.getPopularEmojis()
            
            if (result is Result.Success) {
                val emojis = result.data
                if (emojis.isEmpty()) {
                    // If user has no history, use a default set of popular emojis
                    _popularEmojis.value = listOf(
                        ReactionType.LIKE,
                        ReactionType.LOVE,
                        ReactionType.HAHA,
                        ReactionType.WOW,
                        ReactionType.FIRE,
                        ReactionType.THUMBS_UP
                    )
                } else {
                    _popularEmojis.value = emojis
                }
            } else {
                // Fallback to defaults if loading fails
                _popularEmojis.value = listOf(
                    ReactionType.LIKE,
                    ReactionType.LOVE,
                    ReactionType.HAHA,
                    ReactionType.WOW,
                    ReactionType.FIRE,
                    ReactionType.THUMBS_UP
                )
            }
        }
    }
    
    /**
     * Get reactions for a list of targets (e.g., to show on multiple chat messages)
     */
    fun loadReactionsForTargets(targetIds: List<String>, targetType: ReactionTargetType) {
        viewModelScope.launch {
            val summaries = mutableMapOf<String, ReactionSummary>()
            
            // Load reaction summaries for each target
            targetIds.forEach { targetId ->
                val result = reactionRepository.getReactionSummary(targetId, targetType)
                if (result is Result.Success) {
                    summaries[targetId] = result.data
                }
            }
            
            // Load user's reactions on these targets
            val userReactionsResult = reactionRepository.getUserReactions(targetIds, targetType)
            if (userReactionsResult is Result.Success) {
                val userReactions = userReactionsResult.data
                
                // Update reaction summaries with user reaction info
                summaries.forEach { (targetId, summary) ->
                    val userReaction = userReactions[targetId]
                    if (userReaction != null) {
                        // Update user reaction in summary
                        summaries[targetId] = summary.copy(userReaction = userReaction.type)
                    }
                }
            }
            
            // Emit updates for each target
            // In a real implementation, we would update a map or collection in the ViewModel
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        summaryJob?.cancel()
    }
}