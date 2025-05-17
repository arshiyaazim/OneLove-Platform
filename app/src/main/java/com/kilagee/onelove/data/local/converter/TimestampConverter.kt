package com.kilagee.onelove.data.local.converter

import androidx.room.TypeConverter
import com.google.firebase.Timestamp
import java.util.Date

/**
 * Room TypeConverter for Firestore Timestamp objects
 * Converts between Timestamp and Long (timestamp in milliseconds)
 */
class TimestampConverter {
    
    @TypeConverter
    fun fromTimestamp(value: Long?): Timestamp? {
        return value?.let { Timestamp(it / 1000, ((it % 1000) * 1000000).toInt()) }
    }
    
    @TypeConverter
    fun timestampToLong(timestamp: Timestamp?): Long? {
        return timestamp?.toDate()?.time
    }
    
    @TypeConverter
    fun dateToTimestamp(date: Date?): Timestamp? {
        return date?.let { Timestamp(it) }
    }
    
    @TypeConverter
    fun timestampToDate(timestamp: Timestamp?): Date? {
        return timestamp?.toDate()
    }
}