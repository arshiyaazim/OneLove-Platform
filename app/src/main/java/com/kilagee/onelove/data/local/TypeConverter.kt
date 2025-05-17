package com.kilagee.onelove.data.local

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.kilagee.onelove.data.model.GeoPoint
import com.kilagee.onelove.data.model.NotificationSettings
import com.kilagee.onelove.data.model.PrivacySettings
import com.kilagee.onelove.data.model.SubscriptionType
import com.kilagee.onelove.data.model.UserGender
import com.kilagee.onelove.data.model.VerificationStatus

/**
 * Type converters for Room database
 */
class TypeConverter {
    private val gson = Gson()
    
    // String Lists
    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return gson.toJson(value)
    }
    
    @TypeConverter
    fun toStringList(value: String): List<String> {
        val listType = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, listType) ?: emptyList()
    }
    
    // GeoPoint
    @TypeConverter
    fun fromGeoPoint(geoPoint: GeoPoint?): String? {
        return geoPoint?.let { gson.toJson(it) }
    }
    
    @TypeConverter
    fun toGeoPoint(value: String?): GeoPoint? {
        return value?.let {
            gson.fromJson(it, GeoPoint::class.java)
        }
    }
    
    // UserGender
    @TypeConverter
    fun fromUserGender(gender: UserGender): String {
        return gender.name
    }
    
    @TypeConverter
    fun toUserGender(value: String): UserGender {
        return try {
            UserGender.valueOf(value)
        } catch (e: Exception) {
            UserGender.OTHER
        }
    }
    
    // UserGender List
    @TypeConverter
    fun fromUserGenderList(genders: List<UserGender>): String {
        return gson.toJson(genders.map { it.name })
    }
    
    @TypeConverter
    fun toUserGenderList(value: String): List<UserGender> {
        val listType = object : TypeToken<List<String>>() {}.type
        val stringList: List<String> = gson.fromJson(value, listType) ?: emptyList()
        return stringList.map {
            try {
                UserGender.valueOf(it)
            } catch (e: Exception) {
                UserGender.OTHER
            }
        }
    }
    
    // VerificationStatus
    @TypeConverter
    fun fromVerificationStatus(status: VerificationStatus): String {
        return status.name
    }
    
    @TypeConverter
    fun toVerificationStatus(value: String): VerificationStatus {
        return try {
            VerificationStatus.valueOf(value)
        } catch (e: Exception) {
            VerificationStatus.UNVERIFIED
        }
    }
    
    // SubscriptionType
    @TypeConverter
    fun fromSubscriptionType(type: SubscriptionType): String {
        return type.name
    }
    
    @TypeConverter
    fun toSubscriptionType(value: String): SubscriptionType {
        return try {
            SubscriptionType.valueOf(value)
        } catch (e: Exception) {
            SubscriptionType.FREE
        }
    }
    
    // NotificationSettings
    @TypeConverter
    fun fromNotificationSettings(settings: NotificationSettings): String {
        return gson.toJson(settings)
    }
    
    @TypeConverter
    fun toNotificationSettings(value: String): NotificationSettings {
        return gson.fromJson(value, NotificationSettings::class.java)
    }
    
    // PrivacySettings
    @TypeConverter
    fun fromPrivacySettings(settings: PrivacySettings): String {
        return gson.toJson(settings)
    }
    
    @TypeConverter
    fun toPrivacySettings(value: String): PrivacySettings {
        return gson.fromJson(value, PrivacySettings::class.java)
    }
}