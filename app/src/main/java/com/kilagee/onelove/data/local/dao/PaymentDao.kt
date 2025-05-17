package com.kilagee.onelove.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.kilagee.onelove.data.model.PaymentMethod
import com.kilagee.onelove.data.model.SubscriptionPlan
import com.kilagee.onelove.data.model.SubscriptionStatus
import com.kilagee.onelove.data.model.Transaction
import com.kilagee.onelove.data.model.TransactionStatus
import com.kilagee.onelove.data.model.TransactionType
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Payment-related operations
 */
@Dao
interface PaymentDao {
    
    // Subscription plans
    @Query("SELECT * FROM subscription_plans WHERE id = :planId")
    fun getSubscriptionPlanById(planId: String): Flow<SubscriptionPlan?>
    
    @Query("SELECT * FROM subscription_plans WHERE isActive = 1 ORDER BY displayOrder ASC")
    fun getActiveSubscriptionPlans(): Flow<List<SubscriptionPlan>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubscriptionPlan(plan: SubscriptionPlan)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubscriptionPlans(plans: List<SubscriptionPlan>)
    
    @Update
    suspend fun updateSubscriptionPlan(plan: SubscriptionPlan)
    
    @Delete
    suspend fun deleteSubscriptionPlan(plan: SubscriptionPlan)
    
    // Subscription statuses
    @Query("SELECT * FROM subscription_statuses WHERE id = :subscriptionId")
    fun getSubscriptionStatusById(subscriptionId: String): Flow<SubscriptionStatus?>
    
    @Query("SELECT * FROM subscription_statuses WHERE userId = :userId ORDER BY createdAt DESC LIMIT 1")
    fun getCurrentSubscriptionForUser(userId: String): Flow<SubscriptionStatus?>
    
    @Query("SELECT * FROM subscription_statuses WHERE userId = :userId ORDER BY createdAt DESC")
    fun getSubscriptionHistoryForUser(userId: String): Flow<List<SubscriptionStatus>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubscriptionStatus(status: SubscriptionStatus)
    
    @Update
    suspend fun updateSubscriptionStatus(status: SubscriptionStatus)
    
    @Query("UPDATE subscription_statuses SET autoRenew = :autoRenew WHERE id = :subscriptionId")
    suspend fun updateAutoRenewStatus(subscriptionId: String, autoRenew: Boolean)
    
    @Query("UPDATE subscription_statuses SET cancelledAt = :timestamp, autoRenew = 0 WHERE id = :subscriptionId")
    suspend fun cancelSubscription(subscriptionId: String, timestamp: Long)
    
    // Payment methods
    @Query("SELECT * FROM payment_methods WHERE id = :paymentMethodId")
    fun getPaymentMethodById(paymentMethodId: String): Flow<PaymentMethod?>
    
    @Query("SELECT * FROM payment_methods WHERE userId = :userId ORDER BY isDefault DESC, createdAt DESC")
    fun getPaymentMethodsForUser(userId: String): Flow<List<PaymentMethod>>
    
    @Query("SELECT * FROM payment_methods WHERE userId = :userId AND isDefault = 1 LIMIT 1")
    fun getDefaultPaymentMethod(userId: String): Flow<PaymentMethod?>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPaymentMethod(paymentMethod: PaymentMethod)
    
    @Update
    suspend fun updatePaymentMethod(paymentMethod: PaymentMethod)
    
    @Delete
    suspend fun deletePaymentMethod(paymentMethod: PaymentMethod)
    
    @Query("DELETE FROM payment_methods WHERE id = :paymentMethodId")
    suspend fun deletePaymentMethodById(paymentMethodId: String)
    
    @Query("""
        UPDATE payment_methods 
        SET isDefault = CASE WHEN id = :paymentMethodId THEN 1 ELSE 0 END
        WHERE userId = :userId
    """)
    suspend fun setDefaultPaymentMethod(userId: String, paymentMethodId: String)
    
    // Transactions
    @Query("SELECT * FROM transactions WHERE id = :transactionId")
    fun getTransactionById(transactionId: String): Flow<Transaction?>
    
    @Query("SELECT * FROM transactions WHERE userId = :userId ORDER BY createdAt DESC")
    fun getTransactionsForUser(userId: String): Flow<List<Transaction>>
    
    @Query("SELECT * FROM transactions WHERE userId = :userId AND type = :type ORDER BY createdAt DESC")
    fun getTransactionsForUserByType(userId: String, type: TransactionType): Flow<List<Transaction>>
    
    @Query("SELECT * FROM transactions WHERE userId = :userId AND status = :status ORDER BY createdAt DESC")
    fun getTransactionsForUserByStatus(userId: String, status: TransactionStatus): Flow<List<Transaction>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction)
    
    @Update
    suspend fun updateTransaction(transaction: Transaction)
    
    @Query("UPDATE transactions SET status = :status WHERE id = :transactionId")
    suspend fun updateTransactionStatus(transactionId: String, status: TransactionStatus)
    
    @Query("UPDATE transactions SET completedAt = :timestamp, status = :status WHERE id = :transactionId")
    suspend fun completeTransaction(transactionId: String, timestamp: Long, status: TransactionStatus)
    
    @Query("""
        SELECT SUM(amount) FROM transactions 
        WHERE userId = :userId AND status = 'COMPLETED'
        AND type NOT IN ('REFUND', 'CHARGEBACK')
        AND createdAt >= :startTimestamp AND createdAt <= :endTimestamp
    """)
    fun getTotalSpendingInPeriod(userId: String, startTimestamp: Long, endTimestamp: Long): Flow<Int>
}