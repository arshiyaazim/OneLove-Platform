package com.kilagee.onelove.data.database.dao

import androidx.room.*
import com.kilagee.onelove.data.model.Notification
import com.kilagee.onelove.data.model.NotificationType
import java.util.Date

/**
 * Data Access Object for Notification entities
 */
@Dao
interface NotificationDao {
    
    /**
     * Insert a notification
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: Notification)
    
    /**
     * Insert multiple notifications
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotifications(notifications: List<Notification>)
    
    /**
     * Get all notifications
     */
    @Query("SELECT * FROM notifications ORDER BY createdAt DESC")
    suspend fun getAllNotifications(): List<Notification>
    
    /**
     * Get all unread notifications
     */
    @Query("SELECT * FROM notifications WHERE isRead = 0 ORDER BY createdAt DESC")
    suspend fun getUnreadNotifications(): List<Notification>
    
    /**
     * Get notifications by type
     */
    @Query("SELECT * FROM notifications WHERE type = :type ORDER BY createdAt DESC")
    suspend fun getNotificationsByType(type: NotificationType): List<Notification>
    
    /**
     * Get notification by ID
     */
    @Query("SELECT * FROM notifications WHERE id = :notificationId")
    suspend fun getNotificationById(notificationId: String): Notification?
    
    /**
     * Get unread notifications count
     */
    @Query("SELECT COUNT(*) FROM notifications WHERE isRead = 0")
    suspend fun getUnreadNotificationsCount(): Int
    
    /**
     * Mark notification as read
     */
    @Query("UPDATE notifications SET isRead = 1, readAt = :readAt WHERE id = :notificationId")
    suspend fun markNotificationAsRead(notificationId: String, readAt: Date)
    
    /**
     * Mark all notifications as read
     */
    @Query("UPDATE notifications SET isRead = 1, readAt = :readAt WHERE isRead = 0")
    suspend fun markAllNotificationsAsRead(readAt: Date)
    
    /**
     * Delete notification by ID
     */
    @Query("DELETE FROM notifications WHERE id = :notificationId")
    suspend fun deleteNotification(notificationId: String)
    
    /**
     * Delete all notifications
     */
    @Query("DELETE FROM notifications")
    suspend fun deleteAllNotifications()
    
    /**
     * Delete notifications by type
     */
    @Query("DELETE FROM notifications WHERE type = :type")
    suspend fun deleteNotificationsByType(type: NotificationType)
    
    /**
     * Delete notifications older than a specific timestamp
     */
    @Query("DELETE FROM notifications WHERE createdAt < :timestamp")
    suspend fun deleteOldNotifications(timestamp: Long)
}