package com.kilagee.onelove.domain.repository

import com.kilagee.onelove.data.model.User
import com.kilagee.onelove.domain.util.Result
import kotlinx.coroutines.flow.Flow

/**
 * Auth repository interface
 */
interface AuthRepository {
    
    /**
     * Get authentication state
     */
    fun getAuthState(): Flow<Boolean>
    
    /**
     * Get current user ID
     */
    fun getCurrentUserId(): String?
    
    /**
     * Get current user
     */
    fun getCurrentUser(): Flow<Result<User>>
    
    /**
     * Sign in with email and password
     */
    suspend fun signInWithEmailPassword(email: String, password: String): Result<User>
    
    /**
     * Sign up with email and password
     */
    suspend fun signUpWithEmailPassword(email: String, password: String, name: String): Result<User>
    
    /**
     * Sign in with Google
     */
    suspend fun signInWithGoogle(idToken: String): Result<User>
    
    /**
     * Sign out
     */
    suspend fun signOut(): Result<Unit>
    
    /**
     * Reset password
     */
    suspend fun resetPassword(email: String): Result<Unit>
    
    /**
     * Update user profile
     */
    suspend fun updateUserProfile(user: User): Result<User>
    
    /**
     * Update user password
     */
    suspend fun updatePassword(oldPassword: String, newPassword: String): Result<Unit>
    
    /**
     * Delete user account
     */
    suspend fun deleteAccount(): Result<Unit>
    
    /**
     * Get user profile
     */
    fun getUserProfile(userId: String? = null): Flow<Result<User>>
}