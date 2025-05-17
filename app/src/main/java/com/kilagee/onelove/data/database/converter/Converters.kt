package com.kilagee.onelove.data.database.converter

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.kilagee.onelove.data.model.CallStatus
import com.kilagee.onelove.data.model.CallType
import com.kilagee.onelove.data.model.NotificationActionType
import com.kilagee.onelove.data.model.NotificationType
import java.util.Date

/**
 * Type converter for Date objects
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

/**
 * Type converter for CallType enum
 */
class CallTypeConverter {
    @TypeConverter
    fun fromCallType(value: CallType): String {
        return value.name
    }
    
    @TypeConverter
    fun toCallType(value: String): CallType {
        return enumValueOf(value)
    }
}

/**
 * Type converter for CallStatus enum
 */
class CallStatusConverter {
    @TypeConverter
    fun fromCallStatus(value: CallStatus): String {
        return value.name
    }
    
    @TypeConverter
    fun toCallStatus(value: String): CallStatus {
        return enumValueOf(value)
    }
}

/**
 * Type converter for NotificationType enum
 */
class NotificationTypeConverter {
    @TypeConverter
    fun fromNotificationType(value: NotificationType): String {
        return value.name
    }
    
    @TypeConverter
    fun toNotificationType(value: String): NotificationType {
        return enumValueOf(value)
    }
}

/**
 * Type converter for NotificationActionType enum
 */
class NotificationActionTypeConverter {
    @TypeConverter
    fun fromNotificationActionType(value: NotificationActionType): String {
        return value.name
    }
    
    @TypeConverter
    fun toNotificationActionType(value: String): NotificationActionType {
        return enumValueOf(value)
    }
}

/**
 * Type converter for List<String>
 */
class ListConverter {
    private val gson = Gson()
    
    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return gson.toJson(value)
    }
    
    @TypeConverter
    fun toStringList(value: String): List<String> {
        val listType = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, listType)
    }
}