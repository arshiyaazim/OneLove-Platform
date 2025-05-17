package com.kilagee.onelove.ui.notifications

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.kilagee.onelove.data.model.Notification
import com.kilagee.onelove.data.model.NotificationActionType
import com.kilagee.onelove.domain.repository.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository,
    private val auth: FirebaseAuth
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(NotificationsUIState())
    val uiState: StateFlow<NotificationsUIState> = _uiState
    
    private var _actionHandler: ((Intent) -> Unit)? = null
    
    fun setActionHandler(handler: (Intent) -> Unit) {
        _actionHandler = handler
    }
    
    fun loadNotifications() {
        val currentUser = auth.currentUser ?: return
        
        _uiState.update { it.copy(isLoading = true) }
        
        viewModelScope.launch {
            notificationRepository.getUserNotifications(currentUser.uid)
                .catch { e ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = e.message ?: "Failed to load notifications"
                        )
                    }
                }
                .collectLatest { notifications ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            notifications = notifications,
                            error = null
                        )
                    }
                }
        }
    }
    
    fun markNotificationAsRead(notificationId: String) {
        viewModelScope.launch {
            notificationRepository.markNotificationAsRead(notificationId)
                .catch { /* Ignore errors */ }
                .collectLatest { /* Success, refresh notifications */ }
        }
        
        // Update local state
        _uiState.update { state ->
            val updatedNotifications = state.notifications.map { notification ->
                if (notification.id == notificationId) {
                    notification.copy(read = true)
                } else {
                    notification
                }
            }
            
            state.copy(notifications = updatedNotifications)
        }
    }
    
    fun markAllAsRead() {
        val currentUser = auth.currentUser ?: return
        
        viewModelScope.launch {
            notificationRepository.markAllNotificationsAsRead(currentUser.uid)
                .catch { /* Ignore errors */ }
                .collectLatest { /* Success */ }
        }
        
        // Update local state
        _uiState.update { state ->
            val updatedNotifications = state.notifications.map { notification ->
                notification.copy(read = true)
            }
            
            state.copy(notifications = updatedNotifications)
        }
    }
    
    fun handleNotificationClick(notification: Notification) {
        // Mark notification as read
        markNotificationAsRead(notification.id)
        
        // Handle notification action if any
        val actionHandler = _actionHandler ?: return
        
        when (notification.actionType) {
            NotificationActionType.OPEN_CONVERSATION -> {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setPackage("com.kilagee.onelove")
                    setClassName("com.kilagee.onelove", "com.kilagee.onelove.ui.chat.ChatActivity")
                    putExtra("conversation_id", notification.actionData)
                }
                actionHandler(intent)
            }
            NotificationActionType.OPEN_PROFILE -> {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setPackage("com.kilagee.onelove")
                    setClassName("com.kilagee.onelove", "com.kilagee.onelove.ui.profile.ProfileActivity")
                    putExtra("profile_id", notification.actionData)
                }
                actionHandler(intent)
            }
            NotificationActionType.OPEN_CALL -> {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setPackage("com.kilagee.onelove")
                    setClassName("com.kilagee.onelove", "com.kilagee.onelove.ui.calls.CallActivity")
                    putExtra("call_id", notification.actionData)
                }
                actionHandler(intent)
            }
            NotificationActionType.OPEN_PAYMENT -> {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setPackage("com.kilagee.onelove")
                    setClassName("com.kilagee.onelove", "com.kilagee.onelove.ui.payment.PaymentDetailsActivity")
                    putExtra("payment_id", notification.actionData)
                }
                actionHandler(intent)
            }
            NotificationActionType.OPEN_SUBSCRIPTION -> {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setPackage("com.kilagee.onelove")
                    setClassName("com.kilagee.onelove", "com.kilagee.onelove.ui.subscription.MyMembershipActivity")
                }
                actionHandler(intent)
            }
            NotificationActionType.OPEN_OFFER -> {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setPackage("com.kilagee.onelove")
                    setClassName("com.kilagee.onelove", "com.kilagee.onelove.ui.offers.OfferDetailsActivity")
                    putExtra("offer_id", notification.actionData)
                }
                actionHandler(intent)
            }
            NotificationActionType.CUSTOM_ACTIVITY -> {
                notification.actionData?.let { actionData ->
                    try {
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            setPackage("com.kilagee.onelove")
                            setClassName("com.kilagee.onelove", actionData)
                        }
                        actionHandler(intent)
                    } catch (e: Exception) {
                        // Invalid action data, ignore
                    }
                }
            }
            null -> {
                // No specific action, just mark as read
            }
        }
    }
    
    fun deleteNotification(notificationId: String) {
        viewModelScope.launch {
            notificationRepository.deleteNotification(notificationId)
                .catch { /* Ignore errors */ }
                .collectLatest { /* Success */ }
        }
        
        // Update local state
        _uiState.update { state ->
            val updatedNotifications = state.notifications.filter { it.id != notificationId }
            state.copy(notifications = updatedNotifications)
        }
    }
}

data class NotificationsUIState(
    val notifications: List<Notification> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)