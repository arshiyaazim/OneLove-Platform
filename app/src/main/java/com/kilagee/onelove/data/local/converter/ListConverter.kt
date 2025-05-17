package com.kilagee.onelove.data.local.converter

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Room TypeConverter for List objects
 * Converts between List and String using JSON serialization
 */
class ListConverter {
    private val gson = Gson()
    
    @TypeConverter
    fun fromStringList(value: List<String>?): String {
        return gson.toJson(value ?: emptyList<String>())
    }
    
    @TypeConverter
    fun toStringList(value: String?): List<String> {
        if (value.isNullOrEmpty()) return emptyList()
        val listType = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, listType) ?: emptyList()
    }
    
    @TypeConverter
    fun fromIntList(value: List<Int>?): String {
        return gson.toJson(value ?: emptyList<Int>())
    }
    
    @TypeConverter
    fun toIntList(value: String?): List<Int> {
        if (value.isNullOrEmpty()) return emptyList()
        val listType = object : TypeToken<List<Int>>() {}.type
        return gson.fromJson(value, listType) ?: emptyList()
    }
    
    @TypeConverter
    fun fromLongList(value: List<Long>?): String {
        return gson.toJson(value ?: emptyList<Long>())
    }
    
    @TypeConverter
    fun toLongList(value: String?): List<Long> {
        if (value.isNullOrEmpty()) return emptyList()
        val listType = object : TypeToken<List<Long>>() {}.type
        return gson.fromJson(value, listType) ?: emptyList()
    }
    
    @TypeConverter
    fun fromDoubleList(value: List<Double>?): String {
        return gson.toJson(value ?: emptyList<Double>())
    }
    
    @TypeConverter
    fun toDoubleList(value: String?): List<Double> {
        if (value.isNullOrEmpty()) return emptyList()
        val listType = object : TypeToken<List<Double>>() {}.type
        return gson.fromJson(value, listType) ?: emptyList()
    }
    
    @TypeConverter
    fun fromBooleanList(value: List<Boolean>?): String {
        return gson.toJson(value ?: emptyList<Boolean>())
    }
    
    @TypeConverter
    fun toBooleanList(value: String?): List<Boolean> {
        if (value.isNullOrEmpty()) return emptyList()
        val listType = object : TypeToken<List<Boolean>>() {}.type
        return gson.fromJson(value, listType) ?: emptyList()
    }
}