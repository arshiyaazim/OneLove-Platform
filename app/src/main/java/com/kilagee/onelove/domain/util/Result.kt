package com.kilagee.onelove.domain.util

/**
 * A generic class that holds a value or an error status.
 * Used as a return type for repository operations that can fail.
 */
sealed class Result<out T> {
    /**
     * Operation completed successfully.
     */
    data class Success<T>(val data: T) : Result<T>()
    
    /**
     * Operation failed.
     */
    data class Error(val message: String, val exception: Throwable? = null) : Result<Nothing>()
    
    /**
     * Operation in progress.
     */
    object Loading : Result<Nothing>()
    
    /**
     * Returns true if this is a Success result.
     */
    val isSuccess: Boolean
        get() = this is Success
    
    /**
     * Returns true if this is an Error result.
     */
    val isError: Boolean
        get() = this is Error
    
    /**
     * Returns true if this is a Loading result.
     */
    val isLoading: Boolean
        get() = this is Loading
    
    /**
     * Returns the data if this is a Success result, or null otherwise.
     */
    fun getOrNull(): T? = when (this) {
        is Success -> data
        else -> null
    }
    
    /**
     * Returns the data if this is a Success result, or throws the exception if it's an Error result.
     */
    fun getOrThrow(): T {
        when (this) {
            is Success -> return data
            is Error -> throw exception ?: IllegalStateException(message)
            is Loading -> throw IllegalStateException("Result is still loading")
        }
    }
    
    /**
     * Maps the data if this is a Success result, or returns the same Error/Loading result.
     */
    fun <R> map(transform: (T) -> R): Result<R> {
        return when (this) {
            is Success -> Success(transform(data))
            is Error -> Error(message, exception)
            is Loading -> Loading
        }
    }
    
    companion object {
        /**
         * Creates a Success result with the given data.
         */
        fun <T> success(data: T): Result<T> = Success(data)
        
        /**
         * Creates an Error result with the given message and optional exception.
         */
        fun error(message: String, exception: Throwable? = null): Result<Nothing> = Error(message, exception)
        
        /**
         * Returns a Loading result.
         */
        fun <T> loading(): Result<T> = Loading
    }
}