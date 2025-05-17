package com.kilagee.onelove.data.database.converter

import androidx.room.TypeConverter
import com.kilagee.onelove.data.model.NotificationType

/**
 * Type converter for NotificationType enum
 */
class NotificationTypeConverter {
    @TypeConverter
    fun fromNotificationType(notificationType: NotificationType?): String? {
        return notificationType?.name
    }
    
    @TypeConverter
    fun toNotificationType(notificationTypeString: String?): NotificationType? {
        return notificationTypeString?.let {
            try {
                NotificationType.valueOf(it)
            } catch (e: IllegalArgumentException) {
                NotificationType.SYSTEM
            }
        }
    }
}