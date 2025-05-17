package com.kilagee.onelove.util

import android.content.Context
import android.content.res.Configuration
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/**
 * Theme utility functions
 */
object ThemeUtils {
    
    /**
     * Check if dark mode is enabled based on user preference or system setting
     */
    @Composable
    fun isDarkTheme(settingsRepository: com.kilagee.onelove.domain.repository.SettingsRepository): Boolean {
        val darkModePreference by settingsRepository.getDarkModeEnabled().collectAsState(initial = false)
        val systemInDarkTheme = isSystemInDarkTheme()
        
        return darkModePreference || systemInDarkTheme
    }
    
    /**
     * Check if the system is in dark mode
     */
    fun isSystemInDarkMode(context: Context): Boolean {
        return (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == 
               Configuration.UI_MODE_NIGHT_YES
    }
    
    /**
     * Get the current theme from preferences
     */
    fun getThemeFromPreferences(context: Context): ThemeMode {
        // In a real implementation, this would read from DataStore
        return try {
            val settingsRepository = (context.applicationContext as com.kilagee.onelove.OneLoveApplication)
                .appComponent
                .getSettingsRepository()
            
            val darkModeEnabled = runBlocking { 
                settingsRepository.getDarkModeEnabled().first() 
            }
            
            when {
                darkModeEnabled -> ThemeMode.DARK
                else -> ThemeMode.LIGHT
            }
        } catch (e: Exception) {
            // Fallback to system default if there's an error
            if (isSystemInDarkMode(context)) ThemeMode.DARK else ThemeMode.LIGHT
        }
    }
}

/**
 * Theme mode enum
 */
enum class ThemeMode {
    LIGHT,
    DARK,
    SYSTEM
}