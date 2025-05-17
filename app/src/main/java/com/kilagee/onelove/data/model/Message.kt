package com.kilagee.onelove.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Message data model
 */
@Entity(tableName = "messages")
data class Message(
    @PrimaryKey
    val id: String,
    val matchId: String,
    val senderId: String,
    val receiverId: String,
    val text: String,
    val timestamp: Date = Date(),
    val isRead: Boolean = false,
    val isDeleted: Boolean = false,
    val reaction: MessageReaction? = null,
    val attachmentUrl: String? = null,
    val attachmentType: AttachmentType? = null
)

/**
 * Message reaction enum
 */
enum class MessageReaction {
    LOVE,
    LAUGH,
    WOW,
    SAD,
    ANGRY,
    LIKE,
    DISLIKE
}

/**
 * Attachment type enum
 */
enum class AttachmentType {
    IMAGE,
    VIDEO,
    AUDIO,
    DOCUMENT,
    LOCATION
}