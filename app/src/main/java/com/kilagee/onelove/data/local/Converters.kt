package com.kilagee.onelove.data.local

import androidx.room.TypeConverter
import com.kilagee.onelove.data.model.MatchStatus
import com.kilagee.onelove.data.model.MessageReaction
import java.util.Date

/**
 * Type converters for Room Database
 */
class Converters {
    
    /**
     * Convert Date to Long timestamp
     */
    @TypeConverter
    fun fromDate(date: Date?): Long? {
        return date?.time
    }
    
    /**
     * Convert Long timestamp to Date
     */
    @TypeConverter
    fun toDate(timestamp: Long?): Date? {
        return timestamp?.let { Date(it) }
    }
    
    /**
     * Convert MessageReaction to String
     */
    @TypeConverter
    fun fromMessageReaction(reaction: MessageReaction?): String? {
        return reaction?.name
    }
    
    /**
     * Convert String to MessageReaction
     */
    @TypeConverter
    fun toMessageReaction(reactionName: String?): MessageReaction? {
        return reactionName?.let { 
            try {
                MessageReaction.valueOf(it)
            } catch (e: IllegalArgumentException) {
                null
            }
        }
    }
    
    /**
     * Convert MatchStatus to String
     */
    @TypeConverter
    fun fromMatchStatus(status: MatchStatus?): String? {
        return status?.name
    }
    
    /**
     * Convert String to MatchStatus
     */
    @TypeConverter
    fun toMatchStatus(statusName: String?): MatchStatus? {
        return statusName?.let { 
            try {
                MatchStatus.valueOf(it)
            } catch (e: IllegalArgumentException) {
                MatchStatus.PENDING
            }
        }
    }
}