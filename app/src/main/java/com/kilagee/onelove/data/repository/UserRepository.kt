package com.kilagee.onelove.data.repository

import android.net.Uri
import com.kilagee.onelove.data.model.Result
import com.kilagee.onelove.data.model.User
import com.kilagee.onelove.data.model.UserGender
import com.kilagee.onelove.data.model.UserPreference
import com.kilagee.onelove.data.model.VerificationStatus
import kotlinx.coroutines.flow.Flow
import java.util.Date

/**
 * Repository interface for user operations
 */
interface UserRepository {
    /**
     * Get user by ID
     */
    suspend fun getUserById(userId: String): Result<User>
    
    /**
     * Get current user data
     */
    suspend fun getCurrentUser(): Result<User>
    
    /**
     * Get current user as flow
     */
    fun getCurrentUserFlow(): Flow<User?>
    
    /**
     * Create user profile
     */
    suspend fun createUserProfile(user: User): Result<User>
    
    /**
     * Update user profile
     */
    suspend fun updateUserProfile(user: User): Result<User>
    
    /**
     * Update user display name
     */
    suspend fun updateDisplayName(userId: String, displayName: String): Result<Unit>
    
    /**
     * Update user bio
     */
    suspend fun updateBio(userId: String, bio: String): Result<Unit>
    
    /**
     * Update user gender
     */
    suspend fun updateGender(userId: String, gender: UserGender): Result<Unit>
    
    /**
     * Update user gender preferences
     */
    suspend fun updateGenderPreferences(userId: String, genderPreferences: List<UserGender>): Result<Unit>
    
    /**
     * Update user birth date
     */
    suspend fun updateBirthDate(userId: String, birthDate: Date): Result<Unit>
    
    /**
     * Update user interests
     */
    suspend fun updateInterests(userId: String, interests: List<String>): Result<Unit>
    
    /**
     * Update user location
     */
    suspend fun updateLocation(
        userId: String,
        latitude: Double,
        longitude: Double,
        locationName: String? = null
    ): Result<Unit>
    
    /**
     * Update user profile photo
     */
    suspend fun updateProfilePhoto(userId: String, photoUri: Uri): Result<String>
    
    /**
     * Update user cover photo
     */
    suspend fun updateCoverPhoto(userId: String, photoUri: Uri): Result<String>
    
    /**
     * Add photo to user gallery
     */
    suspend fun addPhotoToGallery(userId: String, photoUri: Uri): Result<String>
    
    /**
     * Remove photo from user gallery
     */
    suspend fun removePhotoFromGallery(userId: String, photoUrl: String): Result<Unit>
    
    /**
     * Update user preferences
     */
    suspend fun updateUserPreference(userId: String, preference: UserPreference): Result<Unit>
    
    /**
     * Update user settings
     */
    suspend fun updateUserSettings(
        userId: String,
        showLocation: Boolean? = null,
        showOnlineStatus: Boolean? = null,
        notificationEnabled: Boolean? = null,
        emailNotificationEnabled: Boolean? = null,
        profileVisibility: Boolean? = null,
        maxDistanceInKm: Int? = null,
        minAgePreference: Int? = null,
        maxAgePreference: Int? = null,
        language: String? = null
    ): Result<Unit>
    
    /**
     * Block user
     */
    suspend fun blockUser(userId: String, blockedUserId: String): Result<Unit>
    
    /**
     * Unblock user
     */
    suspend fun unblockUser(userId: String, blockedUserId: String): Result<Unit>
    
    /**
     * Report user
     */
    suspend fun reportUser(
        userId: String,
        reportedUserId: String,
        reason: String,
        details: String? = null
    ): Result<Unit>
    
    /**
     * Submit verification documents
     */
    suspend fun submitVerificationDocuments(
        userId: String,
        documentType: String,
        documentUri: Uri
    ): Result<String>
    
    /**
     * Check verification status
     */
    suspend fun checkVerificationStatus(userId: String): Result<VerificationStatus>
    
    /**
     * Search users
     */
    suspend fun searchUsers(query: String, limit: Int = 20): Result<List<User>>
    
    /**
     * Get nearby users
     */
    suspend fun getNearbyUsers(
        latitude: Double,
        longitude: Double,
        radius: Double,
        limit: Int = 20
    ): Result<List<User>>
    
    /**
     * Get recommended matches
     */
    suspend fun getRecommendedMatches(userId: String, limit: Int = 20): Result<List<User>>
    
    /**
     * Add user to favorites
     */
    suspend fun addUserToFavorites(userId: String, favoriteUserId: String): Result<Unit>
    
    /**
     * Remove user from favorites
     */
    suspend fun removeUserFromFavorites(userId: String, favoriteUserId: String): Result<Unit>
    
    /**
     * Update user online status
     */
    suspend fun updateUserOnlineStatus(userId: String, isOnline: Boolean): Result<Unit>
    
    /**
     * Update user last active timestamp
     */
    suspend fun updateUserLastActive(userId: String): Result<Unit>
    
    /**
     * Add points to user
     */
    suspend fun addUserPoints(userId: String, points: Int): Result<Int>
    
    /**
     * Subtract points from user
     */
    suspend fun subtractUserPoints(userId: String, points: Int): Result<Int>
    
    /**
     * Update user language
     */
    suspend fun updateUserLanguage(userId: String, language: String): Result<Unit>
    
    /**
     * Update user languages spoken
     */
    suspend fun updateUserLanguages(userId: String, languages: List<String>): Result<Unit>
    
    /**
     * Delete user account
     */
    suspend fun deleteUserAccount(userId: String): Result<Unit>
}