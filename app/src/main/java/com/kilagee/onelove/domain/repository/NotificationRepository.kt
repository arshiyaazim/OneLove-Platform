package com.kilagee.onelove.domain.repository

import com.kilagee.onelove.data.model.Notification
import com.kilagee.onelove.data.model.NotificationType
import com.kilagee.onelove.domain.util.Result
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for notification-related operations
 */
interface NotificationRepository {
    
    /**
     * Get all notifications for the current user
     * 
     * @param limit Maximum number of notifications to retrieve
     * @return Flow of a list of notifications
     */
    fun getNotifications(limit: Int = 50): Flow<Result<List<Notification>>>
    
    /**
     * Get unread notifications for the current user
     * 
     * @param limit Maximum number of notifications to retrieve
     * @return Flow of a list of notifications
     */
    fun getUnreadNotifications(limit: Int = 20): Flow<Result<List<Notification>>>
    
    /**
     * Get the count of unread notifications
     * 
     * @return Flow of the count
     */
    fun getUnreadNotificationCount(): Flow<Int>
    
    /**
     * Get notifications by type
     * 
     * @param type Type of notifications to retrieve
     * @param limit Maximum number of notifications to retrieve
     * @return Flow of a list of notifications
     */
    fun getNotificationsByType(
        type: NotificationType,
        limit: Int = 20
    ): Flow<Result<List<Notification>>>
    
    /**
     * Mark a notification as read
     * 
     * @param notificationId ID of the notification
     * @return Result of the operation
     */
    suspend fun markNotificationAsRead(notificationId: String): Result<Unit>
    
    /**
     * Mark all notifications as read
     * 
     * @return Result of the operation
     */
    suspend fun markAllNotificationsAsRead(): Result<Unit>
    
    /**
     * Delete a notification
     * 
     * @param notificationId ID of the notification
     * @return Result of the operation
     */
    suspend fun deleteNotification(notificationId: String): Result<Unit>
    
    /**
     * Delete all notifications
     * 
     * @return Result of the operation
     */
    suspend fun deleteAllNotifications(): Result<Unit>
    
    /**
     * Send a notification to a user
     * 
     * @param userId ID of the user to notify
     * @param type Type of notification
     * @param title Notification title
     * @param content Notification content
     * @param data Additional data for the notification
     * @return Result of the operation
     */
    suspend fun sendNotification(
        userId: String,
        type: NotificationType,
        title: String,
        content: String,
        data: Map<String, String> = emptyMap()
    ): Result<Notification>
    
    /**
     * Get notification preferences
     * 
     * @return Flow of a map of notification type to enabled status
     */
    fun getNotificationPreferences(): Flow<Result<Map<NotificationType, Boolean>>>
    
    /**
     * Update notification preferences
     * 
     * @param preferences Map of notification type to enabled status
     * @return Result of the operation
     */
    suspend fun updateNotificationPreferences(
        preferences: Map<NotificationType, Boolean>
    ): Result<Unit>
    
    /**
     * Register FCM token
     * 
     * @param token FCM token
     * @return Result of the operation
     */
    suspend fun registerFcmToken(token: String): Result<Unit>
    
    /**
     * Unregister FCM token
     * 
     * @return Result of the operation
     */
    suspend fun unregisterFcmToken(): Result<Unit>
    
    /**
     * Get prioritized notifications using the smart algorithm
     * 
     * @param limit Maximum number of notifications to retrieve
     * @return Flow of a list of prioritized notifications
     */
    fun getPrioritizedNotifications(limit: Int = 20): Flow<Result<List<Notification>>>
}