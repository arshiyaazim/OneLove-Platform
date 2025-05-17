package com.kilagee.onelove.domain.repository

import com.kilagee.onelove.data.model.BillingPeriod
import com.kilagee.onelove.data.model.PaymentMethod
import com.kilagee.onelove.data.model.SubscriptionPlan
import com.kilagee.onelove.data.model.SubscriptionPurchaseResult
import com.kilagee.onelove.data.model.SubscriptionTier
import com.kilagee.onelove.data.model.UserSubscription
import com.kilagee.onelove.domain.util.Result
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for subscription-related functionality
 */
interface SubscriptionRepository {

    /**
     * Get available subscription plans
     */
    fun getSubscriptionPlans(): Flow<Result<List<SubscriptionPlan>>>

    /**
     * Get user's current subscription
     */
    fun getUserSubscription(userId: String): Flow<Result<UserSubscription?>>

    /**
     * Subscribe user to a plan
     * 
     * @param userId The user ID
     * @param planId The plan ID
     * @param paymentMethodId The payment method ID (or null for new payment)
     * @param billingPeriod The billing period (MONTHLY or YEARLY)
     */
    suspend fun subscribeToPlan(
        userId: String, 
        planId: String, 
        paymentMethodId: String?,
        billingPeriod: BillingPeriod
    ): Result<SubscriptionPurchaseResult>

    /**
     * Complete subscription purchase with action
     */
    suspend fun completeSubscription(
        userId: String,
        subscriptionId: String,
        actionSecret: String
    ): Result<SubscriptionPurchaseResult>
    
    /**
     * Cancel subscription
     */
    suspend fun cancelSubscription(userId: String): Result<Boolean>
    
    /**
     * Update subscription auto-renew setting
     */
    suspend fun updateAutoRenew(userId: String, autoRenew: Boolean): Result<Boolean>
    
    /**
     * Change subscription plan
     */
    suspend fun changeSubscriptionPlan(
        userId: String,
        newPlanId: String,
        newBillingPeriod: BillingPeriod
    ): Result<SubscriptionPurchaseResult>
    
    /**
     * Get user's payment methods
     */
    fun getUserPaymentMethods(userId: String): Flow<Result<List<PaymentMethod>>>
    
    /**
     * Add payment method
     */
    suspend fun addPaymentMethod(
        userId: String,
        cardNumber: String,
        expiryMonth: Int,
        expiryYear: Int,
        cvc: String,
        isDefault: Boolean
    ): Result<PaymentMethod>
    
    /**
     * Remove payment method
     */
    suspend fun removePaymentMethod(userId: String, paymentMethodId: String): Result<Boolean>
    
    /**
     * Set default payment method
     */
    suspend fun setDefaultPaymentMethod(userId: String, paymentMethodId: String): Result<Boolean>
    
    /**
     * Check if subscribed to specific tier
     */
    suspend fun isSubscribedToTier(userId: String, tier: SubscriptionTier): Result<Boolean>
}