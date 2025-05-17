package com.kilagee.onelove.data.model

import com.kilagee.onelove.util.AppError

/**
 * A sealed class that encapsulates successful outcome with a value of type [T]
 * or a failure with an error of type [AppError]
 */
sealed class Result<out T> {
    data class Success<out T>(val data: T) : Result<T>()
    data class Error(val error: AppError) : Result<Nothing>()
    class Loading<out T> : Result<T>()
    
    /**
     * Map the result to a new result with a different type
     */
    fun <R> map(transform: (T) -> R): Result<R> {
        return when (this) {
            is Success -> Success(transform(data))
            is Error -> this
            is Loading -> Loading()
        }
    }
    
    /**
     * Return the encapsulated data if this instance represents [Success] or null otherwise
     */
    fun getOrNull(): T? {
        return when (this) {
            is Success -> data
            else -> null
        }
    }
    
    /**
     * Return the encapsulated data if this instance represents [Success]
     * or the result of [defaultValue] function otherwise
     */
    fun getOrElse(defaultValue: () -> T): T {
        return when (this) {
            is Success -> data
            else -> defaultValue()
        }
    }
    
    /**
     * Return true if this is a [Success]
     */
    fun isSuccess(): Boolean {
        return this is Success
    }
    
    /**
     * Return true if this is an [Error]
     */
    fun isError(): Boolean {
        return this is Error
    }
    
    /**
     * Return true if this is [Loading]
     */
    fun isLoading(): Boolean {
        return this is Loading
    }
    
    companion object {
        /**
         * Create a [Success] instance
         */
        fun <T> success(data: T): Result<T> {
            return Success(data)
        }
        
        /**
         * Create an [Error] instance
         */
        fun <T> error(error: AppError): Result<T> {
            return Error(error)
        }
        
        /**
         * Create a [Loading] instance
         */
        fun <T> loading(): Result<T> {
            return Loading()
        }
    }
}