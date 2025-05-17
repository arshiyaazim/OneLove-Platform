package com.kilagee.onelove.data.local.converter

import androidx.room.TypeConverter
import java.util.Date

/**
 * Room TypeConverter for Date objects
 * Converts between Date and Long (timestamp in milliseconds)
 */
class DateConverter {
    
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }
    
    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
}