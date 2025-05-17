package com.kilagee.onelove.data.database.dao

import androidx.room.*
import com.kilagee.onelove.data.model.Subscription
import com.kilagee.onelove.data.model.SubscriptionStatus
import com.kilagee.onelove.data.model.SubscriptionType
import kotlinx.coroutines.flow.Flow
import java.util.Date

/**
 * Data Access Object for Subscription entities
 */
@Dao
interface SubscriptionDao {
    
    /**
     * Insert a subscription
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubscription(subscription: Subscription)
    
    /**
     * Insert multiple subscriptions
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubscriptions(subscriptions: List<Subscription>)
    
    /**
     * Update subscription
     */
    @Update
    suspend fun updateSubscription(subscription: Subscription)
    
    /**
     * Get all subscriptions for a user
     */
    @Query("SELECT * FROM subscriptions WHERE userId = :userId ORDER BY createdAt DESC")
    fun getSubscriptionsForUser(userId: String): Flow<List<Subscription>>
    
    /**
     * Get active subscriptions for a user
     */
    @Query("SELECT * FROM subscriptions WHERE userId = :userId AND status = :status ORDER BY createdAt DESC")
    fun getSubscriptionsByStatus(userId: String, status: SubscriptionStatus): Flow<List<Subscription>>
    
    /**
     * Get subscription by ID
     */
    @Query("SELECT * FROM subscriptions WHERE id = :subscriptionId")
    suspend fun getSubscriptionById(subscriptionId: String): Subscription?
    
    /**
     * Get subscription by provider subscription ID
     */
    @Query("SELECT * FROM subscriptions WHERE providerSubscriptionId = :providerSubscriptionId")
    suspend fun getSubscriptionByProviderSubscriptionId(providerSubscriptionId: String): Subscription?
    
    /**
     * Get active subscriptions that need renewal (due within provided time)
     */
    @Query("SELECT * FROM subscriptions WHERE status = 'ACTIVE' AND autoRenew = 1 AND currentPeriodEnd < :beforeDate")
    suspend fun getSubscriptionsDueForRenewal(beforeDate: Date): List<Subscription>
    
    /**
     * Delete subscription
     */
    @Delete
    suspend fun deleteSubscription(subscription: Subscription)
    
    /**
     * Delete subscription by ID
     */
    @Query("DELETE FROM subscriptions WHERE id = :subscriptionId")
    suspend fun deleteSubscriptionById(subscriptionId: String)
    
    /**
     * Check if user has active subscription of specific type
     */
    @Query("SELECT COUNT(*) > 0 FROM subscriptions WHERE userId = :userId AND type = :type AND status = 'ACTIVE' AND currentPeriodEnd > :now")
    suspend fun hasActiveSubscriptionOfType(userId: String, type: SubscriptionType, now: Date): Boolean
    
    /**
     * Check if user has any active subscription
     */
    @Query("SELECT COUNT(*) > 0 FROM subscriptions WHERE userId = :userId AND status = 'ACTIVE' AND currentPeriodEnd > :now")
    suspend fun hasActiveSubscription(userId: String, now: Date): Boolean
    
    /**
     * Get current active subscription for user
     */
    @Query("SELECT * FROM subscriptions WHERE userId = :userId AND status = 'ACTIVE' AND currentPeriodEnd > :now ORDER BY type DESC LIMIT 1")
    suspend fun getCurrentActiveSubscription(userId: String, now: Date): Subscription?
}