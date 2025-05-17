package com.kilagee.onelove.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kilagee.onelove.data.model.Notification
import com.kilagee.onelove.data.model.NotificationType
import com.kilagee.onelove.domain.repository.NotificationRepository
import com.kilagee.onelove.domain.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Notification view model
 */
@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository
) : ViewModel() {
    
    private val _notifications = MutableStateFlow<Result<List<Notification>>>(Result.Loading)
    val notifications: StateFlow<Result<List<Notification>>> = _notifications
    
    private val _unreadNotifications = MutableStateFlow<Result<List<Notification>>>(Result.Loading)
    val unreadNotifications: StateFlow<Result<List<Notification>>> = _unreadNotifications
    
    private val _prioritizedNotifications = MutableStateFlow<Result<List<Notification>>>(Result.Loading)
    val prioritizedNotifications: StateFlow<Result<List<Notification>>> = _prioritizedNotifications
    
    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount
    
    private val _notificationPreferences = MutableStateFlow<Result<Map<NotificationType, Boolean>>>(Result.Loading)
    val notificationPreferences: StateFlow<Result<Map<NotificationType, Boolean>>> = _notificationPreferences
    
    private val _actionState = MutableStateFlow<NotificationActionState>(NotificationActionState.Idle)
    val actionState: StateFlow<NotificationActionState> = _actionState
    
    init {
        loadNotifications()
        loadUnreadNotifications()
        loadPrioritizedNotifications()
        observeUnreadCount()
        loadNotificationPreferences()
    }
    
    /**
     * Load all notifications
     */
    fun loadNotifications() {
        viewModelScope.launch {
            notificationRepository.getNotifications()
                .catch { e ->
                    Timber.e(e, "Error loading notifications")
                    _notifications.value = Result.Error("Failed to load notifications: ${e.message}")
                }
                .collect { result ->
                    _notifications.value = result
                }
        }
    }
    
    /**
     * Load unread notifications
     */
    fun loadUnreadNotifications() {
        viewModelScope.launch {
            notificationRepository.getUnreadNotifications()
                .catch { e ->
                    Timber.e(e, "Error loading unread notifications")
                    _unreadNotifications.value = Result.Error("Failed to load unread notifications: ${e.message}")
                }
                .collect { result ->
                    _unreadNotifications.value = result
                }
        }
    }
    
    /**
     * Load prioritized notifications
     */
    fun loadPrioritizedNotifications() {
        viewModelScope.launch {
            notificationRepository.getPrioritizedNotifications()
                .catch { e ->
                    Timber.e(e, "Error loading prioritized notifications")
                    _prioritizedNotifications.value = Result.Error("Failed to load prioritized notifications: ${e.message}")
                }
                .collect { result ->
                    _prioritizedNotifications.value = result
                }
        }
    }
    
    /**
     * Observe unread notification count
     */
    private fun observeUnreadCount() {
        viewModelScope.launch {
            notificationRepository.getUnreadNotificationCount()
                .catch { e ->
                    Timber.e(e, "Error getting unread notification count")
                    _unreadCount.value = 0
                }
                .collect { count ->
                    _unreadCount.value = count
                }
        }
    }
    
    /**
     * Load notification preferences
     */
    fun loadNotificationPreferences() {
        viewModelScope.launch {
            notificationRepository.getNotificationPreferences()
                .catch { e ->
                    Timber.e(e, "Error loading notification preferences")
                    _notificationPreferences.value = Result.Error("Failed to load notification preferences: ${e.message}")
                }
                .collect { result ->
                    _notificationPreferences.value = result
                }
        }
    }
    
    /**
     * Mark a notification as read
     * 
     * @param notificationId ID of the notification
     */
    fun markNotificationAsRead(notificationId: String) {
        viewModelScope.launch {
            try {
                val result = notificationRepository.markNotificationAsRead(notificationId)
                
                if (result is Result.Success) {
                    // Refresh notification lists
                    loadNotifications()
                    loadUnreadNotifications()
                    loadPrioritizedNotifications()
                }
            } catch (e: Exception) {
                Timber.e(e, "Error marking notification as read")
            }
        }
    }
    
    /**
     * Mark all notifications as read
     */
    fun markAllNotificationsAsRead() {
        viewModelScope.launch {
            try {
                _actionState.value = NotificationActionState.Processing
                val result = notificationRepository.markAllNotificationsAsRead()
                
                when (result) {
                    is Result.Success -> {
                        _actionState.value = NotificationActionState.MarkedAllAsRead
                        // Refresh notification lists
                        loadNotifications()
                        loadUnreadNotifications()
                        loadPrioritizedNotifications()
                    }
                    is Result.Error -> {
                        _actionState.value = NotificationActionState.Error(result.message)
                    }
                    is Result.Loading -> {
                        _actionState.value = NotificationActionState.Processing
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error marking all notifications as read")
                _actionState.value = NotificationActionState.Error(e.message ?: "Failed to mark all notifications as read")
            } finally {
                // Reset state after a delay
                kotlinx.coroutines.delay(2000)
                _actionState.value = NotificationActionState.Idle
            }
        }
    }
    
    /**
     * Delete a notification
     * 
     * @param notificationId ID of the notification
     */
    fun deleteNotification(notificationId: String) {
        viewModelScope.launch {
            try {
                val result = notificationRepository.deleteNotification(notificationId)
                
                if (result is Result.Success) {
                    // Refresh notification lists
                    loadNotifications()
                    loadUnreadNotifications()
                    loadPrioritizedNotifications()
                }
            } catch (e: Exception) {
                Timber.e(e, "Error deleting notification")
            }
        }
    }
    
    /**
     * Delete all notifications
     */
    fun deleteAllNotifications() {
        viewModelScope.launch {
            try {
                _actionState.value = NotificationActionState.Processing
                val result = notificationRepository.deleteAllNotifications()
                
                when (result) {
                    is Result.Success -> {
                        _actionState.value = NotificationActionState.DeletedAll
                        // Refresh notification lists
                        loadNotifications()
                        loadUnreadNotifications()
                        loadPrioritizedNotifications()
                    }
                    is Result.Error -> {
                        _actionState.value = NotificationActionState.Error(result.message)
                    }
                    is Result.Loading -> {
                        _actionState.value = NotificationActionState.Processing
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error deleting all notifications")
                _actionState.value = NotificationActionState.Error(e.message ?: "Failed to delete all notifications")
            } finally {
                // Reset state after a delay
                kotlinx.coroutines.delay(2000)
                _actionState.value = NotificationActionState.Idle
            }
        }
    }
    
    /**
     * Update notification preferences
     * 
     * @param preferences Map of notification type to enabled status
     */
    fun updateNotificationPreferences(preferences: Map<NotificationType, Boolean>) {
        viewModelScope.launch {
            try {
                _actionState.value = NotificationActionState.Processing
                val result = notificationRepository.updateNotificationPreferences(preferences)
                
                when (result) {
                    is Result.Success -> {
                        _actionState.value = NotificationActionState.PreferencesUpdated
                        // Refresh notification preferences
                        loadNotificationPreferences()
                    }
                    is Result.Error -> {
                        _actionState.value = NotificationActionState.Error(result.message)
                    }
                    is Result.Loading -> {
                        _actionState.value = NotificationActionState.Processing
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error updating notification preferences")
                _actionState.value = NotificationActionState.Error(e.message ?: "Failed to update notification preferences")
            } finally {
                // Reset state after a delay
                kotlinx.coroutines.delay(2000)
                _actionState.value = NotificationActionState.Idle
            }
        }
    }
    
    /**
     * Toggle a specific notification preference
     * 
     * @param type Notification type
     * @param enabled Whether the notification type should be enabled
     */
    fun toggleNotificationPreference(type: NotificationType, enabled: Boolean) {
        val currentPreferences = (_notificationPreferences.value as? Result.Success)?.data ?: return
        val updatedPreferences = currentPreferences.toMutableMap().apply {
            this[type] = enabled
        }
        updateNotificationPreferences(updatedPreferences)
    }
    
    /**
     * Register FCM token
     * 
     * @param token FCM token
     */
    fun registerFcmToken(token: String) {
        viewModelScope.launch {
            try {
                notificationRepository.registerFcmToken(token)
            } catch (e: Exception) {
                Timber.e(e, "Error registering FCM token")
            }
        }
    }
    
    /**
     * Unregister FCM token
     */
    fun unregisterFcmToken() {
        viewModelScope.launch {
            try {
                notificationRepository.unregisterFcmToken()
            } catch (e: Exception) {
                Timber.e(e, "Error unregistering FCM token")
            }
        }
    }
}

/**
 * Notification action state
 */
sealed class NotificationActionState {
    object Idle : NotificationActionState()
    object Processing : NotificationActionState()
    object MarkedAllAsRead : NotificationActionState()
    object DeletedAll : NotificationActionState()
    object PreferencesUpdated : NotificationActionState()
    data class Error(val message: String) : NotificationActionState()
}