package com.kilagee.onelove.domain.model

import java.util.Date

/**
 * Domain model for User
 */
data class User(
    val id: String = "",
    val displayName: String = "",
    val email: String = "",
    val profilePhotoUrl: String? = null,
    val coverPhotoUrl: String? = null,
    val bio: String = "",
    val gender: String = "",
    val birthDate: Date? = null,
    val photos: List<String> = emptyList(),
    val interests: List<String> = emptyList(),
    val location: Map<String, Double> = emptyMap(),
    val isOnline: Boolean = false,
    val lastActive: Date? = null,
    val isVerified: Boolean = false,
    val isPremium: Boolean = false,
    val createdAt: Date = Date(),
)