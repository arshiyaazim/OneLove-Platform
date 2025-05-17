package com.kilagee.onelove.data.repository

import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseUser
import com.kilagee.onelove.data.model.Result
import com.kilagee.onelove.data.model.User
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for authentication operations
 */
interface AuthRepository {
    /**
     * Get the current authenticated user
     */
    fun getCurrentUser(): FirebaseUser?
    
    /**
     * Get the current user's UID
     */
    fun getCurrentUserId(): String?
    
    /**
     * Get the current authentication state as a Flow
     * Returns null when not logged in, and FirebaseUser when logged in
     */
    fun getCurrentUserFlow(): Flow<FirebaseUser?>
    
    /**
     * Check if the user is logged in
     */
    fun isLoggedIn(): Boolean
    
    /**
     * Register with email and password
     */
    suspend fun registerWithEmailAndPassword(
        email: String,
        password: String,
        displayName: String
    ): Result<FirebaseUser>
    
    /**
     * Log in with email and password
     */
    suspend fun loginWithEmailAndPassword(
        email: String,
        password: String
    ): Result<FirebaseUser>
    
    /**
     * Sign in with Google or other providers using AuthCredential
     */
    suspend fun signInWithCredential(
        credential: AuthCredential
    ): Result<FirebaseUser>
    
    /**
     * Send email verification
     */
    suspend fun sendEmailVerification(): Result<Unit>
    
    /**
     * Check if email is verified
     */
    fun isEmailVerified(): Boolean
    
    /**
     * Resend email verification
     */
    suspend fun resendEmailVerification(): Result<Unit>
    
    /**
     * Send password reset email
     */
    suspend fun sendPasswordResetEmail(email: String): Result<Unit>
    
    /**
     * Confirm password reset with code and new password
     */
    suspend fun confirmPasswordReset(
        code: String,
        newPassword: String
    ): Result<Unit>
    
    /**
     * Update password for logged in user
     */
    suspend fun updatePassword(
        currentPassword: String,
        newPassword: String
    ): Result<Unit>
    
    /**
     * Update email for logged in user
     */
    suspend fun updateEmail(
        newEmail: String,
        password: String
    ): Result<Unit>
    
    /**
     * Update display name for logged in user
     */
    suspend fun updateDisplayName(displayName: String): Result<Unit>
    
    /**
     * Link email provider to anonymous account
     */
    suspend fun linkEmailProvider(
        email: String,
        password: String
    ): Result<FirebaseUser>
    
    /**
     * Link phone number to account
     */
    suspend fun linkPhoneNumber(
        phoneNumber: String,
        verificationId: String,
        verificationCode: String
    ): Result<FirebaseUser>
    
    /**
     * Sign out
     */
    suspend fun signOut()
    
    /**
     * Delete user account
     */
    suspend fun deleteAccount(password: String): Result<Unit>
    
    /**
     * Get the initial user data after authentication
     */
    suspend fun getUserData(): Result<User>
}