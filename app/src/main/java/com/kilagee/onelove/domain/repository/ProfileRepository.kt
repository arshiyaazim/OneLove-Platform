package com.kilagee.onelove.domain.repository

import android.net.Uri
import com.kilagee.onelove.data.model.User
import com.kilagee.onelove.data.model.UserPreferences
import com.kilagee.onelove.domain.util.Result
import kotlinx.coroutines.flow.Flow
import java.io.File

/**
 * Interface for profile-related operations
 */
interface ProfileRepository {
    
    /**
     * Get the current user's profile
     * @return The current [User]
     */
    suspend fun getCurrentUserProfile(): Result<User>
    
    /**
     * Get current user profile as a flow for real-time updates
     * @return Flow of the current [User]
     */
    fun getCurrentUserProfileFlow(): Flow<Result<User>>
    
    /**
     * Update the current user's profile
     * @param name Optional new name
     * @param bio Optional new bio
     * @param occupation Optional new occupation
     * @param education Optional new education
     * @param interests Optional new interests
     * @param height Optional new height
     * @return The updated [User]
     */
    suspend fun updateProfile(
        name: String? = null,
        bio: String? = null,
        occupation: String? = null,
        education: String? = null,
        interests: List<String>? = null,
        height: Int? = null
    ): Result<User>
    
    /**
     * Upload a profile image
     * @param imageFile File to upload
     * @param isPrimary Whether this is the primary profile image
     * @return URL of the uploaded image
     */
    suspend fun uploadProfileImage(imageFile: File, isPrimary: Boolean = false): Result<String>
    
    /**
     * Upload a profile image from URI
     * @param imageUri URI of the image to upload
     * @param isPrimary Whether this is the primary profile image
     * @return URL of the uploaded image
     */
    suspend fun uploadProfileImageFromUri(imageUri: Uri, isPrimary: Boolean = false): Result<String>
    
    /**
     * Delete a profile image
     * @param imageUrl URL of the image to delete
     */
    suspend fun deleteProfileImage(imageUrl: String): Result<Unit>
    
    /**
     * Reorder profile images
     * @param imageUrls List of image URLs in the desired order
     * @return The updated list of image URLs
     */
    suspend fun reorderProfileImages(imageUrls: List<String>): Result<List<String>>
    
    /**
     * Get the current user's preferences
     * @return The current [UserPreferences]
     */
    suspend fun getUserPreferences(): Result<UserPreferences>
    
    /**
     * Update the current user's preferences
     * @param minAge Optional minimum age preference
     * @param maxAge Optional maximum age preference
     * @param maxDistance Optional maximum distance preference
     * @param genderPreferences Optional gender preferences
     * @param showMe Optional show me setting
     * @param autoPlayVideos Optional auto-play videos setting
     * @param showOnlineStatus Optional show online status setting
     * @param showLastActive Optional show last active setting
     * @return The updated [UserPreferences]
     */
    suspend fun updateUserPreferences(
        minAge: Int? = null,
        maxAge: Int? = null,
        maxDistance: Int? = null,
        genderPreferences: List<String>? = null,
        showMe: Boolean? = null,
        autoPlayVideos: Boolean? = null,
        showOnlineStatus: Boolean? = null,
        showLastActive: Boolean? = null
    ): Result<UserPreferences>
    
    /**
     * Get verification status
     * @return The current verification level (0-4)
     */
    suspend fun getVerificationStatus(): Result<Int>
    
    /**
     * Start email verification
     * @return true if verification email was sent
     */
    suspend fun startEmailVerification(): Result<Boolean>
    
    /**
     * Start phone verification
     * @param phoneNumber Phone number to verify
     * @return true if verification SMS was sent
     */
    suspend fun startPhoneVerification(phoneNumber: String): Result<Boolean>
    
    /**
     * Verify phone with code
     * @param verificationId Verification ID from SMS
     * @param code Verification code from SMS
     * @return true if verification was successful
     */
    suspend fun verifyPhoneWithCode(verificationId: String, code: String): Result<Boolean>
    
    /**
     * Start ID verification
     * @param idType Type of ID document
     * @param idFrontFile File with front of ID
     * @param idBackFile Optional file with back of ID
     * @param selfieFile File with selfie
     * @return Verification request ID
     */
    suspend fun startIdVerification(
        idType: String,
        idFrontFile: File,
        idBackFile: File? = null,
        selfieFile: File
    ): Result<String>
    
    /**
     * Check ID verification status
     * @param verificationRequestId ID of the verification request
     * @return Verification status
     */
    suspend fun checkIdVerificationStatus(verificationRequestId: String): Result<String>
    
    /**
     * Deactivate account
     * @param reason Optional reason for deactivation
     */
    suspend fun deactivateAccount(reason: String? = null): Result<Unit>
    
    /**
     * Delete account
     * @param reason Optional reason for deletion
     * @param feedback Optional feedback
     */
    suspend fun deleteAccount(
        reason: String? = null,
        feedback: String? = null
    ): Result<Unit>
    
    /**
     * Update location
     * @param latitude Latitude
     * @param longitude Longitude
     * @param city Optional city
     * @param country Optional country
     */
    suspend fun updateLocation(
        latitude: Double,
        longitude: Double,
        city: String? = null,
        country: String? = null
    ): Result<Unit>
}