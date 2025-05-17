package com.kilagee.onelove.ui.screens.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kilagee.onelove.data.model.Notification
import com.kilagee.onelove.data.model.NotificationType
import com.kilagee.onelove.data.model.PushNotificationSettings
import com.kilagee.onelove.data.model.User
import com.kilagee.onelove.domain.repository.NotificationRepository
import com.kilagee.onelove.domain.repository.ProfileRepository
import com.kilagee.onelove.domain.repository.UserRepository
import com.kilagee.onelove.domain.util.NotificationPrioritizer
import com.kilagee.onelove.domain.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for notifications screen
 */
@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository,
    private val profileRepository: ProfileRepository,
    private val userRepository: UserRepository
) : ViewModel() {
    
    // UI state
    private val _uiState = MutableStateFlow<NotificationsUiState>(NotificationsUiState.Loading)
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()
    
    // One-time events
    private val _events = MutableSharedFlow<NotificationEvent>()
    val events: SharedFlow<NotificationEvent> = _events.asSharedFlow()
    
    // Notifications
    private val _notifications = MutableStateFlow<List<Notification>>(emptyList())
    val notifications: StateFlow<List<Notification>> = _notifications.asStateFlow()
    
    // Unread count
    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()
    
    // Notification settings
    private val _notificationSettings = MutableStateFlow<PushNotificationSettings?>(null)
    val notificationSettings: StateFlow<PushNotificationSettings?> = _notificationSettings.asStateFlow()
    
    // Selected filter
    private val _selectedFilter = MutableStateFlow<NotificationType?>(null)
    val selectedFilter: StateFlow<NotificationType?> = _selectedFilter.asStateFlow()
    
    // Is settings open
    private val _isSettingsOpen = MutableStateFlow(false)
    val isSettingsOpen: StateFlow<Boolean> = _isSettingsOpen.asStateFlow()
    
    // Prioritization enabled
    private val _prioritizationEnabled = MutableStateFlow(true)
    val prioritizationEnabled: StateFlow<Boolean> = _prioritizationEnabled.asStateFlow()
    
    // Current user
    private val _currentUser = MutableStateFlow<User?>(null)
    
    // User context cache
    private val userCache = mutableMapOf<String, User>()
    
    // Interaction history
    private var interactionHistory = NotificationPrioritizer.InteractionHistory()
    
    // Active jobs
    private var notificationsJob: Job? = null
    private var unreadCountJob: Job? = null
    private var currentUserJob: Job? = null
    
    init {
        loadCurrentUser()
        loadNotifications()
        observeUnreadCount()
        loadNotificationSettings()
        loadInteractionHistory()
    }
    
    /**
     * Load current user
     */
    private fun loadCurrentUser() {
        currentUserJob = viewModelScope.launch {
            val result = profileRepository.getCurrentUserProfile()
            if (result is Result.Success) {
                _currentUser.value = result.data
            }
        }
    }
    
    /**
     * Load interaction history for prioritization
     */
    private fun loadInteractionHistory() {
        viewModelScope.launch {
            // Get recently chatted users
            val chatResult = userRepository.getRecentChatPartners(10)
            
            // Get matched users
            val matchResult = userRepository.getMatches(50)
            
            // Get liked users
            val likeResult = userRepository.getLikedUsers(50)
            
            // Get visited profiles
            val visitResult = userRepository.getVisitedProfiles(50)
            
            // Combine the results
            val recentlyChatted = if (chatResult is Result.Success) {
                chatResult.data.map { it.id }.toSet()
            } else emptySet()
            
            val matches = if (matchResult is Result.Success) {
                matchResult.data.map { it.id }.toSet()
            } else emptySet()
            
            val likes = if (likeResult is Result.Success) {
                likeResult.data.map { it.id }.toSet()
            } else emptySet()
            
            val profileVisits = if (visitResult is Result.Success) {
                visitResult.data.map { it.id }.toSet()
            } else emptySet()
            
            interactionHistory = NotificationPrioritizer.InteractionHistory(
                recentlyChatted = recentlyChatted,
                matches = matches,
                likes = likes,
                profileVisits = profileVisits
            )
            
            // Pre-populate user cache with users we've interacted with
            val allUserIds = (recentlyChatted + matches + likes + profileVisits).toList()
            fetchAndCacheUsers(allUserIds)
        }
    }
    
    /**
     * Fetch and cache user profiles by IDs
     */
    private fun fetchAndCacheUsers(userIds: List<String>) {
        if (userIds.isEmpty()) return
        
        viewModelScope.launch {
            val result = userRepository.getUsersById(userIds)
            if (result is Result.Success) {
                result.data.forEach { user ->
                    userCache[user.id] = user
                }
            }
        }
    }
    
    /**
     * Load notifications
     */
    private fun loadNotifications() {
        // Cancel any existing job
        notificationsJob?.cancel()
        
        // Start new job
        notificationsJob = viewModelScope.launch {
            _uiState.value = NotificationsUiState.Loading
            
            val filter = _selectedFilter.value
            val flow = if (filter != null) {
                // Use regular repository method for filtered notifications
                val result = notificationRepository.getNotificationsByType(filter)
                if (result is Result.Success) {
                    processAndPrioritizeNotifications(result.data)
                    _uiState.value = NotificationsUiState.Success
                } else if (result is Result.Error) {
                    _events.emit(NotificationEvent.Error(result.message ?: "Failed to load notifications"))
                    _uiState.value = NotificationsUiState.Error(result.message ?: "Failed to load notifications")
                }
                return@launch
            } else {
                // Use flow for unfiltered notifications
                notificationRepository.getNotificationsFlow()
            }
            
            // Collect flow updates
            flow.collectLatest { result ->
                when (result) {
                    is Result.Success -> {
                        processAndPrioritizeNotifications(result.data)
                        _uiState.value = if (result.data.isEmpty()) {
                            NotificationsUiState.Empty
                        } else {
                            NotificationsUiState.Success
                        }
                    }
                    is Result.Error -> {
                        _events.emit(NotificationEvent.Error(result.message ?: "Failed to load notifications"))
                        _uiState.value = NotificationsUiState.Error(result.message ?: "Failed to load notifications")
                    }
                    is Result.Loading -> {
                        _uiState.value = NotificationsUiState.Loading
                    }
                }
            }
        }
    }
    
    /**
     * Process and prioritize notifications
     */
    private fun processAndPrioritizeNotifications(notifications: List<Notification>) {
        // Collect unique user IDs from notifications to fetch
        val userIds = notifications.mapNotNull { it.getTargetId() }
            .filter { !userCache.containsKey(it) } // Only fetch users we don't have cached
        
        // Fetch user data if needed
        if (userIds.isNotEmpty()) {
            fetchAndCacheUsers(userIds)
        }
        
        // Apply prioritization if enabled
        if (_prioritizationEnabled.value && _currentUser.value != null) {
            val prioritizedList = NotificationPrioritizer.prioritizeNotifications(
                notifications,
                _currentUser.value!!,
                userCache,
                interactionHistory
            )
            _notifications.value = prioritizedList
        } else {
            // Just sort by date if prioritization is disabled
            _notifications.value = notifications.sortedByDescending { it.createdAt }
        }
    }
    
    /**
     * Observe unread notifications count
     */
    private fun observeUnreadCount() {
        // Cancel any existing job
        unreadCountJob?.cancel()
        
        // Start new job
        unreadCountJob = viewModelScope.launch {
            notificationRepository.getUnreadCountFlow().collectLatest { result ->
                if (result is Result.Success) {
                    _unreadCount.value = result.data
                }
            }
        }
    }
    
    /**
     * Load notification settings
     */
    private fun loadNotificationSettings() {
        viewModelScope.launch {
            val result = notificationRepository.getNotificationSettings()
            
            if (result is Result.Success) {
                _notificationSettings.value = result.data
            }
        }
    }
    
    /**
     * Mark a notification as read
     */
    fun markAsRead(notificationId: String) {
        viewModelScope.launch {
            val result = notificationRepository.markAsRead(listOf(notificationId))
            
            if (result is Result.Error) {
                _events.emit(NotificationEvent.Error(result.message ?: "Failed to mark notification as read"))
            }
        }
    }
    
    /**
     * Mark all notifications as read
     */
    fun markAllAsRead() {
        viewModelScope.launch {
            val result = notificationRepository.markAllAsRead()
            
            when (result) {
                is Result.Success -> {
                    _events.emit(NotificationEvent.AllMarkedAsRead)
                }
                is Result.Error -> {
                    _events.emit(NotificationEvent.Error(result.message ?: "Failed to mark notifications as read"))
                }
                is Result.Loading -> {
                    // Do nothing for loading state
                }
            }
        }
    }
    
    /**
     * Delete a notification
     */
    fun deleteNotification(notificationId: String) {
        viewModelScope.launch {
            val result = notificationRepository.deleteNotification(notificationId)
            
            when (result) {
                is Result.Success -> {
                    _events.emit(NotificationEvent.NotificationDeleted)
                }
                is Result.Error -> {
                    _events.emit(NotificationEvent.Error(result.message ?: "Failed to delete notification"))
                }
                is Result.Loading -> {
                    // Do nothing for loading state
                }
            }
        }
    }
    
    /**
     * Delete all notifications
     */
    fun deleteAllNotifications() {
        viewModelScope.launch {
            val result = notificationRepository.deleteAllNotifications()
            
            when (result) {
                is Result.Success -> {
                    _events.emit(NotificationEvent.AllNotificationsDeleted)
                }
                is Result.Error -> {
                    _events.emit(NotificationEvent.Error(result.message ?: "Failed to delete notifications"))
                }
                is Result.Loading -> {
                    // Do nothing for loading state
                }
            }
        }
    }
    
    /**
     * Update notification settings
     */
    fun updateNotificationSettings(settings: PushNotificationSettings) {
        viewModelScope.launch {
            val result = notificationRepository.updateNotificationSettings(settings)
            
            when (result) {
                is Result.Success -> {
                    _notificationSettings.value = settings
                    _events.emit(NotificationEvent.SettingsUpdated)
                }
                is Result.Error -> {
                    _events.emit(NotificationEvent.Error(result.message ?: "Failed to update notification settings"))
                }
                is Result.Loading -> {
                    // Do nothing for loading state
                }
            }
        }
    }
    
    /**
     * Toggle a specific notification setting
     */
    fun toggleSetting(settingName: String) {
        val currentSettings = _notificationSettings.value ?: return
        
        // Create a copy of the current settings with the toggled value
        val updatedSettings = when (settingName) {
            "enabled" -> currentSettings.copy(enabled = !currentSettings.enabled)
            "matchEnabled" -> currentSettings.copy(matchEnabled = !currentSettings.matchEnabled)
            "messageEnabled" -> currentSettings.copy(messageEnabled = !currentSettings.messageEnabled)
            "likeEnabled" -> currentSettings.copy(likeEnabled = !currentSettings.likeEnabled)
            "superLikeEnabled" -> currentSettings.copy(superLikeEnabled = !currentSettings.superLikeEnabled)
            "visitEnabled" -> currentSettings.copy(visitEnabled = !currentSettings.visitEnabled)
            "subscriptionEnabled" -> currentSettings.copy(subscriptionEnabled = !currentSettings.subscriptionEnabled)
            "paymentEnabled" -> currentSettings.copy(paymentEnabled = !currentSettings.paymentEnabled)
            "systemEnabled" -> currentSettings.copy(systemEnabled = !currentSettings.systemEnabled)
            "offerEnabled" -> currentSettings.copy(offerEnabled = !currentSettings.offerEnabled)
            "eventEnabled" -> currentSettings.copy(eventEnabled = !currentSettings.eventEnabled)
            "verificationEnabled" -> currentSettings.copy(verificationEnabled = !currentSettings.verificationEnabled)
            "pointsEnabled" -> currentSettings.copy(pointsEnabled = !currentSettings.pointsEnabled)
            "callEnabled" -> currentSettings.copy(callEnabled = !currentSettings.callEnabled)
            "aiInteractionEnabled" -> currentSettings.copy(aiInteractionEnabled = !currentSettings.aiInteractionEnabled)
            "profileUpdateEnabled" -> currentSettings.copy(profileUpdateEnabled = !currentSettings.profileUpdateEnabled)
            "adminMessageEnabled" -> currentSettings.copy(adminMessageEnabled = !currentSettings.adminMessageEnabled)
            "vibrate" -> currentSettings.copy(vibrate = !currentSettings.vibrate)
            "sound" -> currentSettings.copy(sound = !currentSettings.sound)
            "quietHoursEnabled" -> currentSettings.copy(quietHoursEnabled = !currentSettings.quietHoursEnabled)
            else -> currentSettings
        }
        
        // Update the settings
        updateNotificationSettings(updatedSettings)
    }
    
    /**
     * Set notification filter
     */
    fun setFilter(type: NotificationType?) {
        _selectedFilter.value = type
        loadNotifications() // Reload with the new filter
    }
    
    /**
     * Toggle settings dialog
     */
    fun toggleSettings() {
        _isSettingsOpen.value = !_isSettingsOpen.value
    }
    
    /**
     * Toggle prioritization
     */
    fun togglePrioritization() {
        _prioritizationEnabled.value = !_prioritizationEnabled.value
        
        // Re-process notifications with new prioritization setting
        val currentNotifications = _notifications.value
        if (currentNotifications.isNotEmpty()) {
            processAndPrioritizeNotifications(currentNotifications)
        }
    }
    
    /**
     * Send test notification
     */
    fun sendTestNotification(type: NotificationType) {
        viewModelScope.launch {
            val result = notificationRepository.sendTestNotification(type)
            
            if (result is Result.Error) {
                _events.emit(NotificationEvent.Error(result.message ?: "Failed to send test notification"))
            } else if (result is Result.Success) {
                _events.emit(NotificationEvent.TestNotificationSent)
            }
        }
    }
    
    /**
     * Clear errors
     */
    fun clearErrors() {
        if (_uiState.value is NotificationsUiState.Error) {
            _uiState.value = NotificationsUiState.Success
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        notificationsJob?.cancel()
        unreadCountJob?.cancel()
        currentUserJob?.cancel()
    }
}

/**
 * UI state for the notifications screen
 */
sealed class NotificationsUiState {
    object Loading : NotificationsUiState()
    object Success : NotificationsUiState()
    object Empty : NotificationsUiState()
    data class Error(val message: String) : NotificationsUiState()
}

/**
 * Events emitted by the notifications screen
 */
sealed class NotificationEvent {
    object AllMarkedAsRead : NotificationEvent()
    object NotificationDeleted : NotificationEvent()
    object AllNotificationsDeleted : NotificationEvent()
    object SettingsUpdated : NotificationEvent()
    object TestNotificationSent : NotificationEvent()
    data class Error(val message: String) : NotificationEvent()
}