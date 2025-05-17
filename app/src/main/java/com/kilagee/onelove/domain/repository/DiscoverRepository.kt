package com.kilagee.onelove.domain.repository

import com.kilagee.onelove.data.model.Match
import com.kilagee.onelove.data.model.User
import com.kilagee.onelove.domain.util.Result
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for discover-related functions
 */
interface DiscoverRepository {

    /**
     * Get potential matches based on user preferences
     */
    fun getPotentialMatches(): Flow<Result<List<User>>>
    
    /**
     * Create a new match
     */
    suspend fun createMatch(match: Match): Result<Match>
    
    /**
     * Check if this is a mutual match
     */
    suspend fun checkForMutualMatch(userId: String, potentialMatchUserId: String): Result<Boolean>
    
    /**
     * Skip a user - don't show them again
     */
    suspend fun skipUser(userId: String, skippedUserId: String): Result<Unit>
    
    /**
     * Get matches for a user
     */
    fun getMatches(userId: String): Flow<Result<List<Match>>>
    
    /**
     * Get users who liked the current user
     */
    fun getLikes(userId: String): Flow<Result<List<User>>>
    
    /**
     * Get match details
     */
    fun getMatchById(matchId: String): Flow<Result<Match>>
    
    /**
     * Update match status
     */
    suspend fun updateMatchStatus(matchId: String, status: com.kilagee.onelove.data.model.MatchStatus): Result<Match>
}