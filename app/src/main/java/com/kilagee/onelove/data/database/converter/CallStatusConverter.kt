package com.kilagee.onelove.data.database.converter

import androidx.room.TypeConverter
import com.kilagee.onelove.data.model.CallStatus

/**
 * Type converter for CallStatus enum
 */
class CallStatusConverter {
    @TypeConverter
    fun fromCallStatus(callStatus: CallStatus?): String? {
        return callStatus?.name
    }
    
    @TypeConverter
    fun toCallStatus(callStatusString: String?): CallStatus? {
        return callStatusString?.let {
            try {
                CallStatus.valueOf(it)
            } catch (e: IllegalArgumentException) {
                CallStatus.FAILED
            }
        }
    }
}