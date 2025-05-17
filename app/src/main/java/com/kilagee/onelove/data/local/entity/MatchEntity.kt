package com.kilagee.onelove.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.google.firebase.Timestamp
import com.kilagee.onelove.data.local.converter.DateConverter
import com.kilagee.onelove.data.local.converter.ListConverter
import com.kilagee.onelove.data.local.converter.MapConverter
import com.kilagee.onelove.data.model.Match
import com.kilagee.onelove.data.model.MatchStatus
import java.util.Date

/**
 * Room Entity class for Match
 */
@Entity(tableName = "matches")
@TypeConverters(
    DateConverter::class,
    ListConverter::class,
    MapConverter::class
)
data class MatchEntity(
    @PrimaryKey
    val id: String,
    
    // User IDs
    val userId1: String,
    val userId2: String,
    
    // User info
    val user1Name: String,
    val user2Name: String,
    val user1PhotoUrl: String?,
    val user2PhotoUrl: String?,
    
    // Match state
    val status: String, // Store enum as string
    val user1LikedUser2: Boolean,
    val user2LikedUser1: Boolean,
    val user1RejectedUser2: Boolean,
    val user2RejectedUser1: Boolean,
    
    // Communication
    val chatId: String?,
    val lastMessageTime: Long?,
    val hasActiveOffers: Boolean,
    
    // Matching reason
    val matchScore: Double,
    val matchReasons: List<String>,
    val commonInterests: List<String>,
    
    // Timestamps
    val createdAt: Long?,
    val updatedAt: Long?,
    val matchedAt: Long?,
    val unmatchedAt: Long?,
    
    // Notification status
    val user1Notified: Boolean,
    val user2Notified: Boolean,
    
    // Additional data
    val metadata: Map<String, String>
) {
    /**
     * Convert MatchEntity to Match model
     */
    fun toMatch(): Match {
        return Match(
            id = id,
            userId1 = userId1,
            userId2 = userId2,
            user1Name = user1Name,
            user2Name = user2Name,
            user1PhotoUrl = user1PhotoUrl,
            user2PhotoUrl = user2PhotoUrl,
            status = MatchStatus.valueOf(status),
            user1LikedUser2 = user1LikedUser2,
            user2LikedUser1 = user2LikedUser1,
            user1RejectedUser2 = user1RejectedUser2,
            user2RejectedUser1 = user2RejectedUser1,
            chatId = chatId,
            lastMessageTime = lastMessageTime?.let { Timestamp(Date(it)) },
            hasActiveOffers = hasActiveOffers,
            matchScore = matchScore,
            matchReasons = matchReasons,
            commonInterests = commonInterests,
            createdAt = createdAt?.let { Timestamp(Date(it)) },
            updatedAt = updatedAt?.let { Timestamp(Date(it)) },
            matchedAt = matchedAt?.let { Timestamp(Date(it)) },
            unmatchedAt = unmatchedAt?.let { Timestamp(Date(it)) },
            user1Notified = user1Notified,
            user2Notified = user2Notified,
            metadata = metadata.mapValues { it.value as Any }
        )
    }
    
    companion object {
        /**
         * Convert Match model to MatchEntity
         */
        fun fromMatch(match: Match): MatchEntity {
            return MatchEntity(
                id = match.id,
                userId1 = match.userId1,
                userId2 = match.userId2,
                user1Name = match.user1Name,
                user2Name = match.user2Name,
                user1PhotoUrl = match.user1PhotoUrl,
                user2PhotoUrl = match.user2PhotoUrl,
                status = match.status.name,
                user1LikedUser2 = match.user1LikedUser2,
                user2LikedUser1 = match.user2LikedUser1,
                user1RejectedUser2 = match.user1RejectedUser2,
                user2RejectedUser1 = match.user2RejectedUser1,
                chatId = match.chatId,
                lastMessageTime = match.lastMessageTime?.toDate()?.time,
                hasActiveOffers = match.hasActiveOffers,
                matchScore = match.matchScore,
                matchReasons = match.matchReasons,
                commonInterests = match.commonInterests,
                createdAt = match.createdAt?.toDate()?.time,
                updatedAt = match.updatedAt?.toDate()?.time,
                matchedAt = match.matchedAt?.toDate()?.time,
                unmatchedAt = match.unmatchedAt?.toDate()?.time,
                user1Notified = match.user1Notified,
                user2Notified = match.user2Notified,
                metadata = match.metadata.mapValues { it.value.toString() }
            )
        }
    }
}