package com.kilagee.onelove.data.model

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import kotlinx.parcelize.Parcelize
import java.util.UUID

/**
 * Verification request status
 */
enum class VerificationRequestStatus {
    PENDING,
    APPROVED,
    REJECTED,
    CANCELLED
}

/**
 * Verification type
 */
enum class VerificationType {
    PHOTO,
    ID,
    PREMIUM
}

/**
 * Verification request entity for user identity verification
 */
@Entity(tableName = "verification_requests")
@Parcelize
data class VerificationRequest(
    @PrimaryKey
    @DocumentId
    val id: String = UUID.randomUUID().toString(),
    
    /**
     * ID of the user making the verification request
     */
    val userId: String = "",
    
    /**
     * Type of verification being requested
     */
    val type: VerificationType = VerificationType.PHOTO,
    
    /**
     * Status of the verification request
     */
    val status: VerificationRequestStatus = VerificationRequestStatus.PENDING,
    
    /**
     * URL of the ID photo (if applicable)
     */
    val idPhotoUrl: String? = null,
    
    /**
     * URL of the selfie photo (if applicable)
     */
    val selfiePhotoUrl: String? = null,
    
    /**
     * First name as it appears on ID
     */
    val firstName: String? = null,
    
    /**
     * Last name as it appears on ID
     */
    val lastName: String? = null,
    
    /**
     * Date of birth as it appears on ID
     */
    val dateOfBirth: Timestamp? = null,
    
    /**
     * ID document number (if applicable)
     */
    val documentNumber: String? = null,
    
    /**
     * Admin notes on the verification
     */
    val adminNotes: String? = null,
    
    /**
     * Rejection reason (if applicable)
     */
    val rejectionReason: String? = null,
    
    /**
     * Timestamp when the request was created
     */
    val createdAt: Timestamp = Timestamp.now(),
    
    /**
     * Timestamp when the request was last updated
     */
    val updatedAt: Timestamp = Timestamp.now(),
    
    /**
     * Timestamp when the request was reviewed by an admin
     */
    val reviewedAt: Timestamp? = null,
    
    /**
     * ID of the admin who reviewed the request
     */
    val reviewedBy: String? = null
) : Parcelable {
    /**
     * Check if the request is pending
     */
    fun isPending(): Boolean {
        return status == VerificationRequestStatus.PENDING
    }
    
    /**
     * Check if the request has been approved
     */
    fun isApproved(): Boolean {
        return status == VerificationRequestStatus.APPROVED
    }
    
    /**
     * Check if the request has been rejected
     */
    fun isRejected(): Boolean {
        return status == VerificationRequestStatus.REJECTED
    }
    
    /**
     * Check if the request has been reviewed
     */
    fun isReviewed(): Boolean {
        return reviewedAt != null && (isApproved() || isRejected())
    }
}