package com.kilagee.onelove.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Subscription types
 */
enum class SubscriptionTier {
    FREE,
    PREMIUM,
    GOLD
}

/**
 * Billing period types
 */
enum class BillingPeriod {
    MONTHLY,
    YEARLY
}

/**
 * Subscription plan data model
 */
data class SubscriptionPlan(
    val id: String,
    val name: String,
    val description: String,
    val tier: SubscriptionTier,
    val priceMonthly: Double,
    val priceYearly: Double,
    val stripePriceIdMonthly: String,
    val stripePriceIdYearly: String,
    val features: List<String>,
    val displayOrder: Int
)

/**
 * Active subscription data
 */
@Entity(tableName = "subscriptions")
data class UserSubscription(
    @PrimaryKey
    val id: String,
    val userId: String,
    val tier: SubscriptionTier,
    val billingPeriod: BillingPeriod,
    val startDate: Date,
    val endDate: Date,
    val autoRenew: Boolean = true,
    val status: SubscriptionStatus,
    val stripeCustomerId: String? = null,
    val stripeSubscriptionId: String? = null,
    val stripePaymentMethodId: String? = null,
    val lastPaymentDate: Date? = null,
    val nextBillingDate: Date? = null,
    val canceledAt: Date? = null
)

/**
 * Subscription status types
 */
enum class SubscriptionStatus {
    ACTIVE,
    CANCELED,
    PAST_DUE,
    INCOMPLETE,
    EXPIRED,
    TRIALING
}

/**
 * Payment method model
 */
data class PaymentMethod(
    val id: String,
    val brand: String,
    val last4: String,
    val expiryMonth: Int,
    val expiryYear: Int,
    val isDefault: Boolean,
    val country: String? = null,
    val customerId: String? = null
)

/**
 * Subscription purchase result
 */
data class SubscriptionPurchaseResult(
    val success: Boolean,
    val subscription: UserSubscription? = null,
    val error: String? = null,
    val requiresAction: Boolean = false,
    val actionUrl: String? = null
)