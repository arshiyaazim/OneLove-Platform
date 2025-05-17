package com.kilagee.onelove.data.model

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import kotlinx.parcelize.Parcelize
import java.util.UUID

/**
 * Types of user interactions
 */
enum class InteractionType {
    PROFILE_VIEW,
    LIKE,
    UNLIKE,
    MATCH,
    UNMATCH,
    MESSAGE_SENT,
    MESSAGE_READ,
    CALL_INITIATED,
    CALL_ANSWERED,
    CALL_DECLINED,
    CALL_MISSED,
    BLOCK,
    UNBLOCK,
    REPORT,
    SUBSCRIPTION_PURCHASED,
    COIN_PURCHASE,
    FEATURE_USED,
    APP_OPEN,
    APP_CLOSE,
    SEARCH,
    FILTER_CHANGE,
    LOCATION_UPDATE,
    PROFILE_UPDATE,
    SETTINGS_CHANGE
}

/**
 * User interaction entity for tracking all interactions within the app
 */
@Entity(tableName = "user_interactions")
@Parcelize
data class UserInteraction(
    @PrimaryKey
    @DocumentId
    val id: String = UUID.randomUUID().toString(),
    
    /**
     * ID of the user performing the interaction
     */
    val userId: String = "",
    
    /**
     * ID of the target user (if applicable)
     */
    val targetUserId: String? = null,
    
    /**
     * Type of interaction
     */
    val type: InteractionType = InteractionType.PROFILE_VIEW,
    
    /**
     * Additional details about the interaction
     */
    val details: Map<String, String> = emptyMap(),
    
    /**
     * The screen or location in the app where the interaction occurred
     */
    val location: String? = null,
    
    /**
     * Whether the interaction was performed by an AI (e.g., recommendation system)
     */
    val isAutomated: Boolean = false,
    
    /**
     * Duration of the interaction in seconds (if applicable)
     */
    val durationSeconds: Long? = null,
    
    /**
     * Session ID for grouping interactions
     */
    val sessionId: String? = null,
    
    /**
     * Timestamp when the interaction occurred
     */
    val timestamp: Timestamp = Timestamp.now()
) : Parcelable {
    
    /**
     * Check if this is a positive interaction
     */
    fun isPositiveInteraction(): Boolean {
        return when (type) {
            InteractionType.LIKE,
            InteractionType.MATCH,
            InteractionType.MESSAGE_SENT,
            InteractionType.MESSAGE_READ,
            InteractionType.CALL_ANSWERED,
            InteractionType.SUBSCRIPTION_PURCHASED,
            InteractionType.COIN_PURCHASE,
            InteractionType.FEATURE_USED -> true
            else -> false
        }
    }
    
    /**
     * Check if this is a negative interaction
     */
    fun isNegativeInteraction(): Boolean {
        return when (type) {
            InteractionType.UNLIKE,
            InteractionType.UNMATCH,
            InteractionType.CALL_DECLINED,
            InteractionType.CALL_MISSED,
            InteractionType.BLOCK,
            InteractionType.REPORT -> true
            else -> false
        }
    }
    
    /**
     * Check if this is a monetization-related interaction
     */
    fun isMonetizationInteraction(): Boolean {
        return when (type) {
            InteractionType.SUBSCRIPTION_PURCHASED,
            InteractionType.COIN_PURCHASE,
            InteractionType.FEATURE_USED -> true
            else -> false
        }
    }
    
    /**
     * Check if this is a communication interaction
     */
    fun isCommunicationInteraction(): Boolean {
        return when (type) {
            InteractionType.MESSAGE_SENT,
            InteractionType.MESSAGE_READ,
            InteractionType.CALL_INITIATED,
            InteractionType.CALL_ANSWERED,
            InteractionType.CALL_DECLINED,
            InteractionType.CALL_MISSED -> true
            else -> false
        }
    }
    
    /**
     * Get duration formatted as mm:ss
     */
    fun getFormattedDuration(): String? {
        val seconds = durationSeconds ?: return null
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return "%02d:%02d".format(minutes, remainingSeconds)
    }
}