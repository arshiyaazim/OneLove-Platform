package com.kilagee.onelove.data.model

import android.os.Parcelable
import com.google.firebase.Timestamp
import kotlinx.parcelize.Parcelize
import java.util.Date

/**
 * Data class representing an AI profile
 */
@Parcelize
data class AIProfile(
    val id: String = "",
    val name: String = "",
    val personalityType: String = "",
    val description: String = "",
    val imageUrl: String? = null,
    val traits: List<String> = emptyList(),
    val interests: List<String> = emptyList(),
    val backstory: String = "",
    val voiceType: String? = null,
    val popularity: Int = 0,
    val creatorId: String? = null,
    val premium: Boolean = false,
    val accessLevel: AccessLevel = AccessLevel.FREE,
    val isActive: Boolean = true,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date(),
    val metadata: Map<String, String> = emptyMap()
) : Parcelable {
    
    /**
     * Convert to a map for Firestore
     */
    fun toMap(): Map<String, Any?> {
        val result = mutableMapOf<String, Any?>()
        result["id"] = id
        result["name"] = name
        result["personalityType"] = personalityType
        result["description"] = description
        result["imageUrl"] = imageUrl
        result["traits"] = traits
        result["interests"] = interests
        result["backstory"] = backstory
        result["voiceType"] = voiceType
        result["popularity"] = popularity
        result["creatorId"] = creatorId
        result["premium"] = premium
        result["accessLevel"] = accessLevel.name
        result["isActive"] = isActive
        result["createdAt"] = Timestamp(createdAt)
        result["updatedAt"] = Timestamp(updatedAt)
        result["metadata"] = metadata
        return result
    }
    
    companion object {
        /**
         * Create from Firestore document
         */
        fun fromMap(map: Map<String, Any?>, id: String): AIProfile {
            return AIProfile(
                id = id,
                name = map["name"] as? String ?: "",
                personalityType = map["personalityType"] as? String ?: "",
                description = map["description"] as? String ?: "",
                imageUrl = map["imageUrl"] as? String,
                traits = (map["traits"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                interests = (map["interests"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                backstory = map["backstory"] as? String ?: "",
                voiceType = map["voiceType"] as? String,
                popularity = (map["popularity"] as? Number)?.toInt() ?: 0,
                creatorId = map["creatorId"] as? String,
                premium = map["premium"] as? Boolean ?: false,
                accessLevel = try {
                    AccessLevel.valueOf(map["accessLevel"] as? String ?: AccessLevel.FREE.name)
                } catch (e: IllegalArgumentException) {
                    AccessLevel.FREE
                },
                isActive = map["isActive"] as? Boolean ?: true,
                createdAt = (map["createdAt"] as? Timestamp)?.toDate() ?: Date(),
                updatedAt = (map["updatedAt"] as? Timestamp)?.toDate() ?: Date(),
                metadata = (map["metadata"] as? Map<*, *>)?.mapNotNull { 
                    if (it.key is String && it.value is String) {
                        it.key as String to it.value as String
                    } else {
                        null
                    }
                }?.toMap() ?: emptyMap()
            )
        }
    }
}

/**
 * Enum for AI profile access levels
 */
enum class AccessLevel {
    FREE,
    BASIC,
    PREMIUM,
    VIP
}

/**
 * Data class representing an AI message
 */
@Parcelize
data class AIMessage(
    val id: String = "",
    val profileId: String = "",
    val interactionId: String = "",
    val content: String = "",
    val timestamp: Date = Date(),
    val isFromUser: Boolean = false,
    val metadata: Map<String, String> = emptyMap()
) : Parcelable {
    
    /**
     * Convert to a map for Firestore
     */
    fun toMap(): Map<String, Any?> {
        val result = mutableMapOf<String, Any?>()
        result["id"] = id
        result["profileId"] = profileId
        result["interactionId"] = interactionId
        result["content"] = content
        result["timestamp"] = Timestamp(timestamp)
        result["isFromUser"] = isFromUser
        result["metadata"] = metadata
        return result
    }
    
    companion object {
        /**
         * Create from Firestore document
         */
        fun fromMap(map: Map<String, Any?>, id: String): AIMessage {
            return AIMessage(
                id = id,
                profileId = map["profileId"] as? String ?: "",
                interactionId = map["interactionId"] as? String ?: "",
                content = map["content"] as? String ?: "",
                timestamp = (map["timestamp"] as? Timestamp)?.toDate() ?: Date(),
                isFromUser = map["isFromUser"] as? Boolean ?: false,
                metadata = (map["metadata"] as? Map<*, *>)?.mapNotNull { 
                    if (it.key is String && it.value is String) {
                        it.key as String to it.value as String
                    } else {
                        null
                    }
                }?.toMap() ?: emptyMap()
            )
        }
    }
}

/**
 * Data class representing an AI interaction
 */
@Parcelize
data class AIInteraction(
    val id: String = "",
    val profileId: String = "",
    val userId: String = "",
    val lastActive: Date = Date(),
    val messageCount: Int = 0,
    val createdAt: Date = Date(),
    val metadata: Map<String, String> = emptyMap()
) : Parcelable {
    
    /**
     * Convert to a map for Firestore
     */
    fun toMap(): Map<String, Any?> {
        val result = mutableMapOf<String, Any?>()
        result["id"] = id
        result["profileId"] = profileId
        result["userId"] = userId
        result["lastActive"] = Timestamp(lastActive)
        result["messageCount"] = messageCount
        result["createdAt"] = Timestamp(createdAt)
        result["metadata"] = metadata
        return result
    }
    
    companion object {
        /**
         * Create from Firestore document
         */
        fun fromMap(map: Map<String, Any?>, id: String): AIInteraction {
            return AIInteraction(
                id = id,
                profileId = map["profileId"] as? String ?: "",
                userId = map["userId"] as? String ?: "",
                lastActive = (map["lastActive"] as? Timestamp)?.toDate() ?: Date(),
                messageCount = (map["messageCount"] as? Number)?.toInt() ?: 0,
                createdAt = (map["createdAt"] as? Timestamp)?.toDate() ?: Date(),
                metadata = (map["metadata"] as? Map<*, *>)?.mapNotNull { 
                    if (it.key is String && it.value is String) {
                        it.key as String to it.value as String
                    } else {
                        null
                    }
                }?.toMap() ?: emptyMap()
            )
        }
    }
}