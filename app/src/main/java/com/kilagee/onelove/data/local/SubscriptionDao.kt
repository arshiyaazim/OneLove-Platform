package com.kilagee.onelove.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.kilagee.onelove.data.model.SubscriptionStatus
import com.kilagee.onelove.data.model.UserSubscription
import kotlinx.coroutines.flow.Flow

/**
 * Data access object for UserSubscription
 */
@Dao
interface SubscriptionDao {
    
    /**
     * Insert a subscription
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubscription(subscription: UserSubscription)
    
    /**
     * Update a subscription
     */
    @Update
    suspend fun updateSubscription(subscription: UserSubscription)
    
    /**
     * Get subscription by ID
     */
    @Query("SELECT * FROM subscriptions WHERE id = :subscriptionId")
    suspend fun getSubscriptionById(subscriptionId: String): UserSubscription?
    
    /**
     * Get subscription by user ID
     */
    @Query("SELECT * FROM subscriptions WHERE userId = :userId ORDER BY endDate DESC LIMIT 1")
    suspend fun getSubscriptionByUserId(userId: String): UserSubscription?
    
    /**
     * Get subscription by user ID as Flow
     */
    @Query("SELECT * FROM subscriptions WHERE userId = :userId ORDER BY endDate DESC LIMIT 1")
    fun getSubscriptionByUserIdFlow(userId: String): Flow<UserSubscription?>
    
    /**
     * Get all subscriptions by user ID
     */
    @Query("SELECT * FROM subscriptions WHERE userId = :userId ORDER BY endDate DESC")
    suspend fun getAllSubscriptionsByUserId(userId: String): List<UserSubscription>
    
    /**
     * Get active subscription by user ID
     */
    @Query("SELECT * FROM subscriptions WHERE userId = :userId AND status = :status ORDER BY endDate DESC LIMIT 1")
    suspend fun getActiveSubscriptionByUserId(userId: String, status: SubscriptionStatus = SubscriptionStatus.ACTIVE): UserSubscription?
    
    /**
     * Get all subscriptions with status
     */
    @Query("SELECT * FROM subscriptions WHERE status = :status")
    suspend fun getSubscriptionsByStatus(status: SubscriptionStatus): List<UserSubscription>
    
    /**
     * Delete a subscription
     */
    @Query("DELETE FROM subscriptions WHERE id = :subscriptionId")
    suspend fun deleteSubscription(subscriptionId: String)
    
    /**
     * Delete all subscriptions for a user
     */
    @Query("DELETE FROM subscriptions WHERE userId = :userId")
    suspend fun deleteAllSubscriptionsForUser(userId: String)
    
    /**
     * Delete all expired subscriptions
     */
    @Query("DELETE FROM subscriptions WHERE endDate < :currentTime")
    suspend fun deleteExpiredSubscriptions(currentTime: Long)
}