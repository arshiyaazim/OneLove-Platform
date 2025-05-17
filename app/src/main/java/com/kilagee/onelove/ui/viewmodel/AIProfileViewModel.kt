package com.kilagee.onelove.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kilagee.onelove.data.model.AIBehavior
import com.kilagee.onelove.data.model.AIPersonalityType
import com.kilagee.onelove.data.model.AIProfile
import com.kilagee.onelove.data.model.Result
import com.kilagee.onelove.data.model.UserGender
import com.kilagee.onelove.data.repository.AIProfileRepository
import com.kilagee.onelove.data.repository.AuthRepository
import com.kilagee.onelove.data.repository.SubscriptionRepository
import com.kilagee.onelove.util.AppError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Sealed class representing AI profile UI state
 */
sealed class AIProfileUiState {
    object Initial : AIProfileUiState()
    object Loading : AIProfileUiState()
    data class Error(val error: AppError, val message: String) : AIProfileUiState()
    data class Success<T>(val data: T) : AIProfileUiState()
}

/**
 * ViewModel for AI Profile operations
 */
@HiltViewModel
class AIProfileViewModel @Inject constructor(
    private val aiProfileRepository: AIProfileRepository,
    private val authRepository: AuthRepository,
    private val subscriptionRepository: SubscriptionRepository
) : ViewModel() {
    
    // Current AI profile UI state
    private val _aiProfileUiState = MutableStateFlow<AIProfileUiState>(AIProfileUiState.Initial)
    val aiProfileUiState: StateFlow<AIProfileUiState> = _aiProfileUiState.asStateFlow()
    
    // Current AI profile
    private val _currentAIProfile = MutableStateFlow<AIProfile?>(null)
    val currentAIProfile: StateFlow<AIProfile?> = _currentAIProfile.asStateFlow()
    
    // All AI profiles
    private val _aiProfiles = MutableStateFlow<List<AIProfile>>(emptyList())
    val aiProfiles: StateFlow<List<AIProfile>> = _aiProfiles.asStateFlow()
    
    // Popular AI profiles
    private val _popularAIProfiles = MutableStateFlow<List<AIProfile>>(emptyList())
    val popularAIProfiles: StateFlow<List<AIProfile>> = _popularAIProfiles.asStateFlow()
    
    // AI profiles by category
    private val _categoryAIProfiles = MutableStateFlow<Map<String, List<AIProfile>>>(emptyMap())
    val categoryAIProfiles: StateFlow<Map<String, List<AIProfile>>> = _categoryAIProfiles.asStateFlow()
    
    // User's favorite AI profiles
    private val _favoriteAIProfiles = MutableStateFlow<List<AIProfile>>(emptyList())
    val favoriteAIProfiles: StateFlow<List<AIProfile>> = _favoriteAIProfiles.asStateFlow()
    
    // Recommended AI profiles for user
    private val _recommendedAIProfiles = MutableStateFlow<List<AIProfile>>(emptyList())
    val recommendedAIProfiles: StateFlow<List<AIProfile>> = _recommendedAIProfiles.asStateFlow()
    
    // AI message suggestions
    private val _aiMessageSuggestions = MutableStateFlow<List<String>>(emptyList())
    val aiMessageSuggestions: StateFlow<List<String>> = _aiMessageSuggestions.asStateFlow()
    
    // Permission to access premium AI profiles
    private val _canAccessPremiumProfiles = MutableStateFlow(false)
    val canAccessPremiumProfiles: StateFlow<Boolean> = _canAccessPremiumProfiles.asStateFlow()
    
    // Upload progress for admin operations
    private val _uploadProgress = MutableStateFlow(0f)
    val uploadProgress: StateFlow<Float> = _uploadProgress.asStateFlow()
    
    // Search results
    private val _searchResults = MutableStateFlow<List<AIProfile>>(emptyList())
    val searchResults: StateFlow<List<AIProfile>> = _searchResults.asStateFlow()
    
    init {
        // Load AI profiles
        loadAIProfiles()
        loadPopularAIProfiles()
        
        // Check access to premium profiles
        val userId = authRepository.getCurrentUserId()
        if (userId != null) {
            checkPremiumAccess(userId)
            loadFavoriteAIProfiles(userId)
            loadRecommendedAIProfiles(userId)
        }
    }
    
    /**
     * Load all AI profiles
     */
    fun loadAIProfiles() {
        viewModelScope.launch {
            _aiProfileUiState.value = AIProfileUiState.Loading
            
            when (val result = aiProfileRepository.getAllAIProfiles()) {
                is Result.Success -> {
                    _aiProfiles.value = result.data
                    _aiProfileUiState.value = AIProfileUiState.Success(result.data)
                    
                    // Group by category
                    val byCategory = result.data.groupBy { it.category }
                    _categoryAIProfiles.value = byCategory
                }
                is Result.Error -> {
                    _aiProfileUiState.value = AIProfileUiState.Error(result.error, "Failed to load AI profiles")
                }
            }
        }
    }
    
    /**
     * Load AI profile by ID
     */
    fun loadAIProfileById(profileId: String) {
        viewModelScope.launch {
            _aiProfileUiState.value = AIProfileUiState.Loading
            
            when (val result = aiProfileRepository.getAIProfileById(profileId)) {
                is Result.Success -> {
                    _currentAIProfile.value = result.data
                    _aiProfileUiState.value = AIProfileUiState.Success(result.data)
                }
                is Result.Error -> {
                    _aiProfileUiState.value = AIProfileUiState.Error(result.error, "Failed to load AI profile")
                }
            }
        }
    }
    
    /**
     * Load popular AI profiles
     */
    private fun loadPopularAIProfiles() {
        viewModelScope.launch {
            when (val result = aiProfileRepository.getPopularAIProfiles()) {
                is Result.Success -> {
                    _popularAIProfiles.value = result.data
                }
                is Result.Error -> {
                    Timber.e(result.error.throwable, "Error loading popular AI profiles")
                }
            }
        }
    }
    
    /**
     * Load AI profiles by category
     */
    fun loadAIProfilesByCategory(category: String) {
        viewModelScope.launch {
            _aiProfileUiState.value = AIProfileUiState.Loading
            
            when (val result = aiProfileRepository.getAIProfilesByCategory(category)) {
                is Result.Success -> {
                    val profiles = result.data
                    _categoryAIProfiles.value = _categoryAIProfiles.value.toMutableMap().apply {
                        put(category, profiles)
                    }
                    _aiProfileUiState.value = AIProfileUiState.Success(profiles)
                }
                is Result.Error -> {
                    _aiProfileUiState.value = AIProfileUiState.Error(result.error, "Failed to load AI profiles for category")
                }
            }
        }
    }
    
    /**
     * Load AI profiles by personality type
     */
    fun loadAIProfilesByPersonality(personalityType: AIPersonalityType) {
        viewModelScope.launch {
            _aiProfileUiState.value = AIProfileUiState.Loading
            
            when (val result = aiProfileRepository.getAIProfilesByPersonality(personalityType)) {
                is Result.Success -> {
                    _aiProfileUiState.value = AIProfileUiState.Success(result.data)
                }
                is Result.Error -> {
                    _aiProfileUiState.value = AIProfileUiState.Error(result.error, "Failed to load AI profiles by personality")
                }
            }
        }
    }
    
    /**
     * Load AI profiles by gender
     */
    fun loadAIProfilesByGender(gender: UserGender) {
        viewModelScope.launch {
            _aiProfileUiState.value = AIProfileUiState.Loading
            
            when (val result = aiProfileRepository.getAIProfilesByGender(gender)) {
                is Result.Success -> {
                    _aiProfileUiState.value = AIProfileUiState.Success(result.data)
                }
                is Result.Error -> {
                    _aiProfileUiState.value = AIProfileUiState.Error(result.error, "Failed to load AI profiles by gender")
                }
            }
        }
    }
    
    /**
     * Get AI message response
     */
    fun getAIMessageResponse(message: String, chatContext: List<String> = emptyList()): StateFlow<AIProfileUiState> {
        val responseState = MutableStateFlow<AIProfileUiState>(AIProfileUiState.Loading)
        
        viewModelScope.launch {
            val profileId = _currentAIProfile.value?.id ?: return@launch
            val userId = authRepository.getCurrentUserId() ?: return@launch
            
            // Track interaction with this profile
            aiProfileRepository.incrementAIProfileInteractionCount(profileId)
            
            when (val result = aiProfileRepository.getAIMessageResponse(profileId, userId, message, chatContext)) {
                is Result.Success -> {
                    responseState.value = AIProfileUiState.Success(result.data)
                }
                is Result.Error -> {
                    responseState.value = AIProfileUiState.Error(result.error, "Failed to get AI response")
                }
            }
        }
        
        return responseState
    }
    
    /**
     * Generate AI message suggestions
     */
    fun generateAIMessageSuggestions(contextMessages: List<String> = emptyList(), count: Int = 3) {
        viewModelScope.launch {
            _aiProfileUiState.value = AIProfileUiState.Loading
            
            val profileId = _currentAIProfile.value?.id ?: return@launch
            val userId = authRepository.getCurrentUserId() ?: return@launch
            
            when (val result = aiProfileRepository.generateAIMessageSuggestions(profileId, userId, contextMessages, count)) {
                is Result.Success -> {
                    _aiMessageSuggestions.value = result.data
                    _aiProfileUiState.value = AIProfileUiState.Success(result.data)
                }
                is Result.Error -> {
                    _aiProfileUiState.value = AIProfileUiState.Error(result.error, "Failed to generate message suggestions")
                }
            }
        }
    }
    
    /**
     * Add AI profile to favorites
     */
    fun addAIProfileToFavorites(profileId: String) {
        viewModelScope.launch {
            _aiProfileUiState.value = AIProfileUiState.Loading
            
            val userId = authRepository.getCurrentUserId() ?: return@launch
            
            when (val result = aiProfileRepository.addAIProfileToFavorites(userId, profileId)) {
                is Result.Success -> {
                    _aiProfileUiState.value = AIProfileUiState.Success(Unit)
                    loadFavoriteAIProfiles(userId)
                }
                is Result.Error -> {
                    _aiProfileUiState.value = AIProfileUiState.Error(result.error, "Failed to add to favorites")
                }
            }
        }
    }
    
    /**
     * Remove AI profile from favorites
     */
    fun removeAIProfileFromFavorites(profileId: String) {
        viewModelScope.launch {
            _aiProfileUiState.value = AIProfileUiState.Loading
            
            val userId = authRepository.getCurrentUserId() ?: return@launch
            
            when (val result = aiProfileRepository.removeAIProfileFromFavorites(userId, profileId)) {
                is Result.Success -> {
                    _aiProfileUiState.value = AIProfileUiState.Success(Unit)
                    loadFavoriteAIProfiles(userId)
                }
                is Result.Error -> {
                    _aiProfileUiState.value = AIProfileUiState.Error(result.error, "Failed to remove from favorites")
                }
            }
        }
    }
    
    /**
     * Load user's favorite AI profiles
     */
    private fun loadFavoriteAIProfiles(userId: String) {
        viewModelScope.launch {
            when (val result = aiProfileRepository.getUserFavoriteAIProfiles(userId)) {
                is Result.Success -> {
                    _favoriteAIProfiles.value = result.data
                }
                is Result.Error -> {
                    Timber.e(result.error.throwable, "Error loading favorite AI profiles")
                }
            }
        }
    }
    
    /**
     * Rate AI profile
     */
    fun rateAIProfile(rating: Double, feedback: String? = null) {
        viewModelScope.launch {
            _aiProfileUiState.value = AIProfileUiState.Loading
            
            val profileId = _currentAIProfile.value?.id ?: return@launch
            val userId = authRepository.getCurrentUserId() ?: return@launch
            
            when (val result = aiProfileRepository.rateAIProfile(profileId, userId, rating, feedback)) {
                is Result.Success -> {
                    _aiProfileUiState.value = AIProfileUiState.Success(Unit)
                }
                is Result.Error -> {
                    _aiProfileUiState.value = AIProfileUiState.Error(result.error, "Failed to rate AI profile")
                }
            }
        }
    }
    
    /**
     * Search AI profiles
     */
    fun searchAIProfiles(query: String) {
        viewModelScope.launch {
            _aiProfileUiState.value = AIProfileUiState.Loading
            
            when (val result = aiProfileRepository.searchAIProfiles(query)) {
                is Result.Success -> {
                    _searchResults.value = result.data
                    _aiProfileUiState.value = AIProfileUiState.Success(result.data)
                }
                is Result.Error -> {
                    _aiProfileUiState.value = AIProfileUiState.Error(result.error, "Failed to search AI profiles")
                }
            }
        }
    }
    
    /**
     * Load recommended AI profiles for user
     */
    private fun loadRecommendedAIProfiles(userId: String) {
        viewModelScope.launch {
            when (val result = aiProfileRepository.getRecommendedAIProfiles(userId)) {
                is Result.Success -> {
                    _recommendedAIProfiles.value = result.data
                }
                is Result.Error -> {
                    Timber.e(result.error.throwable, "Error loading recommended AI profiles")
                }
            }
        }
    }
    
    /**
     * Check if user can access premium AI profiles
     */
    private fun checkPremiumAccess(userId: String) {
        viewModelScope.launch {
            when (val result = aiProfileRepository.canAccessPremiumAIProfiles(userId)) {
                is Result.Success -> {
                    _canAccessPremiumProfiles.value = result.data
                }
                is Result.Error -> {
                    Timber.e(result.error.throwable, "Error checking premium access")
                }
            }
        }
    }
    
    /**
     * Admin-only: Create new AI profile
     */
    fun createAIProfile(
        name: String,
        gender: UserGender,
        age: Int,
        bio: String,
        description: String,
        personality: AIPersonalityType,
        interests: List<String>,
        traits: List<String>,
        occupation: String,
        background: String,
        profileImageUri: Uri,
        galleryImageUris: List<Uri>? = null,
        voiceUri: Uri? = null,
        behaviors: List<AIBehavior> = emptyList(),
        greetings: List<String> = emptyList(),
        farewells: List<String> = emptyList(),
        questions: List<String> = emptyList(),
        responses: Map<String, List<String>> = emptyMap(),
        icebreakers: List<String> = emptyList(),
        category: String = "",
        tags: List<String> = emptyList(),
        isPremiumOnly: Boolean = false
    ) {
        viewModelScope.launch {
            _aiProfileUiState.value = AIProfileUiState.Loading
            _uploadProgress.value = 0f
            
            when (val result = aiProfileRepository.createAIProfile(
                name = name,
                gender = gender,
                age = age,
                bio = bio,
                description = description,
                personality = personality,
                interests = interests,
                traits = traits,
                occupation = occupation,
                background = background,
                profileImageUri = profileImageUri,
                galleryImageUris = galleryImageUris,
                voiceUri = voiceUri,
                behaviors = behaviors,
                greetings = greetings,
                farewells = farewells,
                questions = questions,
                responses = responses,
                icebreakers = icebreakers,
                category = category,
                tags = tags,
                isPremiumOnly = isPremiumOnly,
                onProgress = { progress -> _uploadProgress.value = progress }
            )) {
                is Result.Success -> {
                    _currentAIProfile.value = result.data
                    _aiProfileUiState.value = AIProfileUiState.Success(result.data)
                    
                    // Refresh AI profiles list
                    loadAIProfiles()
                }
                is Result.Error -> {
                    _aiProfileUiState.value = AIProfileUiState.Error(result.error, "Failed to create AI profile")
                }
            }
        }
    }
    
    /**
     * Admin-only: Update AI profile
     */
    fun updateAIProfile(
        profileId: String,
        name: String? = null,
        bio: String? = null,
        description: String? = null,
        interests: List<String>? = null,
        traits: List<String>? = null,
        occupation: String? = null,
        background: String? = null,
        profileImageUri: Uri? = null,
        behaviors: List<AIBehavior>? = null,
        greetings: List<String>? = null,
        farewells: List<String>? = null,
        questions: List<String>? = null,
        responses: Map<String, List<String>>? = null,
        icebreakers: List<String>? = null,
        category: String? = null,
        tags: List<String>? = null,
        isPremiumOnly: Boolean? = null,
        isActive: Boolean? = null
    ) {
        viewModelScope.launch {
            _aiProfileUiState.value = AIProfileUiState.Loading
            _uploadProgress.value = 0f
            
            when (val result = aiProfileRepository.updateAIProfile(
                profileId = profileId,
                name = name,
                bio = bio,
                description = description,
                interests = interests,
                traits = traits,
                occupation = occupation,
                background = background,
                profileImageUri = profileImageUri,
                behaviors = behaviors,
                greetings = greetings,
                farewells = farewells,
                questions = questions,
                responses = responses,
                icebreakers = icebreakers,
                category = category,
                tags = tags,
                isPremiumOnly = isPremiumOnly,
                isActive = isActive,
                onProgress = { progress -> _uploadProgress.value = progress }
            )) {
                is Result.Success -> {
                    _currentAIProfile.value = result.data
                    _aiProfileUiState.value = AIProfileUiState.Success(result.data)
                    
                    // Refresh AI profiles list
                    loadAIProfiles()
                }
                is Result.Error -> {
                    _aiProfileUiState.value = AIProfileUiState.Error(result.error, "Failed to update AI profile")
                }
            }
        }
    }
    
    /**
     * Admin-only: Add gallery image to AI profile
     */
    fun addGalleryImageToAIProfile(profileId: String, imageUri: Uri) {
        viewModelScope.launch {
            _aiProfileUiState.value = AIProfileUiState.Loading
            _uploadProgress.value = 0f
            
            when (val result = aiProfileRepository.addAIProfileGalleryImage(
                profileId = profileId,
                imageUri = imageUri,
                onProgress = { progress -> _uploadProgress.value = progress }
            )) {
                is Result.Success -> {
                    _aiProfileUiState.value = AIProfileUiState.Success(result.data)
                    
                    // Refresh current AI profile
                    loadAIProfileById(profileId)
                }
                is Result.Error -> {
                    _aiProfileUiState.value = AIProfileUiState.Error(result.error, "Failed to add gallery image")
                }
            }
        }
    }
    
    /**
     * Admin-only: Remove gallery image from AI profile
     */
    fun removeGalleryImageFromAIProfile(profileId: String, imageUrl: String) {
        viewModelScope.launch {
            _aiProfileUiState.value = AIProfileUiState.Loading
            
            when (val result = aiProfileRepository.removeAIProfileGalleryImage(profileId, imageUrl)) {
                is Result.Success -> {
                    _aiProfileUiState.value = AIProfileUiState.Success(Unit)
                    
                    // Refresh current AI profile
                    loadAIProfileById(profileId)
                }
                is Result.Error -> {
                    _aiProfileUiState.value = AIProfileUiState.Error(result.error, "Failed to remove gallery image")
                }
            }
        }
    }
    
    /**
     * Admin-only: Update AI profile voice
     */
    fun updateAIProfileVoice(profileId: String, voiceUri: Uri) {
        viewModelScope.launch {
            _aiProfileUiState.value = AIProfileUiState.Loading
            _uploadProgress.value = 0f
            
            when (val result = aiProfileRepository.updateAIProfileVoice(
                profileId = profileId,
                voiceUri = voiceUri,
                onProgress = { progress -> _uploadProgress.value = progress }
            )) {
                is Result.Success -> {
                    _aiProfileUiState.value = AIProfileUiState.Success(result.data)
                    
                    // Refresh current AI profile
                    loadAIProfileById(profileId)
                }
                is Result.Error -> {
                    _aiProfileUiState.value = AIProfileUiState.Error(result.error, "Failed to update voice")
                }
            }
        }
    }
    
    /**
     * Reset AI profile UI state
     */
    fun resetAIProfileState() {
        _aiProfileUiState.value = AIProfileUiState.Initial
    }
}