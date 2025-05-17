package com.kilagee.onelove.data.model

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User preferences model that stores all user settings and preferences
 */
data class UserPreferences(
    /**
     * App theme preference (light, dark, system)
     */
    val theme: String = THEME_SYSTEM,
    
    /**
     * Language preference
     */
    val language: String = "en",
    
    /**
     * Notification preferences
     */
    val notifyNewMatches: Boolean = true,
    val notifyMessages: Boolean = true,
    val notifyLikes: Boolean = true,
    val notifyProfileViews: Boolean = true,
    val notifyCalls: Boolean = true,
    val notifyPromotional: Boolean = false,
    
    /**
     * Discovery preferences
     */
    val discoveryEnabled: Boolean = true,
    val maxDistance: Int = 50,
    val minAge: Int = 18,
    val maxAge: Int = 50,
    val showOnlyVerified: Boolean = false,
    
    /**
     * Privacy preferences
     */
    val showOnlineStatus: Boolean = true,
    val showLastActive: Boolean = true,
    val showReadReceipts: Boolean = true,
    val showDistanceAs: String = DISTANCE_EXACT,
    
    /**
     * Priority contacts (get boosted notifications)
     */
    val priorityContacts: Set<String> = emptySet(),
    
    /**
     * Muted contacts (receive minimal notifications)
     */
    val mutedContacts: Set<String> = emptySet(),
    
    /**
     * Blocked users
     */
    val blockedUsers: Set<String> = emptySet()
) {
    companion object {
        // Theme options
        const val THEME_LIGHT = "light"
        const val THEME_DARK = "dark"
        const val THEME_SYSTEM = "system"
        
        // Distance display options
        const val DISTANCE_EXACT = "exact"
        const val DISTANCE_APPROXIMATE = "approximate"
        const val DISTANCE_HIDE = "hide"
    }
}

/**
 * Extension for datastore to help with user preferences
 */
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

/**
 * Repository for accessing and modifying user preferences
 */
@Singleton
class UserPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gson: Gson
) {
    // Preference keys
    private object PreferencesKeys {
        val THEME = stringPreferencesKey("theme")
        val LANGUAGE = stringPreferencesKey("language")
        val NOTIFY_NEW_MATCHES = booleanPreferencesKey("notify_new_matches")
        val NOTIFY_MESSAGES = booleanPreferencesKey("notify_messages")
        val NOTIFY_LIKES = booleanPreferencesKey("notify_likes")
        val NOTIFY_PROFILE_VIEWS = booleanPreferencesKey("notify_profile_views")
        val NOTIFY_CALLS = booleanPreferencesKey("notify_calls")
        val NOTIFY_PROMOTIONAL = booleanPreferencesKey("notify_promotional")
        val DISCOVERY_ENABLED = booleanPreferencesKey("discovery_enabled")
        val MAX_DISTANCE = stringPreferencesKey("max_distance")
        val MIN_AGE = stringPreferencesKey("min_age")
        val MAX_AGE = stringPreferencesKey("max_age")
        val SHOW_ONLY_VERIFIED = booleanPreferencesKey("show_only_verified")
        val SHOW_ONLINE_STATUS = booleanPreferencesKey("show_online_status")
        val SHOW_LAST_ACTIVE = booleanPreferencesKey("show_last_active")
        val SHOW_READ_RECEIPTS = booleanPreferencesKey("show_read_receipts")
        val SHOW_DISTANCE_AS = stringPreferencesKey("show_distance_as")
        val PRIORITY_CONTACTS = stringPreferencesKey("priority_contacts")
        val MUTED_CONTACTS = stringPreferencesKey("muted_contacts")
        val BLOCKED_USERS = stringSetPreferencesKey("blocked_users")
    }
    
    // Get user preferences as a Flow
    val userPreferencesFlow: Flow<UserPreferences> = context.dataStore.data.map { preferences ->
        // Create UserPreferences from DataStore preferences
        UserPreferences(
            theme = preferences[PreferencesKeys.THEME] ?: UserPreferences.THEME_SYSTEM,
            language = preferences[PreferencesKeys.LANGUAGE] ?: "en",
            notifyNewMatches = preferences[PreferencesKeys.NOTIFY_NEW_MATCHES] ?: true,
            notifyMessages = preferences[PreferencesKeys.NOTIFY_MESSAGES] ?: true,
            notifyLikes = preferences[PreferencesKeys.NOTIFY_LIKES] ?: true,
            notifyProfileViews = preferences[PreferencesKeys.NOTIFY_PROFILE_VIEWS] ?: true,
            notifyCalls = preferences[PreferencesKeys.NOTIFY_CALLS] ?: true,
            notifyPromotional = preferences[PreferencesKeys.NOTIFY_PROMOTIONAL] ?: false,
            discoveryEnabled = preferences[PreferencesKeys.DISCOVERY_ENABLED] ?: true,
            maxDistance = preferences[PreferencesKeys.MAX_DISTANCE]?.toIntOrNull() ?: 50,
            minAge = preferences[PreferencesKeys.MIN_AGE]?.toIntOrNull() ?: 18,
            maxAge = preferences[PreferencesKeys.MAX_AGE]?.toIntOrNull() ?: 50,
            showOnlyVerified = preferences[PreferencesKeys.SHOW_ONLY_VERIFIED] ?: false,
            showOnlineStatus = preferences[PreferencesKeys.SHOW_ONLINE_STATUS] ?: true,
            showLastActive = preferences[PreferencesKeys.SHOW_LAST_ACTIVE] ?: true,
            showReadReceipts = preferences[PreferencesKeys.SHOW_READ_RECEIPTS] ?: true,
            showDistanceAs = preferences[PreferencesKeys.SHOW_DISTANCE_AS] ?: UserPreferences.DISTANCE_EXACT,
            priorityContacts = deserializeStringSet(preferences[PreferencesKeys.PRIORITY_CONTACTS]),
            mutedContacts = deserializeStringSet(preferences[PreferencesKeys.MUTED_CONTACTS]),
            blockedUsers = preferences[PreferencesKeys.BLOCKED_USERS] ?: emptySet()
        )
    }
    
    // Update theme preference
    suspend fun updateTheme(theme: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME] = theme
        }
    }
    
    // Update language preference
    suspend fun updateLanguage(language: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LANGUAGE] = language
        }
    }
    
    // Update notification preferences
    suspend fun updateNotificationPreferences(
        notifyNewMatches: Boolean? = null,
        notifyMessages: Boolean? = null,
        notifyLikes: Boolean? = null,
        notifyProfileViews: Boolean? = null,
        notifyCalls: Boolean? = null,
        notifyPromotional: Boolean? = null
    ) {
        context.dataStore.edit { preferences ->
            notifyNewMatches?.let { preferences[PreferencesKeys.NOTIFY_NEW_MATCHES] = it }
            notifyMessages?.let { preferences[PreferencesKeys.NOTIFY_MESSAGES] = it }
            notifyLikes?.let { preferences[PreferencesKeys.NOTIFY_LIKES] = it }
            notifyProfileViews?.let { preferences[PreferencesKeys.NOTIFY_PROFILE_VIEWS] = it }
            notifyCalls?.let { preferences[PreferencesKeys.NOTIFY_CALLS] = it }
            notifyPromotional?.let { preferences[PreferencesKeys.NOTIFY_PROMOTIONAL] = it }
        }
    }
    
    // Update discovery preferences
    suspend fun updateDiscoveryPreferences(
        discoveryEnabled: Boolean? = null,
        maxDistance: Int? = null,
        minAge: Int? = null,
        maxAge: Int? = null,
        showOnlyVerified: Boolean? = null
    ) {
        context.dataStore.edit { preferences ->
            discoveryEnabled?.let { preferences[PreferencesKeys.DISCOVERY_ENABLED] = it }
            maxDistance?.let { preferences[PreferencesKeys.MAX_DISTANCE] = it.toString() }
            minAge?.let { preferences[PreferencesKeys.MIN_AGE] = it.toString() }
            maxAge?.let { preferences[PreferencesKeys.MAX_AGE] = it.toString() }
            showOnlyVerified?.let { preferences[PreferencesKeys.SHOW_ONLY_VERIFIED] = it }
        }
    }
    
    // Update privacy preferences
    suspend fun updatePrivacyPreferences(
        showOnlineStatus: Boolean? = null,
        showLastActive: Boolean? = null,
        showReadReceipts: Boolean? = null,
        showDistanceAs: String? = null
    ) {
        context.dataStore.edit { preferences ->
            showOnlineStatus?.let { preferences[PreferencesKeys.SHOW_ONLINE_STATUS] = it }
            showLastActive?.let { preferences[PreferencesKeys.SHOW_LAST_ACTIVE] = it }
            showReadReceipts?.let { preferences[PreferencesKeys.SHOW_READ_RECEIPTS] = it }
            showDistanceAs?.let { preferences[PreferencesKeys.SHOW_DISTANCE_AS] = it }
        }
    }
    
    // Add a user to priority contacts
    suspend fun addPriorityContact(userId: String) {
        context.dataStore.edit { preferences ->
            val currentContacts = deserializeStringSet(preferences[PreferencesKeys.PRIORITY_CONTACTS])
            val newContacts = currentContacts + userId
            preferences[PreferencesKeys.PRIORITY_CONTACTS] = serializeStringSet(newContacts)
        }
    }
    
    // Remove a user from priority contacts
    suspend fun removePriorityContact(userId: String) {
        context.dataStore.edit { preferences ->
            val currentContacts = deserializeStringSet(preferences[PreferencesKeys.PRIORITY_CONTACTS])
            val newContacts = currentContacts - userId
            preferences[PreferencesKeys.PRIORITY_CONTACTS] = serializeStringSet(newContacts)
        }
    }
    
    // Add a user to muted contacts
    suspend fun addMutedContact(userId: String) {
        context.dataStore.edit { preferences ->
            val currentContacts = deserializeStringSet(preferences[PreferencesKeys.MUTED_CONTACTS])
            val newContacts = currentContacts + userId
            preferences[PreferencesKeys.MUTED_CONTACTS] = serializeStringSet(newContacts)
        }
    }
    
    // Remove a user from muted contacts
    suspend fun removeMutedContact(userId: String) {
        context.dataStore.edit { preferences ->
            val currentContacts = deserializeStringSet(preferences[PreferencesKeys.MUTED_CONTACTS])
            val newContacts = currentContacts - userId
            preferences[PreferencesKeys.MUTED_CONTACTS] = serializeStringSet(newContacts)
        }
    }
    
    // Block a user
    suspend fun blockUser(userId: String) {
        context.dataStore.edit { preferences ->
            val currentBlocked = preferences[PreferencesKeys.BLOCKED_USERS] ?: emptySet()
            preferences[PreferencesKeys.BLOCKED_USERS] = currentBlocked + userId
        }
    }
    
    // Unblock a user
    suspend fun unblockUser(userId: String) {
        context.dataStore.edit { preferences ->
            val currentBlocked = preferences[PreferencesKeys.BLOCKED_USERS] ?: emptySet()
            preferences[PreferencesKeys.BLOCKED_USERS] = currentBlocked - userId
        }
    }
    
    // Helper function to serialize a Set<String> to a single String for storage
    private fun serializeStringSet(set: Set<String>): String {
        return gson.toJson(set)
    }
    
    // Helper function to deserialize a String back to a Set<String>
    private fun deserializeStringSet(json: String?): Set<String> {
        if (json.isNullOrEmpty()) return emptySet()
        
        val type = object : TypeToken<Set<String>>() {}.type
        return try {
            gson.fromJson(json, type)
        } catch (e: Exception) {
            emptySet()
        }
    }
}