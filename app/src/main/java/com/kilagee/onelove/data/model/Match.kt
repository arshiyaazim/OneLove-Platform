package com.kilagee.onelove.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Match status enum
 */
enum class MatchStatus {
    PENDING, // Match request sent, waiting for response
    ACTIVE,  // Both users have matched
    DECLINED, // Match request declined
    EXPIRED, // Match request expired
    BLOCKED, // Match blocked by one of the users
    UNMATCHED // Users have unmatched
}

/**
 * Match data model
 */
@Entity(tableName = "matches")
data class Match(
    @PrimaryKey
    val id: String,
    val userId: String, // The initiator of the match
    val matchedUserId: String, // The matched user
    val status: MatchStatus = MatchStatus.PENDING,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date(),
    val lastMessageText: String? = null,
    val lastMessageTimestamp: Date? = null,
    val unreadCount: Int = 0,
    val isNew: Boolean = true, // Whether the match is new (unread)
    val isFavorite: Boolean = false, // Whether the match is marked as favorite
    val compatibility: Float = 0f, // Compatibility score (0-1)
    val matchNote: String? = null // Private note added by the user
)