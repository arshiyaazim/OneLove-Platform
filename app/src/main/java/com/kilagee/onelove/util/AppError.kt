package com.kilagee.onelove.util

/**
 * Hierarchy of app errors for consistent error handling across the app
 */
sealed class AppError(
    open val message: String,
    open val cause: Throwable? = null
) {
    /**
     * Authentication related errors
     */
    sealed class AuthError(
        override val message: String,
        override val cause: Throwable? = null
    ) : AppError(message, cause) {
        
        class InvalidCredentials(
            override val message: String,
            override val cause: Throwable? = null
        ) : AuthError(message, cause)
        
        class EmailAlreadyInUse(
            override val message: String,
            override val cause: Throwable? = null
        ) : AuthError(message, cause)
        
        class WeakPassword(
            override val message: String,
            override val cause: Throwable? = null
        ) : AuthError(message, cause)
        
        class UnknownUser(
            override val message: String,
            override val cause: Throwable? = null
        ) : AuthError(message, cause)
        
        class AccountDisabled(
            override val message: String,
            override val cause: Throwable? = null
        ) : AuthError(message, cause)
        
        class NotAuthenticated(
            override val message: String,
            override val cause: Throwable? = null
        ) : AuthError(message, cause)
        
        class SessionExpired(
            override val message: String,
            override val cause: Throwable? = null
        ) : AuthError(message, cause)
        
        class Other(
            override val message: String,
            override val cause: Throwable? = null
        ) : AuthError(message, cause)
    }
    
    /**
     * Network related errors
     */
    sealed class NetworkError(
        override val message: String,
        override val cause: Throwable? = null
    ) : AppError(message, cause) {
        
        class NoConnection(
            override val message: String,
            override val cause: Throwable? = null
        ) : NetworkError(message, cause)
        
        class Timeout(
            override val message: String,
            override val cause: Throwable? = null
        ) : NetworkError(message, cause)
        
        class ServerError(
            override val message: String,
            override val cause: Throwable? = null
        ) : NetworkError(message, cause)
        
        class Other(
            override val message: String,
            override val cause: Throwable? = null
        ) : NetworkError(message, cause)
    }
    
    /**
     * Data related errors
     */
    sealed class DataError(
        override val message: String,
        override val cause: Throwable? = null
    ) : AppError(message, cause) {
        
        class NotFound(
            override val message: String,
            override val cause: Throwable? = null
        ) : DataError(message, cause)
        
        class InvalidData(
            override val message: String,
            override val cause: Throwable? = null
        ) : DataError(message, cause)
        
        class PermissionDenied(
            override val message: String,
            override val cause: Throwable? = null
        ) : DataError(message, cause)
        
        class Other(
            override val message: String,
            override val cause: Throwable? = null
        ) : DataError(message, cause)
    }
    
    /**
     * Validation errors
     */
    sealed class ValidationError(
        override val message: String,
        override val cause: Throwable? = null
    ) : AppError(message, cause) {
        
        class EmptyField(
            val fieldName: String,
            override val message: String,
            override val cause: Throwable? = null
        ) : ValidationError(message, cause)
        
        class InvalidFormat(
            val fieldName: String,
            override val message: String,
            override val cause: Throwable? = null
        ) : ValidationError(message, cause)
        
        class TooShort(
            val fieldName: String,
            override val message: String,
            override val cause: Throwable? = null
        ) : ValidationError(message, cause)
        
        class TooLong(
            val fieldName: String,
            override val message: String,
            override val cause: Throwable? = null
        ) : ValidationError(message, cause)
        
        class Other(
            override val message: String,
            override val cause: Throwable? = null
        ) : ValidationError(message, cause)
    }
    
    /**
     * Payment related errors
     */
    sealed class PaymentError(
        override val message: String,
        override val cause: Throwable? = null
    ) : AppError(message, cause) {
        
        class PaymentFailed(
            override val message: String,
            override val cause: Throwable? = null
        ) : PaymentError(message, cause)
        
        class InsufficientFunds(
            override val message: String,
            override val cause: Throwable? = null
        ) : PaymentError(message, cause)
        
        class PaymentMethodRequired(
            override val message: String,
            override val cause: Throwable? = null
        ) : PaymentError(message, cause)
        
        class SubscriptionError(
            override val message: String,
            override val cause: Throwable? = null
        ) : PaymentError(message, cause)
        
        class Other(
            override val message: String,
            override val cause: Throwable? = null
        ) : PaymentError(message, cause)
    }
    
    /**
     * Feature related errors
     */
    sealed class FeatureError(
        override val message: String,
        override val cause: Throwable? = null
    ) : AppError(message, cause) {
        
        class PremiumRequired(
            override val message: String,
            override val cause: Throwable? = null
        ) : FeatureError(message, cause)
        
        class InsufficientPoints(
            override val message: String,
            override val cause: Throwable? = null
        ) : FeatureError(message, cause)
        
        class VerificationRequired(
            override val message: String,
            override val cause: Throwable? = null
        ) : FeatureError(message, cause)
        
        class Blocked(
            override val message: String,
            override val cause: Throwable? = null
        ) : FeatureError(message, cause)
        
        class Other(
            override val message: String,
            override val cause: Throwable? = null
        ) : FeatureError(message, cause)
    }
    
    /**
     * Storage related errors
     */
    sealed class StorageError(
        override val message: String,
        override val cause: Throwable? = null
    ) : AppError(message, cause) {
        
        class UploadFailed(
            override val message: String,
            override val cause: Throwable? = null
        ) : StorageError(message, cause)
        
        class DownloadFailed(
            override val message: String,
            override val cause: Throwable? = null
        ) : StorageError(message, cause)
        
        class FileTooLarge(
            override val message: String,
            override val cause: Throwable? = null
        ) : StorageError(message, cause)
        
        class InvalidFile(
            override val message: String,
            override val cause: Throwable? = null
        ) : StorageError(message, cause)
        
        class QuotaExceeded(
            override val message: String,
            override val cause: Throwable? = null
        ) : StorageError(message, cause)
        
        class Other(
            override val message: String,
            override val cause: Throwable? = null
        ) : StorageError(message, cause)
    }
    
    /**
     * Generic unknown error
     */
    class Unknown(
        override val message: String = "An unknown error occurred",
        override val cause: Throwable? = null
    ) : AppError(message, cause)
}