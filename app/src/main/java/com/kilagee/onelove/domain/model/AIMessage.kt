package com.kilagee.onelove.domain.model

import java.util.UUID

/**
 * Represents a message in an AI chat conversation
 */
data class AIMessage(
    val id: String = UUID.randomUUID().toString(),
    val conversationId: String = "",
    val sender: MessageSender = MessageSender.AI,
    val content: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val responseType: AIResponseType = AIResponseType.NORMAL
) {
    /**
     * Converts this message to a map for Firestore
     */
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "conversationId" to conversationId,
            "sender" to sender.name,
            "content" to content,
            "timestamp" to timestamp,
            "isRead" to isRead,
            "responseType" to responseType.name
        )
    }
    
    companion object {
        /**
         * Creates an AIMessage from a map
         */
        fun fromMap(map: Map<String, Any?>): AIMessage {
            return AIMessage(
                id = map["id"] as? String ?: UUID.randomUUID().toString(),
                conversationId = map["conversationId"] as? String ?: "",
                sender = try {
                    MessageSender.valueOf(map["sender"] as? String ?: MessageSender.AI.name)
                } catch (e: Exception) {
                    MessageSender.AI
                },
                content = map["content"] as? String ?: "",
                timestamp = (map["timestamp"] as? Long) ?: System.currentTimeMillis(),
                isRead = map["isRead"] as? Boolean ?: false,
                responseType = try {
                    AIResponseType.valueOf(map["responseType"] as? String ?: AIResponseType.NORMAL.name)
                } catch (e: Exception) {
                    AIResponseType.NORMAL
                }
            )
        }
    }
}

/**
 * Enum representing the sender of a message
 */
enum class MessageSender {
    USER,
    AI
}

/**
 * Enum representing the type of AI response
 */
enum class AIResponseType {
    NORMAL,
    FLIRTY,
    ROMANTIC,
    FUNNY,
    SUPPORTIVE,
    CURIOUS,
    EXCITED,
    DEEP
}