package com.kilagee.onelove.domain.model

import android.os.Parcelable
import com.kilagee.onelove.data.model.User
import com.kilagee.onelove.data.model.UserSettings
import kotlinx.parcelize.Parcelize
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Date

/**
 * Domain model for User
 * Represents a user in the domain layer with clean types
 */
@Parcelize
data class UserDomain(
    val id: String,
    val email: String,
    val name: String,
    val birthDate: LocalDate?,
    val age: Int?,
    val gender: Gender,
    val phoneNumber: String,
    val bio: String,
    val occupation: String,
    val education: String,
    val interests: List<String>,
    val photoUrls: List<String>,
    val latitude: Double?,
    val longitude: Double?,
    val lastActive: LocalDateTime?,
    val joinDate: LocalDateTime?,
    val isPremium: Boolean,
    val premiumExpiryDate: LocalDateTime?,
    val points: Int,
    val verificationStatus: VerificationStatus,
    val verificationLevel: Int, 
    val isActive: Boolean,
    val blockedUserIds: List<String>,
    val settings: UserSettingsDomain,
    val preferredAgeRange: IntRange,
    val preferredDistance: Int,
    val preferredGenders: List<Gender>,
    val hideProfile: Boolean,
    val fcmTokens: List<String>
) : Parcelable {
    
    fun toDataModel(): User {
        // Implement conversion logic to data model
        return User(
            id = id,
            email = email,
            name = name,
            // ... other conversions
        )
    }
    
    companion object {
        fun fromDataModel(dataModel: User): UserDomain {
            // Implement conversion logic from data model
            return UserDomain(
                id = dataModel.id,
                email = dataModel.email,
                name = dataModel.name,
                // ... other conversions
                birthDate = dataModel.birthday?.toDate()?.let { Date(it.time) }?.let { 
                    LocalDate.of(it.year + 1900, it.month + 1, it.date)
                },
                age = calculateAge(dataModel.birthday?.toDate()),
                gender = when(dataModel.gender) {
                    User.GENDER_MALE -> Gender.MALE
                    User.GENDER_FEMALE -> Gender.FEMALE
                    User.GENDER_NON_BINARY -> Gender.NON_BINARY
                    else -> Gender.OTHER
                },
                phoneNumber = dataModel.phoneNumber,
                bio = dataModel.bio,
                occupation = dataModel.occupation,
                education = dataModel.education,
                interests = dataModel.interests,
                photoUrls = dataModel.photos,
                latitude = dataModel.location?.latitude,
                longitude = dataModel.location?.longitude,
                lastActive = dataModel.lastActive?.toDate()?.toInstant()?.atZone(java.time.ZoneId.systemDefault())?.toLocalDateTime(),
                joinDate = dataModel.joinDate?.toDate()?.toInstant()?.atZone(java.time.ZoneId.systemDefault())?.toLocalDateTime(),
                isPremium = dataModel.isPremium,
                premiumExpiryDate = dataModel.premiumExpiry?.toDate()?.toInstant()?.atZone(java.time.ZoneId.systemDefault())?.toLocalDateTime(),
                points = dataModel.points,
                verificationStatus = when(dataModel.verificationStatus) {
                    User.VERIFICATION_UNVERIFIED -> VerificationStatus.UNVERIFIED
                    User.VERIFICATION_PENDING -> VerificationStatus.PENDING
                    User.VERIFICATION_VERIFIED -> VerificationStatus.VERIFIED
                    User.VERIFICATION_REJECTED -> VerificationStatus.REJECTED
                    else -> VerificationStatus.UNVERIFIED
                },
                verificationLevel = dataModel.verificationLevel,
                isActive = dataModel.isActive,
                blockedUserIds = dataModel.blockedUsers,
                settings = UserSettingsDomain.fromDataModel(dataModel.settings),
                preferredAgeRange = dataModel.preferredAgeRange[0]..dataModel.preferredAgeRange[1],
                preferredDistance = dataModel.preferredDistance,
                preferredGenders = dataModel.preferredGenders.map { 
                    when(it) {
                        User.GENDER_MALE -> Gender.MALE
                        User.GENDER_FEMALE -> Gender.FEMALE
                        User.GENDER_NON_BINARY -> Gender.NON_BINARY
                        else -> Gender.OTHER
                    }
                },
                hideProfile = dataModel.hideProfile,
                fcmTokens = dataModel.fcmTokens
            )
        }
        
        private fun calculateAge(birthDate: Date?): Int? {
            if (birthDate == null) return null
            
            val today = LocalDate.now()
            val birthLocalDate = LocalDate.of(
                birthDate.year + 1900,
                birthDate.month + 1,
                birthDate.date
            )
            
            var age = today.year - birthLocalDate.year
            
            if (today.monthValue < birthLocalDate.monthValue || 
                (today.monthValue == birthLocalDate.monthValue && today.dayOfMonth < birthLocalDate.dayOfMonth)) {
                age--
            }
            
            return age
        }
    }
}

/**
 * Domain model for UserSettings
 */
@Parcelize
data class UserSettingsDomain(
    val darkMode: Boolean,
    val showOnlineStatus: Boolean,
    val showDistance: Boolean,
    val showAge: Boolean,
    val allowMessages: Boolean,
    val allowNotifications: Boolean,
    val emailNotifications: Boolean,
    val pushNotifications: Boolean,
    val distanceUnit: DistanceUnit,
    val language: String
) : Parcelable {
    
    fun toDataModel(): UserSettings {
        return UserSettings(
            darkMode = darkMode,
            showOnlineStatus = showOnlineStatus,
            showDistance = showDistance,
            showAge = showAge,
            allowMessages = allowMessages,
            allowNotifications = allowNotifications,
            emailNotifications = emailNotifications,
            pushNotifications = pushNotifications,
            distanceUnit = distanceUnit.name,
            language = language
        )
    }
    
    companion object {
        fun fromDataModel(dataModel: UserSettings): UserSettingsDomain {
            return UserSettingsDomain(
                darkMode = dataModel.darkMode,
                showOnlineStatus = dataModel.showOnlineStatus,
                showDistance = dataModel.showDistance,
                showAge = dataModel.showAge,
                allowMessages = dataModel.allowMessages,
                allowNotifications = dataModel.allowNotifications,
                emailNotifications = dataModel.emailNotifications,
                pushNotifications = dataModel.pushNotifications,
                distanceUnit = try {
                    DistanceUnit.valueOf(dataModel.distanceUnit)
                } catch (e: Exception) {
                    DistanceUnit.KM
                },
                language = dataModel.language
            )
        }
    }
}

/**
 * Gender enum
 */
enum class Gender {
    MALE, FEMALE, NON_BINARY, OTHER;
    
    fun toDataValue(): String {
        return when(this) {
            MALE -> User.GENDER_MALE
            FEMALE -> User.GENDER_FEMALE
            NON_BINARY -> User.GENDER_NON_BINARY
            OTHER -> User.GENDER_OTHER
        }
    }
}

/**
 * VerificationStatus enum
 */
enum class VerificationStatus {
    UNVERIFIED, PENDING, VERIFIED, REJECTED;
    
    fun toDataValue(): String {
        return when(this) {
            UNVERIFIED -> User.VERIFICATION_UNVERIFIED
            PENDING -> User.VERIFICATION_PENDING
            VERIFIED -> User.VERIFICATION_VERIFIED
            REJECTED -> User.VERIFICATION_REJECTED
        }
    }
}

/**
 * DistanceUnit enum
 */
enum class DistanceUnit {
    KM, MILES
}