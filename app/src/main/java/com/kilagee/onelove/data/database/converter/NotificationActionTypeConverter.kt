package com.kilagee.onelove.data.database.converter

import androidx.room.TypeConverter
import com.kilagee.onelove.data.model.NotificationActionType

/**
 * Type converter for NotificationActionType enum
 */
class NotificationActionTypeConverter {
    @TypeConverter
    fun fromNotificationActionType(actionType: NotificationActionType?): String? {
        return actionType?.name
    }
    
    @TypeConverter
    fun toNotificationActionType(actionTypeString: String?): NotificationActionType? {
        return actionTypeString?.let {
            try {
                NotificationActionType.valueOf(it)
            } catch (e: IllegalArgumentException) {
                NotificationActionType.NONE
            }
        }
    }
}