package com.kilagee.onelove.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.kilagee.onelove.data.local.UserDao
import com.kilagee.onelove.data.model.User
import com.kilagee.onelove.domain.repository.UserRepository
import com.kilagee.onelove.domain.util.Result
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.util.Date
import javax.inject.Inject

/**
 * Repository implementation for user-related functions
 */
class UserRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val userDao: UserDao
) : UserRepository {

    private val usersCollection = firestore.collection("users")
    private val blockedUsersCollection = firestore.collection("blockedUsers")
    private val reportedUsersCollection = firestore.collection("reportedUsers")
    
    /**
     * Get a user by ID
     */
    override fun getUserById(userId: String): Flow<Result<User>> = callbackFlow {
        trySend(Result.Loading)
        
        try {
            // Try to get from local DB first
            val cachedUser = userDao.getUserById(userId)
            if (cachedUser != null) {
                trySend(Result.Success(cachedUser))
            }
            
            // Set up listener for real-time updates
            val listener = usersCollection.document(userId)
                .addSnapshotListener { snapshot, exception ->
                    if (exception != null) {
                        trySend(Result.Error("Failed to listen for user: ${exception.message}"))
                        return@addSnapshotListener
                    }
                    
                    if (snapshot != null && snapshot.exists()) {
                        val user = snapshot.toObject(User::class.java)
                        if (user != null) {
                            // Cache in local DB
                            userDao.insertUser(user)
                            trySend(Result.Success(user))
                        } else {
                            trySend(Result.Error("Failed to parse user data"))
                        }
                    } else {
                        trySend(Result.Error("User not found"))
                    }
                }
            
            awaitClose { listener.remove() }
        } catch (e: Exception) {
            Timber.e(e, "Error getting user")
            trySend(Result.Error("Failed to get user: ${e.message}"))
            close()
        }
    }.catch { e ->
        Timber.e(e, "Exception in getUserById flow")
        emit(Result.Error("Exception: ${e.message}"))
    }
    
    /**
     * Get users by IDs
     */
    override fun getUsersByIds(userIds: List<String>): Flow<Result<List<User>>> = flow {
        emit(Result.Loading)
        
        try {
            if (userIds.isEmpty()) {
                emit(Result.Success(emptyList()))
                return@flow
            }
            
            // Try to get from local DB first
            val cachedUsers = userDao.getUsersByIds(userIds)
            if (cachedUsers.isNotEmpty()) {
                emit(Result.Success(cachedUsers))
            }
            
            // Get from Firestore
            val users = mutableListOf<User>()
            
            // Process in batches of 10 to avoid Firestore limitations
            val batches = userIds.chunked(10)
            
            for (batch in batches) {
                val query = usersCollection.whereIn("id", batch).get().await()
                
                val batchUsers = query.documents.mapNotNull { doc ->
                    doc.toObject(User::class.java)
                }
                
                // Cache in local DB
                userDao.insertUsers(batchUsers)
                
                users.addAll(batchUsers)
            }
            
            emit(Result.Success(users))
        } catch (e: Exception) {
            Timber.e(e, "Error getting users by IDs")
            emit(Result.Error("Failed to get users: ${e.message}"))
        }
    }.catch { e ->
        Timber.e(e, "Exception in getUsersByIds flow")
        emit(Result.Error("Exception: ${e.message}"))
    }
    
    /**
     * Search users by name or other criteria
     */
    override fun searchUsers(query: String): Flow<Result<List<User>>> = flow {
        emit(Result.Loading)
        
        try {
            if (query.isBlank()) {
                emit(Result.Success(emptyList()))
                return@flow
            }
            
            // Make query lowercase for case-insensitive search
            val lowerQuery = query.lowercase()
            
            // Firestore doesn't support full text search, so we'll use prefix query
            val nameQuery = usersCollection
                .orderBy("name")
                .startAt(lowerQuery)
                .endAt(lowerQuery + "\uf8ff")
                .limit(20)
                .get()
                .await()
            
            val users = nameQuery.documents.mapNotNull { doc ->
                doc.toObject(User::class.java)
            }
            
            // Cache in local DB
            userDao.insertUsers(users)
            
            emit(Result.Success(users))
        } catch (e: Exception) {
            Timber.e(e, "Error searching users")
            emit(Result.Error("Failed to search users: ${e.message}"))
        }
    }.catch { e ->
        Timber.e(e, "Exception in searchUsers flow")
        emit(Result.Error("Exception: ${e.message}"))
    }
    
    /**
     * Get users near the specified location
     */
    override fun getNearbyUsers(latitude: Double, longitude: Double, maxDistance: Int): Flow<Result<List<User>>> = flow {
        emit(Result.Loading)
        
        try {
            // Firestore doesn't support geospatial queries directly
            // So we'll get all users and filter by distance in the app
            val allUsersQuery = usersCollection
                .whereNotEqualTo("latitude", null)
                .whereNotEqualTo("longitude", null)
                .limit(100) // Limit to avoid getting too many users
                .get()
                .await()
            
            val allUsers = allUsersQuery.documents.mapNotNull { doc ->
                doc.toObject(User::class.java)
            }
            
            // Filter by distance
            val nearbyUsers = allUsers.filter { user ->
                if (user.latitude != null && user.longitude != null) {
                    calculateDistance(
                        latitude,
                        longitude,
                        user.latitude,
                        user.longitude
                    ) <= maxDistance
                } else {
                    false
                }
            }
            
            // Sort by distance
            val sortedUsers = nearbyUsers.sortedBy { user ->
                calculateDistance(
                    latitude,
                    longitude,
                    user.latitude ?: 0.0,
                    user.longitude ?: 0.0
                )
            }
            
            // Cache in local DB
            userDao.insertUsers(sortedUsers)
            
            emit(Result.Success(sortedUsers))
        } catch (e: Exception) {
            Timber.e(e, "Error getting nearby users")
            emit(Result.Error("Failed to get nearby users: ${e.message}"))
        }
    }.catch { e ->
        Timber.e(e, "Exception in getNearbyUsers flow")
        emit(Result.Error("Exception: ${e.message}"))
    }
    
    /**
     * Get users filtered by criteria
     */
    override fun getFilteredUsers(
        minAge: Int?,
        maxAge: Int?,
        gender: String?,
        interests: List<String>?,
        location: String?,
        maxDistance: Int?
    ): Flow<Result<List<User>>> = flow {
        emit(Result.Loading)
        
        try {
            // Start with a base query
            var query = usersCollection.limit(50)
            
            // Apply filters for age
            if (minAge != null) {
                query = query.whereGreaterThanOrEqualTo("age", minAge)
            }
            if (maxAge != null) {
                query = query.whereLessThanOrEqualTo("age", maxAge)
            }
            
            // Apply filter for gender
            if (gender != null) {
                query = query.whereEqualTo("gender", gender)
            }
            
            // Get filtered users
            val filteredUsersQuery = query.get().await()
            
            var filteredUsers = filteredUsersQuery.documents.mapNotNull { doc ->
                doc.toObject(User::class.java)
            }
            
            // Filter by interests (need to do in app because Firestore doesn't support array contains any easily)
            if (interests != null && interests.isNotEmpty()) {
                filteredUsers = filteredUsers.filter { user ->
                    user.interests?.any { interest ->
                        interests.contains(interest)
                    } ?: false
                }
            }
            
            // Filter by location
            if (location != null) {
                filteredUsers = filteredUsers.filter { user ->
                    user.location?.contains(location, ignoreCase = true) ?: false
                }
            }
            
            // Cache in local DB
            userDao.insertUsers(filteredUsers)
            
            emit(Result.Success(filteredUsers))
        } catch (e: Exception) {
            Timber.e(e, "Error getting filtered users")
            emit(Result.Error("Failed to get filtered users: ${e.message}"))
        }
    }.catch { e ->
        Timber.e(e, "Exception in getFilteredUsers flow")
        emit(Result.Error("Exception: ${e.message}"))
    }
    
    /**
     * Update user profile
     */
    override suspend fun updateUserProfile(user: User): Result<User> {
        return try {
            val updatedUser = user.copy(updatedAt = Date())
            
            usersCollection.document(user.id).set(updatedUser).await()
            
            // Update local cache
            userDao.updateUser(updatedUser)
            
            Result.Success(updatedUser)
        } catch (e: Exception) {
            Timber.e(e, "Error updating user profile")
            Result.Error("Failed to update user profile: ${e.message}")
        }
    }
    
    /**
     * Update user location
     */
    override suspend fun updateUserLocation(
        userId: String,
        latitude: Double,
        longitude: Double,
        locationName: String?
    ): Result<User> {
        return try {
            val updates = hashMapOf<String, Any>(
                "latitude" to latitude,
                "longitude" to longitude,
                "updatedAt" to Date()
            )
            
            if (locationName != null) {
                updates["location"] = locationName
            }
            
            usersCollection.document(userId).update(updates).await()
            
            // Get updated user
            val userDoc = usersCollection.document(userId).get().await()
            val updatedUser = userDoc.toObject(User::class.java)
            
            if (updatedUser != null) {
                // Update local cache
                userDao.updateUser(updatedUser)
                Result.Success(updatedUser)
            } else {
                Result.Error("Failed to get updated user")
            }
        } catch (e: Exception) {
            Timber.e(e, "Error updating user location")
            Result.Error("Failed to update user location: ${e.message}")
        }
    }
    
    /**
     * Update user online status
     */
    override suspend fun updateUserOnlineStatus(userId: String, isOnline: Boolean): Result<Unit> {
        return try {
            val updates = hashMapOf<String, Any>(
                "isOnline" to isOnline,
                "lastActive" to Date()
            )
            
            usersCollection.document(userId).update(updates).await()
            
            // Update local cache if available
            val cachedUser = userDao.getUserById(userId)
            if (cachedUser != null) {
                val updatedUser = cachedUser.copy(
                    isOnline = isOnline,
                    lastActive = Date()
                )
                userDao.updateUser(updatedUser)
            }
            
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error updating user online status")
            Result.Error("Failed to update user online status: ${e.message}")
        }
    }
    
    /**
     * Update user preferences
     */
    override suspend fun updateUserPreferences(
        userId: String,
        minAgePreference: Int?,
        maxAgePreference: Int?,
        genderPreference: List<String>?,
        maxDistance: Int?
    ): Result<User> {
        return try {
            val updates = hashMapOf<String, Any?>("updatedAt" to Date())
            
            if (minAgePreference != null) {
                updates["minAgePreference"] = minAgePreference
            }
            
            if (maxAgePreference != null) {
                updates["maxAgePreference"] = maxAgePreference
            }
            
            if (genderPreference != null) {
                updates["lookingFor"] = genderPreference
            }
            
            if (maxDistance != null) {
                updates["maxDistance"] = maxDistance
            }
            
            usersCollection.document(userId).update(updates).await()
            
            // Get updated user
            val userDoc = usersCollection.document(userId).get().await()
            val updatedUser = userDoc.toObject(User::class.java)
            
            if (updatedUser != null) {
                // Update local cache
                userDao.updateUser(updatedUser)
                Result.Success(updatedUser)
            } else {
                Result.Error("Failed to get updated user")
            }
        } catch (e: Exception) {
            Timber.e(e, "Error updating user preferences")
            Result.Error("Failed to update user preferences: ${e.message}")
        }
    }
    
    /**
     * Block a user
     */
    override suspend fun blockUser(userId: String, blockedUserId: String): Result<Unit> {
        return try {
            val blockData = hashMapOf(
                "userId" to userId,
                "blockedUserId" to blockedUserId,
                "timestamp" to Date()
            )
            
            val blockId = "$userId-$blockedUserId"
            blockedUsersCollection.document(blockId).set(blockData).await()
            
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error blocking user")
            Result.Error("Failed to block user: ${e.message}")
        }
    }
    
    /**
     * Report a user
     */
    override suspend fun reportUser(
        userId: String,
        reportedUserId: String,
        reason: String
    ): Result<Unit> {
        return try {
            val reportData = hashMapOf(
                "userId" to userId,
                "reportedUserId" to reportedUserId,
                "reason" to reason,
                "timestamp" to Date(),
                "status" to "pending"
            )
            
            val reportId = "$userId-$reportedUserId-${Date().time}"
            reportedUsersCollection.document(reportId).set(reportData).await()
            
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error reporting user")
            Result.Error("Failed to report user: ${e.message}")
        }
    }
    
    /**
     * Calculate distance between two coordinates in kilometers using Haversine formula
     */
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Int {
        val earthRadius = 6371 // Earth's radius in kilometers
        
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        
        return (earthRadius * c).toInt()
    }
}