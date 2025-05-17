package com.kilagee.onelove.domain.model

import java.util.UUID

/**
 * Data class representing a system notification sent by admins
 */
data class SystemNotification(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val message: String = "",
    val imageUrl: String? = null,
    val deepLink: String? = null,
    val targetType: NotificationTargetType = NotificationTargetType.ALL_USERS,
    val targetIds: List<String> = emptyList(),
    val targetFilters: Map<String, Any> = emptyMap(),
    val priority: NotificationPriority = NotificationPriority.NORMAL,
    val expiresAt: Long? = null,
    val sentAt: Long = System.currentTimeMillis(),
    val sentBy: String = "",
    val sentByName: String = "",
    val sentCount: Int = 0,
    val readCount: Int = 0,
    val clickCount: Int = 0,
    val category: String = "announcement"
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "title" to title,
            "message" to message,
            "imageUrl" to imageUrl,
            "deepLink" to deepLink,
            "targetType" to targetType.name,
            "targetIds" to targetIds,
            "targetFilters" to targetFilters,
            "priority" to priority.name,
            "expiresAt" to expiresAt,
            "sentAt" to sentAt,
            "sentBy" to sentBy,
            "sentByName" to sentByName,
            "sentCount" to sentCount,
            "readCount" to readCount,
            "clickCount" to clickCount,
            "category" to category
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any?>): SystemNotification {
            val targetTypeStr = map["targetType"] as? String ?: NotificationTargetType.ALL_USERS.name
            val targetType = try {
                NotificationTargetType.valueOf(targetTypeStr)
            } catch (e: Exception) {
                NotificationTargetType.ALL_USERS
            }

            val priorityStr = map["priority"] as? String ?: NotificationPriority.NORMAL.name
            val priority = try {
                NotificationPriority.valueOf(priorityStr)
            } catch (e: Exception) {
                NotificationPriority.NORMAL
            }

            @Suppress("UNCHECKED_CAST")
            return SystemNotification(
                id = map["id"] as? String ?: UUID.randomUUID().toString(),
                title = map["title"] as? String ?: "",
                message = map["message"] as? String ?: "",
                imageUrl = map["imageUrl"] as? String,
                deepLink = map["deepLink"] as? String,
                targetType = targetType,
                targetIds = (map["targetIds"] as? List<String>) ?: emptyList(),
                targetFilters = (map["targetFilters"] as? Map<String, Any>) ?: emptyMap(),
                priority = priority,
                expiresAt = map["expiresAt"] as? Long,
                sentAt = (map["sentAt"] as? Long) ?: System.currentTimeMillis(),
                sentBy = map["sentBy"] as? String ?: "",
                sentByName = map["sentByName"] as? String ?: "",
                sentCount = (map["sentCount"] as? Number)?.toInt() ?: 0,
                readCount = (map["readCount"] as? Number)?.toInt() ?: 0,
                clickCount = (map["clickCount"] as? Number)?.toInt() ?: 0,
                category = map["category"] as? String ?: "announcement"
            )
        }
    }
}

/**
 * Enum representing notification target types
 */
enum class NotificationTargetType {
    ALL_USERS,         // Send to all users
    SPECIFIC_USERS,    // Send to specific users by ID
    PREMIUM_USERS,     // Send to all premium users
    NON_PREMIUM_USERS, // Send to all non-premium users
    NEW_USERS,         // Send to users who registered recently
    INACTIVE_USERS,    // Send to users who haven't been active recently
    FILTERED_USERS     // Send to users matching specific criteria
}

/**
 * Enum representing notification priority levels
 */
enum class NotificationPriority {
    LOW,
    NORMAL,
    HIGH,
    URGENT
}