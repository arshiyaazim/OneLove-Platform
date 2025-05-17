package com.kilagee.onelove.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.messaging.FirebaseMessaging
import com.kilagee.onelove.domain.model.AdminLog
import com.kilagee.onelove.domain.model.AdminUser
import com.kilagee.onelove.domain.model.AIProfile
import com.kilagee.onelove.domain.model.AppSettings
import com.kilagee.onelove.domain.model.SubscriptionRequest
import com.kilagee.onelove.domain.model.SubscriptionRequestStatus
import com.kilagee.onelove.domain.model.SystemNotification
import com.kilagee.onelove.domain.model.UserProfile
import com.kilagee.onelove.domain.model.VerificationRequest
import com.kilagee.onelove.domain.model.VerificationStatus
import com.kilagee.onelove.domain.repository.AdminRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdminRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val messaging: FirebaseMessaging
) : AdminRepository {

    // Collection references
    private val adminsCollection: CollectionReference
        get() = firestore.collection("admins")
    
    private val usersCollection: CollectionReference
        get() = firestore.collection("users")
    
    private val verificationRequestsCollection: CollectionReference
        get() = firestore.collection("verification_requests")
    
    private val subscriptionRequestsCollection: CollectionReference
        get() = firestore.collection("subscription_requests")
    
    private val aiProfilesCollection: CollectionReference
        get() = firestore.collection("ai_profiles")
    
    private val adminLogsCollection: CollectionReference
        get() = firestore.collection("admin_logs")
    
    private val settingsDocument: DocumentReference
        get() = firestore.collection("settings").document("app_settings")
    
    private val flaggedContentCollection: CollectionReference
        get() = firestore.collection("flagged_content")
    
    private val notificationsCollection: CollectionReference
        get() = firestore.collection("notifications")
    
    // Admin-related functions
    override fun checkAdminStatus(userId: String): Flow<Boolean> = callbackFlow {
        val registration = adminsCollection.whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(false)
                    return@addSnapshotListener
                }
                
                trySend(snapshot?.documents?.isNotEmpty() ?: false)
            }
            
        awaitClose { registration.remove() }
    }

    override fun getAdminUser(userId: String): Flow<AdminUser?> = callbackFlow {
        val registration = adminsCollection.whereEqualTo("userId", userId)
            .limit(1)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    trySend(null)
                    return@addSnapshotListener
                }
                
                val document = snapshot.documents.firstOrNull()
                if (document != null) {
                    val adminUser = AdminUser.fromMap(document.data ?: emptyMap())
                    trySend(adminUser)
                } else {
                    trySend(null)
                }
            }
            
        awaitClose { registration.remove() }
    }

    override fun getAllAdminUsers(): Flow<List<AdminUser>> = callbackFlow {
        val registration = adminsCollection.orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                val adminUsers = snapshot.documents.mapNotNull { document ->
                    document.data?.let { AdminUser.fromMap(it) }
                }
                
                trySend(adminUsers)
            }
            
        awaitClose { registration.remove() }
    }

    override suspend fun saveAdminUser(adminUser: AdminUser): Result<AdminUser> = try {
        val adminData = adminUser.toMap()
        
        if (adminUser.id.isEmpty()) {
            // Create new admin
            val newDocRef = adminsCollection.document()
            val newAdmin = adminUser.copy(id = newDocRef.id)
            newDocRef.set(newAdmin.toMap()).await()
            Result.success(newAdmin)
        } else {
            // Update existing admin
            val updatedAdmin = adminUser.copy(updatedAt = System.currentTimeMillis())
            adminsCollection.document(adminUser.id).set(updatedAdmin.toMap()).await()
            Result.success(updatedAdmin)
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun removeAdminUser(adminUserId: String): Result<Boolean> = try {
        adminsCollection.document(adminUserId).delete().await()
        Result.success(true)
    } catch (e: Exception) {
        Result.failure(e)
    }

    // User profile management
    override fun getAllUsers(limit: Int, lastUserId: String?): Flow<List<UserProfile>> = callbackFlow {
        var query = usersCollection
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            
        if (!lastUserId.isNullOrEmpty()) {
            val lastDoc = usersCollection.document(lastUserId).get().await()
            if (lastDoc.exists()) {
                query = query.startAfter(lastDoc)
            }
        }
        
        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            
            val users = snapshot.documents.mapNotNull { document ->
                document.data?.let { UserProfile.fromMap(it) }
            }
            
            trySend(users)
        }
        
        awaitClose { registration.remove() }
    }

    override fun searchUsers(query: String): Flow<List<UserProfile>> = callbackFlow {
        // This is a simple implementation; in a real app, you might use a more sophisticated search approach
        val registration = usersCollection
            .orderBy("displayName")
            .whereGreaterThanOrEqualTo("displayName", query)
            .whereLessThanOrEqualTo("displayName", query + "\uf8ff")
            .limit(20)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                val users = snapshot.documents.mapNotNull { document ->
                    document.data?.let { UserProfile.fromMap(it) }
                }
                
                trySend(users)
            }
            
        awaitClose { registration.remove() }
    }

    override fun getUserProfile(userId: String): Flow<UserProfile?> = callbackFlow {
        val registration = usersCollection.document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || !snapshot.exists()) {
                    trySend(null)
                    return@addSnapshotListener
                }
                
                val profile = UserProfile.fromMap(snapshot.data ?: emptyMap())
                trySend(profile)
            }
            
        awaitClose { registration.remove() }
    }

    override suspend fun updateUserProfile(profile: UserProfile): Result<UserProfile> = try {
        val updatedProfile = profile.copy(updatedAt = System.currentTimeMillis())
        usersCollection.document(profile.id).set(updatedProfile.toMap()).await()
        Result.success(updatedProfile)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun deleteUser(userId: String): Result<Boolean> = try {
        // Mark user as deleted in Firestore
        usersCollection.document(userId)
            .update(mapOf(
                "isDeleted" to true,
                "updatedAt" to System.currentTimeMillis()
            )).await()
        
        // In a real implementation, you might want to schedule a background job
        // to clean up the user's data or handle the actual account deletion
        
        Result.success(true)
    } catch (e: Exception) {
        Result.failure(e)
    }

    // Verification requests
    override fun getPendingVerificationRequests(limit: Int, lastRequestId: String?): Flow<List<VerificationRequest>> = callbackFlow {
        var query = verificationRequestsCollection
            .whereEqualTo("status", VerificationStatus.PENDING.name)
            .orderBy("submittedAt", Query.Direction.ASCENDING)
            .limit(limit.toLong())
            
        if (!lastRequestId.isNullOrEmpty()) {
            val lastDoc = verificationRequestsCollection.document(lastRequestId).get().await()
            if (lastDoc.exists()) {
                query = query.startAfter(lastDoc)
            }
        }
        
        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            
            val requests = snapshot.documents.mapNotNull { document ->
                document.data?.let { VerificationRequest.fromMap(it) }
            }
            
            trySend(requests)
        }
        
        awaitClose { registration.remove() }
    }

    override suspend fun approveVerification(requestId: String, adminId: String, notes: String?): Result<Boolean> = try {
        val batch = firestore.batch()
        
        // 1. Get the verification request
        val requestDoc = verificationRequestsCollection.document(requestId).get().await()
        if (!requestDoc.exists()) {
            return Result.failure(Exception("Verification request not found"))
        }
        
        val request = VerificationRequest.fromMap(requestDoc.data ?: emptyMap())
        
        // 2. Update request status
        val updatedRequest = request.copy(
            status = VerificationStatus.APPROVED,
            processedAt = System.currentTimeMillis(),
            processedBy = adminId,
            adminNotes = notes
        )
        
        batch.set(verificationRequestsCollection.document(requestId), updatedRequest.toMap())
        
        // 3. Update user's verification status
        batch.update(usersCollection.document(request.userId), 
            mapOf(
                "isVerified" to true,
                "verificationLevel" to 1,
                "updatedAt" to System.currentTimeMillis()
            )
        )
        
        // 4. Execute batch
        batch.commit().await()
        
        Result.success(true)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun rejectVerification(requestId: String, adminId: String, reason: String): Result<Boolean> = try {
        val batch = firestore.batch()
        
        // 1. Get the verification request
        val requestDoc = verificationRequestsCollection.document(requestId).get().await()
        if (!requestDoc.exists()) {
            return Result.failure(Exception("Verification request not found"))
        }
        
        val request = VerificationRequest.fromMap(requestDoc.data ?: emptyMap())
        
        // 2. Update request status
        val updatedRequest = request.copy(
            status = VerificationStatus.REJECTED,
            processedAt = System.currentTimeMillis(),
            processedBy = adminId,
            rejectionReason = reason
        )
        
        batch.set(verificationRequestsCollection.document(requestId), updatedRequest.toMap())
        
        // 3. Execute batch
        batch.commit().await()
        
        Result.success(true)
    } catch (e: Exception) {
        Result.failure(e)
    }

    // Subscription requests
    override fun getPendingSubscriptionRequests(limit: Int, lastRequestId: String?): Flow<List<SubscriptionRequest>> = callbackFlow {
        var query = subscriptionRequestsCollection
            .whereEqualTo("status", SubscriptionRequestStatus.PENDING.name)
            .orderBy("submittedAt", Query.Direction.ASCENDING)
            .limit(limit.toLong())
            
        if (!lastRequestId.isNullOrEmpty()) {
            val lastDoc = subscriptionRequestsCollection.document(lastRequestId).get().await()
            if (lastDoc.exists()) {
                query = query.startAfter(lastDoc)
            }
        }
        
        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            
            val requests = snapshot.documents.mapNotNull { document ->
                document.data?.let { SubscriptionRequest.fromMap(it) }
            }
            
            trySend(requests)
        }
        
        awaitClose { registration.remove() }
    }

    override suspend fun approveSubscription(requestId: String, adminId: String): Result<Boolean> = try {
        val batch = firestore.batch()
        
        // 1. Get the subscription request
        val requestDoc = subscriptionRequestsCollection.document(requestId).get().await()
        if (!requestDoc.exists()) {
            return Result.failure(Exception("Subscription request not found"))
        }
        
        val request = SubscriptionRequest.fromMap(requestDoc.data ?: emptyMap())
        
        // 2. Update request status
        val updatedRequest = request.copy(
            status = SubscriptionRequestStatus.APPROVED,
            processedAt = System.currentTimeMillis(),
            processedBy = adminId
        )
        
        batch.set(subscriptionRequestsCollection.document(requestId), updatedRequest.toMap())
        
        // 3. Update user's subscription status
        val expiresAt = System.currentTimeMillis() + (request.duration * 24 * 60 * 60 * 1000L)
        
        batch.update(usersCollection.document(request.userId), 
            mapOf(
                "isPremium" to true,
                "premiumTier" to request.tierId,
                "premiumExpiresAt" to expiresAt,
                "updatedAt" to System.currentTimeMillis()
            )
        )
        
        // 4. Execute batch
        batch.commit().await()
        
        Result.success(true)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun rejectSubscription(requestId: String, adminId: String, reason: String): Result<Boolean> = try {
        val batch = firestore.batch()
        
        // 1. Get the subscription request
        val requestDoc = subscriptionRequestsCollection.document(requestId).get().await()
        if (!requestDoc.exists()) {
            return Result.failure(Exception("Subscription request not found"))
        }
        
        val request = SubscriptionRequest.fromMap(requestDoc.data ?: emptyMap())
        
        // 2. Update request status
        val updatedRequest = request.copy(
            status = SubscriptionRequestStatus.REJECTED,
            processedAt = System.currentTimeMillis(),
            processedBy = adminId,
            rejectionReason = reason
        )
        
        batch.set(subscriptionRequestsCollection.document(requestId), updatedRequest.toMap())
        
        // 3. Execute batch
        batch.commit().await()
        
        Result.success(true)
    } catch (e: Exception) {
        Result.failure(e)
    }

    // AI profiles
    override fun getAllAIProfiles(limit: Int, lastProfileId: String?): Flow<List<AIProfile>> = callbackFlow {
        var query = aiProfilesCollection
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            
        if (!lastProfileId.isNullOrEmpty()) {
            val lastDoc = aiProfilesCollection.document(lastProfileId).get().await()
            if (lastDoc.exists()) {
                query = query.startAfter(lastDoc)
            }
        }
        
        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            
            val profiles = snapshot.documents.mapNotNull { document ->
                document.data?.let { 
                    AIProfile.fromMap(it) 
                }
            }
            
            trySend(profiles)
        }
        
        awaitClose { registration.remove() }
    }

    override suspend fun createAIProfile(profile: AIProfile): Result<AIProfile> = try {
        val profileData = profile.toMap()
        
        if (profile.id.isEmpty()) {
            // Create new profile
            val newDocRef = aiProfilesCollection.document()
            val newProfile = profile.copy(
                id = newDocRef.id,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            newDocRef.set(newProfile.toMap()).await()
            Result.success(newProfile)
        } else {
            // Update existing profile
            val updatedProfile = profile.copy(
                updatedAt = System.currentTimeMillis()
            )
            aiProfilesCollection.document(profile.id).set(updatedProfile.toMap()).await()
            Result.success(updatedProfile)
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun updateAIProfile(profile: AIProfile): Result<AIProfile> = try {
        val updatedProfile = profile.copy(updatedAt = System.currentTimeMillis())
        aiProfilesCollection.document(profile.id).set(updatedProfile.toMap()).await()
        Result.success(updatedProfile)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun deleteAIProfile(profileId: String): Result<Boolean> = try {
        aiProfilesCollection.document(profileId).delete().await()
        Result.success(true)
    } catch (e: Exception) {
        Result.failure(e)
    }

    // App settings
    override fun getAppSettings(): Flow<AppSettings> = callbackFlow {
        val registration = settingsDocument
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(AppSettings())
                    return@addSnapshotListener
                }
                
                if (snapshot != null && snapshot.exists()) {
                    val settings = AppSettings.fromMap(snapshot.data ?: emptyMap())
                    trySend(settings)
                } else {
                    // If settings don't exist, create them with defaults
                    val defaultSettings = AppSettings()
                    settingsDocument.set(defaultSettings.toMap())
                    trySend(defaultSettings)
                }
            }
            
        awaitClose { registration.remove() }
    }

    override suspend fun updateAppSettings(settings: AppSettings): Result<AppSettings> = try {
        val updatedSettings = settings.copy(
            updatedAt = System.currentTimeMillis(),
            updatedBy = auth.currentUser?.uid
        )
        settingsDocument.set(updatedSettings.toMap()).await()
        Result.success(updatedSettings)
    } catch (e: Exception) {
        Result.failure(e)
    }

    // Admin logs
    override fun getAdminLogs(limit: Int, lastLogId: String?): Flow<List<AdminLog>> = callbackFlow {
        var query = adminLogsCollection
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            
        if (!lastLogId.isNullOrEmpty()) {
            val lastDoc = adminLogsCollection.document(lastLogId).get().await()
            if (lastDoc.exists()) {
                query = query.startAfter(lastDoc)
            }
        }
        
        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            
            val logs = snapshot.documents.mapNotNull { document ->
                document.data?.let { AdminLog.fromMap(it) }
            }
            
            trySend(logs)
        }
        
        awaitClose { registration.remove() }
    }

    override suspend fun logAdminAction(log: AdminLog): Result<AdminLog> = try {
        val logData = log.toMap()
        
        if (log.id.isEmpty()) {
            // Create new log
            val newDocRef = adminLogsCollection.document()
            val newLog = log.copy(id = newDocRef.id)
            newDocRef.set(newLog.toMap()).await()
            Result.success(newLog)
        } else {
            // Set existing log
            adminLogsCollection.document(log.id).set(logData).await()
            Result.success(log)
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    // Analytics
    override fun getAppAnalytics(startDate: Long, endDate: Long): Flow<Map<String, Any>> = flow {
        // In a real app, you would likely have a more sophisticated analytics system
        // This is a placeholder implementation
        
        try {
            val userCount = usersCollection.count().get().await().count
            val premiumUsers = usersCollection.whereEqualTo("isPremium", true).count().get().await().count
            val verifiedUsers = usersCollection.whereEqualTo("isVerified", true).count().get().await().count
            val newUsersQuery = usersCollection
                .whereGreaterThanOrEqualTo("createdAt", startDate)
                .whereLessThanOrEqualTo("createdAt", endDate)
            val newUsers = newUsersQuery.count().get().await().count
            
            // Dummy analytics data for illustration
            val analyticsData = mapOf(
                "totalUsers" to userCount,
                "premiumUsers" to premiumUsers,
                "verifiedUsers" to verifiedUsers,
                "newUsersInPeriod" to newUsers,
                "dailyActiveUsers" to (userCount * 0.45).toInt(),
                "monthlyActiveUsers" to (userCount * 0.75).toInt(),
                "averageSessionDuration" to 15.5, // in minutes
                "premiumConversionRate" to 0.08,  // 8%
                "verificationRate" to 0.35,       // 35%
                "messagesSent" to 25000,
                "matchesMade" to 8500,
                "callsInitiated" to 2800,
                "callsCompleted" to 1900,
                "offersCreated" to 5600,
                "offersAccepted" to 3100,
                "startDate" to startDate,
                "endDate" to endDate
            )
            
            emit(analyticsData)
        } catch (e: Exception) {
            emit(emptyMap<String, Any>())
        }
    }

    // System notifications
    override suspend fun sendSystemNotification(notification: SystemNotification): Result<Boolean> = try {
        // 1. Save notification to Firestore
        val notificationData = notification.toMap()
        val docRef = if (notification.id.isEmpty()) {
            notificationsCollection.document()
        } else {
            notificationsCollection.document(notification.id)
        }
        
        val notificationId = docRef.id
        val updatedNotification = notification.copy(
            id = notificationId,
            sentAt = System.currentTimeMillis()
        )
        
        docRef.set(updatedNotification.toMap()).await()
        
        // 2. In a real app, you would send this notification through Firebase Cloud Messaging
        // or another notification service
        
        Result.success(true)
    } catch (e: Exception) {
        Result.failure(e)
    }

    // Flagged content
    override fun getFlaggedContent(limit: Int, lastItemId: String?): Flow<List<Any>> = callbackFlow {
        var query = flaggedContentCollection
            .orderBy("reportedAt", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            
        if (!lastItemId.isNullOrEmpty()) {
            val lastDoc = flaggedContentCollection.document(lastItemId).get().await()
            if (lastDoc.exists()) {
                query = query.startAfter(lastDoc)
            }
        }
        
        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) {
                trySend(emptyList<Any>())
                return@addSnapshotListener
            }
            
            val flaggedItems = snapshot.documents.mapNotNull { document ->
                document.data
            }
            
            trySend(flaggedItems)
        }
        
        awaitClose { registration.remove() }
    }

    override suspend fun removeFlaggedContent(contentId: String, contentType: String, adminId: String, reason: String): Result<Boolean> = try {
        // 1. Get the flagged content
        val contentDoc = flaggedContentCollection.document(contentId).get().await()
        if (!contentDoc.exists()) {
            return Result.failure(Exception("Flagged content not found"))
        }
        
        // 2. Remove or update the content based on its type
        // In a real app, this would be more sophisticated based on content type
        val batch = firestore.batch()
        
        // Mark the flagged item as processed
        batch.update(flaggedContentCollection.document(contentId), 
            mapOf(
                "status" to "REMOVED",
                "processedAt" to System.currentTimeMillis(),
                "processedBy" to adminId,
                "removalReason" to reason
            )
        )
        
        // Based on content type, remove the actual content
        when (contentType) {
            "MESSAGE" -> {
                val originalContentId = contentDoc.getString("originalContentId") ?: ""
                val chatId = contentDoc.getString("chatId") ?: ""
                
                if (originalContentId.isNotEmpty() && chatId.isNotEmpty()) {
                    batch.update(
                        firestore.collection("chats").document(chatId)
                            .collection("messages").document(originalContentId),
                        mapOf(
                            "isRemoved" to true,
                            "removedAt" to System.currentTimeMillis(),
                            "removedBy" to adminId,
                            "removedReason" to reason
                        )
                    )
                }
            }
            
            "PROFILE_PHOTO" -> {
                val userId = contentDoc.getString("userId") ?: ""
                val photoUrl = contentDoc.getString("photoUrl") ?: ""
                
                if (userId.isNotEmpty() && photoUrl.isNotEmpty()) {
                    // Get user's photos array
                    val userDoc = usersCollection.document(userId).get().await()
                    if (userDoc.exists()) {
                        val photos = userDoc.get("photos") as? List<String> ?: emptyList()
                        val updatedPhotos = photos.filter { it != photoUrl }
                        
                        batch.update(usersCollection.document(userId),
                            mapOf("photos" to updatedPhotos)
                        )
                    }
                }
            }
            
            "USER_PROFILE" -> {
                val userId = contentDoc.getString("userId") ?: ""
                
                if (userId.isNotEmpty()) {
                    batch.update(usersCollection.document(userId),
                        mapOf(
                            "isBanned" to true,
                            "banReason" to reason,
                            "updatedAt" to System.currentTimeMillis()
                        )
                    )
                }
            }
        }
        
        // 3. Execute the batch
        batch.commit().await()
        
        Result.success(true)
    } catch (e: Exception) {
        Result.failure(e)
    }
}