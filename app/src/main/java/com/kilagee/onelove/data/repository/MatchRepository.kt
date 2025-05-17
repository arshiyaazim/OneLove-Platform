package com.kilagee.onelove.data.repository

import com.kilagee.onelove.data.model.Match
import com.kilagee.onelove.data.model.MatchStatus
import com.kilagee.onelove.data.model.Result
import com.kilagee.onelove.data.model.User
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for match operations
 */
interface MatchRepository {
    /**
     * Get match by ID
     */
    suspend fun getMatchById(matchId: String): Result<Match>
    
    /**
     * Get match by ID as Flow
     */
    fun getMatchByIdFlow(matchId: String): Flow<Match?>
    
    /**
     * Get matches for user
     */
    suspend fun getMatchesForUser(userId: String): Result<List<Match>>
    
    /**
     * Get matches for user as Flow
     */
    fun getMatchesForUserFlow(userId: String): Flow<List<Match>>
    
    /**
     * Get active matches for user
     */
    suspend fun getActiveMatchesForUser(userId: String): Result<List<Match>>
    
    /**
     * Get pending matches for user
     */
    suspend fun getPendingMatchesForUser(userId: String): Result<List<Match>>
    
    /**
     * Get match between users
     */
    suspend fun getMatchBetweenUsers(userId1: String, userId2: String): Result<Match?>
    
    /**
     * Create a new potential match
     */
    suspend fun createPotentialMatch(userId1: String, userId2: String, matchScore: Double = 0.0): Result<Match>
    
    /**
     * Like user
     */
    suspend fun likeUser(userId: String, likedUserId: String): Result<Match>
    
    /**
     * Reject user
     */
    suspend fun rejectUser(userId: String, rejectedUserId: String): Result<Match>
    
    /**
     * Unmatch user
     */
    suspend fun unmatchUser(userId: String, unmatchUserId: String): Result<Match>
    
    /**
     * Update match status
     */
    suspend fun updateMatchStatus(matchId: String, status: MatchStatus): Result<Match>
    
    /**
     * Create chat for match
     */
    suspend fun createChatForMatch(matchId: String): Result<String>
    
    /**
     * Get recommended matches (discovery)
     */
    suspend fun getRecommendedMatches(
        userId: String,
        limit: Int = 20,
        excludeIds: List<String> = emptyList()
    ): Result<List<User>>
    
    /**
     * Get users who liked current user
     */
    suspend fun getUsersWhoLikedMe(userId: String): Result<List<User>>
    
    /**
     * Get match statistics for user
     */
    suspend fun getMatchStatistics(userId: String): Result<Map<String, Int>>
    
    /**
     * Check if users are matched
     */
    suspend fun checkIfUsersAreMatched(userId1: String, userId2: String): Result<Boolean>
    
    /**
     * Search for potential matches with filters
     */
    suspend fun searchPotentialMatches(
        userId: String,
        minAge: Int? = null,
        maxAge: Int? = null,
        distance: Int? = null,
        interests: List<String>? = null,
        genders: List<String>? = null,
        limit: Int = 20
    ): Result<List<User>>
    
    /**
     * Get match count for user
     */
    suspend fun getMatchCount(userId: String): Result<Int>
    
    /**
     * Report a match issue
     */
    suspend fun reportMatchIssue(
        matchId: String,
        userId: String,
        reason: String,
        details: String? = null
    ): Result<Unit>
}