package com.kilagee.onelove.data.repository.impl

import android.net.Uri
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.kilagee.onelove.data.local.dao.UserDao
import com.kilagee.onelove.data.local.entity.UserEntity
import com.kilagee.onelove.data.model.Result
import com.kilagee.onelove.data.model.User
import com.kilagee.onelove.data.model.UserGender
import com.kilagee.onelove.data.model.VerificationStatus
import com.kilagee.onelove.data.repository.StorageRepository
import com.kilagee.onelove.data.repository.UserRepository
import com.kilagee.onelove.util.AppError
import com.kilagee.onelove.util.NetworkMonitor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of UserRepository using Firebase Firestore and Room
 */
@Singleton
class UserRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val storageRepository: StorageRepository,
    private val userDao: UserDao,
    private val networkMonitor: NetworkMonitor
) : UserRepository {

    companion object {
        private const val TAG = "UserRepositoryImpl"
        private const val USERS_COLLECTION = "users"
    }

    override suspend fun getUserById(userId: String): Result<User> {
        try {
            // First try to get from local cache
            val cachedUser = userDao.getUserById(userId)
            if (cachedUser != null) {
                return Result.success(cachedUser.toUser())
            }
            
            // If not in cache, try to get from Firestore
            if (!networkMonitor.isOnline.value) {
                return Result.error(AppError.NetworkError.NoConnection("No internet connection"))
            }
            
            val documentSnapshot = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .get()
                .await()
            
            if (documentSnapshot.exists()) {
                val user = documentSnapshot.toObject(User::class.java)
                    ?: return Result.error(AppError.DataError.InvalidData("Failed to parse user data"))
                
                // Cache user locally
                userDao.insertUser(UserEntity.fromUser(user))
                
                return Result.success(user)
            } else {
                return Result.error(AppError.DataError.NotFound("User not found"))
            }
        } catch (e: Exception) {
            Timber.e(TAG, "getUserById error", e)
            return Result.error(AppError.DataError.Other("Failed to get user: ${e.message}", e))
        }
    }

    override fun getUserByIdFlow(userId: String): Flow<Result<User>> = flow {
        emit(Result.loading())
        
        // First emit from cache if available
        val cachedUser = userDao.getUserById(userId)
        if (cachedUser != null) {
            emit(Result.success(cachedUser.toUser()))
        }
        
        // Then try to get fresh data from Firestore
        if (!networkMonitor.isOnline.value) {
            if (cachedUser == null) {
                emit(Result.error(AppError.NetworkError.NoConnection("No internet connection")))
            }
            return@flow
        }
        
        val documentSnapshot = firestore.collection(USERS_COLLECTION)
            .document(userId)
            .get()
            .await()
        
        if (documentSnapshot.exists()) {
            val user = documentSnapshot.toObject(User::class.java)
                ?: throw Exception("Failed to parse user data")
            
            // Cache user locally
            userDao.insertUser(UserEntity.fromUser(user))
            
            emit(Result.success(user))
        } else {
            throw Exception("User not found")
        }
    }.catch { e ->
        Timber.e(TAG, "getUserByIdFlow error", e)
        
        val error = when (e.message) {
            "User not found" -> AppError.DataError.NotFound("User not found")
            "Failed to parse user data" -> AppError.DataError.InvalidData("Failed to parse user data")
            else -> AppError.DataError.Other("Failed to get user: ${e.message}", e)
        }
        emit(Result.error(error))
    }

    override suspend fun createUser(user: User): Result<String> {
        try {
            val userWithTimestamp = user.copy(
                createdAt = Timestamp.now(),
                lastActive = Timestamp.now()
            )
            
            // Create user document
            val userRef = if (user.id.isNotEmpty()) {
                firestore.collection(USERS_COLLECTION).document(user.id)
            } else {
                firestore.collection(USERS_COLLECTION).document()
            }
            
            val userId = userRef.id
            val userWithId = userWithTimestamp.copy(id = userId)
            
            userRef.set(userWithId).await()
            
            // Cache user locally
            userDao.insertUser(UserEntity.fromUser(userWithId))
            
            return Result.success(userId)
        } catch (e: Exception) {
            Timber.e(TAG, "createUser error", e)
            return Result.error(AppError.DataError.Other("Failed to create user: ${e.message}", e))
        }
    }

    override suspend fun updateUser(user: User): Result<User> {
        try {
            val userWithTimestamp = user.copy(
                lastActive = Timestamp.now()
            )
            
            firestore.collection(USERS_COLLECTION)
                .document(user.id)
                .set(userWithTimestamp)
                .await()
            
            // Update local cache
            userDao.insertUser(UserEntity.fromUser(userWithTimestamp))
            
            return Result.success(userWithTimestamp)
        } catch (e: Exception) {
            Timber.e(TAG, "updateUser error", e)
            return Result.error(AppError.DataError.Other("Failed to update user: ${e.message}", e))
        }
    }

    override suspend fun updateProfilePicture(userId: String, imageUri: Uri): Result<String> {
        try {
            // Upload image to storage
            val uploadResult = storageRepository.uploadUserProfileImage(userId, imageUri)
            
            if (uploadResult is Result.Error) {
                return uploadResult
            }
            
            val imageUrl = (uploadResult as Result.Success).data
            
            // Update user document with new profile picture URL
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .update("profilePhotoUrl", imageUrl)
                .await()
            
            // Update local cache
            val cachedUser = userDao.getUserById(userId)
            if (cachedUser != null) {
                val updatedUser = cachedUser.copy(profilePhotoUrl = imageUrl)
                userDao.insertUser(updatedUser)
            }
            
            return Result.success(imageUrl)
        } catch (e: Exception) {
            Timber.e(TAG, "updateProfilePicture error", e)
            return Result.error(AppError.DataError.Other("Failed to update profile picture: ${e.message}", e))
        }
    }

    override suspend fun updateCoverPhoto(userId: String, imageUri: Uri): Result<String> {
        try {
            // Upload image to storage
            val uploadResult = storageRepository.uploadUserCoverImage(userId, imageUri)
            
            if (uploadResult is Result.Error) {
                return uploadResult
            }
            
            val imageUrl = (uploadResult as Result.Success).data
            
            // Update user document with new cover photo URL
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .update("coverPhotoUrl", imageUrl)
                .await()
            
            // Update local cache
            val cachedUser = userDao.getUserById(userId)
            if (cachedUser != null) {
                val updatedUser = cachedUser.copy(coverPhotoUrl = imageUrl)
                userDao.insertUser(updatedUser)
            }
            
            return Result.success(imageUrl)
        } catch (e: Exception) {
            Timber.e(TAG, "updateCoverPhoto error", e)
            return Result.error(AppError.DataError.Other("Failed to update cover photo: ${e.message}", e))
        }
    }

    override suspend fun uploadUserPhoto(userId: String, imageUri: Uri): Result<String> {
        try {
            // Upload image to storage
            val uploadResult = storageRepository.uploadUserGalleryImage(userId, imageUri)
            
            if (uploadResult is Result.Error) {
                return uploadResult
            }
            
            val imageUrl = (uploadResult as Result.Success).data
            
            // Add photo URL to user's photo list
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .update("photos", FieldValue.arrayUnion(imageUrl))
                .await()
            
            // Update local cache
            val cachedUser = userDao.getUserById(userId)
            if (cachedUser != null) {
                val updatedPhotos = cachedUser.photos.toMutableList().apply { add(imageUrl) }
                val updatedUser = cachedUser.copy(photos = updatedPhotos)
                userDao.insertUser(updatedUser)
            }
            
            return Result.success(imageUrl)
        } catch (e: Exception) {
            Timber.e(TAG, "uploadUserPhoto error", e)
            return Result.error(AppError.DataError.Other("Failed to upload photo: ${e.message}", e))
        }
    }

    override suspend fun deleteUserPhoto(userId: String, photoUrl: String): Result<Unit> {
        try {
            // Delete photo from storage
            storageRepository.deleteFile(photoUrl)
            
            // Remove photo URL from user's photo list
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .update("photos", FieldValue.arrayRemove(photoUrl))
                .await()
            
            // Update local cache
            val cachedUser = userDao.getUserById(userId)
            if (cachedUser != null) {
                val updatedPhotos = cachedUser.photos.toMutableList().apply { remove(photoUrl) }
                val updatedUser = cachedUser.copy(photos = updatedPhotos)
                userDao.insertUser(updatedUser)
            }
            
            return Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(TAG, "deleteUserPhoto error", e)
            return Result.error(AppError.DataError.Other("Failed to delete photo: ${e.message}", e))
        }
    }

    override suspend fun uploadVerificationDocuments(userId: String, documentUris: List<Uri>): Result<List<String>> {
        try {
            val documentUrls = mutableListOf<String>()
            
            for ((index, uri) in documentUris.withIndex()) {
                val docType = "verification_doc_$index"
                val uploadResult = storageRepository.uploadVerificationDocument(userId, uri, docType)
                
                if (uploadResult is Result.Error) {
                    return uploadResult.map { emptyList<String>() }
                }
                
                val documentUrl = (uploadResult as Result.Success).data
                documentUrls.add(documentUrl)
            }
            
            // Update user document with verification documents
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .update(
                    mapOf(
                        "verificationDocuments" to documentUrls,
                        "verificationStatus" to VerificationStatus.PENDING.name
                    )
                )
                .await()
            
            // Update local cache
            val cachedUser = userDao.getUserById(userId)
            if (cachedUser != null) {
                val updatedUser = cachedUser.copy(
                    verificationDocuments = documentUrls,
                    verificationStatus = VerificationStatus.PENDING
                )
                userDao.insertUser(updatedUser)
            }
            
            return Result.success(documentUrls)
        } catch (e: Exception) {
            Timber.e(TAG, "uploadVerificationDocuments error", e)
            return Result.error(AppError.DataError.Other("Failed to upload verification documents: ${e.message}", e))
        }
    }

    override suspend fun updateVerificationStatus(userId: String, status: VerificationStatus): Result<Unit> {
        try {
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .update("verificationStatus", status.name)
                .await()
            
            // Update local cache
            val cachedUser = userDao.getUserById(userId)
            if (cachedUser != null) {
                val updatedUser = cachedUser.copy(verificationStatus = status)
                userDao.insertUser(updatedUser)
            }
            
            return Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(TAG, "updateVerificationStatus error", e)
            return Result.error(AppError.DataError.Other("Failed to update verification status: ${e.message}", e))
        }
    }

    override suspend fun updateOnlineStatus(userId: String, isOnline: Boolean): Result<Unit> {
        try {
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .update(
                    mapOf(
                        "isOnline" to isOnline,
                        "lastActive" to Timestamp.now()
                    )
                )
                .await()
            
            // Update local cache
            userDao.updateUserOnlineStatus(userId, isOnline)
            userDao.updateUserLastActive(userId, System.currentTimeMillis())
            
            return Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(TAG, "updateOnlineStatus error", e)
            return Result.error(AppError.DataError.Other("Failed to update online status: ${e.message}", e))
        }
    }

    override suspend fun updateLastActive(userId: String, lastActive: Date): Result<Unit> {
        try {
            val timestamp = Timestamp(lastActive)
            
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .update("lastActive", timestamp)
                .await()
            
            // Update local cache
            userDao.updateUserLastActive(userId, lastActive.time)
            
            return Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(TAG, "updateLastActive error", e)
            return Result.error(AppError.DataError.Other("Failed to update last active: ${e.message}", e))
        }
    }

    override suspend fun updateFcmToken(userId: String, token: String): Result<Unit> {
        try {
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .update("fcmTokens", FieldValue.arrayUnion(token))
                .await()
            
            // Update local cache
            val cachedUser = userDao.getUserById(userId)
            if (cachedUser != null && !cachedUser.fcmTokens.contains(token)) {
                val updatedTokens = cachedUser.fcmTokens.toMutableList().apply { add(token) }
                val updatedUser = cachedUser.copy(fcmTokens = updatedTokens)
                userDao.insertUser(updatedUser)
            }
            
            return Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(TAG, "updateFcmToken error", e)
            return Result.error(AppError.DataError.Other("Failed to update FCM token: ${e.message}", e))
        }
    }

    override suspend fun blockUser(userId: String, blockedUserId: String): Result<Unit> {
        try {
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .update("blockedUsers", FieldValue.arrayUnion(blockedUserId))
                .await()
            
            // Update local cache
            val cachedUser = userDao.getUserById(userId)
            if (cachedUser != null && !cachedUser.blockedUsers.contains(blockedUserId)) {
                val updatedBlockedUsers = cachedUser.blockedUsers.toMutableList().apply { add(blockedUserId) }
                val updatedUser = cachedUser.copy(blockedUsers = updatedBlockedUsers)
                userDao.insertUser(updatedUser)
            }
            
            return Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(TAG, "blockUser error", e)
            return Result.error(AppError.DataError.Other("Failed to block user: ${e.message}", e))
        }
    }

    override suspend fun unblockUser(userId: String, unblockedUserId: String): Result<Unit> {
        try {
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .update("blockedUsers", FieldValue.arrayRemove(unblockedUserId))
                .await()
            
            // Update local cache
            val cachedUser = userDao.getUserById(userId)
            if (cachedUser != null) {
                val updatedBlockedUsers = cachedUser.blockedUsers.toMutableList().apply { remove(unblockedUserId) }
                val updatedUser = cachedUser.copy(blockedUsers = updatedBlockedUsers)
                userDao.insertUser(updatedUser)
            }
            
            return Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(TAG, "unblockUser error", e)
            return Result.error(AppError.DataError.Other("Failed to unblock user: ${e.message}", e))
        }
    }

    override suspend fun getBlockedUsers(userId: String): Result<List<String>> {
        try {
            val documentSnapshot = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .get()
                .await()
            
            if (documentSnapshot.exists()) {
                @Suppress("UNCHECKED_CAST")
                val blockedUsers = documentSnapshot.get("blockedUsers") as? List<String> ?: emptyList()
                return Result.success(blockedUsers)
            } else {
                return Result.error(AppError.DataError.NotFound("User not found"))
            }
        } catch (e: Exception) {
            Timber.e(TAG, "getBlockedUsers error", e)
            return Result.error(AppError.DataError.Other("Failed to get blocked users: ${e.message}", e))
        }
    }

    override suspend fun searchUsers(query: String, limit: Int): Result<List<User>> {
        try {
            val searchQuery = "%$query%"
            
            // First try from local cache
            val localResults = userDao.searchUsers(searchQuery)
            if (localResults.isNotEmpty()) {
                return Result.success(localResults.map { it.toUser() })
            }
            
            // If not in cache or empty results, try Firestore
            if (!networkMonitor.isOnline.value) {
                return Result.error(AppError.NetworkError.NoConnection("No internet connection"))
            }
            
            val querySnapshot = firestore.collection(USERS_COLLECTION)
                .orderBy("displayName")
                .startAt(query)
                .endAt(query + "\uf8ff")
                .limit(limit.toLong())
                .get()
                .await()
            
            val users = querySnapshot.documents.mapNotNull { it.toObject(User::class.java) }
            
            // Cache results locally
            userDao.insertUsers(users.map { UserEntity.fromUser(it) })
            
            return Result.success(users)
        } catch (e: Exception) {
            Timber.e(TAG, "searchUsers error", e)
            return Result.error(AppError.DataError.Other("Failed to search users: ${e.message}", e))
        }
    }

    override suspend fun getSuggestedUsers(userId: String, limit: Int): Result<List<User>> {
        try {
            // Get current user to check preferences
            val userResult = getUserById(userId)
            if (userResult is Result.Error) {
                return userResult.map { emptyList<User>() }
            }
            
            val currentUser = (userResult as Result.Success).data
            val preferredGenders = currentUser.genderPreference
            
            if (!networkMonitor.isOnline.value) {
                return Result.error(AppError.NetworkError.NoConnection("No internet connection"))
            }
            
            // Query users based on preferences
            var query = firestore.collection(USERS_COLLECTION)
                .whereNotEqualTo("id", userId)
                .limit(limit.toLong())
            
            // Filter by gender preference if specified
            if (preferredGenders.isNotEmpty()) {
                query = query.whereIn("gender", preferredGenders.map { it.name })
            }
            
            // Get users not in blocked list
            val blockedUsersResult = getBlockedUsers(userId)
            val blockedUsers = if (blockedUsersResult is Result.Success) {
                blockedUsersResult.data
            } else {
                emptyList()
            }
            
            val querySnapshot = query.get().await()
            val allUsers = querySnapshot.documents.mapNotNull { it.toObject(User::class.java) }
            
            // Filter out blocked users
            val suggestedUsers = allUsers.filter { !blockedUsers.contains(it.id) }
            
            return Result.success(suggestedUsers)
        } catch (e: Exception) {
            Timber.e(TAG, "getSuggestedUsers error", e)
            return Result.error(AppError.DataError.Other("Failed to get suggested users: ${e.message}", e))
        }
    }

    override suspend fun updateGender(userId: String, gender: UserGender): Result<Unit> {
        try {
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .update("gender", gender.name)
                .await()
            
            // Update local cache
            val cachedUser = userDao.getUserById(userId)
            if (cachedUser != null) {
                val updatedUser = cachedUser.copy(gender = gender)
                userDao.insertUser(updatedUser)
            }
            
            return Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(TAG, "updateGender error", e)
            return Result.error(AppError.DataError.Other("Failed to update gender: ${e.message}", e))
        }
    }

    override suspend fun updateGenderPreferences(userId: String, preferences: List<UserGender>): Result<Unit> {
        try {
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .update("genderPreference", preferences.map { it.name })
                .await()
            
            // Update local cache
            val cachedUser = userDao.getUserById(userId)
            if (cachedUser != null) {
                val updatedUser = cachedUser.copy(genderPreference = preferences)
                userDao.insertUser(updatedUser)
            }
            
            return Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(TAG, "updateGenderPreferences error", e)
            return Result.error(AppError.DataError.Other("Failed to update gender preferences: ${e.message}", e))
        }
    }

    override suspend fun updateInterests(userId: String, interests: List<String>): Result<Unit> {
        try {
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .update("interests", interests)
                .await()
            
            // Update local cache
            val cachedUser = userDao.getUserById(userId)
            if (cachedUser != null) {
                val updatedUser = cachedUser.copy(interests = interests)
                userDao.insertUser(updatedUser)
            }
            
            return Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(TAG, "updateInterests error", e)
            return Result.error(AppError.DataError.Other("Failed to update interests: ${e.message}", e))
        }
    }

    override suspend fun addPoints(userId: String, points: Int): Result<Int> {
        try {
            // Get current points from Firestore
            val documentSnapshot = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .get()
                .await()
            
            if (!documentSnapshot.exists()) {
                return Result.error(AppError.DataError.NotFound("User not found"))
            }
            
            val currentPoints = documentSnapshot.getLong("points")?.toInt() ?: 0
            val newPoints = currentPoints + points
            
            // Update points in Firestore
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .update("points", newPoints)
                .await()
            
            // Update local cache
            val cachedUser = userDao.getUserById(userId)
            if (cachedUser != null) {
                val updatedUser = cachedUser.copy(points = newPoints)
                userDao.insertUser(updatedUser)
            }
            
            return Result.success(newPoints)
        } catch (e: Exception) {
            Timber.e(TAG, "addPoints error", e)
            return Result.error(AppError.DataError.Other("Failed to add points: ${e.message}", e))
        }
    }

    override suspend fun subtractPoints(userId: String, points: Int): Result<Int> {
        try {
            // Get current points from Firestore
            val documentSnapshot = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .get()
                .await()
            
            if (!documentSnapshot.exists()) {
                return Result.error(AppError.DataError.NotFound("User not found"))
            }
            
            val currentPoints = documentSnapshot.getLong("points")?.toInt() ?: 0
            
            // Check if user has enough points
            if (currentPoints < points) {
                return Result.error(AppError.DataError.Other("Insufficient points"))
            }
            
            val newPoints = currentPoints - points
            
            // Update points in Firestore
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .update("points", newPoints)
                .await()
            
            // Update local cache
            val cachedUser = userDao.getUserById(userId)
            if (cachedUser != null) {
                val updatedUser = cachedUser.copy(points = newPoints)
                userDao.insertUser(updatedUser)
            }
            
            return Result.success(newPoints)
        } catch (e: Exception) {
            Timber.e(TAG, "subtractPoints error", e)
            return Result.error(AppError.DataError.Other("Failed to subtract points: ${e.message}", e))
        }
    }

    override suspend fun getVerifiedUsers(limit: Int): Result<List<User>> {
        try {
            // First try from local cache
            val localResults = userDao.getVerifiedUsers()
            if (localResults.isNotEmpty()) {
                return Result.success(localResults.map { it.toUser() })
            }
            
            // If not in cache or empty results, try Firestore
            if (!networkMonitor.isOnline.value) {
                return Result.error(AppError.NetworkError.NoConnection("No internet connection"))
            }
            
            val querySnapshot = firestore.collection(USERS_COLLECTION)
                .whereEqualTo("verificationStatus", VerificationStatus.VERIFIED.name)
                .limit(limit.toLong())
                .get()
                .await()
            
            val users = querySnapshot.documents.mapNotNull { it.toObject(User::class.java) }
            
            // Cache results locally
            userDao.insertUsers(users.map { UserEntity.fromUser(it) })
            
            return Result.success(users)
        } catch (e: Exception) {
            Timber.e(TAG, "getVerifiedUsers error", e)
            return Result.error(AppError.DataError.Other("Failed to get verified users: ${e.message}", e))
        }
    }

    override suspend fun getAdminUsers(): Result<List<User>> {
        try {
            // First try from local cache
            val localResults = userDao.getAdminUsers()
            if (localResults.isNotEmpty()) {
                return Result.success(localResults.map { it.toUser() })
            }
            
            // If not in cache or empty results, try Firestore
            if (!networkMonitor.isOnline.value) {
                return Result.error(AppError.NetworkError.NoConnection("No internet connection"))
            }
            
            val querySnapshot = firestore.collection(USERS_COLLECTION)
                .whereEqualTo("isAdmin", true)
                .get()
                .await()
            
            val users = querySnapshot.documents.mapNotNull { it.toObject(User::class.java) }
            
            // Cache results locally
            userDao.insertUsers(users.map { UserEntity.fromUser(it) })
            
            return Result.success(users)
        } catch (e: Exception) {
            Timber.e(TAG, "getAdminUsers error", e)
            return Result.error(AppError.DataError.Other("Failed to get admin users: ${e.message}", e))
        }
    }

    override suspend fun deleteUserAccount(userId: String): Result<Unit> {
        try {
            // Delete user from Firestore
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .delete()
                .await()
            
            // Delete from local cache
            userDao.deleteUserById(userId)
            
            return Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(TAG, "deleteUserAccount error", e)
            return Result.error(AppError.DataError.Other("Failed to delete user account: ${e.message}", e))
        }
    }
}