package com.kilagee.onelove.util

import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * Helper class for Firebase operations
 */
object FirebaseHelper {
    
    /**
     * Upload an image to Firebase Storage
     * 
     * @param uri The local URI of the image to upload
     * @param path The path in Firebase Storage where the image should be stored
     * @return The download URL of the uploaded image
     */
    suspend fun uploadImage(uri: Uri, path: String): Result<String> {
        return try {
            val storage = FirebaseStorage.getInstance()
            val fileName = "${UUID.randomUUID()}.jpg"
            val storageRef = storage.reference.child("$path/$fileName")
            
            val uploadTask = storageRef.putFile(uri).await()
            val downloadUrl = storageRef.downloadUrl.await().toString()
            
            Result.success(downloadUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Upload a video to Firebase Storage
     * 
     * @param uri The local URI of the video to upload
     * @param path The path in Firebase Storage where the video should be stored
     * @return The download URL of the uploaded video
     */
    suspend fun uploadVideo(uri: Uri, path: String): Result<String> {
        return try {
            val storage = FirebaseStorage.getInstance()
            val fileName = "${UUID.randomUUID()}.mp4"
            val storageRef = storage.reference.child("$path/$fileName")
            
            val uploadTask = storageRef.putFile(uri).await()
            val downloadUrl = storageRef.downloadUrl.await().toString()
            
            Result.success(downloadUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
