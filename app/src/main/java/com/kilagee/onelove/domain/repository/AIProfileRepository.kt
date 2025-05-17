package com.kilagee.onelove.domain.repository

import com.kilagee.onelove.data.model.AIProfile
import com.kilagee.onelove.data.model.AIProfileCategory
import com.kilagee.onelove.data.model.Message
import com.kilagee.onelove.domain.util.Result
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for AI profile operations
 */
interface AIProfileRepository {
    
    /**
     * Get available AI profiles
     */
    suspend fun getAvailableAIProfiles(limit: Int = 20): Result<List<AIProfile>>
    
    /**
     * Get AI profiles by category
     */
    suspend fun getAIProfilesByCategory(category: AIProfileCategory, limit: Int = 20): Result<List<AIProfile>>
    
    /**
     * Get popular AI profiles
     */
    suspend fun getPopularAIProfiles(limit: Int = 10): Result<List<AIProfile>>
    
    /**
     * Get new AI profiles
     */
    suspend fun getNewAIProfiles(limit: Int = 10): Result<List<AIProfile>>
    
    /**
     * Get premium AI profiles
     */
    suspend fun getPremiumAIProfiles(limit: Int = 10): Result<List<AIProfile>>
    
    /**
     * Get AI profile by ID
     */
    suspend fun getAIProfileById(profileId: String): Result<AIProfile>
    
    /**
     * Get AI profiles unlocked by a user
     */
    suspend fun getUnlockedAIProfiles(userId: String): Result<List<AIProfile>>
    
    /**
     * Check if a user has unlocked an AI profile
     */
    suspend fun hasUserUnlockedAIProfile(userId: String, profileId: String): Result<Boolean>
    
    /**
     * Unlock an AI profile for a user
     */
    suspend fun unlockAIProfile(userId: String, profileId: String): Result<Boolean>
    
    /**
     * Generate AI response message
     */
    suspend fun generateAIResponse(
        matchId: String,
        aiProfileId: String,
        userMessage: Message,
        previousMessages: List<Message> = listOf()
    ): Result<Message>
    
    /**
     * Get AI script triggers for a message
     */
    suspend fun getAIScriptTriggers(
        aiProfileId: String,
        message: String
    ): Result<List<String>>
    
    /**
     * Get available AI features for a user
     * (Based on subscription status)
     */
    suspend fun getAvailableAIFeatures(userId: String): Result<AIFeatures>
}

/**
 * Data class for AI features available to a user
 */
data class AIFeatures(
    val maxProfiles: Int = 2,
    val canAccessPremiumProfiles: Boolean = false,
    val messageLimit: Int = 20,
    val canInitiateConversation: Boolean = false,
    val canReceiveMedia: Boolean = false,
    val canSendMedia: Boolean = false,
    val conversationDepth: Int = 1 // Higher means more context-aware responses
)