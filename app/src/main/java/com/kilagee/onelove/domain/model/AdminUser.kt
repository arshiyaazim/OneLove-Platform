package com.kilagee.onelove.domain.model

import java.util.Date

/**
 * Data class representing an admin user in the app
 * Admin users have elevated privileges to manage app content and users
 */
data class AdminUser(
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val email: String = "",
    val role: AdminRole = AdminRole.MODERATOR,
    val permissions: List<AdminPermission> = emptyList(),
    val lastLogin: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    /**
     * Creates a map representation of this AdminUser object for Firestore
     */
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "userId" to userId,
            "name" to name,
            "email" to email,
            "role" to role.name,
            "permissions" to permissions.map { it.name },
            "lastLogin" to lastLogin,
            "createdAt" to createdAt,
            "updatedAt" to updatedAt
        )
    }
    
    companion object {
        /**
         * Creates an AdminUser from a map (Firestore document)
         */
        fun fromMap(map: Map<String, Any?>): AdminUser {
            val permissionStrings = (map["permissions"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
            val permissions = permissionStrings.mapNotNull { permString ->
                try {
                    AdminPermission.valueOf(permString)
                } catch (e: Exception) {
                    null
                }
            }
            
            val roleString = map["role"] as? String ?: AdminRole.MODERATOR.name
            val role = try {
                AdminRole.valueOf(roleString)
            } catch (e: Exception) {
                AdminRole.MODERATOR
            }
            
            return AdminUser(
                id = map["id"] as? String ?: "",
                userId = map["userId"] as? String ?: "",
                name = map["name"] as? String ?: "",
                email = map["email"] as? String ?: "",
                role = role,
                permissions = permissions,
                lastLogin = (map["lastLogin"] as? Long) ?: System.currentTimeMillis(),
                createdAt = (map["createdAt"] as? Long) ?: System.currentTimeMillis(),
                updatedAt = (map["updatedAt"] as? Long) ?: System.currentTimeMillis()
            )
        }
    }
    
    /**
     * Check if this admin has a specific permission
     */
    fun hasPermission(permission: AdminPermission): Boolean {
        // SUPER_ADMIN has all permissions
        if (role == AdminRole.SUPER_ADMIN) {
            return true
        }
        
        return permissions.contains(permission)
    }
}

/**
 * Enum representing admin roles in the system
 */
enum class AdminRole {
    MODERATOR,      // Basic admin role with limited permissions
    ADMIN,          // Standard admin with most permissions
    SUPER_ADMIN     // Highest level with all permissions
}

/**
 * Enum representing specific admin permissions
 */
enum class AdminPermission {
    // User management permissions
    VIEW_USERS,
    EDIT_USERS,
    DELETE_USERS,
    
    // Profile verification permissions
    APPROVE_VERIFICATION,
    REJECT_VERIFICATION,
    
    // Subscription management
    MANAGE_SUBSCRIPTIONS,
    
    // Content management
    VIEW_FLAGGED_CONTENT,
    REMOVE_CONTENT,
    
    // AI profile management
    MANAGE_AI_PROFILES,
    
    // System management
    VIEW_LOGS,
    VIEW_ANALYTICS,
    SEND_NOTIFICATIONS,
    MODIFY_SYSTEM_LABELS,
    MANAGE_APP_SETTINGS
}