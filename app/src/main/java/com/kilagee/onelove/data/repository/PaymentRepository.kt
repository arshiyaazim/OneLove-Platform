package com.kilagee.onelove.data.repository

import com.kilagee.onelove.data.model.Payment
import com.kilagee.onelove.data.model.PaymentStatus
import com.kilagee.onelove.data.model.PaymentType
import com.kilagee.onelove.data.model.Result
import kotlinx.coroutines.flow.Flow
import java.util.Date

/**
 * Repository interface for payments
 */
interface PaymentRepository {
    
    /**
     * Get payment by ID
     * @param paymentId Payment ID
     * @return Result containing the payment or error
     */
    suspend fun getPaymentById(paymentId: String): Result<Payment>
    
    /**
     * Get payment by ID as Flow
     * @param paymentId Payment ID
     * @return Flow emitting Result containing the payment or error
     */
    fun getPaymentByIdFlow(paymentId: String): Flow<Result<Payment>>
    
    /**
     * Get user payments
     * @param userId User ID
     * @param type Optional payment type filter
     * @param status Optional payment status filter
     * @param limit Maximum number of results
     * @return Result containing list of payments or error
     */
    suspend fun getUserPayments(
        userId: String,
        type: PaymentType? = null,
        status: PaymentStatus? = null,
        limit: Int = 50
    ): Result<List<Payment>>
    
    /**
     * Get user payments as Flow
     * @param userId User ID
     * @param type Optional payment type filter
     * @param status Optional payment status filter
     * @return Flow emitting Result containing list of payments or error
     */
    fun getUserPaymentsFlow(
        userId: String,
        type: PaymentType? = null,
        status: PaymentStatus? = null
    ): Flow<Result<List<Payment>>>
    
    /**
     * Create payment intent
     * @param userId User ID
     * @param amount Payment amount
     * @param currency Payment currency
     * @param paymentType Payment type
     * @param description Payment description
     * @param metadata Optional additional metadata
     * @return Result containing Stripe payment intent client secret or error
     */
    suspend fun createPaymentIntent(
        userId: String,
        amount: Double,
        currency: String = "USD",
        paymentType: PaymentType,
        description: String,
        metadata: Map<String, String>? = null
    ): Result<String>
    
    /**
     * Confirm payment intent
     * @param userId User ID
     * @param paymentIntentId Stripe payment intent ID
     * @param paymentMethodId Stripe payment method ID
     * @return Result containing the payment or error
     */
    suspend fun confirmPaymentIntent(
        userId: String,
        paymentIntentId: String,
        paymentMethodId: String
    ): Result<Payment>
    
    /**
     * Handle payment action
     * @param userId User ID
     * @param paymentIntentId Stripe payment intent ID
     * @param paymentIntentClientSecret Stripe payment intent client secret
     * @return Result containing the payment or error
     */
    suspend fun handlePaymentAction(
        userId: String,
        paymentIntentId: String,
        paymentIntentClientSecret: String
    ): Result<Payment>
    
    /**
     * Get payment methods for user
     * @param userId User ID
     * @return Result containing list of payment methods or error
     */
    suspend fun getPaymentMethods(userId: String): Result<List<Map<String, Any>>>
    
    /**
     * Add payment method
     * @param userId User ID
     * @param paymentMethodId Stripe payment method ID
     * @param isDefault Whether this is the default payment method
     * @return Result indicating success or error
     */
    suspend fun addPaymentMethod(
        userId: String,
        paymentMethodId: String,
        isDefault: Boolean = false
    ): Result<Unit>
    
    /**
     * Remove payment method
     * @param userId User ID
     * @param paymentMethodId Stripe payment method ID
     * @return Result indicating success or error
     */
    suspend fun removePaymentMethod(
        userId: String,
        paymentMethodId: String
    ): Result<Unit>
    
    /**
     * Set default payment method
     * @param userId User ID
     * @param paymentMethodId Stripe payment method ID
     * @return Result indicating success or error
     */
    suspend fun setDefaultPaymentMethod(
        userId: String,
        paymentMethodId: String
    ): Result<Unit>
    
    /**
     * Process payment for subscription
     * @param userId User ID
     * @param subscriptionType Subscription type string identifier
     * @param paymentMethodId Stripe payment method ID
     * @return Result containing the subscription ID or error
     */
    suspend fun processSubscriptionPayment(
        userId: String,
        subscriptionType: String,
        paymentMethodId: String
    ): Result<String>
    
    /**
     * Process payment for points
     * @param userId User ID
     * @param pointsPackage Points package identifier
     * @param paymentMethodId Stripe payment method ID
     * @return Result containing the payment ID or error
     */
    suspend fun processPointsPayment(
        userId: String,
        pointsPackage: String,
        paymentMethodId: String
    ): Result<String>
    
    /**
     * Request refund
     * @param paymentId Payment ID
     * @param amount Optional refund amount (full refund if null)
     * @param reason Optional refund reason
     * @return Result containing the updated payment or error
     */
    suspend fun requestRefund(
        paymentId: String,
        amount: Double? = null,
        reason: String? = null
    ): Result<Payment>
    
    /**
     * Get payment receipt URL
     * @param paymentId Payment ID
     * @return Result containing the receipt URL or error
     */
    suspend fun getPaymentReceiptUrl(paymentId: String): Result<String>
    
    /**
     * Get payment statistics
     * @param startDate Optional start date for statistics
     * @param endDate Optional end date for statistics
     * @return Result containing payment statistics or error
     */
    suspend fun getPaymentStatistics(
        startDate: Date? = null,
        endDate: Date? = null
    ): Result<Map<String, Any>>
    
    /**
     * Sync payment with Stripe
     * @param paymentId Payment ID
     * @return Result containing the updated payment or error
     */
    suspend fun syncPaymentWithStripe(paymentId: String): Result<Payment>
}