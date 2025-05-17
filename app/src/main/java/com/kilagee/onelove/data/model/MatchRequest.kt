package com.kilagee.onelove.data.model

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import kotlinx.parcelize.Parcelize
import java.util.UUID

/**
 * Match request status
 */
enum class MatchRequestStatus {
    PENDING,
    ACCEPTED,
    DECLINED,
    EXPIRED
}

/**
 * Match request entity representing a like or connection request from one user to another
 */
@Entity(tableName = "match_requests")
@Parcelize
data class MatchRequest(
    @PrimaryKey
    @DocumentId
    val id: String = UUID.randomUUID().toString(),
    
    /**
     * ID of the user who sent the request
     */
    val senderId: String = "",
    
    /**
     * ID of the user who received the request
     */
    val recipientId: String = "",
    
    /**
     * Status of the request
     */
    val status: MatchRequestStatus = MatchRequestStatus.PENDING,
    
    /**
     * Optional message included with the request
     */
    val message: String? = null,
    
    /**
     * Timestamp when the request was created
     */
    val createdAt: Timestamp = Timestamp.now(),
    
    /**
     * Timestamp when the request was last updated
     */
    val updatedAt: Timestamp = Timestamp.now(),
    
    /**
     * Timestamp when the request expires
     */
    val expiresAt: Timestamp? = null,
    
    /**
     * Timestamp when the request was viewed by the recipient
     */
    val viewedAt: Timestamp? = null
) : Parcelable {
    /**
     * Check if the request is pending
     */
    fun isPending(): Boolean {
        return status == MatchRequestStatus.PENDING
    }
    
    /**
     * Check if the request has been accepted
     */
    fun isAccepted(): Boolean {
        return status == MatchRequestStatus.ACCEPTED
    }
    
    /**
     * Check if the request has been declined
     */
    fun isDeclined(): Boolean {
        return status == MatchRequestStatus.DECLINED
    }
    
    /**
     * Check if the request has expired
     */
    fun isExpired(): Boolean {
        return status == MatchRequestStatus.EXPIRED || 
            (expiresAt != null && expiresAt.toDate().time < System.currentTimeMillis())
    }
    
    /**
     * Check if the request has been viewed
     */
    fun isViewed(): Boolean {
        return viewedAt != null
    }
}