package com.kilagee.onelove.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.kilagee.onelove.data.model.User
import com.kilagee.onelove.data.model.UserInteraction
import com.kilagee.onelove.data.model.VerificationRequest
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for User-related operations
 */
@Dao
interface UserDao {
    
    // User queries
    @Query("SELECT * FROM users WHERE id = :userId")
    fun getUserById(userId: String): Flow<User?>
    
    @Query("SELECT * FROM users WHERE id IN (:userIds)")
    fun getUsersByIds(userIds: List<String>): Flow<List<User>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<User>)
    
    @Update
    suspend fun updateUser(user: User)
    
    @Delete
    suspend fun deleteUser(user: User)
    
    @Query("DELETE FROM users")
    suspend fun deleteAllUsers()
    
    // Verification requests
    @Query("SELECT * FROM verification_requests WHERE userId = :userId ORDER BY createdAt DESC LIMIT 1")
    fun getLatestVerificationRequest(userId: String): Flow<VerificationRequest?>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVerificationRequest(request: VerificationRequest)
    
    @Update
    suspend fun updateVerificationRequest(request: VerificationRequest)
    
    // User interactions
    @Query("SELECT * FROM user_interactions WHERE userId = :userId ORDER BY timestamp DESC LIMIT :limit")
    fun getUserInteractions(userId: String, limit: Int): Flow<List<UserInteraction>>
    
    @Query("SELECT * FROM user_interactions ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentInteractions(limit: Int): Flow<List<UserInteraction>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserInteraction(interaction: UserInteraction)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserInteractions(interactions: List<UserInteraction>)
    
    @Query("DELETE FROM user_interactions WHERE timestamp < :timestamp")
    suspend fun deleteOldInteractions(timestamp: Long)
    
    // Search
    @Query("SELECT * FROM users WHERE name LIKE '%' || :query || '%' LIMIT :limit")
    fun searchUsersByName(query: String, limit: Int): Flow<List<User>>
    
    // Location-based queries
    @Query("""
        SELECT *, 
        ((latitude - :lat) * (latitude - :lat) + (longitude - :lon) * (longitude - :lon)) AS distance 
        FROM users 
        WHERE id NOT IN (:excludeIds) 
        AND (:minAge IS NULL OR age >= :minAge) 
        AND (:maxAge IS NULL OR age <= :maxAge)
        ORDER BY distance 
        LIMIT :limit
    """)
    fun getNearbyUsers(
        lat: Double, 
        lon: Double, 
        limit: Int, 
        excludeIds: List<String>,
        minAge: Int?,
        maxAge: Int?
    ): Flow<List<User>>
}