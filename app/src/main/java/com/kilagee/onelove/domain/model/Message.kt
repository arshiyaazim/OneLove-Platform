package com.kilagee.onelove.domain.model

import java.util.Date

/**
 * Domain model for Message
 */
data class Message(
    val id: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val content: String = "",
    val timestamp: Date = Date(),
    val isRead: Boolean = false,
    val attachmentUrls: List<String> = emptyList(),
    val messageType: String = "TEXT"
)