package com.kilagee.onelove.util

import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.functions.FirebaseFunctionsException
import com.google.firebase.storage.StorageException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import retrofit2.HttpException
import timber.log.Timber

sealed class AppError(
    val message: String,
    val cause: Throwable? = null
) {
    class NetworkError(message: String, cause: Throwable? = null) : AppError(message, cause)
    class AuthError(message: String, cause: Throwable? = null) : AppError(message, cause)
    class DataError(message: String, cause: Throwable? = null) : AppError(message, cause)
    class PaymentError(message: String, cause: Throwable? = null) : AppError(message, cause)
    class ValidationError(message: String, cause: Throwable? = null) : AppError(message, cause)
    class PermissionError(message: String, cause: Throwable? = null) : AppError(message, cause)
    class GenericError(message: String, cause: Throwable? = null) : AppError(message, cause)
}

/**
 * Error handler to convert exceptions to user-friendly error messages
 */
object ErrorHandler {
    
    /**
     * Convert an exception to a user-friendly AppError
     */
    fun handleException(throwable: Throwable): AppError {
        Timber.e(throwable, "Error occurred")
        
        return when (throwable) {
            // Network errors
            is UnknownHostException, 
            is SocketTimeoutException, 
            is IOException,
            is FirebaseNetworkException -> {
                AppError.NetworkError("Network connection error. Please check your internet connection.", throwable)
            }
            
            // Authentication errors
            is FirebaseAuthException -> {
                val message = getFirebaseAuthErrorMessage(throwable.errorCode)
                AppError.AuthError(message, throwable)
            }
            
            // Firestore errors
            is FirebaseFirestoreException -> {
                val message = getFirestoreErrorMessage(throwable.code.name)
                AppError.DataError(message, throwable)
            }
            
            // Storage errors 
            is StorageException -> {
                val message = "Storage error: ${throwable.message ?: "Unknown storage error"}"
                AppError.DataError(message, throwable)
            }
            
            // Cloud Functions errors
            is FirebaseFunctionsException -> {
                val message = when (throwable.code) {
                    FirebaseFunctionsException.Code.ABORTED -> "Operation aborted"
                    FirebaseFunctionsException.Code.INVALID_ARGUMENT -> "Invalid input provided"
                    FirebaseFunctionsException.Code.PERMISSION_DENIED -> "Permission denied"
                    FirebaseFunctionsException.Code.UNAUTHENTICATED -> "Authentication required"
                    FirebaseFunctionsException.Code.UNAVAILABLE -> "Service currently unavailable"
                    else -> "Function error: ${throwable.message ?: "Unknown function error"}"
                }
                AppError.DataError(message, throwable)
            }
            
            // HTTP errors
            is HttpException -> {
                val message = when (throwable.code()) {
                    401 -> "Authentication required"
                    403 -> "Permission denied"
                    404 -> "Resource not found"
                    in 500..599 -> "Server error, please try again later"
                    else -> "HTTP error: ${throwable.message()}"
                }
                AppError.NetworkError(message, throwable)
            }
            
            // Firebase general errors
            is FirebaseException -> {
                AppError.GenericError("Firebase error: ${throwable.message ?: "Unknown firebase error"}", throwable)
            }
            
            // Other errors
            else -> {
                AppError.GenericError("An unexpected error occurred: ${throwable.message ?: "Unknown error"}", throwable)
            }
        }
    }
    
    /**
     * Get user-friendly error message for Firebase Auth error codes
     */
    private fun getFirebaseAuthErrorMessage(errorCode: String): String {
        return when (errorCode) {
            "ERROR_INVALID_EMAIL" -> "Invalid email address format"
            "ERROR_WRONG_PASSWORD" -> "Incorrect password"
            "ERROR_USER_NOT_FOUND" -> "No account found with this email"
            "ERROR_USER_DISABLED" -> "This account has been disabled"
            "ERROR_TOO_MANY_REQUESTS" -> "Too many sign-in attempts, please try again later"
            "ERROR_OPERATION_NOT_ALLOWED" -> "This sign-in method is not allowed"
            "ERROR_EMAIL_ALREADY_IN_USE" -> "This email is already in use by another account"
            "ERROR_WEAK_PASSWORD" -> "Password is too weak, please use a stronger password"
            "ERROR_INVALID_CREDENTIAL" -> "The credential is invalid or has expired"
            "ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL" -> "An account already exists with the same email but different sign-in credentials"
            "ERROR_REQUIRES_RECENT_LOGIN" -> "This operation requires recent authentication, please sign in again"
            else -> "Authentication error: $errorCode"
        }
    }
    
    /**
     * Get user-friendly error message for Firestore error codes
     */
    private fun getFirestoreErrorMessage(errorCode: String): String {
        return when (errorCode) {
            "ABORTED" -> "Operation aborted, please try again"
            "ALREADY_EXISTS" -> "The document already exists"
            "CANCELLED" -> "Operation cancelled"
            "DATA_LOSS" -> "Data loss or corruption error"
            "DEADLINE_EXCEEDED" -> "Operation timed out, please try again"
            "FAILED_PRECONDITION" -> "Operation cannot be executed in the current system state"
            "INTERNAL" -> "Internal server error, please try again later"
            "INVALID_ARGUMENT" -> "Invalid argument provided"
            "NOT_FOUND" -> "Document not found"
            "OK" -> "Operation successful"
            "OUT_OF_RANGE" -> "Operation attempted past valid range"
            "PERMISSION_DENIED" -> "You don't have permission to perform this operation"
            "RESOURCE_EXHAUSTED" -> "Resource has been exhausted, please try again later"
            "UNAUTHENTICATED" -> "Authentication is required for this operation"
            "UNAVAILABLE" -> "Service is currently unavailable, please try again later"
            "UNIMPLEMENTED" -> "Operation is not implemented or not supported"
            "UNKNOWN" -> "Unknown error occurred"
            else -> "Database error: $errorCode"
        }
    }
}