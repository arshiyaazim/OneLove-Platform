package com.kilagee.onelove.util

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Transaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages user points for the app's reward system
 */
@Singleton
class PointsManager @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    /**
     * Get current user points
     */
    fun getUserPoints(): Flow<Int> = callbackFlow {
        val userId = auth.currentUser?.uid ?: run {
            trySend(0)
            return@callbackFlow
        }
        
        val listenerRegistration = firestore.collection("users")
            .document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // If there is an error, send 0 points
                    trySend(0)
                    return@addSnapshotListener
                }
                
                if (snapshot != null && snapshot.exists()) {
                    val points = snapshot.getLong("points")?.toInt() ?: 0
                    trySend(points)
                } else {
                    trySend(0)
                }
            }
        
        kotlinx.coroutines.channels.awaitClose { listenerRegistration.remove() }
    }
    
    /**
     * Add points to user's account
     */
    suspend fun addPoints(points: Int, reason: PointsReason): Boolean {
        val userId = auth.currentUser?.uid ?: return false
        
        return try {
            firestore.runTransaction { transaction ->
                val userRef = firestore.collection("users").document(userId)
                val userSnapshot = transaction.get(userRef)
                
                if (userSnapshot.exists()) {
                    val currentPoints = userSnapshot.getLong("points")?.toInt() ?: 0
                    val newPoints = currentPoints + points
                    
                    transaction.update(userRef, "points", newPoints)
                    
                    // Record the points transaction
                    recordPointsTransaction(transaction, userId, points, reason)
                } else {
                    transaction.set(
                        userRef,
                        hashMapOf(
                            "points" to points,
                            "userId" to userId
                        )
                    )
                    
                    // Record the points transaction
                    recordPointsTransaction(transaction, userId, points, reason)
                }
            }.await()
            
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Deduct points from user's account
     */
    suspend fun deductPoints(points: Int, reason: PointsReason): Boolean {
        val userId = auth.currentUser?.uid ?: return false
        
        return try {
            var success = false
            
            firestore.runTransaction { transaction ->
                val userRef = firestore.collection("users").document(userId)
                val userSnapshot = transaction.get(userRef)
                
                if (userSnapshot.exists()) {
                    val currentPoints = userSnapshot.getLong("points")?.toInt() ?: 0
                    
                    if (currentPoints >= points) {
                        val newPoints = currentPoints - points
                        transaction.update(userRef, "points", newPoints)
                        
                        // Record the points transaction
                        recordPointsTransaction(transaction, userId, -points, reason)
                        
                        success = true
                    } else {
                        success = false
                    }
                } else {
                    success = false
                }
            }.await()
            
            success
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Check if user has enough points
     */
    suspend fun hasEnoughPoints(points: Int): Boolean {
        val userId = auth.currentUser?.uid ?: return false
        
        return try {
            val userSnapshot = firestore.collection("users")
                .document(userId)
                .get()
                .await()
            
            if (userSnapshot.exists()) {
                val currentPoints = userSnapshot.getLong("points")?.toInt() ?: 0
                currentPoints >= points
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Record a points transaction in the database
     */
    private fun recordPointsTransaction(
        transaction: Transaction,
        userId: String,
        points: Int,
        reason: PointsReason
    ) {
        val pointsTransactionRef = firestore.collection("pointsTransactions").document()
        
        transaction.set(
            pointsTransactionRef,
            hashMapOf(
                "userId" to userId,
                "points" to points,
                "reason" to reason.name,
                "timestamp" to com.google.firebase.Timestamp.now()
            )
        )
    }
    
    /**
     * Get the cost of an action in points
     */
    fun getActionCost(action: PointsAction): Int {
        return when (action) {
            PointsAction.PROFILE_BOOST -> 100
            PointsAction.SEND_SPECIAL_OFFER -> 50
            PointsAction.UNLOCK_CONVERSATION -> 200
            PointsAction.SEND_PRIORITY_MESSAGE -> 20
            PointsAction.VIEW_VISITORS -> 30
            PointsAction.UNDO_SWIPE -> 25
        }
    }
    
    /**
     * Get the reward for an action in points
     */
    fun getActionReward(action: PointsReward): Int {
        return when (action) {
            PointsReward.DAILY_LOGIN -> 10
            PointsReward.COMPLETE_PROFILE -> 50
            PointsReward.VERIFY_PROFILE -> 100
            PointsReward.RECEIVE_MATCH -> 15
            PointsReward.SUBSCRIPTION_RENEWAL -> 200
            PointsReward.INVITE_FRIEND -> 30
        }
    }
}

/**
 * Enum class for points actions (things that cost points)
 */
enum class PointsAction {
    PROFILE_BOOST,
    SEND_SPECIAL_OFFER,
    UNLOCK_CONVERSATION,
    SEND_PRIORITY_MESSAGE,
    VIEW_VISITORS,
    UNDO_SWIPE
}

/**
 * Enum class for points rewards (things that give points)
 */
enum class PointsReward {
    DAILY_LOGIN,
    COMPLETE_PROFILE,
    VERIFY_PROFILE,
    RECEIVE_MATCH,
    SUBSCRIPTION_RENEWAL,
    INVITE_FRIEND
}

/**
 * Enum class for points transaction reasons
 */
enum class PointsReason {
    PROFILE_BOOST,
    SEND_SPECIAL_OFFER,
    UNLOCK_CONVERSATION,
    SEND_PRIORITY_MESSAGE,
    VIEW_VISITORS,
    UNDO_SWIPE,
    DAILY_LOGIN,
    COMPLETE_PROFILE,
    VERIFY_PROFILE,
    RECEIVE_MATCH,
    SUBSCRIPTION_RENEWAL,
    INVITE_FRIEND,
    PURCHASE,
    REFUND,
    ADMIN_ADJUSTMENT
}