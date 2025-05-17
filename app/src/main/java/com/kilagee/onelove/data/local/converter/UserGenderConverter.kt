package com.kilagee.onelove.data.local.converter

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.kilagee.onelove.data.model.UserGender

/**
 * Room TypeConverter for UserGender enum
 * Converts between UserGender and String
 */
class UserGenderConverter {
    private val gson = Gson()
    
    @TypeConverter
    fun fromUserGender(value: UserGender?): String {
        return value?.name ?: UserGender.PREFER_NOT_TO_SAY.name
    }
    
    @TypeConverter
    fun toUserGender(value: String?): UserGender {
        return if (value.isNullOrEmpty()) {
            UserGender.PREFER_NOT_TO_SAY
        } else {
            try {
                UserGender.valueOf(value)
            } catch (e: IllegalArgumentException) {
                UserGender.PREFER_NOT_TO_SAY
            }
        }
    }
    
    @TypeConverter
    fun fromUserGenderList(value: List<UserGender>?): String {
        return gson.toJson(value?.map { it.name } ?: emptyList<String>())
    }
    
    @TypeConverter
    fun toUserGenderList(value: String?): List<UserGender> {
        if (value.isNullOrEmpty()) return emptyList()
        val listType = object : TypeToken<List<String>>() {}.type
        return (gson.fromJson<List<String>>(value, listType) ?: emptyList())
            .mapNotNull {
                try {
                    UserGender.valueOf(it)
                } catch (e: IllegalArgumentException) {
                    null
                }
            }
    }
}