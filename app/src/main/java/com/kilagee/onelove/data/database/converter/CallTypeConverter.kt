package com.kilagee.onelove.data.database.converter

import androidx.room.TypeConverter
import com.kilagee.onelove.data.model.CallType

/**
 * Type converter for CallType enum
 */
class CallTypeConverter {
    @TypeConverter
    fun fromCallType(callType: CallType?): String? {
        return callType?.name
    }
    
    @TypeConverter
    fun toCallType(callTypeString: String?): CallType? {
        return callTypeString?.let {
            try {
                CallType.valueOf(it)
            } catch (e: IllegalArgumentException) {
                CallType.AUDIO
            }
        }
    }
}