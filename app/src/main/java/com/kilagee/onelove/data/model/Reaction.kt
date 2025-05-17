package com.kilagee.onelove.data.model

import android.os.Parcelable
import com.google.firebase.Timestamp
import kotlinx.parcelize.Parcelize
import java.util.Date

/**
 * Data class representing a reaction
 */
@Parcelize
data class Reaction(
    val id: String = "",
    val userId: String = "",
    val targetId: String = "", // ID of the target (message, profile, etc.)
    val targetType: ReactionTargetType = ReactionTargetType.MESSAGE,
    val type: ReactionType = ReactionType.LIKE,
    val timestamp: Date = Date(),
    val metadata: Map<String, String> = emptyMap()
) : Parcelable {
    
    /**
     * Convert to a map for Firestore
     */
    fun toMap(): Map<String, Any?> {
        val result = mutableMapOf<String, Any?>()
        result["id"] = id
        result["userId"] = userId
        result["targetId"] = targetId
        result["targetType"] = targetType.name
        result["type"] = type.name
        result["timestamp"] = Timestamp(timestamp)
        result["metadata"] = metadata
        return result
    }
    
    companion object {
        /**
         * Create from Firestore document
         */
        fun fromMap(map: Map<String, Any?>, id: String): Reaction {
            return Reaction(
                id = id,
                userId = map["userId"] as? String ?: "",
                targetId = map["targetId"] as? String ?: "",
                targetType = try {
                    ReactionTargetType.valueOf(map["targetType"] as? String ?: ReactionTargetType.MESSAGE.name)
                } catch (e: IllegalArgumentException) {
                    ReactionTargetType.MESSAGE
                },
                type = try {
                    ReactionType.valueOf(map["type"] as? String ?: ReactionType.LIKE.name)
                } catch (e: IllegalArgumentException) {
                    ReactionType.LIKE
                },
                timestamp = (map["timestamp"] as? Timestamp)?.toDate() ?: Date(),
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
 * Enum for reaction target types
 */
enum class ReactionTargetType {
    MESSAGE,
    PROFILE,
    POST,
    COMMENT,
    AI_MESSAGE
}

/**
 * Enum for reaction types
 */
enum class ReactionType(val emoji: String, val label: String) {
    LIKE("❤️", "Like"),
    LOVE("😍", "Love"),
    LAUGH("😂", "Laugh"),
    WOW("😮", "Wow"),
    SAD("😢", "Sad"),
    ANGRY("😡", "Angry"),
    THINKING("🤔", "Thinking"),
    CELEBRATE("🎉", "Celebrate"),
    FIRE("🔥", "Fire"),
    THUMBS_UP("👍", "Thumbs Up"),
    THUMBS_DOWN("👎", "Thumbs Down"),
    CLAP("👏", "Clap"),
    SPARKLE("✨", "Sparkle"),
    EYES("👀", "Eyes"),
    HEART_EYES("😍", "Heart Eyes"),
    CRYING("😭", "Crying"),
    BLUSH("😊", "Blush"),
    WINK("😉", "Wink"),
    KISS("😘", "Kiss"),
    COOL("😎", "Cool"),
    SHOCKED("😱", "Shocked"),
    NERVOUS("😅", "Nervous"),
    SMIRK("😏", "Smirk");
    
    companion object {
        // Get a reaction type from emoji
        fun fromEmoji(emoji: String): ReactionType? {
            return values().find { it.emoji == emoji }
        }
        
        // Get common reactions (subset of all reactions)
        fun getCommonReactions(): List<ReactionType> {
            return listOf(LIKE, LOVE, LAUGH, WOW, SAD, ANGRY)
        }
    }
}

/**
 * Data class representing a reaction summary (count of each type)
 */
@Parcelize
data class ReactionSummary(
    val targetId: String = "",
    val counts: Map<ReactionType, Int> = emptyMap(),
    val total: Int = 0,
    val userReaction: ReactionType? = null
) : Parcelable {
    
    /**
     * Convert to a map for Firestore
     */
    fun toMap(): Map<String, Any?> {
        val result = mutableMapOf<String, Any?>()
        result["targetId"] = targetId
        result["counts"] = counts.mapKeys { it.key.name }
        result["total"] = total
        result["userReaction"] = userReaction?.name
        return result
    }
    
    companion object {
        /**
         * Create from Firestore document
         */
        fun fromMap(map: Map<String, Any?>, id: String): ReactionSummary {
            val countsMap = (map["counts"] as? Map<*, *>)?.mapNotNull { 
                try {
                    val type = ReactionType.valueOf(it.key as? String ?: return@mapNotNull null)
                    val count = (it.value as? Number)?.toInt() ?: 0
                    type to count
                } catch (e: IllegalArgumentException) {
                    null
                }
            }?.toMap() ?: emptyMap()
            
            val userReaction = try {
                (map["userReaction"] as? String)?.let { ReactionType.valueOf(it) }
            } catch (e: IllegalArgumentException) {
                null
            }
            
            return ReactionSummary(
                targetId = id,
                counts = countsMap,
                total = (map["total"] as? Number)?.toInt() ?: 0,
                userReaction = userReaction
            )
        }
    }
}