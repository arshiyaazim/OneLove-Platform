package com.kilagee.onelove.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.kilagee.onelove.data.local.Converters
import java.util.Date

/**
 * User data model
 */
@Entity(tableName = "users")
@TypeConverters(Converters::class)
data class User(
    @PrimaryKey
    val id: String,
    val name: String,
    val email: String,
    val profilePictureUrl: String? = null,
    val bio: String? = null,
    val age: Int? = null,
    val gender: String? = null,
    val lookingFor: List<String>? = null,
    val interests: List<String>? = null,
    val location: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val isVerified: Boolean = false,
    val verificationLevel: Int = 0,
    val isPremium: Boolean = false,
    val subscriptionType: String? = null,
    val subscriptionExpiryDate: Date? = null,
    val lastActive: Date? = null,
    val isOnline: Boolean = false,
    val points: Int = 0,
    val isProfileComplete: Boolean = false,
    val minAgePreference: Int? = null,
    val maxAgePreference: Int? = null,
    val maxDistance: Int? = null,
    val createdAt: Date? = null,
    val updatedAt: Date? = null,
    val photos: List<String>? = null,
    val isLikedByMe: Boolean? = null,
    val isAdmin: Boolean = false
)