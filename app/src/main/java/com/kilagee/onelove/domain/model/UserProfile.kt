package com.kilagee.onelove.domain.model

/**
 * Data class representing a user profile in the app
 */
data class UserProfile(
    val id: String = "",
    val displayName: String = "",
    val email: String = "",
    val phoneNumber: String? = null,
    val photoUrl: String? = null,
    val bio: String? = null,
    val birthDate: Long? = null,
    val gender: String? = null,
    val interestedIn: List<String> = emptyList(),
    val location: GeoLocation? = null,
    val interests: List<String> = emptyList(),
    val photos: List<String> = emptyList(),
    val isVerified: Boolean = false,
    val verificationLevel: Int = 0,
    val isPremium: Boolean = false,
    val premiumTier: String? = null,
    val premiumExpiresAt: Long? = null,
    val isOnline: Boolean = false,
    val lastActive: Long? = null,
    val points: Int = 0,
    val matchesCount: Int = 0,
    val offersCount: Int = 0,
    val completionPercentage: Int = 0,
    val isBanned: Boolean = false,
    val banReason: String? = null,
    val isDeleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "displayName" to displayName,
            "email" to email,
            "phoneNumber" to phoneNumber,
            "photoUrl" to photoUrl,
            "bio" to bio,
            "birthDate" to birthDate,
            "gender" to gender,
            "interestedIn" to interestedIn,
            "location" to location?.toMap(),
            "interests" to interests,
            "photos" to photos,
            "isVerified" to isVerified,
            "verificationLevel" to verificationLevel,
            "isPremium" to isPremium,
            "premiumTier" to premiumTier,
            "premiumExpiresAt" to premiumExpiresAt,
            "isOnline" to isOnline,
            "lastActive" to lastActive,
            "points" to points,
            "matchesCount" to matchesCount,
            "offersCount" to offersCount,
            "completionPercentage" to completionPercentage,
            "isBanned" to isBanned,
            "banReason" to banReason,
            "isDeleted" to isDeleted,
            "createdAt" to createdAt,
            "updatedAt" to updatedAt
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any?>): UserProfile {
            val locationMap = map["location"] as? Map<String, Any?>
            val location = if (locationMap != null) GeoLocation.fromMap(locationMap) else null

            @Suppress("UNCHECKED_CAST")
            return UserProfile(
                id = map["id"] as? String ?: "",
                displayName = map["displayName"] as? String ?: "",
                email = map["email"] as? String ?: "",
                phoneNumber = map["phoneNumber"] as? String,
                photoUrl = map["photoUrl"] as? String,
                bio = map["bio"] as? String,
                birthDate = map["birthDate"] as? Long,
                gender = map["gender"] as? String,
                interestedIn = (map["interestedIn"] as? List<String>) ?: emptyList(),
                location = location,
                interests = (map["interests"] as? List<String>) ?: emptyList(),
                photos = (map["photos"] as? List<String>) ?: emptyList(),
                isVerified = map["isVerified"] as? Boolean ?: false,
                verificationLevel = (map["verificationLevel"] as? Number)?.toInt() ?: 0,
                isPremium = map["isPremium"] as? Boolean ?: false,
                premiumTier = map["premiumTier"] as? String,
                premiumExpiresAt = map["premiumExpiresAt"] as? Long,
                isOnline = map["isOnline"] as? Boolean ?: false,
                lastActive = map["lastActive"] as? Long,
                points = (map["points"] as? Number)?.toInt() ?: 0,
                matchesCount = (map["matchesCount"] as? Number)?.toInt() ?: 0,
                offersCount = (map["offersCount"] as? Number)?.toInt() ?: 0,
                completionPercentage = (map["completionPercentage"] as? Number)?.toInt() ?: 0,
                isBanned = map["isBanned"] as? Boolean ?: false,
                banReason = map["banReason"] as? String,
                isDeleted = map["isDeleted"] as? Boolean ?: false,
                createdAt = (map["createdAt"] as? Long) ?: System.currentTimeMillis(),
                updatedAt = (map["updatedAt"] as? Long) ?: System.currentTimeMillis()
            )
        }
    }
}

/**
 * Data class representing a geographical location
 */
data class GeoLocation(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val city: String? = null,
    val state: String? = null,
    val country: String? = null,
    val formattedAddress: String? = null
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "latitude" to latitude,
            "longitude" to longitude,
            "city" to city,
            "state" to state,
            "country" to country,
            "formattedAddress" to formattedAddress
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any?>): GeoLocation {
            return GeoLocation(
                latitude = (map["latitude"] as? Number)?.toDouble() ?: 0.0,
                longitude = (map["longitude"] as? Number)?.toDouble() ?: 0.0,
                city = map["city"] as? String,
                state = map["state"] as? String,
                country = map["country"] as? String,
                formattedAddress = map["formattedAddress"] as? String
            )
        }
    }
}