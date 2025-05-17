package com.kilagee.onelove.data.repository

import android.net.Uri
import com.kilagee.onelove.data.model.Result
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for Firebase Storage operations
 */
interface StorageRepository {
    
    /**
     * Upload user profile image
     * @param userId User ID
     * @param imageUri Image URI
     * @param onProgress Optional progress callback
     * @return Result containing the download URL or error
     */
    suspend fun uploadUserProfileImage(
        userId: String,
        imageUri: Uri,
        onProgress: ((Float) -> Unit)? = null
    ): Result<String>
    
    /**
     * Upload user cover image
     * @param userId User ID
     * @param imageUri Image URI
     * @param onProgress Optional progress callback
     * @return Result containing the download URL or error
     */
    suspend fun uploadUserCoverImage(
        userId: String,
        imageUri: Uri,
        onProgress: ((Float) -> Unit)? = null
    ): Result<String>
    
    /**
     * Upload user gallery image
     * @param userId User ID
     * @param imageUri Image URI
     * @param onProgress Optional progress callback
     * @return Result containing the download URL or error
     */
    suspend fun uploadUserGalleryImage(
        userId: String,
        imageUri: Uri,
        onProgress: ((Float) -> Unit)? = null
    ): Result<String>
    
    /**
     * Upload message image
     * @param chatId Chat ID
     * @param senderId Sender ID
     * @param imageUri Image URI
     * @param onProgress Optional progress callback
     * @return Result containing the pair of download URL and thumbnail URL or error
     */
    suspend fun uploadMessageImage(
        chatId: String,
        senderId: String,
        imageUri: Uri,
        onProgress: ((Float) -> Unit)? = null
    ): Result<Pair<String, String>>
    
    /**
     * Upload message video
     * @param chatId Chat ID
     * @param senderId Sender ID
     * @param videoUri Video URI
     * @param onProgress Optional progress callback
     * @return Result containing the pair of download URL and thumbnail URL or error
     */
    suspend fun uploadMessageVideo(
        chatId: String,
        senderId: String,
        videoUri: Uri,
        onProgress: ((Float) -> Unit)? = null
    ): Result<Pair<String, String>>
    
    /**
     * Upload message audio
     * @param chatId Chat ID
     * @param senderId Sender ID
     * @param audioUri Audio URI
     * @param onProgress Optional progress callback
     * @return Result containing the download URL or error
     */
    suspend fun uploadMessageAudio(
        chatId: String,
        senderId: String,
        audioUri: Uri,
        onProgress: ((Float) -> Unit)? = null
    ): Result<String>
    
    /**
     * Upload offer media
     * @param offerId Offer ID
     * @param mediaUri Media URI
     * @param onProgress Optional progress callback
     * @return Result containing the download URL or error
     */
    suspend fun uploadOfferMedia(
        offerId: String,
        mediaUri: Uri,
        onProgress: ((Float) -> Unit)? = null
    ): Result<String>
    
    /**
     * Upload verification document
     * @param userId User ID
     * @param documentUri Document URI
     * @param documentType Document type identifier
     * @param onProgress Optional progress callback
     * @return Result containing the download URL or error
     */
    suspend fun uploadVerificationDocument(
        userId: String,
        documentUri: Uri,
        documentType: String,
        onProgress: ((Float) -> Unit)? = null
    ): Result<String>
    
    /**
     * Upload AI profile avatar
     * @param profileId AI profile ID
     * @param imageUri Image URI
     * @param onProgress Optional progress callback
     * @return Result containing the download URL or error
     */
    suspend fun uploadAIProfileAvatar(
        profileId: String,
        imageUri: Uri,
        onProgress: ((Float) -> Unit)? = null
    ): Result<String>
    
    /**
     * Upload AI profile gallery image
     * @param profileId AI profile ID
     * @param imageUri Image URI
     * @param onProgress Optional progress callback
     * @return Result containing the download URL or error
     */
    suspend fun uploadAIProfileGalleryImage(
        profileId: String,
        imageUri: Uri,
        onProgress: ((Float) -> Unit)? = null
    ): Result<String>
    
    /**
     * Upload AI profile voice
     * @param profileId AI profile ID
     * @param audioUri Audio URI
     * @param onProgress Optional progress callback
     * @return Result containing the download URL or error
     */
    suspend fun uploadAIProfileVoice(
        profileId: String,
        audioUri: Uri,
        onProgress: ((Float) -> Unit)? = null
    ): Result<String>
    
    /**
     * Delete file by URL
     * @param fileUrl File URL
     * @return Result indicating success or error
     */
    suspend fun deleteFile(fileUrl: String): Result<Unit>
    
    /**
     * Delete user image
     * @param userId User ID
     * @param imageUrl Image URL
     * @return Result indicating success or error
     */
    suspend fun deleteUserImage(userId: String, imageUrl: String): Result<Unit>
    
    /**
     * Get download URL for reference path
     * @param path Storage reference path
     * @return Result containing the download URL or error
     */
    suspend fun getDownloadUrl(path: String): Result<String>
    
    /**
     * Check if file exists at path
     * @param path Storage reference path
     * @return Result containing boolean indicating existence or error
     */
    suspend fun fileExists(path: String): Result<Boolean>
    
    /**
     * Get file metadata
     * @param path Storage reference path
     * @return Result containing map of metadata or error
     */
    suspend fun getFileMetadata(path: String): Result<Map<String, Any>>
    
    /**
     * Get file download progress
     * @param fileUrl File URL
     * @return Flow emitting download progress (0.0f to 1.0f)
     */
    fun getDownloadProgress(fileUrl: String): Flow<Float>
    
    /**
     * Optimize image before upload
     * @param imageUri Image URI
     * @param maxWidth Maximum width
     * @param maxHeight Maximum height
     * @param quality JPEG quality (0-100)
     * @return Result containing the optimized image URI or error
     */
    suspend fun optimizeImage(
        imageUri: Uri,
        maxWidth: Int = 1080,
        maxHeight: Int = 1920,
        quality: Int = 80
    ): Result<Uri>
}