package com.kilagee.onelove.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.kilagee.onelove.data.model.Match
import com.kilagee.onelove.data.model.MatchRequest
import com.kilagee.onelove.data.model.MatchRequestStatus
import com.kilagee.onelove.data.model.MatchStatus
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Match-related operations
 */
@Dao
interface MatchDao {
    
    // Match queries
    @Query("SELECT * FROM matches WHERE id = :matchId")
    fun getMatchById(matchId: String): Flow<Match?>
    
    @Query("SELECT * FROM matches WHERE :userId IN (userIds) ORDER BY createdAt DESC")
    fun getMatchesForUser(userId: String): Flow<List<Match>>
    
    @Query("SELECT * FROM matches WHERE :userId IN (userIds) AND status = :status ORDER BY createdAt DESC")
    fun getMatchesForUserByStatus(userId: String, status: MatchStatus): Flow<List<Match>>
    
    @Query("""
        SELECT * FROM matches 
        WHERE :userId IN (userIds) 
        AND (SELECT COUNT(*) FROM messages WHERE chatId = matches.chatId) = 0
        AND status = 'ACTIVE'
        ORDER BY createdAt DESC
    """)
    fun getNewMatchesWithoutMessages(userId: String): Flow<List<Match>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMatch(match: Match)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMatches(matches: List<Match>)
    
    @Update
    suspend fun updateMatch(match: Match)
    
    @Query("UPDATE matches SET status = :status WHERE id = :matchId")
    suspend fun updateMatchStatus(matchId: String, status: MatchStatus)
    
    @Query("DELETE FROM matches WHERE id = :matchId")
    suspend fun deleteMatchById(matchId: String)
    
    // Match requests
    @Query("SELECT * FROM match_requests WHERE id = :requestId")
    fun getMatchRequestById(requestId: String): Flow<MatchRequest?>
    
    @Query("SELECT * FROM match_requests WHERE senderId = :userId OR recipientId = :userId ORDER BY createdAt DESC")
    fun getMatchRequestsForUser(userId: String): Flow<List<MatchRequest>>
    
    @Query("SELECT * FROM match_requests WHERE senderId = :userId ORDER BY createdAt DESC")
    fun getSentMatchRequests(userId: String): Flow<List<MatchRequest>>
    
    @Query("SELECT * FROM match_requests WHERE recipientId = :userId ORDER BY createdAt DESC")
    fun getReceivedMatchRequests(userId: String): Flow<List<MatchRequest>>
    
    @Query("SELECT * FROM match_requests WHERE recipientId = :userId AND status = :status ORDER BY createdAt DESC")
    fun getReceivedMatchRequestsByStatus(userId: String, status: MatchRequestStatus): Flow<List<MatchRequest>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMatchRequest(request: MatchRequest)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMatchRequests(requests: List<MatchRequest>)
    
    @Update
    suspend fun updateMatchRequest(request: MatchRequest)
    
    @Query("UPDATE match_requests SET status = :status WHERE id = :requestId")
    suspend fun updateMatchRequestStatus(requestId: String, status: MatchRequestStatus)
    
    @Query("UPDATE match_requests SET viewedAt = :timestamp WHERE id = :requestId")
    suspend fun markMatchRequestAsViewed(requestId: String, timestamp: Long)
    
    @Query("DELETE FROM match_requests WHERE id = :requestId")
    suspend fun deleteMatchRequestById(requestId: String)
    
    // Suggestions
    @Transaction
    @Query("""
        SELECT u.* FROM users u
        WHERE u.id NOT IN (
            SELECT CASE 
                WHEN userIds[0] = :userId THEN userIds[1]
                ELSE userIds[0]
            END
            FROM matches
            WHERE :userId IN (userIds)
        )
        AND u.id NOT IN (
            SELECT senderId FROM match_requests WHERE recipientId = :userId
        )
        AND u.id NOT IN (
            SELECT recipientId FROM match_requests WHERE senderId = :userId
        )
        AND u.id != :userId
        ORDER BY RANDOM()
        LIMIT :limit
    """)
    fun getMatchSuggestions(userId: String, limit: Int): Flow<List<Match>>
}