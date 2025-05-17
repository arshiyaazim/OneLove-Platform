package com.kilagee.onelove.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.kilagee.onelove.data.database.dao.UserDao
import com.kilagee.onelove.data.model.User
import com.kilagee.onelove.data.model.UserPreferences
import com.kilagee.onelove.domain.matching.MatchEngine
import com.kilagee.onelove.domain.model.Resource
import com.kilagee.onelove.domain.repository.MatchRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseMatchRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val userDao: UserDao
) : MatchRepository {
    
    private val usersCollection = firestore.collection("users")
    private val preferencesCollection = firestore.collection("user_preferences")
    private val matchEngine = MatchEngine()
    
    override fun getPotentialMatches(
        minMatchPercentage: Int,
        limit: Int
    ): Flow<Resource<List<MatchEngine.MatchResult>>> = flow {
        emit(Resource.Loading)
        
        try {
            val currentUserId = auth.currentUser?.uid ?: run {
                emit(Resource.error("User not authenticated"))
                return@flow
            }
            
            // 1. Get current user with preferences
            val currentUser = getUserWithPreferences(currentUserId) ?: run {
                emit(Resource.error("Failed to get current user data"))
                return@flow
            }
            
            // 2. Get all potential matches based on basic filters
            val potentialMatchesQuery = usersCollection
                // Exclude current user
                .whereNotEqualTo("id", currentUserId)
            
            // Apply gender filter if available
            val genderPreference = currentUser.preferences?.genderPreference
            if (genderPreference != null && genderPreference != "Any") {
                potentialMatchesQuery.whereEqualTo("gender", genderPreference)
            }
            
            // Get users from Firestore
            val potentialMatchesSnapshot = potentialMatchesQuery.get().await()
            val potentialMatches = potentialMatchesSnapshot.documents.mapNotNull { document ->
                val user = document.toObject(User::class.java)
                user?.copy(id = document.id)
            }
            
            // 3. Get preferences for all potential matches
            val potentialMatchesWithPreferences = potentialMatches.map { user ->
                val preferences = getUserPreferences(user.id)
                user.copy(preferences = preferences)
            }
            
            // 4. Calculate match percentages and filter
            val matchResults = matchEngine.findMatches(
                currentUser = currentUser,
                potentialMatches = potentialMatchesWithPreferences,
                minMatchPercentage = minMatchPercentage
            ).take(limit)
            
            emit(Resource.success(matchResults))
        } catch (e: Exception) {
            emit(Resource.error("Failed to get potential matches: ${e.message}"))
        }
    }
    
    override fun likeUser(userId: String): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading)
        
        try {
            val currentUserId = auth.currentUser?.uid ?: run {
                emit(Resource.error("User not authenticated"))
                return@flow
            }
            
            // Get current user's liked list
            val currentUserDoc = usersCollection.document(currentUserId).get().await()
            val likedUserIds = currentUserDoc.get("liked_user_ids") as? List<String> ?: emptyList()
            
            // Add userId to liked list if not already there
            if (userId !in likedUserIds) {
                val updatedLikedUserIds = likedUserIds + userId
                usersCollection.document(currentUserId)
                    .update("liked_user_ids", updatedLikedUserIds)
                    .await()
            }
            
            // Check if the other user has already liked the current user (mutual like = match)
            val otherUserDoc = usersCollection.document(userId).get().await()
            val otherUserLikedIds = otherUserDoc.get("liked_user_ids") as? List<String> ?: emptyList()
            
            val isMatch = currentUserId in otherUserLikedIds
            
            // If it's a match, update both users' matched list
            if (isMatch) {
                // Get current user's matched list
                val currentUserMatchedIds = currentUserDoc.get("matched_user_ids") as? List<String> ?: emptyList()
                val updatedCurrentUserMatchedIds = currentUserMatchedIds + userId
                
                // Get other user's matched list
                val otherUserMatchedIds = otherUserDoc.get("matched_user_ids") as? List<String> ?: emptyList()
                val updatedOtherUserMatchedIds = otherUserMatchedIds + currentUserId
                
                // Update both users
                usersCollection.document(currentUserId)
                    .update("matched_user_ids", updatedCurrentUserMatchedIds)
                    .await()
                
                usersCollection.document(userId)
                    .update("matched_user_ids", updatedOtherUserMatchedIds)
                    .await()
            }
            
            emit(Resource.success(isMatch))
        } catch (e: Exception) {
            emit(Resource.error("Failed to like user: ${e.message}"))
        }
    }
    
    override fun rejectUser(userId: String): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading)
        
        try {
            val currentUserId = auth.currentUser?.uid ?: run {
                emit(Resource.error("User not authenticated"))
                return@flow
            }
            
            // Get current user's rejected list
            val currentUserDoc = usersCollection.document(currentUserId).get().await()
            val rejectedUserIds = currentUserDoc.get("rejected_user_ids") as? List<String> ?: emptyList()
            
            // Add userId to rejected list if not already there
            if (userId !in rejectedUserIds) {
                val updatedRejectedUserIds = rejectedUserIds + userId
                usersCollection.document(currentUserId)
                    .update("rejected_user_ids", updatedRejectedUserIds)
                    .await()
            }
            
            emit(Resource.success(Unit))
        } catch (e: Exception) {
            emit(Resource.error("Failed to reject user: ${e.message}"))
        }
    }
    
    override fun getMatches(): Flow<Resource<List<User>>> = flow {
        emit(Resource.Loading)
        
        try {
            val currentUserId = auth.currentUser?.uid ?: run {
                emit(Resource.error("User not authenticated"))
                return@flow
            }
            
            // Get current user's matched list
            val currentUserDoc = usersCollection.document(currentUserId).get().await()
            val matchedUserIds = currentUserDoc.get("matched_user_ids") as? List<String> ?: emptyList()
            
            if (matchedUserIds.isEmpty()) {
                emit(Resource.success(emptyList()))
                return@flow
            }
            
            // Get all matched users
            val matchedUsers = matchedUserIds.mapNotNull { userId ->
                try {
                    val userDoc = usersCollection.document(userId).get().await()
                    val user = userDoc.toObject(User::class.java)
                    user?.copy(id = userDoc.id)
                } catch (e: Exception) {
                    null
                }
            }
            
            emit(Resource.success(matchedUsers))
        } catch (e: Exception) {
            emit(Resource.error("Failed to get matches: ${e.message}"))
        }
    }
    
    override fun unmatchUser(userId: String): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading)
        
        try {
            val currentUserId = auth.currentUser?.uid ?: run {
                emit(Resource.error("User not authenticated"))
                return@flow
            }
            
            // Get current user's matched list
            val currentUserDoc = usersCollection.document(currentUserId).get().await()
            val matchedUserIds = currentUserDoc.get("matched_user_ids") as? List<String> ?: emptyList()
            
            // Remove userId from matched list
            val updatedMatchedUserIds = matchedUserIds.filter { it != userId }
            usersCollection.document(currentUserId)
                .update("matched_user_ids", updatedMatchedUserIds)
                .await()
            
            // Also remove from liked list if present
            val likedUserIds = currentUserDoc.get("liked_user_ids") as? List<String> ?: emptyList()
            val updatedLikedUserIds = likedUserIds.filter { it != userId }
            usersCollection.document(currentUserId)
                .update("liked_user_ids", updatedLikedUserIds)
                .await()
            
            // Update other user's matched list too
            val otherUserDoc = usersCollection.document(userId).get().await()
            val otherUserMatchedIds = otherUserDoc.get("matched_user_ids") as? List<String> ?: emptyList()
            val updatedOtherUserMatchedIds = otherUserMatchedIds.filter { it != currentUserId }
            usersCollection.document(userId)
                .update("matched_user_ids", updatedOtherUserMatchedIds)
                .await()
            
            emit(Resource.success(Unit))
        } catch (e: Exception) {
            emit(Resource.error("Failed to unmatch user: ${e.message}"))
        }
    }
    
    // Helper method to get a user with their preferences
    private suspend fun getUserWithPreferences(userId: String): User? {
        try {
            val userDoc = usersCollection.document(userId).get().await()
            val user = userDoc.toObject(User::class.java)?.copy(id = userDoc.id) ?: return null
            
            // Get preferences
            val preferences = getUserPreferences(userId)
            
            return user.copy(preferences = preferences)
        } catch (e: Exception) {
            return null
        }
    }
    
    // Helper method to get user preferences
    private suspend fun getUserPreferences(userId: String): UserPreferences? {
        return try {
            val preferencesDoc = preferencesCollection.document(userId).get().await()
            if (preferencesDoc.exists()) {
                preferencesDoc.toObject(UserPreferences::class.java)
            } else {
                // Default preferences if none exist
                UserPreferences(userId = userId)
            }
        } catch (e: Exception) {
            // Default preferences on error
            UserPreferences(userId = userId)
        }
    }
}