package com.kilagee.onelove.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.kilagee.onelove.data.database.dao.NotificationDao
import com.kilagee.onelove.data.model.Notification
import com.kilagee.onelove.data.model.NotificationActionType
import com.kilagee.onelove.data.model.NotificationType
import com.kilagee.onelove.domain.model.Resource
import com.kilagee.onelove.domain.repository.NotificationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseNotificationRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val notificationDao: NotificationDao
) : NotificationRepository {
    
    private val notificationsCollection = firestore.collection("notifications")
    
    override fun getUserNotifications(userId: String): Flow<List<Notification>> = callbackFlow {
        val listenerRegistration = notificationsCollection
            .whereEqualTo("userId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // Send empty list on error
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    val notifications = snapshot.documents.mapNotNull { doc ->
                        try {
                            val id = doc.id
                            val data = doc.data ?: return@mapNotNull null
                            
                            val userId = data["userId"] as? String ?: return@mapNotNull null
                            val title = data["title"] as? String ?: return@mapNotNull null
                            val body = data["body"] as? String ?: return@mapNotNull null
                            val timestamp = data["timestamp"] as? Date ?: Date()
                            val read = data["read"] as? Boolean ?: false
                            val typeStr = data["type"] as? String ?: return@mapNotNull null
                            
                            @Suppress("UNCHECKED_CAST")
                            val notificationData = data["data"] as? Map<String, String> ?: emptyMap()
                            
                            val actionTypeStr = data["actionType"] as? String
                            val actionData = data["actionData"] as? String
                            
                            val type = try {
                                NotificationType.valueOf(typeStr)
                            } catch (e: Exception) {
                                NotificationType.SYSTEM
                            }
                            
                            val actionType = if (actionTypeStr != null) {
                                try {
                                    NotificationActionType.valueOf(actionTypeStr)
                                } catch (e: Exception) {
                                    null
                                }
                            } else null
                            
                            Notification(
                                id = id,
                                userId = userId,
                                title = title,
                                body = body,
                                timestamp = timestamp,
                                read = read,
                                type = type,
                                data = notificationData,
                                actionType = actionType,
                                actionData = actionData
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }
                    
                    // Update local database
                    try {
                        notificationDao.insertNotifications(notifications)
                    } catch (e: Exception) {
                        // Ignore database errors
                    }
                    
                    trySend(notifications)
                } else {
                    trySend(emptyList())
                }
            }
        
        awaitClose { listenerRegistration.remove() }
    }.flowOn(Dispatchers.IO)
    
    override fun getUnreadNotifications(userId: String): Flow<List<Notification>> = callbackFlow {
        val listenerRegistration = notificationsCollection
            .whereEqualTo("userId", userId)
            .whereEqualTo("read", false)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // Send empty list on error
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    val notifications = snapshot.documents.mapNotNull { doc ->
                        try {
                            val id = doc.id
                            val data = doc.data ?: return@mapNotNull null
                            
                            val userId = data["userId"] as? String ?: return@mapNotNull null
                            val title = data["title"] as? String ?: return@mapNotNull null
                            val body = data["body"] as? String ?: return@mapNotNull null
                            val timestamp = data["timestamp"] as? Date ?: Date()
                            val read = data["read"] as? Boolean ?: false
                            val typeStr = data["type"] as? String ?: return@mapNotNull null
                            
                            @Suppress("UNCHECKED_CAST")
                            val notificationData = data["data"] as? Map<String, String> ?: emptyMap()
                            
                            val actionTypeStr = data["actionType"] as? String
                            val actionData = data["actionData"] as? String
                            
                            val type = try {
                                NotificationType.valueOf(typeStr)
                            } catch (e: Exception) {
                                NotificationType.SYSTEM
                            }
                            
                            val actionType = if (actionTypeStr != null) {
                                try {
                                    NotificationActionType.valueOf(actionTypeStr)
                                } catch (e: Exception) {
                                    null
                                }
                            } else null
                            
                            Notification(
                                id = id,
                                userId = userId,
                                title = title,
                                body = body,
                                timestamp = timestamp,
                                read = read,
                                type = type,
                                data = notificationData,
                                actionType = actionType,
                                actionData = actionData
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }
                    
                    trySend(notifications)
                } else {
                    trySend(emptyList())
                }
            }
        
        awaitClose { listenerRegistration.remove() }
    }.flowOn(Dispatchers.IO)
    
    override fun getNotificationById(notificationId: String): Flow<Resource<Notification>> = flow {
        emit(Resource.Loading)
        
        try {
            // Try to get from local database first
            val localNotification = notificationDao.getNotificationById(notificationId)
            if (localNotification != null) {
                emit(Resource.success(localNotification))
            }
            
            // Get from Firestore
            val documentSnapshot = notificationsCollection.document(notificationId).get().await()
            if (documentSnapshot.exists()) {
                val data = documentSnapshot.data
                if (data != null) {
                    try {
                        val userId = data["userId"] as? String ?: throw Exception("Invalid userId")
                        val title = data["title"] as? String ?: throw Exception("Invalid title")
                        val body = data["body"] as? String ?: throw Exception("Invalid body")
                        val timestamp = data["timestamp"] as? Date ?: Date()
                        val read = data["read"] as? Boolean ?: false
                        val typeStr = data["type"] as? String ?: throw Exception("Invalid type")
                        
                        @Suppress("UNCHECKED_CAST")
                        val notificationData = data["data"] as? Map<String, String> ?: emptyMap()
                        
                        val actionTypeStr = data["actionType"] as? String
                        val actionData = data["actionData"] as? String
                        
                        val type = try {
                            NotificationType.valueOf(typeStr)
                        } catch (e: Exception) {
                            NotificationType.SYSTEM
                        }
                        
                        val actionType = if (actionTypeStr != null) {
                            try {
                                NotificationActionType.valueOf(actionTypeStr)
                            } catch (e: Exception) {
                                null
                            }
                        } else null
                        
                        val notification = Notification(
                            id = notificationId,
                            userId = userId,
                            title = title,
                            body = body,
                            timestamp = timestamp,
                            read = read,
                            type = type,
                            data = notificationData,
                            actionType = actionType,
                            actionData = actionData
                        )
                        
                        // Update local database
                        notificationDao.insertNotification(notification)
                        
                        emit(Resource.success(notification))
                    } catch (e: Exception) {
                        emit(Resource.error("Invalid notification data: ${e.message}"))
                    }
                } else {
                    emit(Resource.error("Invalid notification data"))
                }
            } else if (localNotification == null) {
                emit(Resource.error("Notification not found"))
            }
        } catch (e: Exception) {
            emit(Resource.error("Failed to get notification: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)
    
    override fun markNotificationAsRead(notificationId: String): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading)
        
        try {
            // Update in Firestore
            notificationsCollection.document(notificationId)
                .update("read", true)
                .await()
            
            // Update in local database
            notificationDao.markNotificationAsRead(notificationId)
            
            emit(Resource.success(Unit))
        } catch (e: Exception) {
            emit(Resource.error("Failed to mark notification as read: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)
    
    override fun markAllNotificationsAsRead(userId: String): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading)
        
        try {
            // Get all unread notifications
            val query = notificationsCollection
                .whereEqualTo("userId", userId)
                .whereEqualTo("read", false)
                .get()
                .await()
            
            // Update each notification
            val batch = firestore.batch()
            for (document in query.documents) {
                batch.update(document.reference, "read", true)
            }
            
            // Commit the batch
            batch.commit().await()
            
            // Update in local database
            notificationDao.markAllNotificationsAsRead(userId)
            
            emit(Resource.success(Unit))
        } catch (e: Exception) {
            emit(Resource.error("Failed to mark all notifications as read: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)
    
    override fun createNotification(notification: Notification): Flow<Resource<Notification>> = flow {
        emit(Resource.Loading)
        
        try {
            val notificationData = hashMapOf(
                "userId" to notification.userId,
                "title" to notification.title,
                "body" to notification.body,
                "timestamp" to notification.timestamp,
                "read" to notification.read,
                "type" to notification.type.name,
                "data" to notification.data,
                "actionType" to (notification.actionType?.name),
                "actionData" to notification.actionData
            )
            
            // Save to Firestore
            val documentReference = if (notification.id.isNotEmpty()) {
                notificationsCollection.document(notification.id)
            } else {
                notificationsCollection.document()
            }
            
            documentReference.set(notificationData).await()
            
            val savedNotification = notification.copy(id = documentReference.id)
            
            // Save to local database
            notificationDao.insertNotification(savedNotification)
            
            emit(Resource.success(savedNotification))
        } catch (e: Exception) {
            emit(Resource.error("Failed to create notification: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)
    
    override fun deleteNotification(notificationId: String): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading)
        
        try {
            // Delete from Firestore
            notificationsCollection.document(notificationId)
                .delete()
                .await()
            
            // Delete from local database
            notificationDao.deleteNotification(notificationId)
            
            emit(Resource.success(Unit))
        } catch (e: Exception) {
            emit(Resource.error("Failed to delete notification: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)
    
    override fun deleteAllNotifications(userId: String): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading)
        
        try {
            // Get all notifications for the user
            val query = notificationsCollection
                .whereEqualTo("userId", userId)
                .get()
                .await()
            
            // Delete each notification
            val batch = firestore.batch()
            for (document in query.documents) {
                batch.delete(document.reference)
            }
            
            // Commit the batch
            batch.commit().await()
            
            // Delete from local database
            notificationDao.deleteAllNotifications(userId)
            
            emit(Resource.success(Unit))
        } catch (e: Exception) {
            emit(Resource.error("Failed to delete all notifications: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)
    
    override fun getUnreadNotificationCount(userId: String): Flow<Int> = callbackFlow {
        val listenerRegistration = notificationsCollection
            .whereEqualTo("userId", userId)
            .whereEqualTo("read", false)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    trySend(0)
                    return@addSnapshotListener
                }
                
                trySend(snapshot.size())
            }
        
        awaitClose { listenerRegistration.remove() }
    }.flowOn(Dispatchers.IO)
}