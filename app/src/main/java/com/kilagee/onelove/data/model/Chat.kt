package com.kilagee.onelove.data.model

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import kotlinx.parcelize.Parcelize
import java.util.UUID

/**
 * Chat entity representing a conversation between two or more users
 */
@Entity(tableName = "chats")
@Parcelize
data class Chat(
    @PrimaryKey
    @DocumentId
    val id: String = UUID.randomUUID().toString(),
    
    /**
     * List of participant user IDs
     */
    val participantIds: List<String> = emptyList(),
    
    /**
     * Last message in the chat
     */
    val lastMessage: String? = null,
    
    /**
     * ID of the last message
     */
    val lastMessageId: String? = null,
    
    /**
     * ID of the user who sent the last message
     */
    val lastMessageSenderId: String? = null,
    
    /**
     * Type of the last message
     */
    val lastMessageType: String? = null,
    
    /**
     * Timestamp of the last message
     */
    val lastMessageTimestamp: Timestamp? = null,
    
    /**
     * Name of the chat (for group chats)
     */
    val name: String? = null,
    
    /**
     * URL of the chat avatar (for group chats)
     */
    val avatarUrl: String? = null,
    
    /**
     * Whether the chat is a group chat
     */
    val isGroupChat: Boolean = false,
    
    /**
     * Map of user IDs to their unread message counts
     */
    val unreadCounts: Map<String, Int> = emptyMap(),
    
    /**
     * Map of user IDs to their typing status
     */
    val typingStatus: Map<String, Boolean> = emptyMap(),
    
    /**
     * Whether the chat is pinned
     */
    val isPinned: Boolean = false,
    
    /**
     * Whether the chat is muted
     */
    val isMuted: Boolean = false,
    
    /**
     * Custom metadata for the chat
     */
    val metadata: Map<String, String> = emptyMap(),
    
    /**
     * Timestamp when the chat was created
     */
    val createdAt: Timestamp = Timestamp.now(),
    
    /**
     * Timestamp when the chat was last updated
     */
    val updatedAt: Timestamp = Timestamp.now()
) : Parcelable {
    
    /**
     * Get the other participant in a one-on-one chat
     * 
     * @param currentUserId ID of the current user
     * @return ID of the other participant or null if not a one-on-one chat
     */
    fun getOtherParticipantId(currentUserId: String): String? {
        if (isGroupChat || participantIds.size != 2) {
            return null
        }
        
        return participantIds.firstOrNull { it != currentUserId }
    }
    
    /**
     * Get the unread count for a user
     * 
     * @param userId ID of the user
     * @return Number of unread messages
     */
    fun getUnreadCount(userId: String): Int {
        return unreadCounts[userId] ?: 0
    }
    
    /**
     * Check if a user is typing
     * 
     * @param userId ID of the user
     * @return True if the user is typing
     */
    fun isUserTyping(userId: String): Boolean {
        return typingStatus[userId] == true
    }
    
    /**
     * Get a list of typing user IDs
     * 
     * @return List of user IDs who are currently typing
     */
    fun getTypingUserIds(): List<String> {
        return typingStatus.filter { it.value }.keys.toList()
    }
    
    /**
     * Get chat display name based on whether it's a group chat or not
     * 
     * @param otherUserName Name of the other participant in a one-on-one chat
     * @return Display name for the chat
     */
    fun getDisplayName(otherUserName: String?): String {
        return when {
            isGroupChat && !name.isNullOrBlank() -> name
            !isGroupChat && otherUserName != null -> otherUserName
            else -> "Chat"
        }
    }
}