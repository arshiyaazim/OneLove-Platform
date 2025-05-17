package com.kilagee.onelove.domain.repository

import com.kilagee.onelove.data.model.User
import com.kilagee.onelove.domain.util.Result
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for user-related functions
 */
interface UserRepository {
    
    /**
     * Get a user by ID
     */
    fun getUserById(userId: String): Flow<Result<User>>
    
    /**
     * Get users by IDs
     */
    fun getUsersByIds(userIds: List<String>): Flow<Result<List<User>>>
    
    /**
     * Search users by name or other criteria
     */
    fun searchUsers(query: String): Flow<Result<List<User>>>
    
    /**
     * Get users near the specified location
     */
    fun getNearbyUsers(latitude: Double, longitude: Double, maxDistance: Int): Flow<Result<List<User>>>
    
    /**
     * Get users filtered by criteria
     */
    fun getFilteredUsers(
        minAge: Int? = null,
        maxAge: Int? = null,
        gender: String? = null,
        interests: List<String>? = null,
        location: String? = null,
        maxDistance: Int? = null
    ): Flow<Result<List<User>>>
    
    /**
     * Update user profile
     */
    suspend fun updateUserProfile(user: User): Result<User>
    
    /**
     * Update user location
     */
    suspend fun updateUserLocation(userId: String, latitude: Double, longitude: Double, locationName: String?): Result<User>
    
    /**
     * Update user online status
     */
    suspend fun updateUserOnlineStatus(userId: String, isOnline: Boolean): Result<Unit>
    
    /**
     * Update user preferences
     */
    suspend fun updateUserPreferences(
        userId: String,
        minAgePreference: Int? = null,
        maxAgePreference: Int? = null,
        genderPreference: List<String>? = null,
        maxDistance: Int? = null
    ): Result<User>
    
    /**
     * Block a user
     */
    suspend fun blockUser(userId: String, blockedUserId: String): Result<Unit>
    
    /**
     * Report a user
     */
    suspend fun reportUser(userId: String, reportedUserId: String, reason: String): Result<Unit>
}