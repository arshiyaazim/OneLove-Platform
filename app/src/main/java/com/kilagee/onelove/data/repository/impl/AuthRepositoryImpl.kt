package com.kilagee.onelove.data.repository.impl

import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.kilagee.onelove.data.model.Result
import com.kilagee.onelove.data.model.User
import com.kilagee.onelove.data.repository.AuthRepository
import com.kilagee.onelove.util.AppError
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of AuthRepository using Firebase Authentication
 */
@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : AuthRepository {

    companion object {
        private const val TAG = "AuthRepositoryImpl"
        private const val USERS_COLLECTION = "users"
    }

    override fun getCurrentFirebaseUser(): FirebaseUser? {
        return auth.currentUser
    }

    override fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    override fun isLoggedIn(): Boolean {
        return auth.currentUser != null
    }

    override suspend fun signInWithEmailPassword(
        email: String,
        password: String
    ): Result<FirebaseUser> {
        return try {
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            val user = authResult.user
                ?: return Result.error(AppError.AuthError.UnknownUser("Failed to retrieve user after sign in"))
            
            Result.success(user)
        } catch (e: Exception) {
            Timber.e(TAG, "signInWithEmailPassword error", e)
            val error = when (e.message) {
                "The password is invalid or the user does not have a password." -> 
                    AppError.AuthError.InvalidCredentials("Invalid password", e)
                "There is no user record corresponding to this identifier. The user may have been deleted." -> 
                    AppError.AuthError.UnknownUser("No user found with this email", e)
                "The user account has been disabled by an administrator." -> 
                    AppError.AuthError.AccountDisabled("Account has been disabled", e)
                "A network error (such as timeout, interrupted connection or unreachable host) has occurred." -> 
                    AppError.NetworkError.NoConnection("Network error during sign in", e)
                else -> AppError.AuthError.Other("Authentication error: ${e.message}", e)
            }
            Result.error(error)
        }
    }

    override suspend fun signUpWithEmailPassword(
        email: String,
        password: String,
        displayName: String
    ): Result<FirebaseUser> {
        return try {
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val user = authResult.user
                ?: return Result.error(AppError.AuthError.Other("Failed to create user"))
            
            // Update display name
            val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                .setDisplayName(displayName)
                .build()
            
            user.updateProfile(profileUpdates).await()
            
            // Create user document in Firestore
            val newUser = User(
                id = user.uid,
                email = email,
                displayName = displayName
            )
            
            firestore.collection(USERS_COLLECTION)
                .document(user.uid)
                .set(newUser)
                .await()
            
            Result.success(user)
        } catch (e: Exception) {
            Timber.e(TAG, "signUpWithEmailPassword error", e)
            val error = when (e.message) {
                "The email address is already in use by another account." -> 
                    AppError.AuthError.EmailAlreadyInUse("Email already in use", e)
                "The given password is invalid. [ Password should be at least 6 characters ]" -> 
                    AppError.AuthError.WeakPassword("Password should be at least 6 characters", e)
                "A network error (such as timeout, interrupted connection or unreachable host) has occurred." -> 
                    AppError.NetworkError.NoConnection("Network error during sign up", e)
                else -> AppError.AuthError.Other("Registration error: ${e.message}", e)
            }
            Result.error(error)
        }
    }

    override suspend fun signOut(): Result<Unit> {
        return try {
            auth.signOut()
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(TAG, "signOut error", e)
            Result.error(AppError.AuthError.Other("Sign out error: ${e.message}", e))
        }
    }

    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(TAG, "sendPasswordResetEmail error", e)
            val error = when (e.message) {
                "There is no user record corresponding to this identifier. The user may have been deleted." -> 
                    AppError.AuthError.UnknownUser("No user found with this email", e)
                "A network error (such as timeout, interrupted connection or unreachable host) has occurred." -> 
                    AppError.NetworkError.NoConnection("Network error when sending password reset email", e)
                else -> AppError.AuthError.Other("Password reset error: ${e.message}", e)
            }
            Result.error(error)
        }
    }

    override suspend fun sendEmailVerification(): Result<Unit> {
        val user = auth.currentUser
            ?: return Result.error(AppError.AuthError.NotAuthenticated("Not logged in"))
        
        return try {
            user.sendEmailVerification().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(TAG, "sendEmailVerification error", e)
            Result.error(AppError.AuthError.Other("Email verification error: ${e.message}", e))
        }
    }

    override suspend fun updatePassword(
        currentPassword: String,
        newPassword: String
    ): Result<Unit> {
        val user = auth.currentUser
            ?: return Result.error(AppError.AuthError.NotAuthenticated("Not logged in"))
        
        val email = user.email
            ?: return Result.error(AppError.AuthError.Other("User email not available"))
        
        return try {
            // Re-authenticate user
            val credential = EmailAuthProvider.getCredential(email, currentPassword)
            user.reauthenticate(credential).await()
            
            // Update password
            user.updatePassword(newPassword).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(TAG, "updatePassword error", e)
            val error = when (e.message) {
                "The password is invalid or the user does not have a password." -> 
                    AppError.AuthError.InvalidCredentials("Current password is incorrect", e)
                "This operation is sensitive and requires recent authentication. Log in again before retrying this request." -> 
                    AppError.AuthError.SessionExpired("Please sign in again to update your password", e)
                else -> AppError.AuthError.Other("Password update error: ${e.message}", e)
            }
            Result.error(error)
        }
    }

    override suspend fun updateEmail(
        newEmail: String,
        password: String
    ): Result<Unit> {
        val user = auth.currentUser
            ?: return Result.error(AppError.AuthError.NotAuthenticated("Not logged in"))
        
        val email = user.email
            ?: return Result.error(AppError.AuthError.Other("User email not available"))
        
        return try {
            // Re-authenticate user
            val credential = EmailAuthProvider.getCredential(email, password)
            user.reauthenticate(credential).await()
            
            // Update email
            user.updateEmail(newEmail).await()
            
            // Update email in Firestore
            firestore.collection(USERS_COLLECTION)
                .document(user.uid)
                .update("email", newEmail)
                .await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(TAG, "updateEmail error", e)
            val error = when (e.message) {
                "The password is invalid or the user does not have a password." -> 
                    AppError.AuthError.InvalidCredentials("Password is incorrect", e)
                "The email address is already in use by another account." -> 
                    AppError.AuthError.EmailAlreadyInUse("Email already in use", e)
                "This operation is sensitive and requires recent authentication. Log in again before retrying this request." -> 
                    AppError.AuthError.SessionExpired("Please sign in again to update your email", e)
                else -> AppError.AuthError.Other("Email update error: ${e.message}", e)
            }
            Result.error(error)
        }
    }

    override suspend fun deleteAccount(password: String): Result<Unit> {
        val user = auth.currentUser
            ?: return Result.error(AppError.AuthError.NotAuthenticated("Not logged in"))
        
        val email = user.email
            ?: return Result.error(AppError.AuthError.Other("User email not available"))
        
        return try {
            // Re-authenticate user
            val credential = EmailAuthProvider.getCredential(email, password)
            user.reauthenticate(credential).await()
            
            // Delete user data from Firestore
            firestore.collection(USERS_COLLECTION)
                .document(user.uid)
                .delete()
                .await()
            
            // Delete user account
            user.delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(TAG, "deleteAccount error", e)
            val error = when (e.message) {
                "The password is invalid or the user does not have a password." -> 
                    AppError.AuthError.InvalidCredentials("Password is incorrect", e)
                "This operation is sensitive and requires recent authentication. Log in again before retrying this request." -> 
                    AppError.AuthError.SessionExpired("Please sign in again to delete your account", e)
                else -> AppError.AuthError.Other("Account deletion error: ${e.message}", e)
            }
            Result.error(error)
        }
    }

    override fun getAuthStateFlow(): Flow<FirebaseUser?> = callbackFlow {
        val authStateListener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser)
        }
        
        auth.addAuthStateListener(authStateListener)
        
        // Initial value
        trySend(auth.currentUser)
        
        awaitClose {
            auth.removeAuthStateListener(authStateListener)
        }
    }

    override suspend fun getCurrentUserProfile(): Result<User> {
        val userId = getCurrentUserId()
            ?: return Result.error(AppError.AuthError.NotAuthenticated("Not logged in"))
        
        return try {
            val documentSnapshot = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .get()
                .await()
            
            if (documentSnapshot.exists()) {
                val user = documentSnapshot.toObject(User::class.java)
                    ?: return Result.error(AppError.DataError.InvalidData("Failed to parse user data"))
                Result.success(user)
            } else {
                Result.error(AppError.DataError.NotFound("User profile not found"))
            }
        } catch (e: Exception) {
            Timber.e(TAG, "getCurrentUserProfile error", e)
            Result.error(AppError.DataError.Other("Failed to fetch user profile: ${e.message}", e))
        }
    }

    override fun getCurrentUserProfileFlow(): Flow<Result<User>> = flow {
        val userId = getCurrentUserId()
            ?: throw Exception("Not logged in")
        
        emit(Result.loading())
        
        val documentSnapshot = firestore.collection(USERS_COLLECTION)
            .document(userId)
            .get()
            .await()
        
        if (documentSnapshot.exists()) {
            val user = documentSnapshot.toObject(User::class.java)
                ?: throw Exception("Failed to parse user data")
            emit(Result.success(user))
        } else {
            throw Exception("User profile not found")
        }
    }.catch { e ->
        Timber.e(TAG, "getCurrentUserProfileFlow error", e)
        val error = when (e.message) {
            "Not logged in" -> AppError.AuthError.NotAuthenticated("Not logged in")
            "User profile not found" -> AppError.DataError.NotFound("User profile not found")
            "Failed to parse user data" -> AppError.DataError.InvalidData("Failed to parse user data")
            else -> AppError.DataError.Other("Failed to fetch user profile: ${e.message}", e)
        }
        emit(Result.error(error))
    }
}