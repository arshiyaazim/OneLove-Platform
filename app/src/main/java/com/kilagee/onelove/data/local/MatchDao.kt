package com.kilagee.onelove.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.kilagee.onelove.data.model.Match
import com.kilagee.onelove.data.model.MatchStatus
import kotlinx.coroutines.flow.Flow

/**
 * Data access object for Match
 */
@Dao
interface MatchDao {
    
    /**
     * Insert a match
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMatch(match: Match)
    
    /**
     * Insert multiple matches
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMatches(matches: List<Match>)
    
    /**
     * Update a match
     */
    @Update
    suspend fun updateMatch(match: Match)
    
    /**
     * Get a match by ID
     */
    @Query("SELECT * FROM matches WHERE id = :matchId")
    suspend fun getMatchById(matchId: String): Match?
    
    /**
     * Get a match by ID as Flow
     */
    @Query("SELECT * FROM matches WHERE id = :matchId")
    fun getMatchByIdFlow(matchId: String): Flow<Match?>
    
    /**
     * Get matches for a user
     */
    @Query("SELECT * FROM matches WHERE (userId = :userId OR matchedUserId = :userId) AND status = :status ORDER BY updatedAt DESC")
    suspend fun getMatchesForUser(userId: String, status: MatchStatus): List<Match>
    
    /**
     * Get matches for a user as Flow
     */
    @Query("SELECT * FROM matches WHERE (userId = :userId OR matchedUserId = :userId) AND status = :status ORDER BY updatedAt DESC")
    fun getMatchesForUserFlow(userId: String, status: MatchStatus): Flow<List<Match>>
    
    /**
     * Get all active matches for a user
     */
    @Query("SELECT * FROM matches WHERE (userId = :userId OR matchedUserId = :userId) AND status = 'ACTIVE' ORDER BY lastMessageTimestamp DESC")
    suspend fun getActiveMatchesForUser(userId: String): List<Match>
    
    /**
     * Get pending matches for a user (where user is the initiator)
     */
    @Query("SELECT * FROM matches WHERE userId = :userId AND status = 'PENDING' ORDER BY createdAt DESC")
    suspend fun getPendingMatchesAsInitiator(userId: String): List<Match>
    
    /**
     * Get pending matches for a user (where user is the matched user)
     */
    @Query("SELECT * FROM matches WHERE matchedUserId = :userId AND status = 'PENDING' ORDER BY createdAt DESC")
    suspend fun getPendingMatchesAsMatched(userId: String): List<Match>
    
    /**
     * Get favorite matches for a user
     */
    @Query("SELECT * FROM matches WHERE (userId = :userId OR matchedUserId = :userId) AND isFavorite = 1 AND status = 'ACTIVE' ORDER BY lastMessageTimestamp DESC")
    suspend fun getFavoriteMatchesForUser(userId: String): List<Match>
    
    /**
     * Get unread match count for a user
     */
    @Query("SELECT COUNT(*) FROM matches WHERE (userId = :userId OR matchedUserId = :userId) AND status = 'ACTIVE' AND unreadCount > 0")
    suspend fun getUnreadMatchCountForUser(userId: String): Int
    
    /**
     * Get unread match count for a user as Flow
     */
    @Query("SELECT COUNT(*) FROM matches WHERE (userId = :userId OR matchedUserId = :userId) AND status = 'ACTIVE' AND unreadCount > 0")
    fun getUnreadMatchCountForUserFlow(userId: String): Flow<Int>
    
    /**
     * Update last message for a match
     */
    @Query("UPDATE matches SET lastMessageText = :lastMessageText, lastMessageTimestamp = :lastMessageTimestamp, updatedAt = :updatedAt WHERE id = :matchId")
    suspend fun updateLastMessage(matchId: String, lastMessageText: String, lastMessageTimestamp: Long, updatedAt: Long)
    
    /**
     * Increment unread count for a match
     */
    @Query("UPDATE matches SET unreadCount = unreadCount + 1 WHERE id = :matchId")
    suspend fun incrementUnreadCount(matchId: String)
    
    /**
     * Reset unread count for a match
     */
    @Query("UPDATE matches SET unreadCount = 0 WHERE id = :matchId")
    suspend fun resetUnreadCount(matchId: String)
    
    /**
     * Mark a match as read (not new)
     */
    @Query("UPDATE matches SET isNew = 0 WHERE id = :matchId")
    suspend fun markMatchAsRead(matchId: String)
    
    /**
     * Toggle favorite status for a match
     */
    @Query("UPDATE matches SET isFavorite = NOT isFavorite WHERE id = :matchId")
    suspend fun toggleFavorite(matchId: String)
    
    /**
     * Delete a match
     */
    @Query("DELETE FROM matches WHERE id = :matchId")
    suspend fun deleteMatch(matchId: String)
    
    /**
     * Delete all matches
     */
    @Query("DELETE FROM matches")
    suspend fun deleteAllMatches()
}