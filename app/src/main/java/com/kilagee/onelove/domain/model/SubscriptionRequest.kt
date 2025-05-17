package com.kilagee.onelove.domain.model

import java.util.UUID

/**
 * Data class representing a subscription request or manual upgrade
 */
data class SubscriptionRequest(
    val id: String = UUID.randomUUID().toString(),
    val userId: String = "",
    val userDisplayName: String = "",
    val userEmail: String = "",
    val tierId: String = "",
    val tierName: String = "",
    val status: SubscriptionRequestStatus = SubscriptionRequestStatus.PENDING,
    val paymentProofUrl: String? = null,
    val paymentMethod: String = "",
    val amount: Double = 0.0,
    val currency: String = "USD",
    val duration: Int = 30, // in days
    val notes: String? = null,
    val submittedAt: Long = System.currentTimeMillis(),
    val processedAt: Long? = null,
    val processedBy: String? = null,
    val adminNotes: String? = null,
    val rejectionReason: String? = null
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "userId" to userId,
            "userDisplayName" to userDisplayName,
            "userEmail" to userEmail,
            "tierId" to tierId,
            "tierName" to tierName,
            "status" to status.name,
            "paymentProofUrl" to paymentProofUrl,
            "paymentMethod" to paymentMethod,
            "amount" to amount,
            "currency" to currency,
            "duration" to duration,
            "notes" to notes,
            "submittedAt" to submittedAt,
            "processedAt" to processedAt,
            "processedBy" to processedBy,
            "adminNotes" to adminNotes,
            "rejectionReason" to rejectionReason
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any?>): SubscriptionRequest {
            val statusStr = map["status"] as? String ?: SubscriptionRequestStatus.PENDING.name
            val status = try {
                SubscriptionRequestStatus.valueOf(statusStr)
            } catch (e: Exception) {
                SubscriptionRequestStatus.PENDING
            }

            return SubscriptionRequest(
                id = map["id"] as? String ?: UUID.randomUUID().toString(),
                userId = map["userId"] as? String ?: "",
                userDisplayName = map["userDisplayName"] as? String ?: "",
                userEmail = map["userEmail"] as? String ?: "",
                tierId = map["tierId"] as? String ?: "",
                tierName = map["tierName"] as? String ?: "",
                status = status,
                paymentProofUrl = map["paymentProofUrl"] as? String,
                paymentMethod = map["paymentMethod"] as? String ?: "",
                amount = (map["amount"] as? Number)?.toDouble() ?: 0.0,
                currency = map["currency"] as? String ?: "USD",
                duration = (map["duration"] as? Number)?.toInt() ?: 30,
                notes = map["notes"] as? String,
                submittedAt = (map["submittedAt"] as? Long) ?: System.currentTimeMillis(),
                processedAt = map["processedAt"] as? Long,
                processedBy = map["processedBy"] as? String,
                adminNotes = map["adminNotes"] as? String,
                rejectionReason = map["rejectionReason"] as? String
            )
        }
    }
}

/**
 * Enum representing subscription request status
 */
enum class SubscriptionRequestStatus {
    PENDING,
    APPROVED,
    REJECTED,
    CANCELLED
}