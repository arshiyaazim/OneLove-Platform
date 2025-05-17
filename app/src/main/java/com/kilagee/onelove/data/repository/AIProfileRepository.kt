package com.kilagee.onelove.data.repository

import android.net.Uri
import com.kilagee.onelove.data.model.AIBehavior
import com.kilagee.onelove.data.model.AIPersonalityType
import com.kilagee.onelove.data.model.AIProfile
import com.kilagee.onelove.data.model.Result
import com.kilagee.onelove.data.model.UserGender
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for AI Profile operations
 */
interface AIProfileRepository {
    /**
     * Get AI profile by ID
     */
    suspend fun getAIProfileById(profileId: String): Result<AIProfile>
    
    /**
     * Get all AI profiles
     */
    suspend fun getAllAIProfiles(limit: Int = 20): Result<List<AIProfile>>
    
    /**
     * Get AI profiles as Flow
     */
    fun getAIProfilesFlow(): Flow<List<AIProfile>>
    
    /**
     * Get AI profiles by category
     */
    suspend fun getAIProfilesByCategory(category: String, limit: Int = 20): Result<List<AIProfile>>
    
    /**
     * Get popular AI profiles
     */
    suspend fun getPopularAIProfiles(limit: Int = 10): Result<List<AIProfile>>
    
    /**
     * Get AI profiles by personality type
     */
    suspend fun getAIProfilesByPersonality(
        personalityType: AIPersonalityType,
        limit: Int = 10
    ): Result<List<AIProfile>>
    
    /**
     * Get AI profiles by gender
     */
    suspend fun getAIProfilesByGender(
        gender: UserGender,
        limit: Int = 10
    ): Result<List<AIProfile>>
    
    /**
     * Create a new AI profile (admin only)
     */
    suspend fun createAIProfile(
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
        isPremiumOnly: Boolean = false,
        onProgress: ((Float) -> Unit)? = null
    ): Result<AIProfile>
    
    /**
     * Update AI profile (admin only)
     */
    suspend fun updateAIProfile(
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
        isActive: Boolean? = null,
        onProgress: ((Float) -> Unit)? = null
    ): Result<AIProfile>
    
    /**
     * Add gallery image to AI profile (admin only)
     */
    suspend fun addAIProfileGalleryImage(
        profileId: String,
        imageUri: Uri,
        onProgress: ((Float) -> Unit)? = null
    ): Result<String>
    
    /**
     * Remove gallery image from AI profile (admin only)
     */
    suspend fun removeAIProfileGalleryImage(
        profileId: String,
        imageUrl: String
    ): Result<Unit>
    
    /**
     * Update AI profile voice (admin only)
     */
    suspend fun updateAIProfileVoice(
        profileId: String,
        voiceUri: Uri,
        onProgress: ((Float) -> Unit)? = null
    ): Result<String>
    
    /**
     * Get AI message response
     */
    suspend fun getAIMessageResponse(
        profileId: String,
        userId: String,
        message: String,
        chatContext: List<String> = emptyList()
    ): Result<String>
    
    /**
     * Generate AI message suggestions
     */
    suspend fun generateAIMessageSuggestions(
        profileId: String,
        userId: String,
        contextMessages: List<String> = emptyList(),
        count: Int = 3
    ): Result<List<String>>
    
    /**
     * Add AI profile to user favorites
     */
    suspend fun addAIProfileToFavorites(
        userId: String,
        profileId: String
    ): Result<Unit>
    
    /**
     * Remove AI profile from user favorites
     */
    suspend fun removeAIProfileFromFavorites(
        userId: String,
        profileId: String
    ): Result<Unit>
    
    /**
     * Get user's favorite AI profiles
     */
    suspend fun getUserFavoriteAIProfiles(
        userId: String
    ): Result<List<AIProfile>>
    
    /**
     * Rate AI profile
     */
    suspend fun rateAIProfile(
        profileId: String,
        userId: String,
        rating: Double,
        feedback: String? = null
    ): Result<Unit>
    
    /**
     * Search AI profiles
     */
    suspend fun searchAIProfiles(
        query: String,
        limit: Int = 20
    ): Result<List<AIProfile>>
    
    /**
     * Get recommended AI profiles for user
     */
    suspend fun getRecommendedAIProfiles(
        userId: String,
        limit: Int = 5
    ): Result<List<AIProfile>>
    
    /**
     * Check if user can access premium AI profiles
     */
    suspend fun canAccessPremiumAIProfiles(
        userId: String
    ): Result<Boolean>
    
    /**
     * Increment AI profile interaction count
     */
    suspend fun incrementAIProfileInteractionCount(
        profileId: String
    ): Result<Unit>
}