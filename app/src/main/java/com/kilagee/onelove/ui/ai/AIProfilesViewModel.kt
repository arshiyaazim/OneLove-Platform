package com.kilagee.onelove.ui.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kilagee.onelove.domain.model.AIProfile
import com.kilagee.onelove.domain.repository.AIProfileRepository
import com.kilagee.onelove.util.PremiumAccessManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the AI Profiles screen
 */
@HiltViewModel
class AIProfilesViewModel @Inject constructor(
    private val repository: AIProfileRepository,
    val premiumAccessManager: PremiumAccessManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(AIProfilesUIState())
    val uiState: StateFlow<AIProfilesUIState> = _uiState.asStateFlow()
    
    private val _searchQuery = MutableStateFlow("")
    
    private var allProfiles = listOf<AIProfile>()
    
    init {
        loadAIProfiles()
        
        // Search functionality with debounce
        viewModelScope.launch {
            @OptIn(FlowPreview::class)
            _searchQuery
                .debounce(300)
                .collectLatest { query ->
                    if (query.isBlank()) {
                        _uiState.update { it.copy(aiProfiles = allProfiles) }
                    } else {
                        val filteredProfiles = allProfiles.filter { profile ->
                            profile.name.contains(query, ignoreCase = true) ||
                            profile.personality.contains(query, ignoreCase = true) ||
                            profile.country.contains(query, ignoreCase = true) ||
                            profile.city.contains(query, ignoreCase = true) ||
                            profile.interests.any { it.contains(query, ignoreCase = true) }
                        }
                        _uiState.update { it.copy(aiProfiles = filteredProfiles) }
                    }
                }
        }
    }
    
    /**
     * Load AI profiles from repository
     */
    private fun loadAIProfiles() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            repository.getAllAIProfiles().collectLatest { profiles ->
                allProfiles = profiles
                _uiState.update { 
                    it.copy(
                        aiProfiles = profiles,
                        isLoading = false
                    )
                }
            }
        }
    }
    
    /**
     * Create the initial AI profiles for first time setup
     */
    fun createInitialProfiles() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            val result = repository.createInitialAIProfiles()
            
            if (result.isSuccess) {
                loadAIProfiles()
            } else {
                _uiState.update { 
                    it.copy(
                        error = result.exceptionOrNull()?.message ?: "Failed to create AI profiles",
                        isLoading = false
                    )
                }
            }
        }
    }
    
    /**
     * Handle search query changes
     */
    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }
    
    /**
     * Apply filters to the AI profiles
     */
    fun applyFilters(
        gender: String? = null,
        minAge: Int? = null,
        maxAge: Int? = null,
        personality: String? = null
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            repository.getAIProfiles(
                gender = gender,
                minAge = minAge,
                maxAge = maxAge,
                personality = personality,
                limit = 100
            ).collectLatest { profiles ->
                _uiState.update { 
                    it.copy(
                        aiProfiles = profiles,
                        isLoading = false
                    )
                }
            }
        }
    }
}

/**
 * UI state for the AI Profiles screen
 */
data class AIProfilesUIState(
    val aiProfiles: List<AIProfile> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)