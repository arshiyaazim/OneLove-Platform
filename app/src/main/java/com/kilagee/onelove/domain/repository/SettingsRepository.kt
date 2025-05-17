package com.kilagee.onelove.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for app settings
 */
interface SettingsRepository {
    /**
     * Get the dark mode setting
     */
    fun getDarkModeEnabled(): Flow<Boolean>
    
    /**
     * Set the dark mode setting
     */
    suspend fun setDarkModeEnabled(enabled: Boolean)
    
    /**
     * Get the notifications setting
     */
    fun getNotificationsEnabled(): Flow<Boolean>
    
    /**
     * Set the notifications setting
     */
    suspend fun setNotificationsEnabled(enabled: Boolean)
    
    /**
     * Get the app language setting
     */
    fun getAppLanguage(): Flow<String>
    
    /**
     * Set the app language setting
     */
    suspend fun setAppLanguage(language: String)
    
    /**
     * Sync user data from servers
     */
    suspend fun syncUserData(userId: String)
    
    /**
     * Clear app cache
     */
    suspend fun clearAppCache()
}