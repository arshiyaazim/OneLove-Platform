package com.kilagee.onelove.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.Date

/**
 * Payment model for financial transactions
 */
@Parcelize
data class Payment(
    val id: String = "",
    val userId: String = "",
    val amount: Double = 0.0,
    val currency: String = "USD",
    val status: PaymentStatus = PaymentStatus.PENDING,
    val paymentMethod: String = "",
    val paymentMethodDetails: PaymentMethodDetails? = null,
    val productType: ProductType = ProductType.SUBSCRIPTION,
    val productId: String = "",
    val subscriptionId: String = "",
    val pointsPackageId: String = "",
    val pointsAmount: Int = 0,
    val transactionId: String = "",
    val paymentIntentId: String = "",
    val createdAt: Date = Date(),
    val updatedAt: Date = Date(),
    val completedAt: Date? = null,
    val failureReason: String = "",
    val receiptUrl: String = "",
    val metadata: Map<String, String> = emptyMap(),
    val refundedAmount: Double = 0.0,
    val isRefunded: Boolean = false,
    val refundReason: String = "",
    val refundedAt: Date? = null
) : Parcelable

/**
 * Payment status enum
 */
@Parcelize
enum class PaymentStatus : Parcelable {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED,
    REFUNDED,
    PARTIALLY_REFUNDED,
    CANCELLED
}

/**
 * Product type enum
 */
@Parcelize
enum class ProductType : Parcelable {
    SUBSCRIPTION,
    POINTS_PACKAGE,
    FEATURE,
    OFFER,
    GIFT,
    DONATION,
    OTHER
}

/**
 * Payment method details
 */
@Parcelize
data class PaymentMethodDetails(
    val id: String = "",
    val type: PaymentMethodType = PaymentMethodType.CARD,
    val brand: String = "",
    val last4: String = "",
    val expiryMonth: Int = 0,
    val expiryYear: Int = 0,
    val cardholderName: String = "",
    val isDefault: Boolean = false,
    val billingAddress: BillingAddress? = null,
    val createdAt: Date = Date()
) : Parcelable

/**
 * Payment method type enum
 */
@Parcelize
enum class PaymentMethodType : Parcelable {
    CARD,
    PAYPAL,
    GOOGLE_PAY,
    APPLE_PAY,
    BANK_TRANSFER,
    WALLET_BALANCE
}

/**
 * Billing address
 */
@Parcelize
data class BillingAddress(
    val line1: String = "",
    val line2: String = "",
    val city: String = "",
    val state: String = "",
    val postalCode: String = "",
    val country: String = ""
) : Parcelable

/**
 * Subscription model
 */
@Parcelize
data class Subscription(
    val id: String = "",
    val userId: String = "",
    val type: SubscriptionType = SubscriptionType.FREE,
    val period: SubscriptionPeriod = SubscriptionPeriod.MONTHLY,
    val status: SubscriptionStatus = SubscriptionStatus.ACTIVE,
    val startDate: Date = Date(),
    val endDate: Date? = null,
    val renewalDate: Date? = null,
    val autoRenew: Boolean = true,
    val price: Double = 0.0,
    val currency: String = "USD",
    val paymentMethodId: String = "",
    val lastPaymentId: String = "",
    val lastPaymentDate: Date? = null,
    val nextPaymentAmount: Double = 0.0,
    val canceledAt: Date? = null,
    val cancelReason: String = "",
    val trialEndDate: Date? = null,
    val isInTrial: Boolean = false,
    val metadata: Map<String, String> = emptyMap(),
    val createdAt: Date = Date(),
    val updatedAt: Date = Date()
) : Parcelable

/**
 * Subscription status enum
 */
@Parcelize
enum class SubscriptionStatus : Parcelable {
    ACTIVE,
    CANCELED,
    EXPIRED,
    PAST_DUE,
    PENDING,
    TRIAL,
    PAUSED
}

/**
 * Points package model
 */
@Parcelize
data class PointsPackage(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val points: Int = 0,
    val price: Double = 0.0,
    val currency: String = "USD",
    val isPopular: Boolean = false,
    val bonusPoints: Int = 0,
    val discountPercent: Int = 0,
    val expiresAt: Date? = null,
    val isActive: Boolean = true,
    val order: Int = 0,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date(),
    val imageUrl: String = "",
    val metadata: Map<String, String> = emptyMap()
) : Parcelable

/**
 * Points transaction model
 */
@Parcelize
data class PointsTransaction(
    val id: String = "",
    val userId: String = "",
    val amount: Int = 0,
    val type: PointsTransactionType = PointsTransactionType.PURCHASE,
    val description: String = "",
    val balanceAfter: Int = 0,
    val referenceId: String = "", // Associated offer/payment/etc ID
    val createdAt: Date = Date(),
    val expiresAt: Date? = null,
    val metadata: Map<String, String> = emptyMap()
) : Parcelable

/**
 * Points transaction type enum
 */
@Parcelize
enum class PointsTransactionType : Parcelable {
    PURCHASE,
    REWARD,
    REDEMPTION,
    REFUND,
    EXPIRATION,
    BONUS,
    OFFER,
    ADMIN_ADJUSTMENT,
    REFERRAL,
    DAILY_LOGIN,
    PROFILE_COMPLETION,
    VERIFICATION,
    FEATURE_USAGE
}