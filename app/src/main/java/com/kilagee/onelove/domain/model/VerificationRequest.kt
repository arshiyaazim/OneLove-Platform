package com.kilagee.onelove.domain.model

import java.util.UUID

/**
 * Data class representing a verification request from a user
 */
data class VerificationRequest(
    val id: String = UUID.randomUUID().toString(),
    val userId: String = "",
    val userDisplayName: String = "",
    val userPhotoUrl: String = "",
    val status: VerificationStatus = VerificationStatus.PENDING,
    val documentType: String = "",
    val documentUrl: String = "",
    val selfieUrl: String = "",
    val additionalInfo: String = "",
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
            "userPhotoUrl" to userPhotoUrl,
            "status" to status.name,
            "documentType" to documentType,
            "documentUrl" to documentUrl,
            "selfieUrl" to selfieUrl,
            "additionalInfo" to additionalInfo,
            "submittedAt" to submittedAt,
            "processedAt" to processedAt,
            "processedBy" to processedBy,
            "adminNotes" to adminNotes,
            "rejectionReason" to rejectionReason
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any?>): VerificationRequest {
            val statusStr = map["status"] as? String ?: VerificationStatus.PENDING.name
            val status = try {
                VerificationStatus.valueOf(statusStr)
            } catch (e: Exception) {
                VerificationStatus.PENDING
            }

            return VerificationRequest(
                id = map["id"] as? String ?: UUID.randomUUID().toString(),
                userId = map["userId"] as? String ?: "",
                userDisplayName = map["userDisplayName"] as? String ?: "",
                userPhotoUrl = map["userPhotoUrl"] as? String ?: "",
                status = status,
                documentType = map["documentType"] as? String ?: "",
                documentUrl = map["documentUrl"] as? String ?: "",
                selfieUrl = map["selfieUrl"] as? String ?: "",
                additionalInfo = map["additionalInfo"] as? String ?: "",
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
 * Enum representing verification request status
 */
enum class VerificationStatus {
    PENDING,
    APPROVED,
    REJECTED
}