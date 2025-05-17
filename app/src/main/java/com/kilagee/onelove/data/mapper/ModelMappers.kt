package com.kilagee.onelove.data.mapper

import com.kilagee.onelove.data.model.AIProfile as DataAIProfile
import com.kilagee.onelove.data.model.User as DataUser
import com.kilagee.onelove.domain.model.AIProfile as DomainAIProfile
import com.kilagee.onelove.domain.model.User as DomainUser
import java.util.Date

/**
 * Mapper functions to convert between data and domain models
 * This helps maintain consistency between layer representations
 */
object ModelMappers {

    /**
     * Map data layer User to domain layer User
     */
    fun mapDataUserToDomain(user: DataUser): DomainUser {
        return DomainUser(
            id = user.id,
            displayName = user.displayName,
            email = user.email,
            profilePhotoUrl = user.profilePhotoUrl,
            coverPhotoUrl = user.coverPhotoUrl,
            bio = user.bio,
            gender = user.gender.name,
            birthDate = user.birthDate,
            photos = user.photos,
            interests = user.interests,
            location = user.location,
            isOnline = user.isOnline,
            lastActive = user.lastActive,
            isVerified = user.isVerified,
            isPremium = user.isPremium,
            createdAt = user.createdAt
        )
    }

    /**
     * Map domain layer User to data layer User
     */
    fun mapDomainUserToData(user: DomainUser): DataUser {
        return DataUser(
            id = user.id,
            displayName = user.displayName,
            email = user.email,
            profilePhotoUrl = user.profilePhotoUrl,
            coverPhotoUrl = user.coverPhotoUrl,
            bio = user.bio,
            gender = DataUser.Gender.valueOf(user.gender),
            birthDate = user.birthDate,
            photos = user.photos,
            interests = user.interests,
            location = user.location,
            isOnline = user.isOnline,
            lastActive = user.lastActive,
            isVerified = user.isVerified,
            isPremium = user.isPremium,
            createdAt = user.createdAt,
            // Default values for other fields not present in domain model
            phoneNumber = "",
            genderPreference = listOf(),
            languages = listOf("en"),
            occupation = "",
            education = "",
            verificationStatus = DataUser.VerificationStatus.UNVERIFIED,
            extraData = emptyMap(),
            blockedUsers = emptyList(),
            subscriptionTier = DataUser.SubscriptionTier.FREE,
            updatedAt = Date(),
            metrics = DataUser.UserMetrics(),
            settings = DataUser.UserSettings()
        )
    }

    /**
     * Map data layer AIProfile to domain layer AIProfile
     */
    fun mapDataAIProfileToDomain(profile: DataAIProfile): DomainAIProfile {
        return DomainAIProfile(
            id = profile.id,
            name = profile.name,
            gender = profile.gender,
            age = profile.age,
            bio = profile.bio,
            description = profile.description,
            personalityType = profile.personalityType.name,
            interests = profile.interests,
            traits = profile.traits,
            occupation = profile.occupation,
            profilePhotoUrl = profile.profilePhotoUrl,
            galleryPhotos = profile.galleryPhotos,
            isPremiumOnly = profile.isPremiumOnly,
            category = profile.category,
            tags = profile.tags,
            popularityScore = profile.popularityScore,
            interactionCount = profile.interactionCount,
            averageRating = profile.averageRating,
            createdAt = profile.createdAt,
            updatedAt = profile.updatedAt
        )
    }

    /**
     * Map domain layer AIProfile to data layer AIProfile
     */
    fun mapDomainAIProfileToData(profile: DomainAIProfile): DataAIProfile {
        return DataAIProfile(
            id = profile.id,
            name = profile.name,
            gender = profile.gender,
            age = profile.age,
            bio = profile.bio,
            description = profile.description,
            personalityType = DataAIProfile.PersonalityType.valueOf(profile.personalityType),
            interests = profile.interests,
            traits = profile.traits,
            occupation = profile.occupation,
            profilePhotoUrl = profile.profilePhotoUrl,
            galleryPhotos = profile.galleryPhotos,
            isPremiumOnly = profile.isPremiumOnly,
            category = profile.category,
            tags = profile.tags,
            popularityScore = profile.popularityScore,
            interactionCount = profile.interactionCount,
            averageRating = profile.averageRating,
            createdAt = profile.createdAt,
            updatedAt = profile.updatedAt,
            // Default values for other fields not in domain model
            background = "",
            voiceUrl = null,
            behaviors = emptyList(),
            greetings = emptyList(),
            farewells = emptyList(),
            questions = emptyList(),
            responses = emptyMap(),
            icebreakers = emptyList(),
            isActive = true,
            ratingCount = 0
        )
    }
}