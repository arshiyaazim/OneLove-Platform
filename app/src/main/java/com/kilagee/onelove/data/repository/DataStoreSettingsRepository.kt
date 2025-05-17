package com.kilagee.onelove.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.firebase.firestore.FirebaseFirestore
import com.kilagee.onelove.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

// DataStore instance at the app level
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "onelove_settings")

/**
 * Implementation of the Settings repository using DataStore
 */
@Singleton
class DataStoreSettingsRepository @Inject constructor(
    private val context: Context,
    private val firestore: FirebaseFirestore
) : SettingsRepository {
    
    companion object {
        private val DARK_MODE_KEY = booleanPreferencesKey("dark_mode")
        private val NOTIFICATIONS_KEY = booleanPreferencesKey("notifications")
        private val APP_LANGUAGE_KEY = stringPreferencesKey("app_language")
    }
    
    override fun getDarkModeEnabled(): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[DARK_MODE_KEY] ?: false
        }
    }
    
    override suspend fun setDarkModeEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DARK_MODE_KEY] = enabled
        }
    }
    
    override fun getNotificationsEnabled(): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[NOTIFICATIONS_KEY] ?: true
        }
    }
    
    override suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[NOTIFICATIONS_KEY] = enabled
        }
        
        // Update notification settings on the server side
        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            try {
                firestore.collection("users")
                    .document(currentUser.uid)
                    .update("notificationsEnabled", enabled)
                    .await()
            } catch (e: Exception) {
                // Error updating notification settings on server
            }
        }
    }
    
    override fun getAppLanguage(): Flow<String> {
        return context.dataStore.data.map { preferences ->
            preferences[APP_LANGUAGE_KEY] ?: "en" // Default to English
        }
    }
    
    override suspend fun setAppLanguage(language: String) {
        context.dataStore.edit { preferences ->
            preferences[APP_LANGUAGE_KEY] = language
        }
    }
    
    override suspend fun syncUserData(userId: String) {
        try {
            // Fetch user data from Firestore
            val userSnapshot = firestore.collection("users")
                .document(userId)
                .get()
                .await()
            
            if (userSnapshot.exists()) {
                // Update local settings based on server data
                val notificationsEnabled = userSnapshot.getBoolean("notificationsEnabled")
                if (notificationsEnabled != null) {
                    setNotificationsEnabled(notificationsEnabled)
                }
                
                val language = userSnapshot.getString("language")
                if (language != null) {
                    setAppLanguage(language)
                }
            }
            
            // You could also sync other data like:
            // - Subscription status
            // - User profile
            // - Matches
            // - Messages
            // - etc.
        } catch (e: Exception) {
            // Error syncing data
        }
    }
    
    override suspend fun clearAppCache() {
        // Clear image cache - would use Glide or other image loader in real implementation
        // Glide.get(context).clearMemory()
        
        // On a worker thread, we would also:
        // Glide.get(context).clearDiskCache()
        
        // Clear WebView cache
        android.webkit.WebStorage.getInstance().deleteAllData()
        
        // Note: We don't clear preferences since those are user settings
    }
}