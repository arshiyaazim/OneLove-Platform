package com.kilagee.onelove.data.repository.impl

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.core.net.toFile
import androidx.core.net.toUri
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import com.google.firebase.storage.StorageReference
import com.kilagee.onelove.data.model.Result
import com.kilagee.onelove.data.repository.StorageRepository
import com.kilagee.onelove.util.AppError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Implementation of StorageRepository using Firebase Storage
 */
@Singleton
class StorageRepositoryImpl @Inject constructor(
    private val context: Context,
    private val storage: FirebaseStorage
) : StorageRepository {

    private val progressMap = mutableMapOf<String, MutableStateFlow<Float>>()
    private val TAG = "StorageRepositoryImpl"

    // User profile image storage path
    private val profileImagesPath = "users/{userId}/profile"
    private val coverImagesPath = "users/{userId}/cover"
    private val galleryImagesPath = "users/{userId}/gallery"
    
    // Message media storage paths
    private val messageImagesPath = "messages/{chatId}/{senderId}/images"
    private val messageVideosPath = "messages/{chatId}/{senderId}/videos"
    private val messageAudiosPath = "messages/{chatId}/{senderId}/audios"
    
    // Offer media storage path
    private val offerMediaPath = "offers/{offerId}/media"
    
    // Verification documents storage path
    private val verificationDocsPath = "verification/{userId}/{docType}"
    
    // AI profile media storage paths
    private val aiProfileAvatarPath = "ai_profiles/{profileId}/avatar"
    private val aiProfileGalleryPath = "ai_profiles/{profileId}/gallery"
    private val aiProfileVoicePath = "ai_profiles/{profileId}/voice"

    override suspend fun uploadUserProfileImage(
        userId: String,
        imageUri: Uri,
        onProgress: ((Float) -> Unit)?
    ): Result<String> {
        val path = profileImagesPath.replace("{userId}", userId)
        val fileName = "profile_${System.currentTimeMillis()}.jpg"
        
        return try {
            // Optimize image before upload
            val optimizedImageUri = optimizeImage(imageUri).getOrNull() ?: imageUri
            
            // Delete old profile images to save storage space
            deleteOldFiles("$path/", listOf(fileName))
            
            // Upload new image
            val downloadUrl = uploadFile(
                optimizedImageUri,
                "$path/$fileName",
                "image/jpeg",
                onProgress
            )
            
            Result.success(downloadUrl)
        } catch (e: Exception) {
            Timber.e(TAG, "uploadUserProfileImage error", e)
            Result.error(AppError.StorageError.UploadFailed("Failed to upload profile image: ${e.message}", e))
        }
    }

    override suspend fun uploadUserCoverImage(
        userId: String,
        imageUri: Uri,
        onProgress: ((Float) -> Unit)?
    ): Result<String> {
        val path = coverImagesPath.replace("{userId}", userId)
        val fileName = "cover_${System.currentTimeMillis()}.jpg"
        
        return try {
            // Optimize image before upload (cover images can be larger)
            val optimizedImageUri = optimizeImage(
                imageUri,
                maxWidth = 2000,
                maxHeight = 1000
            ).getOrNull() ?: imageUri
            
            // Delete old cover images to save storage space
            deleteOldFiles("$path/", listOf(fileName))
            
            // Upload new image
            val downloadUrl = uploadFile(
                optimizedImageUri,
                "$path/$fileName",
                "image/jpeg",
                onProgress
            )
            
            Result.success(downloadUrl)
        } catch (e: Exception) {
            Timber.e(TAG, "uploadUserCoverImage error", e)
            Result.error(AppError.StorageError.UploadFailed("Failed to upload cover image: ${e.message}", e))
        }
    }

    override suspend fun uploadUserGalleryImage(
        userId: String,
        imageUri: Uri,
        onProgress: ((Float) -> Unit)?
    ): Result<String> {
        val path = galleryImagesPath.replace("{userId}", userId)
        val fileName = "gallery_${System.currentTimeMillis()}.jpg"
        
        return try {
            // Optimize image before upload
            val optimizedImageUri = optimizeImage(
                imageUri,
                maxWidth = 1200,
                maxHeight = 1200
            ).getOrNull() ?: imageUri
            
            // Upload new image
            val downloadUrl = uploadFile(
                optimizedImageUri,
                "$path/$fileName",
                "image/jpeg",
                onProgress
            )
            
            Result.success(downloadUrl)
        } catch (e: Exception) {
            Timber.e(TAG, "uploadUserGalleryImage error", e)
            Result.error(AppError.StorageError.UploadFailed("Failed to upload gallery image: ${e.message}", e))
        }
    }

    override suspend fun uploadMessageImage(
        chatId: String,
        senderId: String,
        imageUri: Uri,
        onProgress: ((Float) -> Unit)?
    ): Result<Pair<String, String>> {
        val path = messageImagesPath
            .replace("{chatId}", chatId)
            .replace("{senderId}", senderId)
        val fileName = "img_${System.currentTimeMillis()}.jpg"
        val thumbnailName = "thumb_$fileName"
        
        return try {
            // Optimize original image
            val optimizedImageUri = optimizeImage(
                imageUri,
                maxWidth = 1920,
                maxHeight = 1920,
                quality = 90
            ).getOrNull() ?: imageUri
            
            // Create thumbnail
            val thumbnailUri = createThumbnail(imageUri, 320, 320)
                .getOrNull() ?: optimizedImageUri
            
            // Upload original image
            val imageUrl = uploadFile(
                optimizedImageUri,
                "$path/$fileName",
                "image/jpeg",
                onProgress
            )
            
            // Upload thumbnail
            val thumbnailUrl = uploadFile(
                thumbnailUri,
                "$path/$thumbnailName",
                "image/jpeg"
            )
            
            // Clean up temporary files if needed
            if (optimizedImageUri.scheme == "file") {
                try {
                    optimizedImageUri.toFile().delete()
                } catch (e: Exception) {
                    Timber.e(TAG, "Error deleting temp file", e)
                }
            }
            if (thumbnailUri.scheme == "file") {
                try {
                    thumbnailUri.toFile().delete()
                } catch (e: Exception) {
                    Timber.e(TAG, "Error deleting temp thumbnail", e)
                }
            }
            
            Result.success(Pair(imageUrl, thumbnailUrl))
        } catch (e: Exception) {
            Timber.e(TAG, "uploadMessageImage error", e)
            Result.error(AppError.StorageError.UploadFailed("Failed to upload message image: ${e.message}", e))
        }
    }

    override suspend fun uploadMessageVideo(
        chatId: String,
        senderId: String,
        videoUri: Uri,
        onProgress: ((Float) -> Unit)?
    ): Result<Pair<String, String>> {
        val path = messageVideosPath
            .replace("{chatId}", chatId)
            .replace("{senderId}", senderId)
        val fileName = "video_${System.currentTimeMillis()}.mp4"
        val thumbnailName = "thumb_${System.currentTimeMillis()}.jpg"
        
        return try {
            // Extract video thumbnail
            val thumbnailUri = extractVideoThumbnail(videoUri)
                .getOrNull()
            
            // Upload video
            val videoUrl = uploadFile(
                videoUri,
                "$path/$fileName",
                "video/mp4",
                onProgress
            )
            
            // Upload thumbnail if available
            val thumbnailUrl = if (thumbnailUri != null) {
                uploadFile(
                    thumbnailUri,
                    "$path/$thumbnailName",
                    "image/jpeg"
                )
            } else {
                ""
            }
            
            // Clean up temporary files
            if (thumbnailUri?.scheme == "file") {
                try {
                    thumbnailUri.toFile().delete()
                } catch (e: Exception) {
                    Timber.e(TAG, "Error deleting temp thumbnail", e)
                }
            }
            
            Result.success(Pair(videoUrl, thumbnailUrl))
        } catch (e: Exception) {
            Timber.e(TAG, "uploadMessageVideo error", e)
            Result.error(AppError.StorageError.UploadFailed("Failed to upload video: ${e.message}", e))
        }
    }

    override suspend fun uploadMessageAudio(
        chatId: String,
        senderId: String,
        audioUri: Uri,
        onProgress: ((Float) -> Unit)?
    ): Result<String> {
        val path = messageAudiosPath
            .replace("{chatId}", chatId)
            .replace("{senderId}", senderId)
        val fileName = "audio_${System.currentTimeMillis()}.m4a"
        
        return try {
            // Upload audio file
            val audioUrl = uploadFile(
                audioUri,
                "$path/$fileName",
                "audio/m4a",
                onProgress
            )
            
            Result.success(audioUrl)
        } catch (e: Exception) {
            Timber.e(TAG, "uploadMessageAudio error", e)
            Result.error(AppError.StorageError.UploadFailed("Failed to upload audio: ${e.message}", e))
        }
    }

    override suspend fun uploadOfferMedia(
        offerId: String,
        mediaUri: Uri,
        onProgress: ((Float) -> Unit)?
    ): Result<String> {
        val path = offerMediaPath.replace("{offerId}", offerId)
        val mimeType = getMimeType(mediaUri)
        val extension = getExtensionFromMimeType(mimeType)
        val fileName = "offer_${System.currentTimeMillis()}.$extension"
        
        return try {
            // Process media based on type
            val processedUri = when {
                mimeType.startsWith("image/") -> {
                    // Optimize image
                    optimizeImage(
                        mediaUri,
                        maxWidth = 1200,
                        maxHeight = 1200
                    ).getOrNull() ?: mediaUri
                }
                else -> mediaUri
            }
            
            // Upload media
            val mediaUrl = uploadFile(
                processedUri,
                "$path/$fileName",
                mimeType,
                onProgress
            )
            
            // Clean up temporary files
            if (processedUri != mediaUri && processedUri.scheme == "file") {
                try {
                    processedUri.toFile().delete()
                } catch (e: Exception) {
                    Timber.e(TAG, "Error deleting temp file", e)
                }
            }
            
            Result.success(mediaUrl)
        } catch (e: Exception) {
            Timber.e(TAG, "uploadOfferMedia error", e)
            Result.error(AppError.StorageError.UploadFailed("Failed to upload offer media: ${e.message}", e))
        }
    }

    override suspend fun uploadVerificationDocument(
        userId: String,
        documentUri: Uri,
        documentType: String,
        onProgress: ((Float) -> Unit)?
    ): Result<String> {
        val path = verificationDocsPath
            .replace("{userId}", userId)
            .replace("{docType}", documentType)
        val mimeType = getMimeType(documentUri)
        val extension = getExtensionFromMimeType(mimeType)
        val fileName = "doc_${System.currentTimeMillis()}.$extension"
        
        return try {
            // Process document if it's an image
            val processedUri = if (mimeType.startsWith("image/")) {
                // Optimize document image with high quality for readability
                optimizeImage(
                    documentUri,
                    maxWidth = 2000,
                    maxHeight = 2000,
                    quality = 95
                ).getOrNull() ?: documentUri
            } else {
                documentUri
            }
            
            // Upload document with metadata
            val metadata = StorageMetadata.Builder()
                .setContentType(mimeType)
                .setCustomMetadata("documentType", documentType)
                .setCustomMetadata("userId", userId)
                .build()
            
            val docUrl = uploadFile(
                processedUri,
                "$path/$fileName",
                mimeType,
                onProgress,
                metadata
            )
            
            // Clean up temporary files
            if (processedUri != documentUri && processedUri.scheme == "file") {
                try {
                    processedUri.toFile().delete()
                } catch (e: Exception) {
                    Timber.e(TAG, "Error deleting temp file", e)
                }
            }
            
            Result.success(docUrl)
        } catch (e: Exception) {
            Timber.e(TAG, "uploadVerificationDocument error", e)
            Result.error(AppError.StorageError.UploadFailed("Failed to upload verification document: ${e.message}", e))
        }
    }

    override suspend fun uploadAIProfileAvatar(
        profileId: String,
        imageUri: Uri,
        onProgress: ((Float) -> Unit)?
    ): Result<String> {
        val path = aiProfileAvatarPath.replace("{profileId}", profileId)
        val fileName = "avatar_${System.currentTimeMillis()}.jpg"
        
        return try {
            // Optimize avatar image
            val optimizedImageUri = optimizeImage(
                imageUri,
                maxWidth = 512,
                maxHeight = 512,
                quality = 90
            ).getOrNull() ?: imageUri
            
            // Delete old avatars to save storage space
            deleteOldFiles("$path/", listOf(fileName))
            
            // Upload new avatar
            val avatarUrl = uploadFile(
                optimizedImageUri,
                "$path/$fileName",
                "image/jpeg",
                onProgress
            )
            
            Result.success(avatarUrl)
        } catch (e: Exception) {
            Timber.e(TAG, "uploadAIProfileAvatar error", e)
            Result.error(AppError.StorageError.UploadFailed("Failed to upload AI profile avatar: ${e.message}", e))
        }
    }

    override suspend fun uploadAIProfileGalleryImage(
        profileId: String,
        imageUri: Uri,
        onProgress: ((Float) -> Unit)?
    ): Result<String> {
        val path = aiProfileGalleryPath.replace("{profileId}", profileId)
        val fileName = "gallery_${System.currentTimeMillis()}.jpg"
        
        return try {
            // Optimize gallery image
            val optimizedImageUri = optimizeImage(
                imageUri,
                maxWidth = 1200,
                maxHeight = 1200,
                quality = 90
            ).getOrNull() ?: imageUri
            
            // Upload new image
            val imageUrl = uploadFile(
                optimizedImageUri,
                "$path/$fileName",
                "image/jpeg",
                onProgress
            )
            
            Result.success(imageUrl)
        } catch (e: Exception) {
            Timber.e(TAG, "uploadAIProfileGalleryImage error", e)
            Result.error(AppError.StorageError.UploadFailed("Failed to upload AI profile gallery image: ${e.message}", e))
        }
    }

    override suspend fun uploadAIProfileVoice(
        profileId: String,
        audioUri: Uri,
        onProgress: ((Float) -> Unit)?
    ): Result<String> {
        val path = aiProfileVoicePath.replace("{profileId}", profileId)
        val fileName = "voice_${System.currentTimeMillis()}.m4a"
        
        return try {
            // Delete old voice recordings to save storage space
            deleteOldFiles("$path/", listOf(fileName))
            
            // Upload new voice recording
            val voiceUrl = uploadFile(
                audioUri,
                "$path/$fileName",
                "audio/m4a",
                onProgress
            )
            
            Result.success(voiceUrl)
        } catch (e: Exception) {
            Timber.e(TAG, "uploadAIProfileVoice error", e)
            Result.error(AppError.StorageError.UploadFailed("Failed to upload AI profile voice: ${e.message}", e))
        }
    }

    override suspend fun deleteFile(fileUrl: String): Result<Unit> {
        if (fileUrl.isEmpty()) return Result.success(Unit)
        
        return try {
            val ref = storage.getReferenceFromUrl(fileUrl)
            ref.delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(TAG, "deleteFile error", e)
            Result.error(AppError.StorageError.Other("Failed to delete file: ${e.message}", e))
        }
    }

    override suspend fun deleteUserImage(userId: String, imageUrl: String): Result<Unit> {
        return deleteFile(imageUrl)
    }

    override suspend fun getDownloadUrl(path: String): Result<String> {
        return try {
            val ref = storage.reference.child(path)
            val url = ref.downloadUrl.await().toString()
            Result.success(url)
        } catch (e: Exception) {
            Timber.e(TAG, "getDownloadUrl error", e)
            Result.error(AppError.StorageError.DownloadFailed("Failed to get download URL: ${e.message}", e))
        }
    }

    override suspend fun fileExists(path: String): Result<Boolean> {
        return try {
            val ref = storage.reference.child(path)
            ref.metadata.await()
            Result.success(true)
        } catch (e: Exception) {
            if (e.message?.contains("not found") == true) {
                Result.success(false)
            } else {
                Timber.e(TAG, "fileExists error", e)
                Result.error(AppError.StorageError.Other("Failed to check if file exists: ${e.message}", e))
            }
        }
    }

    override suspend fun getFileMetadata(path: String): Result<Map<String, Any>> {
        return try {
            val ref = storage.reference.child(path)
            val metadata = ref.metadata.await()
            val result = mutableMapOf<String, Any>()
            
            metadata.path?.let { result["path"] = it }
            metadata.name?.let { result["name"] = it }
            metadata.contentType?.let { result["contentType"] = it }
            result["size"] = metadata.sizeBytes
            metadata.creationTimeMillis.let { result["createdAt"] = it }
            metadata.updatedTimeMillis.let { result["updatedAt"] = it }
            
            // Add custom metadata
            metadata.customMetadataKeys.forEach { key ->
                metadata.getCustomMetadata(key)?.let { value ->
                    result[key] = value
                }
            }
            
            Result.success(result)
        } catch (e: Exception) {
            Timber.e(TAG, "getFileMetadata error", e)
            Result.error(AppError.StorageError.Other("Failed to get file metadata: ${e.message}", e))
        }
    }

    override fun getDownloadProgress(fileUrl: String): Flow<Float> {
        val uploadId = fileUrl.hashCode().toString()
        return progressMap[uploadId] ?: MutableStateFlow(0f)
    }

    override suspend fun optimizeImage(
        imageUri: Uri,
        maxWidth: Int,
        maxHeight: Int,
        quality: Int
    ): Result<Uri> {
        return try {
            withContext(Dispatchers.IO) {
                // Load bitmap options to check dimensions
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                context.contentResolver.openInputStream(imageUri)?.use { inputStream ->
                    BitmapFactory.decodeStream(inputStream, null, options)
                }
                
                // Calculate scaling factor
                val width = options.outWidth
                val height = options.outHeight
                var scaleFactor = 1
                
                if (width > maxWidth || height > maxHeight) {
                    val widthScale = width.toFloat() / maxWidth
                    val heightScale = height.toFloat() / maxHeight
                    scaleFactor = Math.ceil(Math.max(widthScale, heightScale).toDouble()).toInt()
                }
                
                // If no scaling needed and input is already a file, return original
                if (scaleFactor == 1 && imageUri.scheme == "file") {
                    return@withContext Result.success(imageUri)
                }
                
                // Load bitmap with scaling
                val loadOptions = BitmapFactory.Options().apply {
                    inSampleSize = scaleFactor
                }
                
                val bitmap = context.contentResolver.openInputStream(imageUri)?.use { inputStream ->
                    BitmapFactory.decodeStream(inputStream, null, loadOptions)
                } ?: return@withContext Result.error(AppError.StorageError.InvalidFile("Failed to decode image"))
                
                // Calculate final dimensions maintaining aspect ratio
                val finalWidth: Int
                val finalHeight: Int
                
                if (bitmap.width > maxWidth || bitmap.height > maxHeight) {
                    if (bitmap.width > bitmap.height) {
                        // Landscape
                        finalWidth = maxWidth
                        finalHeight = (maxWidth.toFloat() / bitmap.width * bitmap.height).toInt()
                    } else {
                        // Portrait
                        finalHeight = maxHeight
                        finalWidth = (maxHeight.toFloat() / bitmap.height * bitmap.width).toInt()
                    }
                    
                    // Create scaled bitmap
                    val scaledBitmap = Bitmap.createScaledBitmap(bitmap, finalWidth, finalHeight, true)
                    bitmap.recycle()
                    
                    // Save to temporary file
                    val outputFile = File(context.cacheDir, "img_${UUID.randomUUID()}.jpg")
                    FileOutputStream(outputFile).use { outputStream ->
                        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
                    }
                    scaledBitmap.recycle()
                    
                    Result.success(outputFile.toUri())
                } else {
                    // Just compress the original bitmap
                    val outputFile = File(context.cacheDir, "img_${UUID.randomUUID()}.jpg")
                    FileOutputStream(outputFile).use { outputStream ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
                    }
                    bitmap.recycle()
                    
                    Result.success(outputFile.toUri())
                }
            }
        } catch (e: Exception) {
            Timber.e(TAG, "optimizeImage error", e)
            Result.error(AppError.StorageError.InvalidFile("Failed to optimize image: ${e.message}", e))
        }
    }

    // Helper function to extract a thumbnail from video
    private suspend fun extractVideoThumbnail(videoUri: Uri): Result<Uri> {
        return try {
            withContext(Dispatchers.IO) {
                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(context, videoUri)
                    
                    // Try to get a frame at 3 seconds, or first frame if video is shorter
                    val durationString = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    val duration = durationString?.toLongOrNull() ?: 0L
                    val timeUs = if (duration > 3000) 3000000 else 0
                    
                    val bitmap = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                        retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST)
                    } else {
                        retriever.getFrameAtTime(timeUs)
                    }
                    
                    if (bitmap == null) {
                        return@withContext Result.error<Uri>(
                            AppError.StorageError.InvalidFile("Could not extract video thumbnail")
                        )
                    }
                    
                    // Scale thumbnail to reasonable size
                    val width = bitmap.width
                    val height = bitmap.height
                    val maxSize = 512
                    
                    val scaledBitmap = if (width > maxSize || height > maxSize) {
                        val scaleFactor = if (width > height) {
                            maxSize.toFloat() / width
                        } else {
                            maxSize.toFloat() / height
                        }
                        val scaledWidth = (width * scaleFactor).toInt()
                        val scaledHeight = (height * scaleFactor).toInt()
                        
                        Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true)
                    } else {
                        bitmap
                    }
                    
                    // Save to temporary file
                    val outputFile = File(context.cacheDir, "thumb_${UUID.randomUUID()}.jpg")
                    FileOutputStream(outputFile).use { outputStream ->
                        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
                    }
                    
                    if (scaledBitmap != bitmap) {
                        scaledBitmap.recycle()
                    }
                    bitmap.recycle()
                    
                    Result.success(outputFile.toUri())
                } finally {
                    retriever.release()
                }
            }
        } catch (e: Exception) {
            Timber.e(TAG, "extractVideoThumbnail error", e)
            Result.error(AppError.StorageError.InvalidFile("Failed to extract video thumbnail: ${e.message}", e))
        }
    }

    // Helper function to create a thumbnail from an image
    private suspend fun createThumbnail(
        imageUri: Uri,
        maxWidth: Int,
        maxHeight: Int
    ): Result<Uri> {
        return try {
            withContext(Dispatchers.IO) {
                // Load bitmap
                val options = BitmapFactory.Options()
                options.inPreferredConfig = Bitmap.Config.ARGB_8888
                
                val bitmap = context.contentResolver.openInputStream(imageUri)?.use { inputStream ->
                    BitmapFactory.decodeStream(inputStream, null, options)
                } ?: return@withContext Result.error<Uri>(
                    AppError.StorageError.InvalidFile("Failed to decode image for thumbnail")
                )
                
                // Scale down to thumbnail size
                val width = bitmap.width
                val height = bitmap.height
                
                val scaleFactor = if (width > height) {
                    maxWidth.toFloat() / width
                } else {
                    maxHeight.toFloat() / height
                }
                
                val scaledWidth = (width * scaleFactor).toInt()
                val scaledHeight = (height * scaleFactor).toInt()
                
                val thumbnailBitmap = Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true)
                bitmap.recycle()
                
                // Save to temporary file
                val outputFile = File(context.cacheDir, "thumb_${UUID.randomUUID()}.jpg")
                FileOutputStream(outputFile).use { outputStream ->
                    thumbnailBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
                }
                thumbnailBitmap.recycle()
                
                Result.success(outputFile.toUri())
            }
        } catch (e: Exception) {
            Timber.e(TAG, "createThumbnail error", e)
            Result.error(AppError.StorageError.InvalidFile("Failed to create thumbnail: ${e.message}", e))
        }
    }

    // Helper function to upload a file to Firebase Storage
    private suspend fun uploadFile(
        fileUri: Uri,
        storagePath: String,
        mimeType: String,
        onProgress: ((Float) -> Unit)? = null,
        metadata: StorageMetadata? = null
    ): String {
        return suspendCancellableCoroutine { continuation ->
            try {
                val uploadId = fileUri.toString().hashCode().toString()
                val progressFlow = MutableStateFlow(0f)
                progressMap[uploadId] = progressFlow
                
                val storageRef = storage.reference.child(storagePath)
                val metadataBuilder = metadata ?: StorageMetadata.Builder()
                    .setContentType(mimeType)
                    .build()
                
                val uploadTask = if (fileUri.scheme == "content") {
                    context.contentResolver.openInputStream(fileUri)?.use { inputStream ->
                        val bytes = inputStream.readBytes()
                        storageRef.putBytes(bytes, metadataBuilder)
                    }
                } else {
                    fileUri.path?.let { path ->
                        storageRef.putFile(fileUri, metadataBuilder)
                    }
                }
                
                if (uploadTask == null) {
                    throw Exception("Failed to start upload task")
                }
                
                // Track progress
                val progressListener = uploadTask.addOnProgressListener { taskSnapshot ->
                    val progress = taskSnapshot.bytesTransferred.toFloat() / taskSnapshot.totalByteCount
                    progressFlow.value = progress
                    onProgress?.invoke(progress)
                }
                
                // Handle upload completion
                uploadTask.continueWithTask { task ->
                    if (!task.isSuccessful) {
                        task.exception?.let {
                            throw it
                        }
                    }
                    storageRef.downloadUrl
                }.addOnSuccessListener { uri ->
                    progressFlow.value = 1f
                    onProgress?.invoke(1f)
                    continuation.resume(uri.toString())
                }.addOnFailureListener { e ->
                    continuation.resumeWithException(e)
                }
                
                // Clean up on cancellation
                continuation.invokeOnCancellation {
                    uploadTask.removeOnProgressListener(progressListener)
                    progressMap.remove(uploadId)
                    try {
                        uploadTask.cancel()
                    } catch (e: Exception) {
                        Timber.e(TAG, "Error cancelling upload", e)
                    }
                }
            } catch (e: Exception) {
                continuation.resumeWithException(e)
            }
        }
    }

    // Helper function to delete old files from a storage path
    private suspend fun deleteOldFiles(path: String, excludeFiles: List<String> = emptyList()) {
        try {
            val storageRef = storage.reference.child(path)
            val listResult = storageRef.listAll().await()
            
            for (item in listResult.items) {
                if (!excludeFiles.contains(item.name)) {
                    try {
                        item.delete().await()
                    } catch (e: Exception) {
                        Timber.e(TAG, "Error deleting old file: ${item.path}", e)
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(TAG, "Error listing files to delete", e)
        }
    }

    // Helper function to get MIME type from URI
    private fun getMimeType(uri: Uri): String {
        return when {
            uri.scheme == "content" -> {
                context.contentResolver.getType(uri) ?: "application/octet-stream"
            }
            uri.path != null -> {
                val extension = uri.path?.substringAfterLast('.', "")
                when (extension) {
                    "jpg", "jpeg" -> "image/jpeg"
                    "png" -> "image/png"
                    "gif" -> "image/gif"
                    "mp4" -> "video/mp4"
                    "m4a" -> "audio/m4a"
                    "mp3" -> "audio/mp3"
                    "pdf" -> "application/pdf"
                    else -> "application/octet-stream"
                }
            }
            else -> "application/octet-stream"
        }
    }

    // Helper function to get file extension from MIME type
    private fun getExtensionFromMimeType(mimeType: String): String {
        return when (mimeType) {
            "image/jpeg" -> "jpg"
            "image/png" -> "png"
            "image/gif" -> "gif"
            "video/mp4" -> "mp4"
            "audio/m4a" -> "m4a"
            "audio/mp3" -> "mp3"
            "application/pdf" -> "pdf"
            else -> "bin"
        }
    }
}