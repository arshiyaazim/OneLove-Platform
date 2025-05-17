package com.kilagee.onelove.domain.model

import java.util.Date

/**
 * Domain model for AI Profile
 */
data class AIProfile(
    val id: String = "",
    val name: String = "",
    val gender: String = "",
    val age: Int = 0,
    val bio: String = "",
    val description: String = "",
    val personalityType: String = "",
    val interests: List<String> = emptyList(),
    val traits: List<String> = emptyList(),
    val occupation: String = "",
    val profilePhotoUrl: String = "",
    val galleryPhotos: List<String> = emptyList(),
    val isPremiumOnly: Boolean = false,
    val category: String = "",
    val tags: List<String> = emptyList(),
    val popularityScore: Double = 0.0,
    val interactionCount: Int = 0,
    val averageRating: Double = 0.0,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date()
)