package com.kilagee.onelove.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.google.firebase.Timestamp
import com.kilagee.onelove.data.local.converter.DateConverter
import com.kilagee.onelove.data.local.converter.GeoLocationConverter
import com.kilagee.onelove.data.local.converter.ListConverter
import com.kilagee.onelove.data.local.converter.MapConverter
import com.kilagee.onelove.data.local.converter.TimestampConverter
import com.kilagee.onelove.data.local.converter.UserGenderConverter
import com.kilagee.onelove.data.model.GeoLocation
import com.kilagee.onelove.data.model.SubscriptionTier
import com.kilagee.onelove.data.model.User
import com.kilagee.onelove.data.model.UserGender
import com.kilagee.onelove.data.model.VerificationStatus
import java.util.Date

/**
 * Room Entity class for User
 */
@Entity(tableName = "users")
@TypeConverters(
    DateConverter::class,
    TimestampConverter::class,
    ListConverter::class,
    MapConverter::class,
    UserGenderConverter::class,
    GeoLocationConverter::class
)
data class UserEntity(
    @PrimaryKey
    val id: String,
    
    // Basic info
    val email: String,
    val displayName: String,
    val phoneNumber: String?,
    val bio: String,
    val birthDate: Date?,
    val gender: UserGender,
    val genderPreference: List<UserGender>,
    val location: GeoLocation?,
    val interests: List<String>,
    
    // Profile media
    val profilePhotoUrl: String?,
    val coverPhotoUrl: String?,
    val photos: List<String>,
    
    // Stats and metrics
    val points: Int,
    val matchesCount: Int,
    val likesCount: Int,
    val offersCount: Int,
    
    // Status
    val isOnline: Boolean,
    val isPremium: Boolean,
    val isVerified: Boolean,
    val isLiked: Boolean,
    val isAdmin: Boolean,
    val isBanned: Boolean,
    val verificationStatus: VerificationStatus,
    val subscriptionTier: SubscriptionTier,
    val subscriptionExpiryDate: Date?,
    val verificationDocuments: List<String>,
    
    // Settings
    val showLocation: Boolean,
    val showOnlineStatus: Boolean,
    val notificationEnabled: Boolean,
    val emailNotificationEnabled: Boolean,
    val profileVisibility: Boolean,
    val maxDistanceInKm: Int,
    val minAgePreference: Int,
    val maxAgePreference: Int,
    val language: String,
    
    // Relationships
    val blockedUsers: List<String>,
    
    // Device info
    val fcmTokens: List<String>,
    val lastKnownDevice: String?,
    
    // Timestamps
    val createdAt: Long?,
    val lastActive: Long?,
    
    // Extra fields
    val extraData: Map<String, String>
) {
    /**
     * Convert UserEntity to User model
     */
    fun toUser(): User {
        return User(
            id = id,
            email = email,
            displayName = displayName,
            phoneNumber = phoneNumber,
            bio = bio,
            birthDate = birthDate,
            gender = gender,
            genderPreference = genderPreference,
            location = location,
            interests = interests,
            profilePhotoUrl = profilePhotoUrl,
            coverPhotoUrl = coverPhotoUrl,
            photos = photos,
            points = points,
            matchesCount = matchesCount,
            likesCount = likesCount,
            offersCount = offersCount,
            isOnline = isOnline,
            isPremium = isPremium,
            isVerified = isVerified,
            isLiked = isLiked,
            isAdmin = isAdmin,
            isBanned = isBanned,
            verificationStatus = verificationStatus,
            subscriptionTier = subscriptionTier,
            subscriptionExpiryDate = subscriptionExpiryDate,
            verificationDocuments = verificationDocuments,
            showLocation = showLocation,
            showOnlineStatus = showOnlineStatus,
            notificationEnabled = notificationEnabled,
            emailNotificationEnabled = emailNotificationEnabled,
            profileVisibility = profileVisibility,
            maxDistanceInKm = maxDistanceInKm,
            minAgePreference = minAgePreference,
            maxAgePreference = maxAgePreference,
            language = language,
            blockedUsers = blockedUsers,
            fcmTokens = fcmTokens,
            lastKnownDevice = lastKnownDevice,
            createdAt = createdAt?.let { Timestamp(Date(it)) },
            lastActive = lastActive?.let { Timestamp(Date(it)) },
            extraData = extraData.mapValues { it.value as Any }
        )
    }
    
    companion object {
        /**
         * Convert User model to UserEntity
         */
        fun fromUser(user: User): UserEntity {
            return UserEntity(
                id = user.id,
                email = user.email,
                displayName = user.displayName,
                phoneNumber = user.phoneNumber,
                bio = user.bio,
                birthDate = user.birthDate,
                gender = user.gender,
                genderPreference = user.genderPreference,
                location = user.location,
                interests = user.interests,
                profilePhotoUrl = user.profilePhotoUrl,
                coverPhotoUrl = user.coverPhotoUrl,
                photos = user.photos,
                points = user.points,
                matchesCount = user.matchesCount,
                likesCount = user.likesCount,
                offersCount = user.offersCount,
                isOnline = user.isOnline,
                isPremium = user.isPremium,
                isVerified = user.isVerified,
                isLiked = user.isLiked,
                isAdmin = user.isAdmin,
                isBanned = user.isBanned,
                verificationStatus = user.verificationStatus,
                subscriptionTier = user.subscriptionTier,
                subscriptionExpiryDate = user.subscriptionExpiryDate,
                verificationDocuments = user.verificationDocuments,
                showLocation = user.showLocation,
                showOnlineStatus = user.showOnlineStatus,
                notificationEnabled = user.notificationEnabled,
                emailNotificationEnabled = user.emailNotificationEnabled,
                profileVisibility = user.profileVisibility,
                maxDistanceInKm = user.maxDistanceInKm,
                minAgePreference = user.minAgePreference,
                maxAgePreference = user.maxAgePreference,
                language = user.language,
                blockedUsers = user.blockedUsers,
                fcmTokens = user.fcmTokens,
                lastKnownDevice = user.lastKnownDevice,
                createdAt = user.createdAt?.toDate()?.time,
                lastActive = user.lastActive?.toDate()?.time,
                extraData = user.extraData.mapValues { it.value.toString() }
            )
        }
    }
}