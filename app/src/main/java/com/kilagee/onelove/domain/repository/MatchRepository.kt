package com.kilagee.onelove.domain.repository

import com.kilagee.onelove.data.model.Match
import com.kilagee.onelove.data.model.MatchRequest
import com.kilagee.onelove.data.model.User
import com.kilagee.onelove.domain.util.Result
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for match-related operations
 */
interface MatchRepository {
    
    /**
     * Get all matches for the current user
     * 
     * @return Flow of a list of matches
     */
    fun getMatches(): Flow<Result<List<Match>>>
    
    /**
     * Get a specific match by ID
     * 
     * @param matchId ID of the match
     * @return Flow of the match
     */
    fun getMatchById(matchId: String): Flow<Result<Match>>
    
    /**
     * Check if there is a match between two users
     * 
     * @param userId1 ID of the first user
     * @param userId2 ID of the second user
     * @return Flow of the match or null if no match exists
     */
    fun getMatchBetweenUsers(userId1: String, userId2: String): Flow<Result<Match?>>
    
    /**
     * Get match requests sent by the current user
     * 
     * @return Flow of a list of match requests
     */
    fun getSentMatchRequests(): Flow<Result<List<MatchRequest>>>
    
    /**
     * Get match requests received by the current user
     * 
     * @return Flow of a list of match requests
     */
    fun getReceivedMatchRequests(): Flow<Result<List<MatchRequest>>>
    
    /**
     * Get the count of unread received match requests
     * 
     * @return Flow of the count
     */
    fun getUnreadMatchRequestCount(): Flow<Int>
    
    /**
     * Send a match request to another user
     * 
     * @param recipientId ID of the recipient
     * @param message Optional message to include with the request
     * @return Result of the sent request
     */
    suspend fun sendMatchRequest(recipientId: String, message: String? = null): Result<MatchRequest>
    
    /**
     * Accept a match request
     * 
     * @param requestId ID of the request
     * @return Result of the created match
     */
    suspend fun acceptMatchRequest(requestId: String): Result<Match>
    
    /**
     * Decline a match request
     * 
     * @param requestId ID of the request
     * @return Result of the operation
     */
    suspend fun declineMatchRequest(requestId: String): Result<Unit>
    
    /**
     * Mark a match request as viewed
     * 
     * @param requestId ID of the request
     * @return Result of the operation
     */
    suspend fun markMatchRequestAsViewed(requestId: String): Result<Unit>
    
    /**
     * Cancel a match
     * 
     * @param matchId ID of the match
     * @return Result of the operation
     */
    suspend fun cancelMatch(matchId: String): Result<Unit>
    
    /**
     * Get match suggestions
     * 
     * @param count Number of suggestions to retrieve
     * @return Flow of a list of suggested users
     */
    fun getMatchSuggestions(count: Int = 10): Flow<Result<List<User>>>
    
    /**
     * Like a user
     * 
     * @param userId ID of the user to like
     * @return Result of the operation, with a Match if a mutual like occurred
     */
    suspend fun likeUser(userId: String): Result<Match?>
    
    /**
     * Unlike a user
     * 
     * @param userId ID of the user to unlike
     * @return Result of the operation
     */
    suspend fun unlikeUser(userId: String): Result<Unit>
    
    /**
     * Get users who have liked the current user
     * 
     * @param limit Maximum number of users to retrieve
     * @return Flow of a list of users
     */
    fun getUsersWhoLikedMe(limit: Int = 20): Flow<Result<List<User>>>
    
    /**
     * Get recent matches
     * 
     * @param limit Maximum number of matches to retrieve
     * @return Flow of a list of matches
     */
    fun getRecentMatches(limit: Int = 10): Flow<Result<List<Match>>>
    
    /**
     * Find users based on search criteria
     * 
     * @param minAge Minimum age
     * @param maxAge Maximum age
     * @param distance Maximum distance in kilometers
     * @param interests List of interests
     * @param limit Maximum number of users to retrieve
     * @return Flow of a list of users
     */
    fun findUsers(
        minAge: Int? = null,
        maxAge: Int? = null,
        distance: Int? = null,
        interests: List<String>? = null,
        limit: Int = 20
    ): Flow<Result<List<User>>>
}