package com.kilagee.onelove.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

/**
 * Helper class for image operations
 */
object ImageHelper {
    
    /**
     * Compress an image from a URI
     * 
     * @param context The application context
     * @param uri The URI of the image to compress
     * @param quality The quality of the compressed image (0-100)
     * @return The URI of the compressed image
     */
    suspend fun compressImage(context: Context, uri: Uri, quality: Int = 80): Result<Uri> {
        return withContext(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                
                val outputStream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
                
                val file = File(context.cacheDir, "compressed_${System.currentTimeMillis()}.jpg")
                val fileOutputStream = FileOutputStream(file)
                fileOutputStream.write(outputStream.toByteArray())
                fileOutputStream.close()
                
                Result.success(Uri.fromFile(file))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    /**
     * Get a bitmap from a URI
     * 
     * @param context The application context
     * @param uri The URI of the image
     * @return The bitmap of the image
     */
    suspend fun getBitmapFromUri(context: Context, uri: Uri): Result<Bitmap> {
        return withContext(Dispatchers.IO) {
            try {
                val bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                Result.success(bitmap)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
