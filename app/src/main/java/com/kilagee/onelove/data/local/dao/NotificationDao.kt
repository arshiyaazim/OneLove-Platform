package com.kilagee.onelove.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.kilagee.onelove.data.model.Notification
import com.kilagee.onelove.data.model.NotificationType
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Notification-related operations
 */
@Dao
interface NotificationDao {
    
    @Query("SELECT * FROM notifications WHERE id = :notificationId")
    fun getNotificationById(notificationId: String): Flow<Notification?>
    
    @Query("SELECT * FROM notifications WHERE userId = :userId ORDER BY createdAt DESC")
    fun getNotificationsForUser(userId: String): Flow<List<Notification>>
    
    @Query("SELECT * FROM notifications WHERE userId = :userId AND isRead = 0 ORDER BY createdAt DESC")
    fun getUnreadNotificationsForUser(userId: String): Flow<List<Notification>>
    
    @Query("SELECT * FROM notifications WHERE userId = :userId AND type = :type ORDER BY createdAt DESC")
    fun getNotificationsForUserByType(userId: String, type: NotificationType): Flow<List<Notification>>
    
    @Query("SELECT COUNT(*) FROM notifications WHERE userId = :userId AND isRead = 0")
    fun getUnreadNotificationCount(userId: String): Flow<Int>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: Notification)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotifications(notifications: List<Notification>)
    
    @Update
    suspend fun updateNotification(notification: Notification)
    
    @Query("UPDATE notifications SET isRead = 1 WHERE id = :notificationId")
    suspend fun markNotificationAsRead(notificationId: String)
    
    @Query("UPDATE notifications SET isRead = 1 WHERE userId = :userId")
    suspend fun markAllNotificationsAsRead(userId: String)
    
    @Delete
    suspend fun deleteNotification(notification: Notification)
    
    @Query("DELETE FROM notifications WHERE id = :notificationId")
    suspend fun deleteNotificationById(notificationId: String)
    
    @Query("DELETE FROM notifications WHERE userId = :userId")
    suspend fun deleteAllNotificationsForUser(userId: String)
    
    @Query("""
        DELETE FROM notifications 
        WHERE userId = :userId AND createdAt < :timestamp
        AND isRead = 1
    """)
    suspend fun deleteOldReadNotifications(userId: String, timestamp: Long)
    
    @Query("""
        SELECT n.* FROM notifications n
        JOIN users u ON n.relatedUserId = u.id
        WHERE n.userId = :userId AND u.name LIKE '%' || :query || '%'
        ORDER BY n.createdAt DESC
    """)
    fun searchNotifications(userId: String, query: String): Flow<List<Notification>>
}