package com.kilagee.onelove.domain.repository

import com.kilagee.onelove.data.model.PaymentMethod
import com.kilagee.onelove.data.model.SubscriptionPlan
import com.kilagee.onelove.data.model.SubscriptionStatus
import com.kilagee.onelove.data.model.Transaction
import com.kilagee.onelove.domain.util.Result
import kotlinx.coroutines.flow.Flow
import java.util.Date

/**
 * Repository interface for payment-related operations
 */
interface PaymentRepository {
    
    /**
     * Get available subscription plans
     * 
     * @return Flow of a list of subscription plans
     */
    fun getSubscriptionPlans(): Flow<Result<List<SubscriptionPlan>>>
    
    /**
     * Get current subscription status for the user
     * 
     * @return Flow of the subscription status or null if not subscribed
     */
    fun getCurrentSubscription(): Flow<Result<SubscriptionStatus?>>
    
    /**
     * Get subscription history for the user
     * 
     * @return Flow of a list of subscription statuses
     */
    fun getSubscriptionHistory(): Flow<Result<List<SubscriptionStatus>>>
    
    /**
     * Subscribe to a plan
     * 
     * @param planId ID of the subscription plan
     * @param paymentMethodId ID of the payment method to use
     * @return Result of the created subscription
     */
    suspend fun subscribeToPlan(planId: String, paymentMethodId: String): Result<SubscriptionStatus>
    
    /**
     * Update auto-renew setting
     * 
     * @param subscriptionId ID of the subscription
     * @param autoRenew Whether auto-renew should be enabled
     * @return Result of the updated subscription
     */
    suspend fun updateAutoRenew(subscriptionId: String, autoRenew: Boolean): Result<SubscriptionStatus>
    
    /**
     * Cancel a subscription
     * 
     * @param subscriptionId ID of the subscription
     * @param reason Optional reason for cancellation
     * @return Result of the operation
     */
    suspend fun cancelSubscription(subscriptionId: String, reason: String? = null): Result<Unit>
    
    /**
     * Get payment methods for the user
     * 
     * @return Flow of a list of payment methods
     */
    fun getPaymentMethods(): Flow<Result<List<PaymentMethod>>>
    
    /**
     * Get default payment method for the user
     * 
     * @return Flow of the default payment method or null if none
     */
    fun getDefaultPaymentMethod(): Flow<Result<PaymentMethod?>>
    
    /**
     * Add a payment method
     * 
     * @param paymentMethodId Stripe payment method ID
     * @param makeDefault Whether to make this the default payment method
     * @return Result of the added payment method
     */
    suspend fun addPaymentMethod(paymentMethodId: String, makeDefault: Boolean = false): Result<PaymentMethod>
    
    /**
     * Set a payment method as default
     * 
     * @param paymentMethodId ID of the payment method
     * @return Result of the operation
     */
    suspend fun setDefaultPaymentMethod(paymentMethodId: String): Result<Unit>
    
    /**
     * Delete a payment method
     * 
     * @param paymentMethodId ID of the payment method
     * @return Result of the operation
     */
    suspend fun deletePaymentMethod(paymentMethodId: String): Result<Unit>
    
    /**
     * Get transaction history
     * 
     * @param limit Maximum number of transactions to retrieve
     * @return Flow of a list of transactions
     */
    fun getTransactionHistory(limit: Int = 20): Flow<Result<List<Transaction>>>
    
    /**
     * Get transaction details
     * 
     * @param transactionId ID of the transaction
     * @return Flow of the transaction
     */
    fun getTransactionDetails(transactionId: String): Flow<Result<Transaction>>
    
    /**
     * Get spending summary
     * 
     * @param startDate Start date for the summary
     * @param endDate End date for the summary
     * @return Result containing the total spending in cents
     */
    suspend fun getSpendingSummary(startDate: Date, endDate: Date): Result<Int>
    
    /**
     * Create a Stripe payment intent
     * 
     * @param amount Amount in cents
     * @param currency Currency code (e.g., USD)
     * @param description Description of the payment
     * @return Result containing the client secret
     */
    suspend fun createPaymentIntent(
        amount: Int,
        currency: String = "USD",
        description: String
    ): Result<String>
    
    /**
     * Confirm a payment
     * 
     * @param paymentIntentId ID of the payment intent
     * @param paymentMethodId ID of the payment method
     * @return Result of the transaction
     */
    suspend fun confirmPayment(
        paymentIntentId: String,
        paymentMethodId: String
    ): Result<Transaction>
    
    /**
     * Purchase coins
     * 
     * @param coinPackageId ID of the coin package
     * @param paymentMethodId ID of the payment method
     * @return Result of the transaction
     */
    suspend fun purchaseCoins(coinPackageId: String, paymentMethodId: String): Result<Transaction>
}