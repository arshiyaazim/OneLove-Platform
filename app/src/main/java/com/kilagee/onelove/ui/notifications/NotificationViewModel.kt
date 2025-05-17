package com.kilagee.onelove.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kilagee.onelove.data.model.Notification
import com.kilagee.onelove.domain.model.Resource
import com.kilagee.onelove.domain.repository.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository
) : ViewModel() {
    
    // State for notifications
    private val _notificationsState = MutableStateFlow<Resource<List<Notification>>>(Resource.Loading)
    val notificationsState: StateFlow<Resource<List<Notification>>> = _notificationsState
    
    // State for unread notification count
    private val _unreadCountState = MutableStateFlow<Resource<Int>>(Resource.Loading)
    val unreadCountState: StateFlow<Resource<Int>> = _unreadCountState
    
    // State for operation status (mark as read, delete, etc.)
    private val _operationState = MutableStateFlow<Resource<Unit>?>(null)
    val operationState: StateFlow<Resource<Unit>?> = _operationState
    
    init {
        loadNotifications()
        loadUnreadCount()
    }
    
    /**
     * Load all notifications
     */
    fun loadNotifications() {
        viewModelScope.launch {
            _notificationsState.value = Resource.Loading
            
            notificationRepository.getNotifications()
                .onEach { resource ->
                    _notificationsState.value = resource
                }
                .catch { e ->
                    _notificationsState.value = Resource.error("Failed to load notifications: ${e.message}")
                }
                .launchIn(viewModelScope)
        }
    }
    
    /**
     * Load unread notification count
     */
    fun loadUnreadCount() {
        viewModelScope.launch {
            _unreadCountState.value = Resource.Loading
            
            notificationRepository.getUnreadNotificationCount()
                .onEach { resource ->
                    _unreadCountState.value = resource
                }
                .catch { e ->
                    _unreadCountState.value = Resource.error("Failed to load unread count: ${e.message}")
                }
                .launchIn(viewModelScope)
        }
    }
    
    /**
     * Mark a notification as read
     */
    fun markAsRead(notificationId: String) {
        viewModelScope.launch {
            _operationState.value = Resource.Loading
            
            notificationRepository.markNotificationAsRead(notificationId)
                .onEach { resource ->
                    _operationState.value = resource
                    
                    if (resource is Resource.Success) {
                        loadNotifications()
                        loadUnreadCount()
                    }
                }
                .catch { e ->
                    _operationState.value = Resource.error("Failed to mark as read: ${e.message}")
                }
                .launchIn(viewModelScope)
        }
    }
    
    /**
     * Mark all notifications as read
     */
    fun markAllAsRead() {
        viewModelScope.launch {
            _operationState.value = Resource.Loading
            
            notificationRepository.markAllNotificationsAsRead()
                .onEach { resource ->
                    _operationState.value = resource
                    
                    if (resource is Resource.Success) {
                        loadNotifications()
                        loadUnreadCount()
                    }
                }
                .catch { e ->
                    _operationState.value = Resource.error("Failed to mark all as read: ${e.message}")
                }
                .launchIn(viewModelScope)
        }
    }
    
    /**
     * Delete a notification
     */
    fun deleteNotification(notificationId: String) {
        viewModelScope.launch {
            _operationState.value = Resource.Loading
            
            notificationRepository.deleteNotification(notificationId)
                .onEach { resource ->
                    _operationState.value = resource
                    
                    if (resource is Resource.Success) {
                        loadNotifications()
                        loadUnreadCount()
                    }
                }
                .catch { e ->
                    _operationState.value = Resource.error("Failed to delete notification: ${e.message}")
                }
                .launchIn(viewModelScope)
        }
    }
    
    /**
     * Delete all notifications
     */
    fun deleteAllNotifications() {
        viewModelScope.launch {
            _operationState.value = Resource.Loading
            
            notificationRepository.deleteAllNotifications()
                .onEach { resource ->
                    _operationState.value = resource
                    
                    if (resource is Resource.Success) {
                        loadNotifications()
                        loadUnreadCount()
                    }
                }
                .catch { e ->
                    _operationState.value = Resource.error("Failed to delete all notifications: ${e.message}")
                }
                .launchIn(viewModelScope)
        }
    }
    
    /**
     * Register FCM token
     */
    fun registerFcmToken(token: String) {
        viewModelScope.launch {
            notificationRepository.registerFcmToken(token)
                .onEach { /* No UI update needed */ }
                .catch { /* Silent failure */ }
                .launchIn(viewModelScope)
        }
    }
    
    /**
     * Unregister FCM token
     */
    fun unregisterFcmToken() {
        viewModelScope.launch {
            notificationRepository.unregisterFcmToken()
                .onEach { /* No UI update needed */ }
                .catch { /* Silent failure */ }
                .launchIn(viewModelScope)
        }
    }
    
    /**
     * Update notification settings
     */
    fun updateNotificationSettings(
        enableMatchNotifications: Boolean,
        enableMessageNotifications: Boolean,
        enableOfferNotifications: Boolean
    ) {
        viewModelScope.launch {
            _operationState.value = Resource.Loading
            
            notificationRepository.updateNotificationSettings(
                enableMatchNotifications = enableMatchNotifications,
                enableMessageNotifications = enableMessageNotifications,
                enableOfferNotifications = enableOfferNotifications
            )
                .onEach { resource ->
                    _operationState.value = resource
                }
                .catch { e ->
                    _operationState.value = Resource.error("Failed to update settings: ${e.message}")
                }
                .launchIn(viewModelScope)
        }
    }
    
    /**
     * Clear operation state
     */
    fun clearOperationState() {
        _operationState.value = null
    }
}