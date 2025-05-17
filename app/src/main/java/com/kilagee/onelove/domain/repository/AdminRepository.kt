package com.kilagee.onelove.domain.repository

import com.kilagee.onelove.domain.model.AdminUser
import com.kilagee.onelove.domain.model.AIProfile
import com.kilagee.onelove.domain.model.UserProfile
import com.kilagee.onelove.domain.model.VerificationRequest
import com.kilagee.onelove.domain.model.SubscriptionRequest
import com.kilagee.onelove.domain.model.AppSettings
import com.kilagee.onelove.domain.model.AdminLog
import com.kilagee.onelove.domain.model.SystemNotification
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for admin-related operations
 */
interface AdminRepository {
    
    /**
     * Check if a user is an admin
     */
    fun checkAdminStatus(userId: String): Flow<Boolean>
    
    /**
     * Get admin user details
     */
    fun getAdminUser(userId: String): Flow<AdminUser?>
    
    /**
     * Get all admin users
     */
    fun getAllAdminUsers(): Flow<List<AdminUser>>
    
    /**
     * Create or update an admin user
     */
    suspend fun saveAdminUser(adminUser: AdminUser): Result<AdminUser>
    
    /**
     * Remove admin privileges from a user
     */
    suspend fun removeAdminUser(adminUserId: String): Result<Boolean>
    
    /**
     * Get all user profiles with pagination
     */
    fun getAllUsers(limit: Int = 20, lastUserId: String? = null): Flow<List<UserProfile>>
    
    /**
     * Search for users by name, email, or other criteria
     */
    fun searchUsers(query: String): Flow<List<UserProfile>>
    
    /**
     * Get a specific user profile
     */
    fun getUserProfile(userId: String): Flow<UserProfile?>
    
    /**
     * Update a user profile
     */
    suspend fun updateUserProfile(profile: UserProfile): Result<UserProfile>
    
    /**
     * Delete a user account
     */
    suspend fun deleteUser(userId: String): Result<Boolean>
    
    /**
     * Get all verification requests with pagination
     */
    fun getPendingVerificationRequests(limit: Int = 20, lastRequestId: String? = null): Flow<List<VerificationRequest>>
    
    /**
     * Approve a verification request
     */
    suspend fun approveVerification(requestId: String, adminId: String, notes: String? = null): Result<Boolean>
    
    /**
     * Reject a verification request
     */
    suspend fun rejectVerification(requestId: String, adminId: String, reason: String): Result<Boolean>
    
    /**
     * Get all pending subscription requests or manual upgrades
     */
    fun getPendingSubscriptionRequests(limit: Int = 20, lastRequestId: String? = null): Flow<List<SubscriptionRequest>>
    
    /**
     * Approve a subscription request
     */
    suspend fun approveSubscription(requestId: String, adminId: String): Result<Boolean>
    
    /**
     * Reject a subscription request
     */
    suspend fun rejectSubscription(requestId: String, adminId: String, reason: String): Result<Boolean>
    
    /**
     * Get all AI profiles
     */
    fun getAllAIProfiles(limit: Int = 50, lastProfileId: String? = null): Flow<List<AIProfile>>
    
    /**
     * Create a new AI profile
     */
    suspend fun createAIProfile(profile: AIProfile): Result<AIProfile>
    
    /**
     * Update an existing AI profile
     */
    suspend fun updateAIProfile(profile: AIProfile): Result<AIProfile>
    
    /**
     * Delete an AI profile
     */
    suspend fun deleteAIProfile(profileId: String): Result<Boolean>
    
    /**
     * Get system app settings
     */
    fun getAppSettings(): Flow<AppSettings>
    
    /**
     * Update system app settings
     */
    suspend fun updateAppSettings(settings: AppSettings): Result<AppSettings>
    
    /**
     * Get admin activity logs
     */
    fun getAdminLogs(limit: Int = 100, lastLogId: String? = null): Flow<List<AdminLog>>
    
    /**
     * Log an admin action
     */
    suspend fun logAdminAction(log: AdminLog): Result<AdminLog>
    
    /**
     * Get app usage analytics
     */
    fun getAppAnalytics(startDate: Long, endDate: Long): Flow<Map<String, Any>>
    
    /**
     * Send a system notification to all users or specific user groups
     */
    suspend fun sendSystemNotification(notification: SystemNotification): Result<Boolean>
    
    /**
     * Get flagged content (reported messages, profiles, etc.)
     */
    fun getFlaggedContent(limit: Int = 20, lastItemId: String? = null): Flow<List<Any>>
    
    /**
     * Remove flagged content
     */
    suspend fun removeFlaggedContent(contentId: String, contentType: String, adminId: String, reason: String): Result<Boolean>
}