package com.kilagee.onelove.data.repository

import com.kilagee.onelove.data.model.Notification
import com.kilagee.onelove.data.model.NotificationActionType
import com.kilagee.onelove.data.model.NotificationType
import com.kilagee.onelove.data.model.Result
import kotlinx.coroutines.flow.Flow
import java.util.Date

/**
 * Repository interface for notifications
 */
interface NotificationRepository {
    
    /**
     * Get notification by ID
     * @param notificationId Notification ID
     * @return Result containing the notification or error
     */
    suspend fun getNotificationById(notificationId: String): Result<Notification>
    
    /**
     * Get user notifications
     * @param userId User ID
     * @param limit Maximum number of results
     * @return Result containing list of notifications or error
     */
    suspend fun getUserNotifications(userId: String, limit: Int = 50): Result<List<Notification>>
    
    /**
     * Get user notifications as Flow
     * @param userId User ID
     * @return Flow emitting Result containing list of notifications or error
     */
    fun getUserNotificationsFlow(userId: String): Flow<Result<List<Notification>>>
    
    /**
     * Get unread notifications count
     * @param userId User ID
     * @return Result containing the unread count or error
     */
    suspend fun getUnreadNotificationsCount(userId: String): Result<Int>
    
    /**
     * Get unread notifications count as Flow
     * @param userId User ID
     * @return Flow emitting Result containing the unread count or error
     */
    fun getUnreadNotificationsCountFlow(userId: String): Flow<Result<Int>>
    
    /**
     * Mark notification as read
     * @param notificationId Notification ID
     * @return Result indicating success or error
     */
    suspend fun markNotificationAsRead(notificationId: String): Result<Unit>
    
    /**
     * Mark all user notifications as read
     * @param userId User ID
     * @return Result indicating success or error
     */
    suspend fun markAllNotificationsAsRead(userId: String): Result<Unit>
    
    /**
     * Delete notification
     * @param notificationId Notification ID
     * @return Result indicating success or error
     */
    suspend fun deleteNotification(notificationId: String): Result<Unit>
    
    /**
     * Delete all user notifications
     * @param userId User ID
     * @return Result indicating success or error
     */
    suspend fun deleteAllUserNotifications(userId: String): Result<Unit>
    
    /**
     * Create user notification
     * @param userId User ID
     * @param title Notification title
     * @param body Notification body
     * @param notificationType Notification type
     * @param actionType Action type
     * @param actionId Action ID
     * @param senderId Optional sender ID
     * @param senderName Optional sender name
     * @param senderAvatar Optional sender avatar URL
     * @param imageUrl Optional image URL
     * @param isImportant Whether notification is important
     * @param expiryDate Optional expiry date
     * @param deepLink Optional deep link
     * @return Result containing the created notification ID or error
     */
    suspend fun createNotification(
        userId: String,
        title: String,
        body: String,
        notificationType: NotificationType,
        actionType: NotificationActionType = NotificationActionType.NONE,
        actionId: String? = null,
        senderId: String? = null,
        senderName: String? = null,
        senderAvatar: String? = null,
        imageUrl: String? = null,
        isImportant: Boolean = false,
        expiryDate: Date? = null,
        deepLink: String? = null
    ): Result<String>
    
    /**
     * Send device notification
     * @param userId User ID
     * @param title Notification title
     * @param body Notification body
     * @param data Additional notification data
     * @return Result indicating success or error
     */
    suspend fun sendDeviceNotification(
        userId: String,
        title: String,
        body: String,
        data: Map<String, String>? = null
    ): Result<Unit>
    
    /**
     * Send notification to multiple users
     * @param userIds List of user IDs
     * @param title Notification title
     * @param body Notification body
     * @param notificationType Notification type
     * @param actionType Action type
     * @param actionId Action ID
     * @param data Additional notification data
     * @return Result indicating success or error
     */
    suspend fun sendBulkNotification(
        userIds: List<String>,
        title: String,
        body: String,
        notificationType: NotificationType,
        actionType: NotificationActionType = NotificationActionType.NONE,
        actionId: String? = null,
        data: Map<String, String>? = null
    ): Result<Unit>
    
    /**
     * Update user FCM token
     * @param userId User ID
     * @param token FCM token
     * @return Result indicating success or error
     */
    suspend fun updateUserFCMToken(userId: String, token: String): Result<Unit>
    
    /**
     * Get notification settings for user
     * @param userId User ID
     * @return Result containing notification settings or error
     */
    suspend fun getNotificationSettings(userId: String): Result<Map<String, Boolean>>
    
    /**
     * Update notification settings for user
     * @param userId User ID
     * @param settings Map of notification settings
     * @return Result indicating success or error
     */
    suspend fun updateNotificationSettings(
        userId: String,
        settings: Map<String, Boolean>
    ): Result<Unit>
    
    /**
     * Get notifications by type
     * @param userId User ID
     * @param type Notification type
     * @param limit Maximum number of results
     * @return Result containing list of notifications or error
     */
    suspend fun getNotificationsByType(
        userId: String,
        type: NotificationType,
        limit: Int = 20
    ): Result<List<Notification>>
    
    /**
     * Delete expired notifications
     * @return Result indicating success or error
     */
    suspend fun deleteExpiredNotifications(): Result<Unit>
}