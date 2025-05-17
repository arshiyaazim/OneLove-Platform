package com.kilagee.onelove.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.google.firebase.Timestamp
import com.kilagee.onelove.data.local.converter.DateConverter
import com.kilagee.onelove.data.local.converter.MapConverter
import com.kilagee.onelove.data.model.Message
import com.kilagee.onelove.data.model.MessageType
import java.util.Date

/**
 * Room Entity class for Message
 */
@Entity(tableName = "messages")
@TypeConverters(
    DateConverter::class,
    MapConverter::class
)
data class MessageEntity(
    @PrimaryKey
    val id: String,
    
    // Chat and participant info
    val chatId: String,
    val senderId: String,
    val receiverId: String,
    val senderName: String,
    val senderPhotoUrl: String?,
    
    // Message content
    val content: String,
    val messageType: String, // Storage as string enum name for easier querying
    val mediaUrl: String?,
    val mediaThumbnail: String?,
    val mediaWidth: Int?,
    val mediaHeight: Int?,
    val mediaDuration: Long?,
    val mediaSize: Long?,
    
    // Reference data
    val replyToMessageId: String?,
    val referenceData: Map<String, String>?,
    
    // Status flags
    val isRead: Boolean,
    val isDelivered: Boolean,
    val isSending: Boolean,
    val isDeleted: Boolean,
    val isEdited: Boolean,
    val isAI: Boolean,
    
    // Timestamps
    val createdAt: Long?,
    val updatedAt: Long?,
    val readAt: Long?,
    val deliveredAt: Long?,
    
    // Additional data
    val metadata: Map<String, String>
) {
    /**
     * Convert MessageEntity to Message model
     */
    fun toMessage(): Message {
        return Message(
            id = id,
            chatId = chatId,
            senderId = senderId,
            receiverId = receiverId,
            senderName = senderName,
            senderPhotoUrl = senderPhotoUrl,
            content = content,
            messageType = MessageType.valueOf(messageType),
            mediaUrl = mediaUrl,
            mediaThumbnail = mediaThumbnail,
            mediaWidth = mediaWidth,
            mediaHeight = mediaHeight,
            mediaDuration = mediaDuration,
            mediaSize = mediaSize,
            replyToMessageId = replyToMessageId,
            referenceData = referenceData?.mapValues { it.value as Any },
            isRead = isRead,
            isDelivered = isDelivered,
            isSending = isSending,
            isDeleted = isDeleted,
            isEdited = isEdited,
            isAI = isAI,
            createdAt = createdAt?.let { Timestamp(Date(it)) },
            updatedAt = updatedAt?.let { Timestamp(Date(it)) },
            readAt = readAt?.let { Timestamp(Date(it)) },
            deliveredAt = deliveredAt?.let { Timestamp(Date(it)) },
            metadata = metadata.mapValues { it.value as Any }
        )
    }
    
    companion object {
        /**
         * Convert Message model to MessageEntity
         */
        fun fromMessage(message: Message): MessageEntity {
            return MessageEntity(
                id = message.id,
                chatId = message.chatId,
                senderId = message.senderId,
                receiverId = message.receiverId,
                senderName = message.senderName,
                senderPhotoUrl = message.senderPhotoUrl,
                content = message.content,
                messageType = message.messageType.name,
                mediaUrl = message.mediaUrl,
                mediaThumbnail = message.mediaThumbnail,
                mediaWidth = message.mediaWidth,
                mediaHeight = message.mediaHeight,
                mediaDuration = message.mediaDuration,
                mediaSize = message.mediaSize,
                replyToMessageId = message.replyToMessageId,
                referenceData = message.referenceData?.mapValues { it.value.toString() },
                isRead = message.isRead,
                isDelivered = message.isDelivered,
                isSending = message.isSending,
                isDeleted = message.isDeleted,
                isEdited = message.isEdited,
                isAI = message.isAI,
                createdAt = message.createdAt?.toDate()?.time,
                updatedAt = message.updatedAt?.toDate()?.time,
                readAt = message.readAt?.toDate()?.time,
                deliveredAt = message.deliveredAt?.toDate()?.time,
                metadata = message.metadata.mapValues { it.value.toString() }
            )
        }
    }
}