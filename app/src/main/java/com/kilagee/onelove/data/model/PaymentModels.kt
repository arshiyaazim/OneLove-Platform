package com.kilagee.onelove.data.model

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import kotlinx.parcelize.Parcelize
import java.util.UUID

/**
 * Payment method types
 */
enum class PaymentMethodType {
    CREDIT_CARD,
    DEBIT_CARD,
    PAYPAL,
    GOOGLE_PAY,
    APPLE_PAY,
    BANK_TRANSFER
}

/**
 * Transaction types
 */
enum class TransactionType {
    SUBSCRIPTION_PURCHASE,
    SUBSCRIPTION_RENEWAL,
    COIN_PURCHASE,
    FEATURE_PURCHASE,
    REFUND,
    CHARGEBACK
}

/**
 * Transaction status
 */
enum class TransactionStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED,
    REFUNDED
}

/**
 * Subscription plan entity
 */
@Entity(tableName = "subscription_plans")
@Parcelize
data class SubscriptionPlan(
    @PrimaryKey
    @DocumentId
    val id: String = UUID.randomUUID().toString(),
    
    /**
     * Name of the plan
     */
    val name: String = "",
    
    /**
     * Description of the plan
     */
    val description: String = "",
    
    /**
     * Price in cents
     */
    val price: Int = 0,
    
    /**
     * Currency code (e.g., USD)
     */
    val currency: String = "USD",
    
    /**
     * Billing interval (monthly, yearly)
     */
    val interval: String = "monthly",
    
    /**
     * Number of intervals for the billing cycle
     */
    val intervalCount: Int = 1,
    
    /**
     * Trial period in days
     */
    val trialPeriodDays: Int? = null,
    
    /**
     * Features included in the plan as JSON string
     */
    val features: List<String> = emptyList(),
    
    /**
     * Stripe price ID
     */
    val stripePriceId: String? = null,
    
    /**
     * Whether the plan is active
     */
    val isActive: Boolean = true,
    
    /**
     * Display order for UI
     */
    val displayOrder: Int = 0,
    
    /**
     * Timestamp when the plan was created
     */
    val createdAt: Timestamp = Timestamp.now(),
    
    /**
     * Timestamp when the plan was last updated
     */
    val updatedAt: Timestamp = Timestamp.now()
) : Parcelable

/**
 * Subscription status entity
 */
@Entity(tableName = "subscription_statuses")
@Parcelize
data class SubscriptionStatus(
    @PrimaryKey
    @DocumentId
    val id: String = UUID.randomUUID().toString(),
    
    /**
     * ID of the user
     */
    val userId: String = "",
    
    /**
     * ID of the subscription plan
     */
    val planId: String = "",
    
    /**
     * Whether the subscription is active
     */
    val isActive: Boolean = false,
    
    /**
     * Whether the subscription will auto-renew
     */
    val autoRenew: Boolean = true,
    
    /**
     * Start date of the subscription
     */
    val startDate: Timestamp = Timestamp.now(),
    
    /**
     * End date of the subscription
     */
    val endDate: Timestamp? = null,
    
    /**
     * Timestamp of the next billing date
     */
    val nextBillingDate: Timestamp? = null,
    
    /**
     * Stripe subscription ID
     */
    val stripeSubscriptionId: String? = null,
    
    /**
     * Timestamp when the subscription was cancelled
     */
    val cancelledAt: Timestamp? = null,
    
    /**
     * Reason for cancellation
     */
    val cancellationReason: String? = null,
    
    /**
     * Timestamp when the subscription was created
     */
    val createdAt: Timestamp = Timestamp.now(),
    
    /**
     * Timestamp when the subscription was last updated
     */
    val updatedAt: Timestamp = Timestamp.now()
) : Parcelable {
    /**
     * Check if the subscription is currently active
     */
    fun isCurrentlyActive(): Boolean {
        return isActive && (endDate == null || endDate.toDate().time > System.currentTimeMillis())
    }
    
    /**
     * Check if the subscription has been cancelled but is still active
     */
    fun isCancelledButActive(): Boolean {
        return isActive && cancelledAt != null && !autoRenew
    }
    
    /**
     * Get days remaining in the subscription
     */
    fun getDaysRemaining(): Int {
        if (!isActive || endDate == null) return 0
        
        val now = System.currentTimeMillis()
        val end = endDate.toDate().time
        
        if (now > end) return 0
        
        return ((end - now) / (1000 * 60 * 60 * 24)).toInt()
    }
}

/**
 * Payment method entity
 */
@Entity(tableName = "payment_methods")
@Parcelize
data class PaymentMethod(
    @PrimaryKey
    @DocumentId
    val id: String = UUID.randomUUID().toString(),
    
    /**
     * ID of the user
     */
    val userId: String = "",
    
    /**
     * Type of payment method
     */
    val type: PaymentMethodType = PaymentMethodType.CREDIT_CARD,
    
    /**
     * Last 4 digits of card or account
     */
    val last4: String? = null,
    
    /**
     * Card brand (Visa, Mastercard, etc.)
     */
    val brand: String? = null,
    
    /**
     * Expiry month
     */
    val expiryMonth: Int? = null,
    
    /**
     * Expiry year
     */
    val expiryYear: Int? = null,
    
    /**
     * Cardholder name
     */
    val name: String? = null,
    
    /**
     * Whether this is the default payment method
     */
    val isDefault: Boolean = false,
    
    /**
     * Stripe payment method ID
     */
    val stripePaymentMethodId: String? = null,
    
    /**
     * Timestamp when the payment method was created
     */
    val createdAt: Timestamp = Timestamp.now(),
    
    /**
     * Timestamp when the payment method was last updated
     */
    val updatedAt: Timestamp = Timestamp.now()
) : Parcelable {
    /**
     * Get a display name for the payment method
     */
    fun getDisplayName(): String {
        return when {
            brand != null && last4 != null -> "$brand •••• $last4"
            type == PaymentMethodType.PAYPAL && name != null -> "PayPal - $name"
            type == PaymentMethodType.GOOGLE_PAY -> "Google Pay"
            type == PaymentMethodType.APPLE_PAY -> "Apple Pay"
            else -> type.name
        }
    }
    
    /**
     * Check if the payment method is expired
     */
    fun isExpired(): Boolean {
        if (type != PaymentMethodType.CREDIT_CARD && type != PaymentMethodType.DEBIT_CARD) {
            return false
        }
        
        if (expiryMonth == null || expiryYear == null) {
            return false
        }
        
        val now = java.util.Calendar.getInstance()
        val currentYear = now.get(java.util.Calendar.YEAR)
        val currentMonth = now.get(java.util.Calendar.MONTH) + 1 // Calendar months are 0-based
        
        return expiryYear < currentYear || (expiryYear == currentYear && expiryMonth < currentMonth)
    }
}

/**
 * Transaction entity
 */
@Entity(tableName = "transactions")
@Parcelize
data class Transaction(
    @PrimaryKey
    @DocumentId
    val id: String = UUID.randomUUID().toString(),
    
    /**
     * ID of the user
     */
    val userId: String = "",
    
    /**
     * Type of transaction
     */
    val type: TransactionType = TransactionType.SUBSCRIPTION_PURCHASE,
    
    /**
     * Status of the transaction
     */
    val status: TransactionStatus = TransactionStatus.PENDING,
    
    /**
     * Amount in cents
     */
    val amount: Int = 0,
    
    /**
     * Currency code (e.g., USD)
     */
    val currency: String = "USD",
    
    /**
     * Description of the transaction
     */
    val description: String = "",
    
    /**
     * ID of the payment method used
     */
    val paymentMethodId: String? = null,
    
    /**
     * ID of the subscription plan (if applicable)
     */
    val planId: String? = null,
    
    /**
     * ID of the coin package (if applicable)
     */
    val coinPackageId: String? = null,
    
    /**
     * Number of coins purchased (if applicable)
     */
    val coinAmount: Int? = null,
    
    /**
     * Stripe payment intent ID
     */
    val stripePaymentIntentId: String? = null,
    
    /**
     * Error message if the transaction failed
     */
    val errorMessage: String? = null,
    
    /**
     * Timestamp when the transaction was created
     */
    val createdAt: Timestamp = Timestamp.now(),
    
    /**
     * Timestamp when the transaction was completed
     */
    val completedAt: Timestamp? = null
) : Parcelable {
    /**
     * Check if the transaction was successful
     */
    fun isSuccessful(): Boolean {
        return status == TransactionStatus.COMPLETED
    }
    
    /**
     * Get the formatted amount with currency
     */
    fun getFormattedAmount(): String {
        val amountInMainUnit = amount / 100.0
        return "%.2f %s".format(amountInMainUnit, currency)
    }
}