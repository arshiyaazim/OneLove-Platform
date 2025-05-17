package com.kilagee.onelove.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.Date

/**
 * Data class representing a match recommendation
 */
@Parcelize
data class MatchRecommendation(
    val id: String,
    val recommendedUser: User,
    val compatibilityScore: Float, // 0 to 1
    val reasons: List<RecommendationReason> = emptyList(),
    val aiSuggestion: String? = null,
    val recommendedAt: Date = Date(),
    val boostApplied: Boolean = false,
    val priority: Int = 0, // Higher number means higher priority
    val isTopPick: Boolean = false,
    val commonInterests: List<String> = emptyList(),
    val commonConnections: List<String> = emptyList(), // IDs of mutual friends/connections
    val commonPlaces: List<String> = emptyList() // Names of common places visited
) : Parcelable

/**
 * Data class representing a reason for a match recommendation
 */
@Parcelize
data class RecommendationReason(
    val type: String, // "INTEREST", "LOCATION", "COMMON_FRIEND", "SIMILAR_PROFILE", etc.
    val description: String,
    val score: Float, // 0 to 1
    val details: Map<String, String> = emptyMap()
) : Parcelable

/**
 * Data class representing a match between two users
 */
@Parcelize
data class Match(
    val id: String,
    val userIds: List<String>, // Usually 2 users
    val createdAt: Date,
    val lastActivity: Date? = null,
    val matchStatus: MatchStatus = MatchStatus.ACTIVE,
    val matchType: MatchType = MatchType.LIKE,
    val hasUnreadMessages: Map<String, Boolean> = emptyMap(), // userId -> hasUnread
    val compatibilityScore: Float? = null, // 0 to 1
    val mutualInterests: List<String> = emptyList()
) : Parcelable

/**
 * Enum representing match status
 */
@Parcelize
enum class MatchStatus : Parcelable {
    ACTIVE,
    PAUSED,
    EXPIRED,
    UNMATCHED,
    BLOCKED
}

/**
 * Enum representing match type
 */
@Parcelize
enum class MatchType : Parcelable {
    LIKE,
    SUPER_LIKE,
    AI_MATCH
}