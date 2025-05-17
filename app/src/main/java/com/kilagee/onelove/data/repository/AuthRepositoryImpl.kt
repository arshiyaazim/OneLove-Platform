package com.kilagee.onelove.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.kilagee.onelove.data.local.UserDao
import com.kilagee.onelove.data.model.User
import com.kilagee.onelove.domain.repository.AuthRepository
import com.kilagee.onelove.domain.util.Result
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.util.Date
import javax.inject.Inject

/**
 * Auth repository implementation
 */
class AuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val userDao: UserDao
) : AuthRepository {
    
    private val usersCollection = firestore.collection("users")
    
    override fun getAuthState(): Flow<Boolean> = callbackFlow {
        val authStateListener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser != null)
        }
        auth.addAuthStateListener(authStateListener)
        awaitClose {
            auth.removeAuthStateListener(authStateListener)
        }
    }
    
    override fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }
    
    override fun getCurrentUser(): Flow<Result<User>> = flow {
        emit(Result.Loading)
        
        val userId = auth.currentUser?.uid
        if (userId == null) {
            emit(Result.Error("User not authenticated"))
            return@flow
        }
        
        try {
            // First try to get from local database
            val cachedUser = userDao.getUserById(userId)
            if (cachedUser != null) {
                emit(Result.Success(cachedUser))
            }
            
            // Then get from Firestore to ensure latest data
            val documentSnapshot = usersCollection.document(userId).get().await()
            if (documentSnapshot.exists()) {
                val userData = documentSnapshot.data
                if (userData != null) {
                    val user = mapFirestoreDataToUser(userData, userId)
                    // Cache the user in local database
                    userDao.insertUser(user)
                    emit(Result.Success(user))
                } else {
                    emit(Result.Error("User data is null"))
                }
            } else {
                emit(Result.Error("User document does not exist"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Error getting current user")
            emit(Result.Error("Failed to get user: ${e.message}"))
        }
    }.catch { e ->
        Timber.e(e, "Error in getCurrentUser flow")
        emit(Result.Error("Exception: ${e.message}"))
    }
    
    override suspend fun signInWithEmailPassword(email: String, password: String): Result<User> {
        return try {
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            val userId = authResult.user?.uid
            
            if (userId != null) {
                val documentSnapshot = usersCollection.document(userId).get().await()
                if (documentSnapshot.exists()) {
                    val userData = documentSnapshot.data
                    if (userData != null) {
                        val user = mapFirestoreDataToUser(userData, userId)
                        userDao.insertUser(user)
                        Result.Success(user)
                    } else {
                        Result.Error("User data is null")
                    }
                } else {
                    // User document doesn't exist, create one
                    val newUser = User(
                        id = userId,
                        name = authResult.user?.displayName ?: "User",
                        email = email,
                        createdAt = Date(),
                        updatedAt = Date()
                    )
                    usersCollection.document(userId).set(newUser).await()
                    userDao.insertUser(newUser)
                    Result.Success(newUser)
                }
            } else {
                Result.Error("Failed to get user ID after sign in")
            }
        } catch (e: Exception) {
            Timber.e(e, "Error signing in with email/password")
            Result.Error("Failed to sign in: ${e.message}")
        }
    }
    
    override suspend fun signUpWithEmailPassword(email: String, password: String, name: String): Result<User> {
        return try {
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val userId = authResult.user?.uid
            
            if (userId != null) {
                // Update display name
                auth.currentUser?.updateProfile(
                    com.google.firebase.auth.UserProfileChangeRequest.Builder()
                        .setDisplayName(name)
                        .build()
                )?.await()
                
                // Create user document in Firestore
                val newUser = User(
                    id = userId,
                    name = name,
                    email = email,
                    createdAt = Date(),
                    updatedAt = Date()
                )
                usersCollection.document(userId).set(newUser).await()
                userDao.insertUser(newUser)
                Result.Success(newUser)
            } else {
                Result.Error("Failed to get user ID after sign up")
            }
        } catch (e: Exception) {
            Timber.e(e, "Error signing up with email/password")
            Result.Error("Failed to sign up: ${e.message}")
        }
    }
    
    override suspend fun signInWithGoogle(idToken: String): Result<User> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = auth.signInWithCredential(credential).await()
            val userId = authResult.user?.uid
            
            if (userId != null) {
                val documentSnapshot = usersCollection.document(userId).get().await()
                if (documentSnapshot.exists()) {
                    val userData = documentSnapshot.data
                    if (userData != null) {
                        val user = mapFirestoreDataToUser(userData, userId)
                        userDao.insertUser(user)
                        Result.Success(user)
                    } else {
                        Result.Error("User data is null")
                    }
                } else {
                    // User document doesn't exist, create one
                    val newUser = User(
                        id = userId,
                        name = authResult.user?.displayName ?: "User",
                        email = authResult.user?.email ?: "",
                        profilePictureUrl = authResult.user?.photoUrl?.toString(),
                        createdAt = Date(),
                        updatedAt = Date()
                    )
                    usersCollection.document(userId).set(newUser).await()
                    userDao.insertUser(newUser)
                    Result.Success(newUser)
                }
            } else {
                Result.Error("Failed to get user ID after Google sign in")
            }
        } catch (e: Exception) {
            Timber.e(e, "Error signing in with Google")
            Result.Error("Failed to sign in with Google: ${e.message}")
        }
    }
    
    override suspend fun signOut(): Result<Unit> {
        return try {
            auth.signOut()
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error signing out")
            Result.Error("Failed to sign out: ${e.message}")
        }
    }
    
    override suspend fun resetPassword(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error resetting password")
            Result.Error("Failed to reset password: ${e.message}")
        }
    }
    
    override suspend fun updateUserProfile(user: User): Result<User> {
        return try {
            val userId = auth.currentUser?.uid
            if (userId == null) {
                Result.Error("User not authenticated")
            } else {
                // Update Firestore document
                val userWithUpdates = user.copy(updatedAt = Date())
                usersCollection.document(userId).set(userWithUpdates).await()
                
                // Update local database
                userDao.updateUser(userWithUpdates)
                
                Result.Success(userWithUpdates)
            }
        } catch (e: Exception) {
            Timber.e(e, "Error updating user profile")
            Result.Error("Failed to update profile: ${e.message}")
        }
    }
    
    override suspend fun updatePassword(oldPassword: String, newPassword: String): Result<Unit> {
        return try {
            val user = auth.currentUser
            if (user == null) {
                Result.Error("User not authenticated")
            } else {
                // Re-authenticate user
                val email = user.email
                if (email != null) {
                    val credential = com.google.firebase.auth.EmailAuthProvider.getCredential(email, oldPassword)
                    user.reauthenticate(credential).await()
                    
                    // Update password
                    user.updatePassword(newPassword).await()
                    
                    Result.Success(Unit)
                } else {
                    Result.Error("User email is null")
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error updating password")
            Result.Error("Failed to update password: ${e.message}")
        }
    }
    
    override suspend fun deleteAccount(): Result<Unit> {
        return try {
            val user = auth.currentUser
            if (user == null) {
                Result.Error("User not authenticated")
            } else {
                val userId = user.uid
                
                // Delete Firestore document
                usersCollection.document(userId).delete().await()
                
                // Delete user from local database
                userDao.getUserById(userId)?.let { userDao.deleteUser(it) }
                
                // Delete Firebase Auth account
                user.delete().await()
                
                Result.Success(Unit)
            }
        } catch (e: Exception) {
            Timber.e(e, "Error deleting account")
            Result.Error("Failed to delete account: ${e.message}")
        }
    }
    
    override fun getUserProfile(userId: String?): Flow<Result<User>> = flow {
        emit(Result.Loading)
        
        val targetUserId = userId ?: auth.currentUser?.uid
        if (targetUserId == null) {
            emit(Result.Error("User ID is null"))
            return@flow
        }
        
        try {
            // First try to get from local database
            val cachedUser = userDao.getUserById(targetUserId)
            if (cachedUser != null) {
                emit(Result.Success(cachedUser))
            }
            
            // Then get from Firestore to ensure latest data
            val documentSnapshot = usersCollection.document(targetUserId).get().await()
            if (documentSnapshot.exists()) {
                val userData = documentSnapshot.data
                if (userData != null) {
                    val user = mapFirestoreDataToUser(userData, targetUserId)
                    // Cache the user in local database
                    userDao.insertUser(user)
                    emit(Result.Success(user))
                } else {
                    emit(Result.Error("User data is null"))
                }
            } else {
                emit(Result.Error("User document does not exist"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Error getting user profile for ID: $targetUserId")
            emit(Result.Error("Failed to get user: ${e.message}"))
        }
    }.catch { e ->
        Timber.e(e, "Error in getUserProfile flow")
        emit(Result.Error("Exception: ${e.message}"))
    }
    
    /**
     * Map Firestore data to User model
     */
    private fun mapFirestoreDataToUser(data: Map<String, Any>, userId: String): User {
        return User(
            id = userId,
            name = data["name"] as? String ?: "",
            email = data["email"] as? String ?: "",
            profilePictureUrl = data["profilePictureUrl"] as? String,
            bio = data["bio"] as? String,
            age = (data["age"] as? Long)?.toInt(),
            gender = data["gender"] as? String,
            lookingFor = (data["lookingFor"] as? List<*>)?.map { it.toString() },
            interests = (data["interests"] as? List<*>)?.map { it.toString() },
            location = data["location"] as? String,
            latitude = data["latitude"] as? Double,
            longitude = data["longitude"] as? Double,
            isVerified = data["isVerified"] as? Boolean ?: false,
            verificationLevel = (data["verificationLevel"] as? Long)?.toInt() ?: 0,
            isPremium = data["isPremium"] as? Boolean ?: false,
            subscriptionType = data["subscriptionType"] as? String,
            subscriptionExpiryDate = (data["subscriptionExpiryDate"] as? com.google.firebase.Timestamp)?.toDate(),
            lastActive = (data["lastActive"] as? com.google.firebase.Timestamp)?.toDate(),
            isOnline = data["isOnline"] as? Boolean ?: false,
            points = (data["points"] as? Long)?.toInt() ?: 0,
            isProfileComplete = data["isProfileComplete"] as? Boolean ?: false,
            minAgePreference = (data["minAgePreference"] as? Long)?.toInt(),
            maxAgePreference = (data["maxAgePreference"] as? Long)?.toInt(),
            maxDistance = (data["maxDistance"] as? Long)?.toInt(),
            createdAt = (data["createdAt"] as? com.google.firebase.Timestamp)?.toDate(),
            updatedAt = (data["updatedAt"] as? com.google.firebase.Timestamp)?.toDate(),
            photos = (data["photos"] as? List<*>)?.map { it.toString() },
            isAdmin = data["isAdmin"] as? Boolean ?: false
        )
    }
}