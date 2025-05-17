package com.kilagee.onelove.domain.repository

import com.kilagee.onelove.data.model.MatchRecommendation
import com.kilagee.onelove.data.model.RecommendationReason
import com.kilagee.onelove.data.model.User
import com.kilagee.onelove.data.model.UserAction
import com.kilagee.onelove.domain.util.Result
import kotlinx.coroutines.flow.Flow

/**
 * Interface for user discovery operations
 */
interface DiscoveryRepository {
    
    /**
     * Get recommended users for discovery
     * @param limit Maximum number of recommendations to fetch
     * @return List of [MatchRecommendation] objects
     */
    suspend fun getRecommendations(limit: Int = 10): Result<List<MatchRecommendation>>
    
    /**
     * Get recommended users as a flow for real-time updates
     * @param limit Maximum number of recommendations to fetch
     * @return Flow of [MatchRecommendation] lists
     */
    fun getRecommendationsFlow(limit: Int = 10): Flow<Result<List<MatchRecommendation>>>
    
    /**
     * Get top picks for the day (premium feature)
     * @param limit Maximum number of top picks to fetch
     * @return List of [MatchRecommendation] objects
     */
    suspend fun getTopPicks(limit: Int = 10): Result<List<MatchRecommendation>>
    
    /**
     * Get users who liked the current user (premium feature)
     * @param limit Maximum number of users to fetch
     * @return List of [User] objects
     */
    suspend fun getLikesYou(limit: Int = 10): Result<List<User>>
    
    /**
     * Get recently active users
     * @param limit Maximum number of users to fetch
     * @return List of [User] objects
     */
    suspend fun getRecentlyActiveUsers(limit: Int = 10): Result<List<User>>
    
    /**
     * Get users with similar interests
     * @param limit Maximum number of users to fetch
     * @return List of [User] objects with common interests
     */
    suspend fun getUsersWithSimilarInterests(limit: Int = 10): Result<List<User>>
    
    /**
     * Get users nearby
     * @param radiusKm Radius in kilometers for the search
     * @param limit Maximum number of users to fetch
     * @return List of [User] objects with location
     */
    suspend fun getNearbyUsers(radiusKm: Int = 10, limit: Int = 10): Result<List<User>>
    
    /**
     * Like a user
     * @param userId ID of the user being liked
     * @return Match ID if there's a match, null otherwise
     */
    suspend fun likeUser(userId: String): Result<String?>
    
    /**
     * Super like a user (premium feature)
     * @param userId ID of the user being super liked
     * @return Match ID if there's a match, null otherwise
     */
    suspend fun superLikeUser(userId: String): Result<String?>
    
    /**
     * Pass on a user (skip)
     * @param userId ID of the user being passed
     */
    suspend fun passUser(userId: String): Result<Unit>
    
    /**
     * Block a user
     * @param userId ID of the user being blocked
     * @param reason Optional reason for blocking
     */
    suspend fun blockUser(userId: String, reason: String? = null): Result<Unit>
    
    /**
     * Report a user
     * @param userId ID of the user being reported
     * @param reason Reason for reporting
     * @param details Additional details about the report
     */
    suspend fun reportUser(userId: String, reason: String, details: String? = null): Result<Unit>
    
    /**
     * Undo the last action (premium feature)
     * @return The user ID for which the action was undone
     */
    suspend fun undoLastAction(): Result<String>
    
    /**
     * Get recommended reasons for two users
     * @param userId1 First user ID
     * @param userId2 Second user ID
     * @return List of [RecommendationReason] objects
     */
    suspend fun getRecommendationReasons(userId1: String, userId2: String): Result<List<RecommendationReason>>
    
    /**
     * Get user actions history
     * @param limit Maximum number of actions to fetch
     * @return Map of user IDs to [UserAction]
     */
    suspend fun getUserActionsHistory(limit: Int = 50): Result<Map<String, UserAction>>
    
    /**
     * Boost user visibility (premium feature)
     * @param durationMinutes Duration of the boost in minutes
     * @return Expiry time of the boost
     */
    suspend fun boostVisibility(durationMinutes: Int = 30): Result<Long>
    
    /**
     * Get remaining likes count for free users
     * @return Number of remaining likes
     */
    suspend fun getRemainingLikesCount(): Result<Int>
    
    /**
     * Get remaining super likes count
     * @return Number of remaining super likes
     */
    suspend fun getRemainingSuperLikesCount(): Result<Int>
    
    /**
     * Get remaining boosts count
     * @return Number of remaining boosts
     */
    suspend fun getRemainingBoostsCount(): Result<Int>
    
    /**
     * Update discovery preferences
     * @param minAge Minimum age preference
     * @param maxAge Maximum age preference
     * @param distance Maximum distance preference (in kilometers or miles)
     * @param genderPreferences List of gender preferences
     */
    suspend fun updateDiscoveryPreferences(
        minAge: Int? = null,
        maxAge: Int? = null,
        distance: Int? = null,
        genderPreferences: List<String>? = null
    ): Result<Unit>
}