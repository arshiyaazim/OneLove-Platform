package com.kilagee.onelove.domain.model

import java.util.UUID

/**
 * Data class representing an admin activity log
 */
data class AdminLog(
    val id: String = UUID.randomUUID().toString(),
    val adminId: String = "",
    val adminName: String = "",
    val action: AdminAction = AdminAction.VIEW,
    val targetType: AdminTargetType = AdminTargetType.USER,
    val targetId: String = "",
    val details: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val ipAddress: String? = null,
    val deviceInfo: String? = null
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "adminId" to adminId,
            "adminName" to adminName,
            "action" to action.name,
            "targetType" to targetType.name,
            "targetId" to targetId,
            "details" to details,
            "timestamp" to timestamp,
            "ipAddress" to ipAddress,
            "deviceInfo" to deviceInfo
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any?>): AdminLog {
            val actionStr = map["action"] as? String ?: AdminAction.VIEW.name
            val action = try {
                AdminAction.valueOf(actionStr)
            } catch (e: Exception) {
                AdminAction.VIEW
            }

            val targetTypeStr = map["targetType"] as? String ?: AdminTargetType.USER.name
            val targetType = try {
                AdminTargetType.valueOf(targetTypeStr)
            } catch (e: Exception) {
                AdminTargetType.USER
            }

            return AdminLog(
                id = map["id"] as? String ?: UUID.randomUUID().toString(),
                adminId = map["adminId"] as? String ?: "",
                adminName = map["adminName"] as? String ?: "",
                action = action,
                targetType = targetType,
                targetId = map["targetId"] as? String ?: "",
                details = map["details"] as? String ?: "",
                timestamp = (map["timestamp"] as? Long) ?: System.currentTimeMillis(),
                ipAddress = map["ipAddress"] as? String,
                deviceInfo = map["deviceInfo"] as? String
            )
        }
    }
}

/**
 * Enum representing admin actions
 */
enum class AdminAction {
    VIEW,
    CREATE,
    UPDATE,
    DELETE,
    APPROVE,
    REJECT,
    SEND_NOTIFICATION,
    MODIFY_SETTINGS,
    LOGIN,
    LOGOUT
}

/**
 * Enum representing admin target types
 */
enum class AdminTargetType {
    USER,
    PROFILE,
    VERIFICATION,
    SUBSCRIPTION,
    AI_PROFILE,
    APP_SETTINGS,
    FLAGGED_CONTENT,
    NOTIFICATION,
    PAYMENT,
    SYSTEM
}