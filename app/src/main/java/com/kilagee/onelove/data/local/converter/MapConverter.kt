package com.kilagee.onelove.data.local.converter

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Room TypeConverter for Map objects
 * Converts between Map and String using JSON serialization
 */
class MapConverter {
    private val gson = Gson()
    
    @TypeConverter
    fun fromStringMap(value: Map<String, String>?): String {
        return gson.toJson(value ?: emptyMap<String, String>())
    }
    
    @TypeConverter
    fun toStringMap(value: String?): Map<String, String> {
        if (value.isNullOrEmpty()) return emptyMap()
        val mapType = object : TypeToken<Map<String, String>>() {}.type
        return gson.fromJson(value, mapType) ?: emptyMap()
    }
    
    @TypeConverter
    fun fromIntMap(value: Map<String, Int>?): String {
        return gson.toJson(value ?: emptyMap<String, Int>())
    }
    
    @TypeConverter
    fun toIntMap(value: String?): Map<String, Int> {
        if (value.isNullOrEmpty()) return emptyMap()
        val mapType = object : TypeToken<Map<String, Int>>() {}.type
        return gson.fromJson(value, mapType) ?: emptyMap()
    }
    
    @TypeConverter
    fun fromDoubleMap(value: Map<String, Double>?): String {
        return gson.toJson(value ?: emptyMap<String, Double>())
    }
    
    @TypeConverter
    fun toDoubleMap(value: String?): Map<String, Double> {
        if (value.isNullOrEmpty()) return emptyMap()
        val mapType = object : TypeToken<Map<String, Double>>() {}.type
        return gson.fromJson(value, mapType) ?: emptyMap()
    }
    
    @TypeConverter
    fun fromBooleanMap(value: Map<String, Boolean>?): String {
        return gson.toJson(value ?: emptyMap<String, Boolean>())
    }
    
    @TypeConverter
    fun toBooleanMap(value: String?): Map<String, Boolean> {
        if (value.isNullOrEmpty()) return emptyMap()
        val mapType = object : TypeToken<Map<String, Boolean>>() {}.type
        return gson.fromJson(value, mapType) ?: emptyMap()
    }
    
    @TypeConverter
    fun fromAnyMap(value: Map<String, Any>?): String {
        return gson.toJson(value ?: emptyMap<String, Any>())
    }
    
    @TypeConverter
    fun toAnyMap(value: String?): Map<String, Any> {
        if (value.isNullOrEmpty()) return emptyMap()
        val mapType = object : TypeToken<Map<String, Any>>() {}.type
        return gson.fromJson(value, mapType) ?: emptyMap()
    }
    
    @TypeConverter
    fun fromStringListMap(value: Map<String, List<String>>?): String {
        return gson.toJson(value ?: emptyMap<String, List<String>>())
    }
    
    @TypeConverter
    fun toStringListMap(value: String?): Map<String, List<String>> {
        if (value.isNullOrEmpty()) return emptyMap()
        val mapType = object : TypeToken<Map<String, List<String>>>() {}.type
        return gson.fromJson(value, mapType) ?: emptyMap()
    }
}