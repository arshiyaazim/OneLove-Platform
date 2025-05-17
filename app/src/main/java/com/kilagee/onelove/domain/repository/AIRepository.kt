package com.kilagee.onelove.domain.repository

import com.kilagee.onelove.data.model.AIInteraction
import com.kilagee.onelove.data.model.AIMessage
import com.kilagee.onelove.data.model.AIProfile
import com.kilagee.onelove.data.model.ReactionType
import com.kilagee.onelove.domain.util.Result
import kotlinx.coroutines.flow.Flow

/**
 * Interface for AI-related operations
 */
interface AIRepository {
    
    /**
     * Get all AI profiles
     * @param includePremium Whether to include premium profiles
     * @param limit Maximum number of profiles to fetch
     * @return List of [AIProfile] objects
     */
    suspend fun getAIProfiles(includePremium: Boolean = true, limit: Int = 20): Result<List<AIProfile>>
    
    /**
     * Get AI profiles as a flow for real-time updates
     * @param includePremium Whether to include premium profiles
     * @param limit Maximum number of profiles to fetch
     * @return Flow of [AIProfile] lists
     */
    fun getAIProfilesFlow(includePremium: Boolean = true, limit: Int = 20): Flow<Result<List<AIProfile>>>
    
    /**
     * Get a specific AI profile
     * @param profileId ID of the profile to retrieve
     * @return The [AIProfile] object
     */
    suspend fun getAIProfile(profileId: String): Result<AIProfile>
    
    /**
     * Create a new AI interaction
     * @param profileId ID of the AI profile
     * @return The ID of the created interaction
     */
    suspend fun createInteraction(profileId: String): Result<String>
    
    /**
     * Get an AI interaction
     * @param interactionId ID of the interaction to retrieve
     * @return The [AIInteraction] object
     */
    suspend fun getInteraction(interactionId: String): Result<AIInteraction>
    
    /**
     * Get all interactions for a user
     * @param limit Maximum number of interactions to fetch
     * @return List of [AIInteraction] objects
     */
    suspend fun getUserInteractions(limit: Int = 20): Result<List<AIInteraction>>
    
    /**
     * Get messages for an interaction
     * @param profileId ID of the AI profile
     * @param interactionId ID of the interaction
     * @param limit Maximum number of messages to fetch
     * @return List of [AIMessage] objects
     */
    suspend fun getMessages(
        profileId: String,
        interactionId: String,
        limit: Int = 50
    ): Result<List<AIMessage>>
    
    /**
     * Get messages as a flow for real-time updates
     * @param profileId ID of the AI profile
     * @param interactionId ID of the interaction
     * @param limit Maximum number of messages to fetch
     * @return Flow of [AIMessage] lists
     */
    fun getMessagesFlow(
        profileId: String,
        interactionId: String,
        limit: Int = 50
    ): Flow<Result<List<AIMessage>>>
    
    /**
     * Send a message to an AI
     * @param message The [AIMessage] to send
     * @return The sent [AIMessage]
     */
    suspend fun sendMessage(message: AIMessage): Result<AIMessage>
    
    /**
     * React to a message
     * @param messageId ID of the message to react to
     * @param reaction Type of reaction
     * @return Success result
     */
    suspend fun reactToMessage(messageId: String, reaction: ReactionType): Result<Unit>
    
    /**
     * Remove reaction from a message
     * @param messageId ID of the message
     * @return Success result
     */
    suspend fun removeReaction(messageId: String): Result<Unit>
    
    /**
     * Get popular AI profiles
     * @param limit Maximum number of profiles to fetch
     * @return List of [AIProfile] objects
     */
    suspend fun getPopularAIProfiles(limit: Int = 10): Result<List<AIProfile>>
    
    /**
     * Get recommended AI profiles for the current user
     * @param limit Maximum number of profiles to fetch
     * @return List of [AIProfile] objects
     */
    suspend fun getRecommendedAIProfiles(limit: Int = 10): Result<List<AIProfile>>
    
    /**
     * Create a custom AI profile
     * @param profile The [AIProfile] to create
     * @return The created [AIProfile]
     */
    suspend fun createAIProfile(profile: AIProfile): Result<AIProfile>
    
    /**
     * Update an AI profile
     * @param profile The [AIProfile] to update
     * @return The updated [AIProfile]
     */
    suspend fun updateAIProfile(profile: AIProfile): Result<AIProfile>
    
    /**
     * Delete an AI profile
     * @param profileId ID of the profile to delete
     * @return Success result
     */
    suspend fun deleteAIProfile(profileId: String): Result<Unit>
    
    /**
     * Get the limit of free interactions
     * @return The limit as an integer
     */
    suspend fun getFreeInteractionLimit(): Result<Int>
    
    /**
     * Get the number of interactions with an AI profile
     * @param profileId ID of the AI profile
     * @return The number of interactions
     */
    suspend fun getInteractionCount(profileId: String): Result<Int>
    
    /**
     * Search AI profiles
     * @param query Search query
     * @param limit Maximum number of profiles to fetch
     * @return List of [AIProfile] objects
     */
    suspend fun searchAIProfiles(query: String, limit: Int = 20): Result<List<AIProfile>>
}