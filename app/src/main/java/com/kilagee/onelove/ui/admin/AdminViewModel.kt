package com.kilagee.onelove.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.kilagee.onelove.domain.model.AdminAction
import com.kilagee.onelove.domain.model.AdminLog
import com.kilagee.onelove.domain.model.AdminPermission
import com.kilagee.onelove.domain.model.AdminTargetType
import com.kilagee.onelove.domain.model.AdminUser
import com.kilagee.onelove.domain.model.AIProfile
import com.kilagee.onelove.domain.model.AppSettings
import com.kilagee.onelove.domain.model.NotificationPriority
import com.kilagee.onelove.domain.model.NotificationTargetType
import com.kilagee.onelove.domain.model.SubscriptionRequest
import com.kilagee.onelove.domain.model.SystemNotification
import com.kilagee.onelove.domain.model.UserProfile
import com.kilagee.onelove.domain.model.VerificationRequest
import com.kilagee.onelove.domain.repository.AdminRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class AdminViewModel @Inject constructor(
    private val adminRepository: AdminRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    // Admin status
    private val _isAdmin = MutableStateFlow(false)
    val isAdmin: StateFlow<Boolean> = _isAdmin.asStateFlow()

    // Current admin user
    private val _currentAdminUser = MutableStateFlow<AdminUser?>(null)
    val currentAdminUser: StateFlow<AdminUser?> = _currentAdminUser.asStateFlow()

    // Error state
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Success message
    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    // App settings
    private val _appSettings = MutableStateFlow(AppSettings())
    val appSettings: StateFlow<AppSettings> = _appSettings.asStateFlow()

    // Users
    private val _users = MutableStateFlow<List<UserProfile>>(emptyList())
    val users: StateFlow<List<UserProfile>> = _users.asStateFlow()

    // Selected user
    private val _selectedUser = MutableStateFlow<UserProfile?>(null)
    val selectedUser: StateFlow<UserProfile?> = _selectedUser.asStateFlow()

    // Verification requests
    private val _verificationRequests = MutableStateFlow<List<VerificationRequest>>(emptyList())
    val verificationRequests: StateFlow<List<VerificationRequest>> = _verificationRequests.asStateFlow()

    // Subscription requests
    private val _subscriptionRequests = MutableStateFlow<List<SubscriptionRequest>>(emptyList())
    val subscriptionRequests: StateFlow<List<SubscriptionRequest>> = _subscriptionRequests.asStateFlow()

    // AI profiles
    private val _aiProfiles = MutableStateFlow<List<AIProfile>>(emptyList())
    val aiProfiles: StateFlow<List<AIProfile>> = _aiProfiles.asStateFlow()

    // Admin logs
    private val _adminLogs = MutableStateFlow<List<AdminLog>>(emptyList())
    val adminLogs: StateFlow<List<AdminLog>> = _adminLogs.asStateFlow()

    // Analytics data
    private val _analyticsData = MutableStateFlow<Map<String, Any>>(emptyMap())
    val analyticsData: StateFlow<Map<String, Any>> = _analyticsData.asStateFlow()

    // Flagged content
    private val _flaggedContent = MutableStateFlow<List<Any>>(emptyList())
    val flaggedContent: StateFlow<List<Any>> = _flaggedContent.asStateFlow()

    // Selected tab
    private val _selectedTab = MutableStateFlow(AdminPanelTab.DASHBOARD)
    val selectedTab: StateFlow<AdminPanelTab> = _selectedTab.asStateFlow()

    // Current user ID
    private val currentUserId: String
        get() = auth.currentUser?.uid ?: ""

    init {
        // Check if the current user is an admin
        checkAdminStatus()
    }

    /**
     * Check if the current user is an admin
     */
    private fun checkAdminStatus() = viewModelScope.launch {
        auth.currentUser?.uid?.let { userId ->
            adminRepository.checkAdminStatus(userId).collectLatest { isAdmin ->
                _isAdmin.value = isAdmin
                
                if (isAdmin) {
                    // Load admin details
                    loadAdminDetails(userId)
                    
                    // Load initial data
                    loadAppSettings()
                }
            }
        }
    }

    /**
     * Load admin details
     */
    private fun loadAdminDetails(userId: String) = viewModelScope.launch {
        adminRepository.getAdminUser(userId).collectLatest { adminUser ->
            _currentAdminUser.value = adminUser
            
            // Log admin login
            adminUser?.let {
                logAdminAction(
                    action = AdminAction.LOGIN,
                    targetType = AdminTargetType.SYSTEM,
                    targetId = "system",
                    details = "Admin login from mobile app"
                )
            }
        }
    }

    /**
     * Switch to a different tab
     */
    fun switchTab(tab: AdminPanelTab) {
        _selectedTab.value = tab
        
        // Load data based on the selected tab
        when (tab) {
            AdminPanelTab.DASHBOARD -> {
                loadDashboardData()
            }
            AdminPanelTab.USERS -> {
                loadUsers()
            }
            AdminPanelTab.VERIFICATION -> {
                loadVerificationRequests()
            }
            AdminPanelTab.SUBSCRIPTIONS -> {
                loadSubscriptionRequests()
            }
            AdminPanelTab.AI_PROFILES -> {
                loadAIProfiles()
            }
            AdminPanelTab.APP_SETTINGS -> {
                loadAppSettings()
            }
            AdminPanelTab.ANALYTICS -> {
                loadAnalytics()
            }
            AdminPanelTab.FLAGGED_CONTENT -> {
                loadFlaggedContent()
            }
            AdminPanelTab.ACTIVITY_LOGS -> {
                loadAdminLogs()
            }
        }
    }

    /**
     * Load dashboard data
     */
    private fun loadDashboardData() = viewModelScope.launch {
        _isLoading.value = true
        
        try {
            // Load analytics for dashboard
            loadAnalytics()
            
            // Load recent verification requests
            adminRepository.getPendingVerificationRequests(5).collectLatest { requests ->
                _verificationRequests.value = requests
            }
            
            // Load recent subscription requests
            adminRepository.getPendingSubscriptionRequests(5).collectLatest { requests ->
                _subscriptionRequests.value = requests
            }
        } catch (e: Exception) {
            _error.value = "Failed to load dashboard data: ${e.message}"
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * Load app settings
     */
    private fun loadAppSettings() = viewModelScope.launch {
        _isLoading.value = true
        
        try {
            adminRepository.getAppSettings().collectLatest { settings ->
                _appSettings.value = settings
            }
        } catch (e: Exception) {
            _error.value = "Failed to load app settings: ${e.message}"
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * Update app settings
     */
    fun updateAppSettings(settings: AppSettings) = viewModelScope.launch {
        _isLoading.value = true
        
        try {
            val result = adminRepository.updateAppSettings(settings)
            
            if (result.isSuccess) {
                _appSettings.value = result.getOrNull() ?: settings
                _successMessage.value = "App settings updated successfully"
                
                // Log the action
                logAdminAction(
                    action = AdminAction.MODIFY_SETTINGS,
                    targetType = AdminTargetType.APP_SETTINGS,
                    targetId = settings.id,
                    details = "Updated app settings"
                )
            } else {
                _error.value = "Failed to update settings: ${result.exceptionOrNull()?.message}"
            }
        } catch (e: Exception) {
            _error.value = "Failed to update settings: ${e.message}"
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * Load users
     */
    fun loadUsers(query: String? = null) = viewModelScope.launch {
        _isLoading.value = true
        
        try {
            if (query != null && query.isNotEmpty()) {
                // Search users
                adminRepository.searchUsers(query).collectLatest { searchResults ->
                    _users.value = searchResults
                }
            } else {
                // Load all users
                adminRepository.getAllUsers().collectLatest { usersList ->
                    _users.value = usersList
                }
            }
        } catch (e: Exception) {
            _error.value = "Failed to load users: ${e.message}"
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * Load a specific user profile
     */
    fun loadUserProfile(userId: String) = viewModelScope.launch {
        _isLoading.value = true
        
        try {
            adminRepository.getUserProfile(userId).collectLatest { profile ->
                _selectedUser.value = profile
                
                // Log the action
                profile?.let {
                    logAdminAction(
                        action = AdminAction.VIEW,
                        targetType = AdminTargetType.USER,
                        targetId = userId,
                        details = "Viewed user profile: ${profile.displayName}"
                    )
                }
            }
        } catch (e: Exception) {
            _error.value = "Failed to load user profile: ${e.message}"
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * Update a user profile
     */
    fun updateUserProfile(profile: UserProfile) = viewModelScope.launch {
        _isLoading.value = true
        
        try {
            val result = adminRepository.updateUserProfile(profile)
            
            if (result.isSuccess) {
                _selectedUser.value = result.getOrNull()
                _successMessage.value = "User profile updated successfully"
                
                // Log the action
                logAdminAction(
                    action = AdminAction.UPDATE,
                    targetType = AdminTargetType.USER,
                    targetId = profile.id,
                    details = "Updated user profile: ${profile.displayName}"
                )
                
                // Refresh the users list
                loadUsers()
            } else {
                _error.value = "Failed to update profile: ${result.exceptionOrNull()?.message}"
            }
        } catch (e: Exception) {
            _error.value = "Failed to update profile: ${e.message}"
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * Delete a user
     */
    fun deleteUser(userId: String, userName: String) = viewModelScope.launch {
        _isLoading.value = true
        
        try {
            val result = adminRepository.deleteUser(userId)
            
            if (result.isSuccess) {
                _successMessage.value = "User deleted successfully"
                _selectedUser.value = null
                
                // Log the action
                logAdminAction(
                    action = AdminAction.DELETE,
                    targetType = AdminTargetType.USER,
                    targetId = userId,
                    details = "Deleted user: $userName"
                )
                
                // Refresh the users list
                loadUsers()
            } else {
                _error.value = "Failed to delete user: ${result.exceptionOrNull()?.message}"
            }
        } catch (e: Exception) {
            _error.value = "Failed to delete user: ${e.message}"
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * Load verification requests
     */
    fun loadVerificationRequests() = viewModelScope.launch {
        _isLoading.value = true
        
        try {
            adminRepository.getPendingVerificationRequests().collectLatest { requests ->
                _verificationRequests.value = requests
            }
        } catch (e: Exception) {
            _error.value = "Failed to load verification requests: ${e.message}"
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * Approve a verification request
     */
    fun approveVerification(requestId: String, notes: String? = null) = viewModelScope.launch {
        _isLoading.value = true
        
        try {
            val result = adminRepository.approveVerification(requestId, currentUserId, notes)
            
            if (result.isSuccess) {
                _successMessage.value = "Verification request approved"
                
                // Log the action
                logAdminAction(
                    action = AdminAction.APPROVE,
                    targetType = AdminTargetType.VERIFICATION,
                    targetId = requestId,
                    details = "Approved verification request"
                )
                
                // Refresh the verification requests
                loadVerificationRequests()
            } else {
                _error.value = "Failed to approve verification: ${result.exceptionOrNull()?.message}"
            }
        } catch (e: Exception) {
            _error.value = "Failed to approve verification: ${e.message}"
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * Reject a verification request
     */
    fun rejectVerification(requestId: String, reason: String) = viewModelScope.launch {
        _isLoading.value = true
        
        try {
            val result = adminRepository.rejectVerification(requestId, currentUserId, reason)
            
            if (result.isSuccess) {
                _successMessage.value = "Verification request rejected"
                
                // Log the action
                logAdminAction(
                    action = AdminAction.REJECT,
                    targetType = AdminTargetType.VERIFICATION,
                    targetId = requestId,
                    details = "Rejected verification request: $reason"
                )
                
                // Refresh the verification requests
                loadVerificationRequests()
            } else {
                _error.value = "Failed to reject verification: ${result.exceptionOrNull()?.message}"
            }
        } catch (e: Exception) {
            _error.value = "Failed to reject verification: ${e.message}"
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * Load subscription requests
     */
    fun loadSubscriptionRequests() = viewModelScope.launch {
        _isLoading.value = true
        
        try {
            adminRepository.getPendingSubscriptionRequests().collectLatest { requests ->
                _subscriptionRequests.value = requests
            }
        } catch (e: Exception) {
            _error.value = "Failed to load subscription requests: ${e.message}"
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * Approve a subscription request
     */
    fun approveSubscription(requestId: String) = viewModelScope.launch {
        _isLoading.value = true
        
        try {
            val result = adminRepository.approveSubscription(requestId, currentUserId)
            
            if (result.isSuccess) {
                _successMessage.value = "Subscription request approved"
                
                // Log the action
                logAdminAction(
                    action = AdminAction.APPROVE,
                    targetType = AdminTargetType.SUBSCRIPTION,
                    targetId = requestId,
                    details = "Approved subscription request"
                )
                
                // Refresh the subscription requests
                loadSubscriptionRequests()
            } else {
                _error.value = "Failed to approve subscription: ${result.exceptionOrNull()?.message}"
            }
        } catch (e: Exception) {
            _error.value = "Failed to approve subscription: ${e.message}"
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * Reject a subscription request
     */
    fun rejectSubscription(requestId: String, reason: String) = viewModelScope.launch {
        _isLoading.value = true
        
        try {
            val result = adminRepository.rejectSubscription(requestId, currentUserId, reason)
            
            if (result.isSuccess) {
                _successMessage.value = "Subscription request rejected"
                
                // Log the action
                logAdminAction(
                    action = AdminAction.REJECT,
                    targetType = AdminTargetType.SUBSCRIPTION,
                    targetId = requestId,
                    details = "Rejected subscription request: $reason"
                )
                
                // Refresh the subscription requests
                loadSubscriptionRequests()
            } else {
                _error.value = "Failed to reject subscription: ${result.exceptionOrNull()?.message}"
            }
        } catch (e: Exception) {
            _error.value = "Failed to reject subscription: ${e.message}"
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * Load AI profiles
     */
    fun loadAIProfiles() = viewModelScope.launch {
        _isLoading.value = true
        
        try {
            adminRepository.getAllAIProfiles().collectLatest { profiles ->
                _aiProfiles.value = profiles
            }
        } catch (e: Exception) {
            _error.value = "Failed to load AI profiles: ${e.message}"
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * Create or update an AI profile
     */
    fun saveAIProfile(profile: AIProfile) = viewModelScope.launch {
        _isLoading.value = true
        
        try {
            val result = adminRepository.createAIProfile(profile)
            
            if (result.isSuccess) {
                _successMessage.value = if (profile.id.isEmpty()) {
                    "AI profile created successfully"
                } else {
                    "AI profile updated successfully"
                }
                
                // Log the action
                logAdminAction(
                    action = if (profile.id.isEmpty()) AdminAction.CREATE else AdminAction.UPDATE,
                    targetType = AdminTargetType.AI_PROFILE,
                    targetId = result.getOrNull()?.id ?: "",
                    details = "${if (profile.id.isEmpty()) "Created" else "Updated"} AI profile: ${profile.name}"
                )
                
                // Refresh the AI profiles
                loadAIProfiles()
            } else {
                _error.value = "Failed to save AI profile: ${result.exceptionOrNull()?.message}"
            }
        } catch (e: Exception) {
            _error.value = "Failed to save AI profile: ${e.message}"
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * Delete an AI profile
     */
    fun deleteAIProfile(profileId: String, profileName: String) = viewModelScope.launch {
        _isLoading.value = true
        
        try {
            val result = adminRepository.deleteAIProfile(profileId)
            
            if (result.isSuccess) {
                _successMessage.value = "AI profile deleted successfully"
                
                // Log the action
                logAdminAction(
                    action = AdminAction.DELETE,
                    targetType = AdminTargetType.AI_PROFILE,
                    targetId = profileId,
                    details = "Deleted AI profile: $profileName"
                )
                
                // Refresh the AI profiles
                loadAIProfiles()
            } else {
                _error.value = "Failed to delete AI profile: ${result.exceptionOrNull()?.message}"
            }
        } catch (e: Exception) {
            _error.value = "Failed to delete AI profile: ${e.message}"
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * Load admin logs
     */
    fun loadAdminLogs() = viewModelScope.launch {
        _isLoading.value = true
        
        try {
            adminRepository.getAdminLogs().collectLatest { logs ->
                _adminLogs.value = logs
            }
        } catch (e: Exception) {
            _error.value = "Failed to load admin logs: ${e.message}"
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * Log an admin action
     */
    private fun logAdminAction(
        action: AdminAction,
        targetType: AdminTargetType,
        targetId: String,
        details: String
    ) = viewModelScope.launch {
        val currentAdmin = _currentAdminUser.value ?: return@launch
        
        val log = AdminLog(
            adminId = currentAdmin.id,
            adminName = currentAdmin.name,
            action = action,
            targetType = targetType,
            targetId = targetId,
            details = details,
            timestamp = System.currentTimeMillis()
        )
        
        try {
            adminRepository.logAdminAction(log)
        } catch (e: Exception) {
            // Silently fail, don't show error to the user for logging failures
        }
    }

    /**
     * Load analytics data
     */
    fun loadAnalytics(
        startDate: Long = System.currentTimeMillis() - (30 * 24 * 60 * 60 * 1000L), // 30 days ago
        endDate: Long = System.currentTimeMillis()
    ) = viewModelScope.launch {
        _isLoading.value = true
        
        try {
            adminRepository.getAppAnalytics(startDate, endDate).collectLatest { data ->
                _analyticsData.value = data
            }
        } catch (e: Exception) {
            _error.value = "Failed to load analytics: ${e.message}"
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * Load flagged content
     */
    fun loadFlaggedContent() = viewModelScope.launch {
        _isLoading.value = true
        
        try {
            adminRepository.getFlaggedContent().collectLatest { content ->
                _flaggedContent.value = content
            }
        } catch (e: Exception) {
            _error.value = "Failed to load flagged content: ${e.message}"
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * Remove flagged content
     */
    fun removeFlaggedContent(contentId: String, contentType: String, reason: String) = viewModelScope.launch {
        _isLoading.value = true
        
        try {
            val result = adminRepository.removeFlaggedContent(contentId, contentType, currentUserId, reason)
            
            if (result.isSuccess) {
                _successMessage.value = "Content removed successfully"
                
                // Log the action
                logAdminAction(
                    action = AdminAction.DELETE,
                    targetType = AdminTargetType.FLAGGED_CONTENT,
                    targetId = contentId,
                    details = "Removed flagged content ($contentType): $reason"
                )
                
                // Refresh the flagged content
                loadFlaggedContent()
            } else {
                _error.value = "Failed to remove content: ${result.exceptionOrNull()?.message}"
            }
        } catch (e: Exception) {
            _error.value = "Failed to remove content: ${e.message}"
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * Send a system notification
     */
    fun sendSystemNotification(
        title: String,
        message: String,
        targetType: NotificationTargetType,
        targetIds: List<String> = emptyList(),
        priority: NotificationPriority = NotificationPriority.NORMAL,
        deepLink: String? = null,
        imageUrl: String? = null,
        expiresAt: Long? = null
    ) = viewModelScope.launch {
        _isLoading.value = true
        
        try {
            val currentAdmin = _currentAdminUser.value
            if (currentAdmin == null) {
                _error.value = "Admin user not found"
                return@launch
            }
            
            val notification = SystemNotification(
                title = title,
                message = message,
                targetType = targetType,
                targetIds = targetIds,
                priority = priority,
                deepLink = deepLink,
                imageUrl = imageUrl,
                expiresAt = expiresAt,
                sentBy = currentAdmin.id,
                sentByName = currentAdmin.name
            )
            
            val result = adminRepository.sendSystemNotification(notification)
            
            if (result.isSuccess) {
                _successMessage.value = "Notification sent successfully"
                
                // Log the action
                logAdminAction(
                    action = AdminAction.SEND_NOTIFICATION,
                    targetType = AdminTargetType.NOTIFICATION,
                    targetId = notification.id,
                    details = "Sent system notification: $title"
                )
            } else {
                _error.value = "Failed to send notification: ${result.exceptionOrNull()?.message}"
            }
        } catch (e: Exception) {
            _error.value = "Failed to send notification: ${e.message}"
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _error.value = null
    }

    /**
     * Clear success message
     */
    fun clearSuccessMessage() {
        _successMessage.value = null
    }
}

/**
 * Enum representing admin panel tabs
 */
enum class AdminPanelTab {
    DASHBOARD,
    USERS,
    VERIFICATION,
    SUBSCRIPTIONS,
    AI_PROFILES,
    APP_SETTINGS,
    ANALYTICS,
    FLAGGED_CONTENT,
    ACTIVITY_LOGS
}