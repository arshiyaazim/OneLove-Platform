package com.kilagee.onelove.util

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager class for handling premium feature access
 */
@Singleton
class PremiumAccessManager @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    
    companion object {
        private const val TAG = "PremiumAccessManager"
        private const val SUBSCRIPTIONS_COLLECTION = "subscriptions"
        private const val SUBSCRIPTION_TIERS_COLLECTION = "subscription_tiers"
    }
    
    /**
     * Check if the current user has access to a specific premium feature
     */
    fun checkFeatureAccess(feature: PremiumFeature): Flow<Boolean> = flow {
        try {
            val userId = auth.currentUser?.uid
            if (userId == null) {
                emit(false)
                return@flow
            }
            
            // Get current active subscription
            val subscriptionSnapshot = firestore
                .collection(SUBSCRIPTIONS_COLLECTION)
                .whereEqualTo("userId", userId)
                .whereEqualTo("status", "active")
                .get()
                .await()
            
            if (subscriptionSnapshot.isEmpty) {
                emit(false)
                return@flow
            }
            
            // Get the subscription document
            val subscription = subscriptionSnapshot.documents.first()
            val subscriptionTierId = subscription.getString("tierId") ?: ""
            
            // Get the tier details to check feature access
            val tierSnapshot = firestore
                .collection(SUBSCRIPTION_TIERS_COLLECTION)
                .document(subscriptionTierId)
                .get()
                .await()
            
            if (!tierSnapshot.exists()) {
                emit(false)
                return@flow
            }
            
            // Get features included in this tier
            val features = tierSnapshot.get("features") as? List<String> ?: emptyList()
            
            // Check if the requested feature is included
            val hasAccess = features.contains(feature.name)
            emit(hasAccess)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error checking feature access: ${e.message}", e)
            emit(false)
        }
    }
    
    /**
     * Check if a user has any active premium subscription
     */
    fun hasAnySubscription(): Flow<Boolean> = flow {
        try {
            val userId = auth.currentUser?.uid
            if (userId == null) {
                emit(false)
                return@flow
            }
            
            // Check for any active subscription
            val subscriptionSnapshot = firestore
                .collection(SUBSCRIPTIONS_COLLECTION)
                .whereEqualTo("userId", userId)
                .whereEqualTo("status", "active")
                .get()
                .await()
            
            emit(!subscriptionSnapshot.isEmpty)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error checking subscription status: ${e.message}", e)
            emit(false)
        }
    }
    
    /**
     * Get the name of the current subscription tier
     */
    fun getCurrentTierName(): Flow<String> = flow {
        try {
            val userId = auth.currentUser?.uid
            if (userId == null) {
                emit("Free")
                return@flow
            }
            
            // Get current active subscription
            val subscriptionSnapshot = firestore
                .collection(SUBSCRIPTIONS_COLLECTION)
                .whereEqualTo("userId", userId)
                .whereEqualTo("status", "active")
                .get()
                .await()
            
            if (subscriptionSnapshot.isEmpty) {
                emit("Free")
                return@flow
            }
            
            // Get the subscription document
            val subscription = subscriptionSnapshot.documents.first()
            val subscriptionTierId = subscription.getString("tierId") ?: ""
            
            // Get the tier details
            val tierSnapshot = firestore
                .collection(SUBSCRIPTION_TIERS_COLLECTION)
                .document(subscriptionTierId)
                .get()
                .await()
            
            if (!tierSnapshot.exists()) {
                emit("Free")
                return@flow
            }
            
            val tierName = tierSnapshot.getString("name") ?: "Free"
            emit(tierName)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error getting subscription tier: ${e.message}", e)
            emit("Free")
        }
    }
    
    /**
     * Check if the current tier includes a specific feature
     */
    fun doesCurrentTierIncludeFeature(feature: PremiumFeature): Flow<Boolean> = checkFeatureAccess(feature)
}

// Extension function to make Firestore Task awaitable
suspend fun <T> com.google.android.gms.tasks.Task<T>.await(): T {
    return kotlinx.coroutines.tasks.await()
}