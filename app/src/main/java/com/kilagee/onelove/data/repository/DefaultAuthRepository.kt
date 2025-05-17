package com.kilagee.onelove.data.repository

import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.kilagee.onelove.data.model.User
import com.kilagee.onelove.data.model.VerificationLevel
import com.kilagee.onelove.domain.repository.AuthRepository
import com.kilagee.onelove.domain.util.Result
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * Implementation of [AuthRepository] that uses Firebase Authentication and Firestore
 */
@Singleton
class DefaultAuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    @Named("IoDispatcher") private val ioDispatcher: CoroutineDispatcher
) : AuthRepository {

    companion object {
        private const val USERS_COLLECTION = "users"
    }
    
    /**
     * Get the current authenticated user as a Flow
     */
    override fun getCurrentUser(): Flow<Result<FirebaseUser?>> = callbackFlow {
        val authStateListener = FirebaseAuth.AuthStateListener { auth ->
            trySend(Result.success(auth.currentUser))
        }
        
        auth.addAuthStateListener(authStateListener)
        
        awaitClose {
            auth.removeAuthStateListener(authStateListener)
        }
    }.catch { e ->
        Timber.e(e, "Error getting current user")
        emit(Result.error(e.message, e))
    }.flowOn(ioDispatcher)
    
    /**
     * Get the current user's profile data from Firestore
     */
    override fun getCurrentUserProfile(): Flow<Result<User?>> = callbackFlow {
        val currentUser = auth.currentUser
        
        if (currentUser == null) {
            trySend(Result.success(null))
            close()
            return@callbackFlow
        }
        
        val registration = firestore.collection(USERS_COLLECTION)
            .document(currentUser.uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Timber.e(error, "Error getting user profile")
                    trySend(Result.error(error.message, error))
                    return@addSnapshotListener
                }
                
                if (snapshot != null && snapshot.exists()) {
                    try {
                        val user = snapshot.toObject(User::class.java)
                        trySend(Result.success(user))
                    } catch (e: Exception) {
                        Timber.e(e, "Error converting snapshot to User")
                        trySend(Result.error(e.message, e))
                    }
                } else {
                    trySend(Result.success(null))
                }
            }
        
        awaitClose {
            registration.remove()
        }
    }.catch { e ->
        Timber.e(e, "Error getting user profile")
        emit(Result.error(e.message, e))
    }.flowOn(ioDispatcher)
    
    /**
     * Sign in with email and password
     */
    override suspend fun signInWithEmailAndPassword(
        email: String,
        password: String
    ): Result<FirebaseUser> = withContext(ioDispatcher) {
        try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            Result.success(result.user!!)
        } catch (e: FirebaseAuthInvalidUserException) {
            Timber.e(e, "User not found")
            Result.error("Account doesn't exist. Please check your email or create a new account.")
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            Timber.e(e, "Invalid credentials")
            Result.error("Invalid email or password. Please try again.")
        } catch (e: Exception) {
            Timber.e(e, "Error signing in")
            Result.error(e.message ?: "An unknown error occurred. Please try again.", e)
        }
    }
    
    /**
     * Register a new account with email and password
     */
    override suspend fun registerWithEmailAndPassword(
        email: String,
        password: String,
        name: String
    ): Result<FirebaseUser> = withContext(ioDispatcher) {
        try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user!!
            
            // Update display name
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(name)
                .build()
            
            user.updateProfile(profileUpdates).await()
            
            // Send email verification
            user.sendEmailVerification().await()
            
            // Create user document in Firestore
            val userModel = User(
                id = user.uid,
                email = email,
                name = name,
                verificationLevel = VerificationLevel.UNVERIFIED
            )
            
            firestore.collection(USERS_COLLECTION)
                .document(user.uid)
                .set(userModel, SetOptions.merge())
                .await()
            
            Result.success(user)
        } catch (e: FirebaseAuthWeakPasswordException) {
            Timber.e(e, "Weak password")
            Result.error("Password is too weak. It must be at least 6 characters.")
        } catch (e: FirebaseAuthUserCollisionException) {
            Timber.e(e, "Email already in use")
            Result.error("Email is already in use. Please use a different email or try logging in.")
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            Timber.e(e, "Invalid email")
            Result.error("Invalid email format. Please enter a valid email address.")
        } catch (e: Exception) {
            Timber.e(e, "Error registering")
            Result.error(e.message ?: "An unknown error occurred. Please try again.", e)
        }
    }
    
    /**
     * Send a password reset email
     */
    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> = withContext(ioDispatcher) {
        try {
            auth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: FirebaseAuthInvalidUserException) {
            Timber.e(e, "No user found with this email")
            Result.error("No account found with this email address.")
        } catch (e: Exception) {
            Timber.e(e, "Error sending password reset")
            Result.error(e.message ?: "Failed to send password reset email. Please try again.", e)
        }
    }
    
    /**
     * Sign out the current user
     */
    override suspend fun signOut(): Result<Unit> = withContext(ioDispatcher) {
        try {
            auth.signOut()
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error signing out")
            Result.error(e.message ?: "Failed to sign out. Please try again.", e)
        }
    }
    
    /**
     * Update the user's profile information in Firestore
     */
    override suspend fun updateUserProfile(user: User): Result<User> = withContext(ioDispatcher) {
        try {
            val currentUser = auth.currentUser
                ?: return@withContext Result.error("User not authenticated")
            
            // Update display name if it has changed
            if (user.name != currentUser.displayName) {
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(user.name)
                    .build()
                
                currentUser.updateProfile(profileUpdates).await()
            }
            
            // Update Firestore document
            firestore.collection(USERS_COLLECTION)
                .document(currentUser.uid)
                .set(user.copy(updatedAt = com.google.firebase.Timestamp.now()), SetOptions.merge())
                .await()
            
            Result.success(user)
        } catch (e: Exception) {
            Timber.e(e, "Error updating profile")
            Result.error(e.message ?: "Failed to update profile. Please try again.", e)
        }
    }
    
    /**
     * Delete the current user's account
     */
    override suspend fun deleteAccount(): Result<Unit> = withContext(ioDispatcher) {
        try {
            val currentUser = auth.currentUser
                ?: return@withContext Result.error("User not authenticated")
            
            // Delete Firestore document first
            firestore.collection(USERS_COLLECTION)
                .document(currentUser.uid)
                .delete()
                .await()
            
            // Then delete the authentication account
            currentUser.delete().await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error deleting account")
            Result.error(e.message ?: "Failed to delete account. Please try again.", e)
        }
    }
    
    /**
     * Send email verification to the current user
     */
    override suspend fun sendEmailVerification(): Result<Unit> = withContext(ioDispatcher) {
        try {
            val currentUser = auth.currentUser
                ?: return@withContext Result.error("User not authenticated")
            
            currentUser.sendEmailVerification().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error sending email verification")
            Result.error(e.message ?: "Failed to send verification email. Please try again.", e)
        }
    }
    
    /**
     * Verify the user's phone number
     */
    override suspend fun verifyPhoneNumber(
        phoneNumber: String,
        verificationCode: String
    ): Result<Unit> = withContext(ioDispatcher) {
        try {
            val currentUser = auth.currentUser
                ?: return@withContext Result.error("User not authenticated")
            
            // This is a simplified version - in a real app you would need to handle the SMS code sending process
            val credential = PhoneAuthProvider.getCredential("verificationId", verificationCode)
            currentUser.updatePhoneNumber(credential).await()
            
            // Update user record in Firestore
            firestore.collection(USERS_COLLECTION)
                .document(currentUser.uid)
                .update("phone", phoneNumber)
                .await()
                
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error verifying phone number")
            Result.error(e.message ?: "Failed to verify phone number. Please try again.", e)
        }
    }
    
    /**
     * Link an anonymous account with email and password
     */
    override suspend fun linkAnonymousAccountWithEmailAndPassword(
        email: String,
        password: String
    ): Result<FirebaseUser> = withContext(ioDispatcher) {
        try {
            val currentUser = auth.currentUser
                ?: return@withContext Result.error("User not authenticated")
            
            val credential = EmailAuthProvider.getCredential(email, password)
            val authResult = currentUser.linkWithCredential(credential).await()
            
            // Update email in Firestore
            firestore.collection(USERS_COLLECTION)
                .document(currentUser.uid)
                .update("email", email)
                .await()
                
            Result.success(authResult.user!!)
        } catch (e: FirebaseAuthUserCollisionException) {
            Timber.e(e, "Email already in use")
            Result.error("Email is already in use. Please use a different email.")
        } catch (e: Exception) {
            Timber.e(e, "Error linking account")
            Result.error(e.message ?: "Failed to link account. Please try again.", e)
        }
    }
    
    /**
     * Check if a user is already signed in
     */
    override fun isUserSignedIn(): Boolean {
        return auth.currentUser != null
    }
    
    /**
     * Check if the current user's email is verified
     */
    override fun isEmailVerified(): Boolean? {
        return auth.currentUser?.isEmailVerified
    }
    
    /**
     * Reauthenticate the current user with their password
     */
    override suspend fun reauthenticateWithPassword(password: String): Result<Unit> = withContext(ioDispatcher) {
        try {
            val currentUser = auth.currentUser
                ?: return@withContext Result.error("User not authenticated")
            
            val email = currentUser.email
                ?: return@withContext Result.error("User has no email address")
                
            val credential = EmailAuthProvider.getCredential(email, password)
            currentUser.reauthenticate(credential).await()
            
            Result.success(Unit)
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            Timber.e(e, "Invalid credentials")
            Result.error("Incorrect password. Please try again.")
        } catch (e: Exception) {
            Timber.e(e, "Error reauthenticating")
            Result.error(e.message ?: "Failed to verify identity. Please try again.", e)
        }
    }
    
    /**
     * Update the user's password
     */
    override suspend fun updatePassword(newPassword: String): Result<Unit> = withContext(ioDispatcher) {
        try {
            val currentUser = auth.currentUser
                ?: return@withContext Result.error("User not authenticated")
                
            currentUser.updatePassword(newPassword).await()
            Result.success(Unit)
        } catch (e: FirebaseAuthWeakPasswordException) {
            Timber.e(e, "Weak password")
            Result.error("Password is too weak. It must be at least 6 characters.")
        } catch (e: Exception) {
            Timber.e(e, "Error updating password")
            Result.error(e.message ?: "Failed to update password. Please try again.", e)
        }
    }
    
    /**
     * Update the user's email address
     */
    override suspend fun updateEmail(newEmail: String): Result<Unit> = withContext(ioDispatcher) {
        try {
            val currentUser = auth.currentUser
                ?: return@withContext Result.error("User not authenticated")
                
            currentUser.updateEmail(newEmail).await()
            
            // Update Firestore document
            firestore.collection(USERS_COLLECTION)
                .document(currentUser.uid)
                .update("email", newEmail)
                .await()
                
            // Send verification email for new email
            currentUser.sendEmailVerification().await()
            
            Result.success(Unit)
        } catch (e: FirebaseAuthUserCollisionException) {
            Timber.e(e, "Email already in use")
            Result.error("Email is already in use. Please use a different email.")
        } catch (e: Exception) {
            Timber.e(e, "Error updating email")
            Result.error(e.message ?: "Failed to update email. Please try again.", e)
        }
    }
}