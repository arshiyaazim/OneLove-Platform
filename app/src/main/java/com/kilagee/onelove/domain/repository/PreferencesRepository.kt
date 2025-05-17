package com.kilagee.onelove.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for user preferences
 */
interface PreferencesRepository {
    
    /**
     * Get current theme preference
     * @return Flow of Boolean, true if dark theme is preferred
     */
    fun getThemePreference(): Flow<Boolean>
    
    /**
     * Set theme preference
     * @param isDarkTheme true for dark theme, false for light theme
     */
    suspend fun setThemePreference(isDarkTheme: Boolean)
    
    /**
     * Get notification preferences
     * @return Flow of NotificationPreferences
     */
    fun getNotificationPreferences(): Flow<NotificationPreferences>
    
    /**
     * Set notification preferences
     * @param preferences NotificationPreferences object
     */
    suspend fun setNotificationPreferences(preferences: NotificationPreferences)
    
    /**
     * Get privacy preferences
     * @return Flow of PrivacyPreferences
     */
    fun getPrivacyPreferences(): Flow<PrivacyPreferences>
    
    /**
     * Set privacy preferences
     * @param preferences PrivacyPreferences object
     */
    suspend fun setPrivacyPreferences(preferences: PrivacyPreferences)
    
    /**
     * Get login credentials for auto-login (if saved)
     * @return LoginCredentials or null if not saved
     */
    suspend fun getSavedLoginCredentials(): LoginCredentials?
    
    /**
     * Save login credentials for auto-login
     * @param credentials LoginCredentials to save
     */
    suspend fun saveLoginCredentials(credentials: LoginCredentials)
    
    /**
     * Clear saved login credentials
     */
    suspend fun clearLoginCredentials()
    
    /**
     * Get app language preference
     * @return Flow of String with language code
     */
    fun getLanguagePreference(): Flow<String>
    
    /**
     * Set app language preference
     * @param languageCode ISO language code
     */
    suspend fun setLanguagePreference(languageCode: String)
    
    /**
     * Check if user has completed onboarding
     * @return true if onboarding is completed
     */
    suspend fun hasCompletedOnboarding(): Boolean
    
    /**
     * Mark onboarding as completed
     */
    suspend fun setOnboardingCompleted()
    
    /**
     * Get location sharing preference
     * @return true if location sharing is enabled
     */
    suspend fun getLocationSharingEnabled(): Boolean
    
    /**
     * Set location sharing preference
     * @param enabled true to enable location sharing
     */
    suspend fun setLocationSharingEnabled(enabled: Boolean)
    
    /**
     * Get account visibility preference
     * @return AccountVisibility setting
     */
    suspend fun getAccountVisibility(): AccountVisibility
    
    /**
     * Set account visibility preference
     * @param visibility AccountVisibility setting
     */
    suspend fun setAccountVisibility(visibility: AccountVisibility)
    
    /**
     * Clear all preferences
     */
    suspend fun clearAllPreferences()
}

/**
 * Data class for notification preferences
 */
data class NotificationPreferences(
    val newMatches: Boolean = true,
    val messages: Boolean = true,
    val messageReactions: Boolean = true,
    val callRequests: Boolean = true,
    val offers: Boolean = true,
    val profileVisits: Boolean = true,
    val subscriptionUpdates: Boolean = true,
    val promotions: Boolean = false,
    val system: Boolean = true
)

/**
 * Data class for privacy preferences
 */
data class PrivacyPreferences(
    val showOnlineStatus: Boolean = true,
    val showLastActive: Boolean = true,
    val allowProfileScreenshots: Boolean = true,
    val showReadReceipts: Boolean = true,
    val showDistanceInProfile: Boolean = true,
    val allowDataCollection: Boolean = true
)

/**
 * Data class for login credentials
 */
data class LoginCredentials(
    val email: String,
    val password: String
)

/**
 * Enum for account visibility settings
 */
enum class AccountVisibility {
    EVERYONE, // Visible to all users
    MATCHES_ONLY, // Visible only to matched users
    HIDDEN // Temporarily hidden from discovery
}