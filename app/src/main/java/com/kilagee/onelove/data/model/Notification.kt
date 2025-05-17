package com.kilagee.onelove.data.model

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import kotlinx.parcelize.Parcelize
import java.util.UUID

/**
 * Notification types
 */
enum class NotificationType {
    MATCH,
    MESSAGE,
    CALL_MISSED,
    VERIFICATION_APPROVED,
    SUBSCRIPTION_EXPIRING,
    PAYMENT_SUCCESS,
    PAYMENT_FAILED,
    PROFILE_VIEW,
    APP_UPDATE,
    SYSTEM
}

/**
 * Notification entity
 */
@Entity(tableName = "notifications")
@Parcelize
data class Notification(
    @PrimaryKey
    @DocumentId
    val id: String = UUID.randomUUID().toString(),
    
    /**
     * ID of the user who will receive the notification
     */
    val userId: String = "",
    
    /**
     * Type of notification
     */
    val type: NotificationType = NotificationType.SYSTEM,
    
    /**
     * Notification title
     */
    val title: String = "",
    
    /**
     * Notification content
     */
    val content: String = "",
    
    /**
     * ID of the user related to the notification (if applicable)
     */
    val relatedUserId: String? = null,
    
    /**
     * ID of the chat related to the notification (if applicable)
     */
    val chatId: String? = null,
    
    /**
     * ID of the match related to the notification (if applicable)
     */
    val matchId: String? = null,
    
    /**
     * Custom data for the notification
     */
    val data: Map<String, String> = emptyMap(),
    
    /**
     * Whether the notification has been read
     */
    val isRead: Boolean = false,
    
    /**
     * When the notification was read
     */
    val readAt: Timestamp? = null,
    
    /**
     * Timestamp when the notification was created
     */
    val createdAt: Timestamp = Timestamp.now()
) : Parcelable {
    
    /**
     * Mark this notification as read
     */
    fun markAsRead(): Notification {
        if (isRead) {
            return this
        }
        return copy(isRead = true, readAt = Timestamp.now())
    }
    
    /**
     * Get notification deep link
     */
    fun getDeepLink(): String? {
        return when (type) {
            NotificationType.MATCH -> matchId?.let { "onelove://matches/$it" }
            NotificationType.MESSAGE -> chatId?.let { "onelove://chat/$it" }
            NotificationType.CALL_MISSED -> relatedUserId?.let { "onelove://call-history" }
            NotificationType.VERIFICATION_APPROVED -> "onelove://profile/verification"
            NotificationType.SUBSCRIPTION_EXPIRING -> "onelove://subscription"
            NotificationType.PAYMENT_SUCCESS, 
            NotificationType.PAYMENT_FAILED -> "onelove://payment-history"
            NotificationType.PROFILE_VIEW -> relatedUserId?.let { "onelove://profile/$it" }
            NotificationType.APP_UPDATE -> "onelove://settings"
            NotificationType.SYSTEM -> null
        }
    }
    
    /**
     * Get notification icon resource name
     */
    fun getIconResourceName(): String {
        return when (type) {
            NotificationType.MATCH -> "ic_notification_match"
            NotificationType.MESSAGE -> "ic_notification_message"
            NotificationType.CALL_MISSED -> "ic_notification_call_missed"
            NotificationType.VERIFICATION_APPROVED -> "ic_notification_verified"
            NotificationType.SUBSCRIPTION_EXPIRING -> "ic_notification_subscription"
            NotificationType.PAYMENT_SUCCESS -> "ic_notification_payment_success"
            NotificationType.PAYMENT_FAILED -> "ic_notification_payment_failed"
            NotificationType.PROFILE_VIEW -> "ic_notification_profile_view"
            NotificationType.APP_UPDATE -> "ic_notification_update"
            NotificationType.SYSTEM -> "ic_notification_system"
        }
    }
}