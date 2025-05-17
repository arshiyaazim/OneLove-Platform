package com.kilagee.onelove.data.local.converter

import androidx.room.TypeConverter
import com.google.firebase.firestore.GeoPoint
import com.google.gson.Gson
import com.kilagee.onelove.data.model.GeoLocation

/**
 * Room TypeConverter for GeoLocation class
 * Converts between GeoLocation and String using JSON serialization
 */
class GeoLocationConverter {
    private val gson = Gson()
    
    @TypeConverter
    fun fromGeoLocation(location: GeoLocation?): String {
        return gson.toJson(location ?: GeoLocation())
    }
    
    @TypeConverter
    fun toGeoLocation(value: String?): GeoLocation {
        if (value.isNullOrEmpty()) return GeoLocation()
        return try {
            gson.fromJson(value, GeoLocation::class.java) ?: GeoLocation()
        } catch (e: Exception) {
            GeoLocation()
        }
    }
    
    @TypeConverter
    fun fromGeoPoint(geoPoint: GeoPoint?): String {
        val location = geoPoint?.let {
            mapOf("latitude" to it.latitude, "longitude" to it.longitude)
        } ?: mapOf("latitude" to 0.0, "longitude" to 0.0)
        return gson.toJson(location)
    }
    
    @TypeConverter
    fun toGeoPoint(value: String?): GeoPoint {
        if (value.isNullOrEmpty()) return GeoPoint(0.0, 0.0)
        return try {
            val map = gson.fromJson(value, Map::class.java)
            val latitude = (map["latitude"] as? Double) ?: 0.0
            val longitude = (map["longitude"] as? Double) ?: 0.0
            GeoPoint(latitude, longitude)
        } catch (e: Exception) {
            GeoPoint(0.0, 0.0)
        }
    }
}