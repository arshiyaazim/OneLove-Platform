package com.kilagee.onelove.data.repository

import com.kilagee.onelove.data.model.PaymentMethod
import com.kilagee.onelove.data.model.Result
import com.kilagee.onelove.data.model.Subscription
import com.kilagee.onelove.data.model.SubscriptionPlan
import com.kilagee.onelove.data.model.SubscriptionStatus
import com.kilagee.onelove.data.model.SubscriptionTier
import kotlinx.coroutines.flow.Flow
import java.util.Date

/**
 * Repository interface for subscription operations
 */
interface SubscriptionRepository {
    /**
     * Get all subscription plans
     */
    suspend fun getSubscriptionPlans(): Result<List<SubscriptionPlan>>
    
    /**
     * Get subscription plan by ID
     */
    suspend fun getSubscriptionPlanById(planId: String): Result<SubscriptionPlan>
    
    /**
     * Get current user subscription
     */
    suspend fun getCurrentSubscription(userId: String): Result<Subscription?>
    
    /**
     * Get current user subscription as Flow
     */
    fun getCurrentSubscriptionFlow(userId: String): Flow<Subscription?>
    
    /**
     * Create a Stripe payment intent
     */
    suspend fun createPaymentIntent(
        userId: String,
        planId: String,
        amount: Double,
        currency: String,
        metadata: Map<String, String> = emptyMap()
    ): Result<Map<String, Any>>
    
    /**
     * Confirm a Stripe payment intent
     */
    suspend fun confirmPaymentIntent(
        paymentIntentId: String,
        paymentMethodId: String
    ): Result<Map<String, Any>>
    
    /**
     * Subscribe to a plan with Stripe
     */
    suspend fun subscribeWithStripe(
        userId: String,
        planId: String,
        paymentMethodId: String,
        promoCode: String? = null
    ): Result<Subscription>
    
    /**
     * Subscribe to a plan with local payment methods (bKash/Nagad)
     */
    suspend fun subscribeWithLocalPayment(
        userId: String,
        planId: String,
        paymentMethod: PaymentMethod,
        transactionId: String,
        amount: Double,
        currency: String = "BDT",
        promoCode: String? = null
    ): Result<Subscription>
    
    /**
     * Cancel subscription
     */
    suspend fun cancelSubscription(
        userId: String,
        subscriptionId: String,
        cancelReason: String? = null
    ): Result<Subscription>
    
    /**
     * Update auto-renew settings
     */
    suspend fun updateAutoRenew(
        userId: String,
        subscriptionId: String,
        autoRenew: Boolean
    ): Result<Subscription>
    
    /**
     * Get subscription status
     */
    suspend fun getSubscriptionStatus(
        userId: String,
        subscriptionId: String
    ): Result<SubscriptionStatus>
    
    /**
     * Get subscription details
     */
    suspend fun getSubscriptionDetails(
        userId: String,
        subscriptionId: String
    ): Result<Subscription>
    
    /**
     * Get subscription by ID
     */
    suspend fun getSubscriptionById(subscriptionId: String): Result<Subscription>
    
    /**
     * Apply promotional code
     */
    suspend fun applyPromoCode(
        promoCode: String,
        planId: String
    ): Result<Map<String, Any>>
    
    /**
     * Verify subscription receipt (for App Store purchases)
     */
    suspend fun verifySubscriptionReceipt(
        userId: String,
        receipt: String,
        planId: String
    ): Result<Subscription>
    
    /**
     * Verify Google Play purchase token
     */
    suspend fun verifyGooglePlayPurchase(
        userId: String,
        purchaseToken: String,
        productId: String,
        planId: String
    ): Result<Subscription>
    
    /**
     * Get subscription history for user
     */
    suspend fun getSubscriptionHistory(userId: String): Result<List<Subscription>>
    
    /**
     * Validate premium features access
     */
    suspend fun validatePremiumAccess(
        userId: String,
        feature: String
    ): Result<Boolean>
    
    /**
     * Get user's subscription tier
     */
    suspend fun getUserSubscriptionTier(userId: String): Result<SubscriptionTier>
    
    /**
     * Sync subscriptions with Stripe
     */
    suspend fun syncSubscriptions(userId: String): Result<Unit>
    
    /**
     * Check for active subscription
     */
    suspend fun hasActiveSubscription(userId: String): Result<Boolean>
    
    /**
     * Get expiry date for active subscription
     */
    suspend fun getSubscriptionExpiryDate(userId: String): Result<Date?>
    
    /**
     * Get payment methods for user
     */
    suspend fun getUserPaymentMethods(userId: String): Result<List<Map<String, Any>>>
    
    /**
     * Add payment method
     */
    suspend fun addPaymentMethod(
        userId: String,
        paymentMethodId: String
    ): Result<Map<String, Any>>
    
    /**
     * Remove payment method
     */
    suspend fun removePaymentMethod(
        userId: String,
        paymentMethodId: String
    ): Result<Unit>
    
    /**
     * Make default payment method
     */
    suspend fun setDefaultPaymentMethod(
        userId: String,
        paymentMethodId: String
    ): Result<Unit>
}