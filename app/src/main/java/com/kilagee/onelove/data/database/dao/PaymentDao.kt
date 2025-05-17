package com.kilagee.onelove.data.database.dao

import androidx.room.*
import com.kilagee.onelove.data.model.Payment
import com.kilagee.onelove.data.model.PaymentStatus
import com.kilagee.onelove.data.model.PaymentType
import kotlinx.coroutines.flow.Flow
import java.util.Date

/**
 * Data Access Object for Payment entities
 */
@Dao
interface PaymentDao {
    
    /**
     * Insert a payment
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: Payment)
    
    /**
     * Insert multiple payments
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayments(payments: List<Payment>)
    
    /**
     * Update payment
     */
    @Update
    suspend fun updatePayment(payment: Payment)
    
    /**
     * Get all payments for a user
     */
    @Query("SELECT * FROM payments WHERE userId = :userId ORDER BY createdAt DESC")
    fun getPaymentsForUser(userId: String): Flow<List<Payment>>
    
    /**
     * Get payments by status for a user
     */
    @Query("SELECT * FROM payments WHERE userId = :userId AND status = :status ORDER BY createdAt DESC")
    fun getPaymentsByStatus(userId: String, status: PaymentStatus): Flow<List<Payment>>
    
    /**
     * Get payments by type for a user
     */
    @Query("SELECT * FROM payments WHERE userId = :userId AND type = :type ORDER BY createdAt DESC")
    fun getPaymentsByType(userId: String, type: PaymentType): Flow<List<Payment>>
    
    /**
     * Get payment by ID
     */
    @Query("SELECT * FROM payments WHERE id = :paymentId")
    suspend fun getPaymentById(paymentId: String): Payment?
    
    /**
     * Get payment by provider payment ID
     */
    @Query("SELECT * FROM payments WHERE providerPaymentId = :providerPaymentId")
    suspend fun getPaymentByProviderPaymentId(providerPaymentId: String): Payment?
    
    /**
     * Get payments for a subscription
     */
    @Query("SELECT * FROM payments WHERE subscriptionId = :subscriptionId ORDER BY createdAt DESC")
    fun getPaymentsForSubscription(subscriptionId: String): Flow<List<Payment>>
    
    /**
     * Get payments for an offer
     */
    @Query("SELECT * FROM payments WHERE offerId = :offerId ORDER BY createdAt DESC")
    fun getPaymentsForOffer(offerId: String): Flow<List<Payment>>
    
    /**
     * Get total spent by user in date range
     */
    @Query("SELECT SUM(amountUsd) FROM payments WHERE userId = :userId AND status = 'SUCCEEDED' AND createdAt BETWEEN :startDate AND :endDate")
    suspend fun getTotalSpentInDateRange(userId: String, startDate: Date, endDate: Date): Double?
    
    /**
     * Delete payment
     */
    @Delete
    suspend fun deletePayment(payment: Payment)
    
    /**
     * Delete payment by ID
     */
    @Query("DELETE FROM payments WHERE id = :paymentId")
    suspend fun deletePaymentById(paymentId: String)
    
    /**
     * Get payments requiring action
     */
    @Query("SELECT * FROM payments WHERE userId = :userId AND requiresAction = 1 AND status = 'PENDING'")
    suspend fun getPaymentsRequiringAction(userId: String): List<Payment>
}