package com.kilagee.onelove.data.model

import android.os.Parcelable
import com.google.firebase.Timestamp
import kotlinx.parcelize.Parcelize
import java.util.Date

/**
 * Data class representing a chat message
 */
@Parcelize
data class ChatMessage(
    val id: String = "",
    val chatId: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val content: String = "",
    val type: MessageType = MessageType.TEXT,
    val timestamp: Date = Date(),
    val status: MessageStatus = MessageStatus.SENDING,
    val deleted: Boolean = false,
    val edited: Boolean = false,
    val readAt: Date? = null,
    val deliveredAt: Date? = null,
    val metadata: Map<String, String> = emptyMap(),
    val replyToMessageId: String? = null,
    val caption: String? = null,
    val localFilePath: String? = null,
    val downloadProgress: Int = 0,
    val uploadProgress: Int = 0
) : Parcelable {
    
    /**
     * Convert to a map for Firestore
     */
    fun toMap(): Map<String, Any?> {
        val result = mutableMapOf<String, Any?>()
        result["id"] = id
        result["chatId"] = chatId
        result["senderId"] = senderId
        result["receiverId"] = receiverId
        result["content"] = content
        result["type"] = type.name
        result["timestamp"] = Timestamp(timestamp)
        result["status"] = status.name
        result["deleted"] = deleted
        result["edited"] = edited
        result["readAt"] = readAt?.let { Timestamp(it) }
        result["deliveredAt"] = deliveredAt?.let { Timestamp(it) }
        result["metadata"] = metadata
        result["replyToMessageId"] = replyToMessageId
        result["caption"] = caption
        return result
    }
    
    companion object {
        /**
         * Create from Firestore document
         */
        fun fromMap(map: Map<String, Any?>, id: String): ChatMessage {
            return ChatMessage(
                id = id,
                chatId = map["chatId"] as? String ?: "",
                senderId = map["senderId"] as? String ?: "",
                receiverId = map["receiverId"] as? String ?: "",
                content = map["content"] as? String ?: "",
                type = try {
                    MessageType.valueOf(map["type"] as? String ?: MessageType.TEXT.name)
                } catch (e: IllegalArgumentException) {
                    MessageType.TEXT
                },
                timestamp = (map["timestamp"] as? Timestamp)?.toDate() ?: Date(),
                status = try {
                    MessageStatus.valueOf(map["status"] as? String ?: MessageStatus.SENDING.name)
                } catch (e: IllegalArgumentException) {
                    MessageStatus.SENDING
                },
                deleted = map["deleted"] as? Boolean ?: false,
                edited = map["edited"] as? Boolean ?: false,
                readAt = (map["readAt"] as? Timestamp)?.toDate(),
                deliveredAt = (map["deliveredAt"] as? Timestamp)?.toDate(),
                metadata = (map["metadata"] as? Map<*, *>)?.mapNotNull { 
                    if (it.key is String && it.value is String) {
                        it.key as String to it.value as String
                    } else {
                        null
                    }
                }?.toMap() ?: emptyMap(),
                replyToMessageId = map["replyToMessageId"] as? String,
                caption = map["caption"] as? String
            )
        }
    }
}

/**
 * Enum representing the type of message
 */
enum class MessageType {
    TEXT,
    IMAGE,
    VIDEO,
    AUDIO,
    FILE,
    LOCATION,
    CONTACT,
    STICKER,
    GIF,
    VOICE,
    SYSTEM
}

/**
 * Enum representing the status of a message
 */
enum class MessageStatus {
    SENDING,
    SENT,
    DELIVERED,
    READ,
    FAILED
}

/**
 * Data class representing a chat thread
 */
@Parcelize
data class ChatThread(
    val id: String = "",
    val participants: List<String> = emptyList(),
    val lastMessage: ChatMessage? = null,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date(),
    val metadata: Map<String, String> = emptyMap(),
    val unreadCount: Map<String, Int> = emptyMap(),
    val typingUsers: List<String> = emptyList(),
    val muted: Boolean = false,
    val blocked: Boolean = false,
    val pinned: Boolean = false,
    val archived: Boolean = false,
    val isGroup: Boolean = false,
    val groupName: String? = null,
    val groupImage: String? = null,
    val admin: String? = null
) : Parcelable {
    
    /**
     * Convert to a map for Firestore
     */
    fun toMap(): Map<String, Any?> {
        val result = mutableMapOf<String, Any?>()
        result["id"] = id
        result["participants"] = participants
        result["lastMessage"] = lastMessage?.toMap()
        result["createdAt"] = Timestamp(createdAt)
        result["updatedAt"] = Timestamp(updatedAt)
        result["metadata"] = metadata
        result["unreadCount"] = unreadCount
        result["typingUsers"] = typingUsers
        result["muted"] = muted
        result["blocked"] = blocked
        result["pinned"] = pinned
        result["archived"] = archived
        result["isGroup"] = isGroup
        result["groupName"] = groupName
        result["groupImage"] = groupImage
        result["admin"] = admin
        return result
    }
    
    companion object {
        /**
         * Create from Firestore document
         */
        fun fromMap(map: Map<String, Any?>, id: String): ChatThread {
            val lastMessageMap = map["lastMessage"] as? Map<String, Any?>
            
            return ChatThread(
                id = id,
                participants = (map["participants"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                lastMessage = lastMessageMap?.let { ChatMessage.fromMap(it, lastMessageMap["id"] as? String ?: "") },
                createdAt = (map["createdAt"] as? Timestamp)?.toDate() ?: Date(),
                updatedAt = (map["updatedAt"] as? Timestamp)?.toDate() ?: Date(),
                metadata = (map["metadata"] as? Map<*, *>)?.mapNotNull { 
                    if (it.key is String && it.value is String) {
                        it.key as String to it.value as String
                    } else {
                        null
                    }
                }?.toMap() ?: emptyMap(),
                unreadCount = (map["unreadCount"] as? Map<*, *>)?.mapNotNull { 
                    if (it.key is String && it.value is Number) {
                        it.key as String to (it.value as Number).toInt()
                    } else {
                        null
                    }
                }?.toMap() ?: emptyMap(),
                typingUsers = (map["typingUsers"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                muted = map["muted"] as? Boolean ?: false,
                blocked = map["blocked"] as? Boolean ?: false,
                pinned = map["pinned"] as? Boolean ?: false,
                archived = map["archived"] as? Boolean ?: false,
                isGroup = map["isGroup"] as? Boolean ?: false,
                groupName = map["groupName"] as? String,
                groupImage = map["groupImage"] as? String,
                admin = map["admin"] as? String
            )
        }
    }
}