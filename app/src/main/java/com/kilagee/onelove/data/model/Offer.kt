package com.kilagee.onelove.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.Date

/**
 * Offer model representing an offer between users
 */
@Parcelize
data class Offer(
    val id: String = "",
    val matchId: String = "",
    val senderId: String = "",
    val recipientId: String = "",
    val type: OfferType = OfferType.DATE,
    val title: String = "",
    val description: String = "",
    val pointsAmount: Int = 0,
    val status: OfferStatus = OfferStatus.PENDING,
    val expiresAt: Date? = null,
    val statusUpdatedAt: Date? = null,
    val statusUpdatedBy: String = "",
    val rejectionReason: String = "",
    val location: GeoPoint? = null,
    val locationName: String = "",
    val scheduledDate: Date? = null,
    val attachments: List<String> = emptyList(), // URLs to images or other attachments
    val createdAt: Date = Date(),
    val updatedAt: Date = Date(),
    val isCounterOffer: Boolean = false,
    val originalOfferId: String = "",
    val terms: List<OfferTerm> = emptyList(),
    val signedBySender: Boolean = false,
    val signedByRecipient: Boolean = false,
    val metadata: Map<String, String> = emptyMap()
) : Parcelable

/**
 * Offer type enum
 */
@Parcelize
enum class OfferType : Parcelable {
    DATE,
    GIFT,
    ACTIVITY,
    VIRTUAL_DATE,
    LESSON,
    CUSTOM,
    TASK,
    SERVICE
}

/**
 * Offer status enum
 */
@Parcelize
enum class OfferStatus : Parcelable {
    PENDING,
    ACCEPTED,
    REJECTED,
    EXPIRED,
    CANCELED,
    COMPLETED,
    DISPUTED
}

/**
 * Offer term model
 */
@Parcelize
data class OfferTerm(
    val id: String = "",
    val offerId: String = "",
    val description: String = "",
    val isRequired: Boolean = true,
    val isCompleted: Boolean = false,
    val completedAt: Date? = null,
    val completedBy: String = "",
    val order: Int = 0
) : Parcelable

/**
 * Offer activity model
 */
@Parcelize
data class OfferActivity(
    val id: String = "",
    val offerId: String = "",
    val userId: String = "",
    val activityType: OfferActivityType = OfferActivityType.VIEW,
    val createdAt: Date = Date(),
    val metadata: Map<String, String> = emptyMap()
) : Parcelable

/**
 * Offer activity type enum
 */
@Parcelize
enum class OfferActivityType : Parcelable {
    VIEW,
    CREATE,
    ACCEPT,
    REJECT,
    CANCEL,
    COMPLETE,
    EXPIRE,
    MODIFY,
    COUNTER,
    COMMENT,
    SIGN
}

/**
 * Offer template model
 */
@Parcelize
data class OfferTemplate(
    val id: String = "",
    val type: OfferType = OfferType.DATE,
    val title: String = "",
    val description: String = "",
    val defaultPointsAmount: Int = 0,
    val terms: List<String> = emptyList(),
    val isSystem: Boolean = false,
    val createdBy: String = "",
    val createdAt: Date = Date(),
    val categoryId: String = "",
    val order: Int = 0,
    val isActive: Boolean = true
) : Parcelable

/**
 * Offer template category model
 */
@Parcelize
data class OfferTemplateCategory(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val iconUrl: String = "",
    val order: Int = 0,
    val isActive: Boolean = true,
    val createdAt: Date = Date()
) : Parcelable