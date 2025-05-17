package com.kilagee.onelove.domain.repository

import com.kilagee.onelove.data.model.Reaction
import com.kilagee.onelove.data.model.ReactionSummary
import com.kilagee.onelove.data.model.ReactionTargetType
import com.kilagee.onelove.data.model.ReactionType
import com.kilagee.onelove.domain.util.Result
import kotlinx.coroutines.flow.Flow

/**
 * Interface for reaction-related operations
 */
interface ReactionRepository {
    
    /**
     * Add or update a reaction
     * @param targetId ID of the target (message, profile, etc.)
     * @param targetType Type of the target
     * @param reactionType Type of reaction
     * @param intensity Intensity level (1-5) for applicable reactions
     * @return The created/updated [Reaction]
     */
    suspend fun addReaction(
        targetId: String,
        targetType: ReactionTargetType,
        reactionType: ReactionType,
        intensity: Int = 1
    ): Result<Reaction>
    
    /**
     * Remove a reaction
     * @param targetId ID of the target
     * @param targetType Type of the target
     * @return Success status
     */
    suspend fun removeReaction(
        targetId: String,
        targetType: ReactionTargetType
    ): Result<Unit>
    
    /**
     * Get reactions for a target
     * @param targetId ID of the target
     * @param targetType Type of the target
     * @param limit Maximum number of reactions to fetch
     * @return List of [Reaction] objects
     */
    suspend fun getReactions(
        targetId: String,
        targetType: ReactionTargetType,
        limit: Int = 50
    ): Result<List<Reaction>>
    
    /**
     * Get a summary of reactions for a target
     * @param targetId ID of the target
     * @param targetType Type of the target
     * @return [ReactionSummary] for the target
     */
    suspend fun getReactionSummary(
        targetId: String,
        targetType: ReactionTargetType
    ): Result<ReactionSummary>
    
    /**
     * Get reaction summary as a flow for real-time updates
     * @param targetId ID of the target
     * @param targetType Type of the target
     * @return Flow of [ReactionSummary]
     */
    fun getReactionSummaryFlow(
        targetId: String,
        targetType: ReactionTargetType
    ): Flow<Result<ReactionSummary>>
    
    /**
     * Get user's current reaction to a target
     * @param targetId ID of the target
     * @param targetType Type of the target
     * @return The user's [Reaction] or null if none exists
     */
    suspend fun getUserReaction(
        targetId: String,
        targetType: ReactionTargetType
    ): Result<Reaction?>
    
    /**
     * Get user's reactions across multiple targets
     * @param targetIds List of target IDs
     * @param targetType Type of the targets
     * @return Map of target ID to [Reaction]
     */
    suspend fun getUserReactions(
        targetIds: List<String>,
        targetType: ReactionTargetType
    ): Result<Map<String, Reaction>>
    
    /**
     * Get popular emojis for the current user based on usage
     * @param limit Maximum number of emoji types to fetch
     * @return List of [ReactionType] in order of popularity
     */
    suspend fun getPopularEmojis(limit: Int = 8): Result<List<ReactionType>>
    
    /**
     * Get trending emojis across the platform
     * @param limit Maximum number of emoji types to fetch
     * @return List of [ReactionType] in order of popularity
     */
    suspend fun getTrendingEmojis(limit: Int = 8): Result<List<ReactionType>>
}