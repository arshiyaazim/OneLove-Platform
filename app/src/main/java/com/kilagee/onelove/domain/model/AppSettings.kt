package com.kilagee.onelove.domain.model

/**
 * Data class representing app settings that can be modified by admins
 */
data class AppSettings(
    val id: String = "app_settings",
    val appName: String = "OneLove",
    val appVersion: String = "1.0.0",
    val maintenanceMode: Boolean = false,
    val maintenanceMessage: String = "We're performing scheduled maintenance. Please try again later.",
    val enableRegistration: Boolean = true,
    val enableMatching: Boolean = true,
    val enableAIProfiles: Boolean = true,
    val enableCalling: Boolean = true,
    val maxMatchesPerDay: Int = 10,
    val maxOffersPerDay: Int = 3,
    val requireEmailVerification: Boolean = true,
    val requirePhoneVerification: Boolean = false,
    val labelMap: Map<String, String> = defaultLabelMap(),
    val colorMap: Map<String, String> = defaultColorMap(),
    val featuredProfiles: List<String> = emptyList(),
    val verifiedBadgeEnabled: Boolean = true,
    val pointsSystemEnabled: Boolean = true,
    val pointsPerAction: Map<String, Int> = defaultPointsMap(),
    val ageRestriction: Int = 18,
    val updatedAt: Long = System.currentTimeMillis(),
    val updatedBy: String? = null
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "appName" to appName,
            "appVersion" to appVersion,
            "maintenanceMode" to maintenanceMode,
            "maintenanceMessage" to maintenanceMessage,
            "enableRegistration" to enableRegistration,
            "enableMatching" to enableMatching,
            "enableAIProfiles" to enableAIProfiles,
            "enableCalling" to enableCalling,
            "maxMatchesPerDay" to maxMatchesPerDay,
            "maxOffersPerDay" to maxOffersPerDay,
            "requireEmailVerification" to requireEmailVerification,
            "requirePhoneVerification" to requirePhoneVerification,
            "labelMap" to labelMap,
            "colorMap" to colorMap,
            "featuredProfiles" to featuredProfiles,
            "verifiedBadgeEnabled" to verifiedBadgeEnabled,
            "pointsSystemEnabled" to pointsSystemEnabled,
            "pointsPerAction" to pointsPerAction,
            "ageRestriction" to ageRestriction,
            "updatedAt" to updatedAt,
            "updatedBy" to updatedBy
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any?>): AppSettings {
            @Suppress("UNCHECKED_CAST")
            return AppSettings(
                id = map["id"] as? String ?: "app_settings",
                appName = map["appName"] as? String ?: "OneLove",
                appVersion = map["appVersion"] as? String ?: "1.0.0",
                maintenanceMode = map["maintenanceMode"] as? Boolean ?: false,
                maintenanceMessage = map["maintenanceMessage"] as? String 
                    ?: "We're performing scheduled maintenance. Please try again later.",
                enableRegistration = map["enableRegistration"] as? Boolean ?: true,
                enableMatching = map["enableMatching"] as? Boolean ?: true,
                enableAIProfiles = map["enableAIProfiles"] as? Boolean ?: true,
                enableCalling = map["enableCalling"] as? Boolean ?: true,
                maxMatchesPerDay = (map["maxMatchesPerDay"] as? Number)?.toInt() ?: 10,
                maxOffersPerDay = (map["maxOffersPerDay"] as? Number)?.toInt() ?: 3,
                requireEmailVerification = map["requireEmailVerification"] as? Boolean ?: true,
                requirePhoneVerification = map["requirePhoneVerification"] as? Boolean ?: false,
                labelMap = (map["labelMap"] as? Map<String, String>) ?: defaultLabelMap(),
                colorMap = (map["colorMap"] as? Map<String, String>) ?: defaultColorMap(),
                featuredProfiles = (map["featuredProfiles"] as? List<String>) ?: emptyList(),
                verifiedBadgeEnabled = map["verifiedBadgeEnabled"] as? Boolean ?: true,
                pointsSystemEnabled = map["pointsSystemEnabled"] as? Boolean ?: true,
                pointsPerAction = (map["pointsPerAction"] as? Map<String, Int>) ?: defaultPointsMap(),
                ageRestriction = (map["ageRestriction"] as? Number)?.toInt() ?: 18,
                updatedAt = (map["updatedAt"] as? Long) ?: System.currentTimeMillis(),
                updatedBy = map["updatedBy"] as? String
            )
        }

        fun defaultLabelMap(): Map<String, String> {
            return mapOf(
                "btn_match" to "Match",
                "btn_skip" to "Skip",
                "btn_like" to "Like",
                "btn_dislike" to "Dislike",
                "btn_send_offer" to "Send Offer",
                "btn_chat" to "Chat",
                "btn_call" to "Call",
                "btn_video" to "Video",
                "btn_upgrade" to "Upgrade",
                "btn_verify" to "Verify",
                "title_home" to "Home",
                "title_discover" to "Discover",
                "title_matches" to "Matches",
                "title_chat" to "Chat",
                "title_profile" to "Profile",
                "title_settings" to "Settings",
                "title_offers" to "Offers",
                "title_points" to "Points"
            )
        }

        fun defaultColorMap(): Map<String, String> {
            return mapOf(
                "primary" to "#FF6200EE",
                "secondary" to "#FF03DAC5",
                "background" to "#FFFFFFFF",
                "surface" to "#FFFFFFFF",
                "error" to "#FFB00020",
                "on_primary" to "#FFFFFFFF",
                "on_secondary" to "#FF000000",
                "on_background" to "#FF000000",
                "on_surface" to "#FF000000",
                "on_error" to "#FFFFFFFF"
            )
        }

        fun defaultPointsMap(): Map<String, Int> {
            return mapOf(
                "daily_login" to 5,
                "profile_completion" to 20,
                "upload_photo" to 10,
                "match_made" to 5,
                "message_sent" to 1,
                "call_made" to 10,
                "video_call_made" to 15,
                "offer_sent" to 5,
                "offer_accepted" to 10,
                "verification_completed" to 50
            )
        }
    }
}