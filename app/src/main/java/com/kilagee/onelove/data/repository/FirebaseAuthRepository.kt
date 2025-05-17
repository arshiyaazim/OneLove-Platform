package com.kilagee.onelove.data.repository

import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.kilagee.onelove.data.model.User
import com.kilagee.onelove.domain.repository.AuthRepository
import com.kilagee.onelove.domain.util.Result
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firebase implementation of AuthRepository
 */
@Singleton
class FirebaseAuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : AuthRepository {
    
    override suspend fun signInWithEmailAndPassword(email: String, password: String): Result<User> {
        return try {
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            val user = authResult.user
            
            if (user != null) {
                val userData = getUserData(user.uid)
                Result.success(userData)
            } else {
                Result.error(message = "Sign in successful but user data is null")
            }
        } catch (e: Exception) {
            handleAuthError(e)
        }
    }
    
    override suspend fun signUpWithEmailAndPassword(
        email: String,
        password: String,
        displayName: String,
        phoneNumber: String?
    ): Result<User> {
        return try {
            // Create user in Firebase Auth
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user
            
            if (firebaseUser != null) {
                // Update display name
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(displayName)
                    .build()
                
                firebaseUser.updateProfile(profileUpdates).await()
                
                // Create user document in Firestore
                val user = User(
                    id = firebaseUser.uid,
                    email = email,
                    displayName = displayName,
                    phoneNumber = phoneNumber ?: ""
                )
                
                // Save user data to Firestore
                firestore.collection("users").document(user.id).set(user).await()
                
                Result.success(user)
            } else {
                Result.error(message = "Sign up successful but user data is null")
            }
        } catch (e: Exception) {
            handleAuthError(e)
        }
    }
    
    override suspend fun signInWithGoogle(idToken: String): Result<User> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = auth.signInWithCredential(credential).await()
            val firebaseUser = authResult.user
            
            if (firebaseUser != null) {
                // Check if the user already exists in Firestore
                val userDoc = firestore.collection("users").document(firebaseUser.uid).get().await()
                
                if (!userDoc.exists()) {
                    // Create new user document
                    val user = User(
                        id = firebaseUser.uid,
                        email = firebaseUser.email ?: "",
                        displayName = firebaseUser.displayName ?: "",
                        phoneNumber = firebaseUser.phoneNumber ?: "",
                        photoUrl = firebaseUser.photoUrl?.toString() ?: ""
                    )
                    
                    // Save user data to Firestore
                    firestore.collection("users").document(user.id).set(user).await()
                    
                    Result.success(user)
                } else {
                    // User exists, get user data
                    val userData = getUserData(firebaseUser.uid)
                    Result.success(userData)
                }
            } else {
                Result.error(message = "Sign in with Google successful but user data is null")
            }
        } catch (e: Exception) {
            handleAuthError(e)
        }
    }
    
    override suspend fun signInWithFacebook(accessToken: String): Result<User> {
        // Implementation similar to Google sign-in
        // Will need to be implemented with Facebook SDK
        return Result.error(message = "Facebook sign-in not implemented yet")
    }
    
    override suspend fun signInWithApple(idToken: String, nonce: String): Result<User> {
        // Implementation for Apple sign-in
        // Will need to be implemented with Apple Sign-In
        return Result.error(message = "Apple sign-in not implemented yet")
    }
    
    override suspend fun signInAnonymously(): Result<User> {
        return try {
            val authResult = auth.signInAnonymously().await()
            val firebaseUser = authResult.user
            
            if (firebaseUser != null) {
                // Create anonymous user in Firestore
                val user = User(
                    id = firebaseUser.uid,
                    displayName = "Anonymous User"
                )
                
                // Save user data to Firestore
                firestore.collection("users").document(user.id).set(user).await()
                
                Result.success(user)
            } else {
                Result.error(message = "Anonymous sign in successful but user data is null")
            }
        } catch (e: Exception) {
            handleAuthError(e)
        }
    }
    
    override suspend fun sendEmailVerification(): Result<Unit> {
        return try {
            val user = auth.currentUser
            if (user != null) {
                user.sendEmailVerification().await()
                Result.success(Unit)
            } else {
                Result.error(message = "No user is signed in")
            }
        } catch (e: Exception) {
            handleAuthError(e)
        }
    }
    
    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleAuthError(e)
        }
    }
    
    override suspend fun verifyPasswordResetCode(code: String): Result<String> {
        return try {
            val email = auth.verifyPasswordResetCode(code).await()
            Result.success(email)
        } catch (e: Exception) {
            handleAuthError(e)
        }
    }
    
    override suspend fun confirmPasswordReset(code: String, newPassword: String): Result<Unit> {
        return try {
            auth.confirmPasswordReset(code, newPassword).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleAuthError(e)
        }
    }
    
    override suspend fun sendPhoneVerificationCode(phoneNumber: String): Result<Unit> {
        // Implementation requires FirebaseAuth PhoneAuthProvider
        // Will need additional setup
        return Result.error(message = "Phone verification not implemented yet")
    }
    
    override suspend fun verifyPhoneWithCode(verificationId: String, code: String): Result<Unit> {
        // Implementation requires FirebaseAuth PhoneAuthProvider
        // Will need additional setup
        return Result.error(message = "Phone verification not implemented yet")
    }
    
    override suspend fun reauthenticate(password: String): Result<Unit> {
        return try {
            val user = auth.currentUser
            if (user != null && user.email != null) {
                val credential = EmailAuthProvider.getCredential(user.email!!, password)
                user.reauthenticate(credential).await()
                Result.success(Unit)
            } else {
                Result.error(message = "No user is signed in or email is null")
            }
        } catch (e: Exception) {
            handleAuthError(e)
        }
    }
    
    override suspend fun changePassword(oldPassword: String, newPassword: String): Result<Unit> {
        return try {
            val user = auth.currentUser
            if (user != null && user.email != null) {
                // First reauthenticate
                val credential = EmailAuthProvider.getCredential(user.email!!, oldPassword)
                user.reauthenticate(credential).await()
                
                // Then change password
                user.updatePassword(newPassword).await()
                Result.success(Unit)
            } else {
                Result.error(message = "No user is signed in or email is null")
            }
        } catch (e: Exception) {
            handleAuthError(e)
        }
    }
    
    override suspend fun linkWithEmailAndPassword(email: String, password: String): Result<User> {
        return try {
            val user = auth.currentUser
            if (user != null) {
                val credential = EmailAuthProvider.getCredential(email, password)
                val authResult = user.linkWithCredential(credential).await()
                val firebaseUser = authResult.user
                
                if (firebaseUser != null) {
                    val userData = getUserData(firebaseUser.uid)
                    Result.success(userData)
                } else {
                    Result.error(message = "Link successful but user data is null")
                }
            } else {
                Result.error(message = "No user is signed in")
            }
        } catch (e: Exception) {
            handleAuthError(e)
        }
    }
    
    override suspend fun linkWithGoogle(idToken: String): Result<User> {
        return try {
            val user = auth.currentUser
            if (user != null) {
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = user.linkWithCredential(credential).await()
                val firebaseUser = authResult.user
                
                if (firebaseUser != null) {
                    val userData = getUserData(firebaseUser.uid)
                    Result.success(userData)
                } else {
                    Result.error(message = "Link successful but user data is null")
                }
            } else {
                Result.error(message = "No user is signed in")
            }
        } catch (e: Exception) {
            handleAuthError(e)
        }
    }
    
    override suspend fun linkWithFacebook(accessToken: String): Result<User> {
        // Implementation similar to linkWithGoogle
        // Will need to be implemented with Facebook SDK
        return Result.error(message = "Facebook linking not implemented yet")
    }
    
    override suspend fun linkWithApple(idToken: String, nonce: String): Result<User> {
        // Implementation for Apple linking
        // Will need to be implemented with Apple Sign-In
        return Result.error(message = "Apple linking not implemented yet")
    }
    
    override suspend fun linkWithPhoneNumber(verificationId: String, code: String): Result<User> {
        // Implementation requires FirebaseAuth PhoneAuthProvider
        // Will need additional setup
        return Result.error(message = "Phone linking not implemented yet")
    }
    
    override suspend fun signOut(): Result<Unit> {
        return try {
            auth.signOut()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.error(exception = e, message = "Error signing out: ${e.message}")
        }
    }
    
    override suspend fun deleteAccount(): Result<Unit> {
        return try {
            val user = auth.currentUser
            if (user != null) {
                // Delete user data from Firestore
                firestore.collection("users").document(user.uid).delete().await()
                
                // Delete Firebase Auth user
                user.delete().await()
                Result.success(Unit)
            } else {
                Result.error(message = "No user is signed in")
            }
        } catch (e: Exception) {
            handleAuthError(e)
        }
    }
    
    override fun isUserLoggedIn(): Boolean {
        return auth.currentUser != null
    }
    
    override suspend fun getCurrentUser(): Result<User?> {
        val firebaseUser = auth.currentUser
        return if (firebaseUser != null) {
            try {
                val userData = getUserData(firebaseUser.uid)
                Result.success(userData)
            } catch (e: Exception) {
                Result.error(exception = e, message = "Error fetching user data: ${e.message}")
            }
        } else {
            Result.success(null)
        }
    }
    
    override fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }
    
    override fun getCurrentUserFlow(): Flow<Result<User?>> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            val firebaseUser = auth.currentUser
            if (firebaseUser != null) {
                try {
                    // Use a coroutine to get user data
                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                        try {
                            val userData = getUserData(firebaseUser.uid)
                            trySend(Result.success(userData))
                        } catch (e: Exception) {
                            trySend(Result.error(exception = e, message = "Error fetching user data: ${e.message}"))
                        }
                    }
                } catch (e: Exception) {
                    trySend(Result.error(exception = e, message = "Error fetching user data: ${e.message}"))
                }
            } else {
                trySend(Result.success(null))
            }
        }
        
        auth.addAuthStateListener(listener)
        
        awaitClose {
            auth.removeAuthStateListener(listener)
        }
    }
    
    override suspend fun isEmailVerified(): Result<Boolean> {
        val user = auth.currentUser
        return if (user != null) {
            Result.success(user.isEmailVerified)
        } else {
            Result.error(message = "No user is signed in")
        }
    }
    
    override suspend fun isPhoneVerified(): Result<Boolean> {
        val user = auth.currentUser
        return if (user != null) {
            // In Firebase, if phone number exists, it's verified
            Result.success(user.phoneNumber != null)
        } else {
            Result.error(message = "No user is signed in")
        }
    }
    
    override suspend fun refreshToken(): Result<String> {
        return try {
            val user = auth.currentUser
            if (user != null) {
                val tokenResult = user.getIdToken(true).await()
                Result.success(tokenResult.token ?: "")
            } else {
                Result.error(message = "No user is signed in")
            }
        } catch (e: Exception) {
            Result.error(exception = e, message = "Error refreshing token: ${e.message}")
        }
    }
    
    override suspend fun getIdToken(forceRefresh: Boolean): Result<String> {
        return try {
            val user = auth.currentUser
            if (user != null) {
                val tokenResult = user.getIdToken(forceRefresh).await()
                Result.success(tokenResult.token ?: "")
            } else {
                Result.error(message = "No user is signed in")
            }
        } catch (e: Exception) {
            Result.error(exception = e, message = "Error getting token: ${e.message}")
        }
    }
    
    /**
     * Helper method to get user data from Firestore
     */
    private suspend fun getUserData(userId: String): User {
        val documentSnapshot = firestore.collection("users").document(userId).get().await()
        return if (documentSnapshot.exists()) {
            documentSnapshot.toObject(User::class.java) ?: User(id = userId)
        } else {
            // Create a basic user if not found
            val user = User(id = userId)
            firestore.collection("users").document(userId).set(user).await()
            user
        }
    }
    
    /**
     * Helper method to handle auth errors
     */
    private fun handleAuthError(e: Exception): Result<Nothing> {
        return when (e) {
            is FirebaseAuthException -> {
                when (e.errorCode) {
                    "ERROR_INVALID_EMAIL" -> Result.error(exception = e, message = "Invalid email format")
                    "ERROR_WRONG_PASSWORD" -> Result.error(exception = e, message = "Incorrect password")
                    "ERROR_USER_NOT_FOUND" -> Result.error(exception = e, message = "User not found")
                    "ERROR_USER_DISABLED" -> Result.error(exception = e, message = "User account has been disabled")
                    "ERROR_TOO_MANY_REQUESTS" -> Result.error(exception = e, message = "Too many attempts. Try again later")
                    "ERROR_OPERATION_NOT_ALLOWED" -> Result.error(exception = e, message = "Operation not allowed")
                    "ERROR_EMAIL_ALREADY_IN_USE" -> Result.error(exception = e, message = "Email already in use")
                    "ERROR_WEAK_PASSWORD" -> Result.error(exception = e, message = "Password is too weak")
                    "ERROR_INVALID_CREDENTIAL" -> Result.error(exception = e, message = "Invalid credential")
                    "ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL" -> Result.error(exception = e, message = "Account exists with different credential")
                    "ERROR_REQUIRES_RECENT_LOGIN" -> Result.error(exception = e, message = "This operation requires recent authentication. Log in again before retrying")
                    else -> Result.error(exception = e, message = e.message ?: "Authentication error")
                }
            }
            else -> Result.error(exception = e, message = e.message ?: "Unknown error")
        }
    }
}