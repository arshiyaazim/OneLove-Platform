package com.kilagee.onelove.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.kilagee.onelove.data.database.dao.CallDao
import com.kilagee.onelove.data.model.Call
import com.kilagee.onelove.data.model.CallStatus
import com.kilagee.onelove.data.model.CallType
import com.kilagee.onelove.data.model.NotificationActionType
import com.kilagee.onelove.data.model.NotificationType
import com.kilagee.onelove.domain.model.Resource
import com.kilagee.onelove.domain.repository.CallRepository
import com.kilagee.onelove.domain.repository.NotificationRepository
import com.kilagee.onelove.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import java.util.Date
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseCallRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val functions: FirebaseFunctions,
    private val callDao: CallDao,
    private val userRepository: UserRepository,
    private val notificationRepository: NotificationRepository
) : CallRepository {
    
    private val callsCollection = firestore.collection("calls")
    
    override fun createCall(receiverId: String, type: CallType): Flow<Resource<Call>> = flow {
        emit(Resource.Loading)
        
        try {
            val currentUserId = auth.currentUser?.uid ?: run {
                emit(Resource.error("User not authenticated"))
                return@flow
            }
            
            // Generate a unique channel name
            val channelName = "onelove_call_${UUID.randomUUID()}"
            
            // Create call object
            val callId = UUID.randomUUID().toString()
            val call = Call(
                id = callId,
                callerId = currentUserId,
                receiverId = receiverId,
                type = type,
                status = CallStatus.INITIATED,
                startTime = null,
                endTime = null,
                duration = null,
                channelName = channelName,
                tokenId = null,
                createdAt = Date()
            )
            
            // Save to Firestore
            callsCollection.document(callId).set(call).await()
            
            // Save to Room
            callDao.insertCall(call)
            
            // Get the receiver's name for the notification
            val callerUser = userRepository.getUserById(currentUserId).await()
            if (callerUser is Resource.Success) {
                val callerName = callerUser.data?.displayName ?: "Someone"
                
                // Send notification to receiver
                val callTypeStr = if (type == CallType.AUDIO) "audio" else "video"
                notificationRepository.createNotification(
                    title = "Incoming $callTypeStr call",
                    message = "$callerName is calling you",
                    type = NotificationType.CALL_MISSED,
                    relatedId = callId,
                    imageUrl = callerUser.data?.profilePhotoUrl,
                    actionType = NotificationActionType.OPEN_CALL,
                    actionData = callId
                )
            }
            
            emit(Resource.success(call))
        } catch (e: Exception) {
            emit(Resource.error("Failed to create call: ${e.message}"))
        }
    }
    
    override fun createGroupCall(participantIds: List<String>, type: CallType): Flow<Resource<Call>> = flow {
        emit(Resource.Loading)
        
        try {
            val currentUserId = auth.currentUser?.uid ?: run {
                emit(Resource.error("User not authenticated"))
                return@flow
            }
            
            // Check if the list contains the current user, add if not
            val allParticipants = participantIds.toMutableList()
            if (!allParticipants.contains(currentUserId)) {
                allParticipants.add(currentUserId)
            }
            
            // Generate a unique channel name
            val channelName = "onelove_group_${UUID.randomUUID()}"
            
            // Create call object
            val callId = UUID.randomUUID().toString()
            val call = Call(
                id = callId,
                callerId = currentUserId,
                receiverId = "",  // No specific receiver for group calls
                type = type,
                status = CallStatus.INITIATED,
                startTime = null,
                endTime = null,
                duration = null,
                channelName = channelName,
                tokenId = null,
                createdAt = Date(),
                isGroupCall = true,
                participants = allParticipants
            )
            
            // Save to Firestore
            callsCollection.document(callId).set(call).await()
            
            // Save to Room
            callDao.insertCall(call)
            
            // Get the caller's name for notifications
            val callerUser = userRepository.getUserById(currentUserId).await()
            if (callerUser is Resource.Success) {
                val callerName = callerUser.data?.displayName ?: "Someone"
                
                // Send notifications to all participants except the caller
                val callTypeStr = if (type == CallType.AUDIO) "audio" else "video"
                val participantsExceptCaller = allParticipants.filter { it != currentUserId }
                
                for (participantId in participantsExceptCaller) {
                    notificationRepository.createNotification(
                        title = "Incoming group $callTypeStr call",
                        message = "$callerName is calling you",
                        type = NotificationType.CALL_MISSED,
                        relatedId = callId,
                        imageUrl = callerUser.data?.profilePhotoUrl,
                        actionType = NotificationActionType.OPEN_CALL,
                        actionData = callId
                    )
                }
            }
            
            emit(Resource.success(call))
        } catch (e: Exception) {
            emit(Resource.error("Failed to create group call: ${e.message}"))
        }
    }
    
    override fun getCall(callId: String): Flow<Resource<Call>> = flow {
        emit(Resource.Loading)
        
        try {
            // Try to get from Room first for quick response
            val localCall = callDao.getCallById(callId)
            
            if (localCall != null) {
                emit(Resource.success(localCall))
            }
            
            // Get from Firestore for most up-to-date data
            val callDoc = callsCollection.document(callId).get().await()
            val call = callDoc.toObject(Call::class.java)
            
            if (call != null) {
                // Update local cache
                callDao.insertCall(call)
                emit(Resource.success(call))
            } else if (localCall == null) {
                emit(Resource.error("Call not found"))
            }
        } catch (e: Exception) {
            emit(Resource.error("Failed to get call: ${e.message}"))
        }
    }
    
    override fun getCalls(): Flow<Resource<List<Call>>> = flow {
        emit(Resource.Loading)
        
        try {
            val currentUserId = auth.currentUser?.uid ?: run {
                emit(Resource.error("User not authenticated"))
                return@flow
            }
            
            // Get calls from Firestore
            val calls = mutableListOf<Call>()
            
            // Get calls where user is caller or receiver
            val outgoingCalls = callsCollection
                .whereEqualTo("callerId", currentUserId)
                .get()
                .await()
            
            val incomingCalls = callsCollection
                .whereEqualTo("receiverId", currentUserId)
                .get()
                .await()
            
            // Get group calls where user is a participant
            val groupCalls = callsCollection
                .whereEqualTo("isGroupCall", true)
                .whereArrayContains("participants", currentUserId)
                .get()
                .await()
            
            // Add all calls to the list
            outgoingCalls.documents.mapNotNull { it.toObject(Call::class.java) }.let { calls.addAll(it) }
            incomingCalls.documents.mapNotNull { it.toObject(Call::class.java) }.let { calls.addAll(it) }
            groupCalls.documents.mapNotNull { it.toObject(Call::class.java) }.let { calls.addAll(it) }
            
            // Remove duplicates and sort by date
            val uniqueCalls = calls.distinctBy { it.id }.sortedByDescending { it.createdAt }
            
            // Update local cache
            uniqueCalls.forEach { callDao.insertCall(it) }
            
            emit(Resource.success(uniqueCalls))
        } catch (e: Exception) {
            emit(Resource.error("Failed to get calls: ${e.message}"))
        }
    }
    
    override fun getCallHistoryWithUser(userId: String): Flow<Resource<List<Call>>> = flow {
        emit(Resource.Loading)
        
        try {
            val currentUserId = auth.currentUser?.uid ?: run {
                emit(Resource.error("User not authenticated"))
                return@flow
            }
            
            // Get calls between the two users
            val outgoingCalls = callsCollection
                .whereEqualTo("callerId", currentUserId)
                .whereEqualTo("receiverId", userId)
                .get()
                .await()
            
            val incomingCalls = callsCollection
                .whereEqualTo("callerId", userId)
                .whereEqualTo("receiverId", currentUserId)
                .get()
                .await()
            
            val calls = mutableListOf<Call>()
            outgoingCalls.documents.mapNotNull { it.toObject(Call::class.java) }.let { calls.addAll(it) }
            incomingCalls.documents.mapNotNull { it.toObject(Call::class.java) }.let { calls.addAll(it) }
            
            // Sort by date
            val sortedCalls = calls.sortedByDescending { it.createdAt }
            
            // Update local cache
            sortedCalls.forEach { callDao.insertCall(it) }
            
            emit(Resource.success(sortedCalls))
        } catch (e: Exception) {
            emit(Resource.error("Failed to get call history: ${e.message}"))
        }
    }
    
    override fun updateCallStatus(callId: String, status: CallStatus): Flow<Resource<Call>> = flow {
        emit(Resource.Loading)
        
        try {
            // Get the call
            val callDoc = callsCollection.document(callId).get().await()
            val call = callDoc.toObject(Call::class.java)
            
            if (call != null) {
                // Update status
                val updatedCall = call.copy(status = status)
                
                // Save to Firestore
                callsCollection.document(callId).update("status", status).await()
                
                // Update local cache
                callDao.updateCall(updatedCall)
                
                emit(Resource.success(updatedCall))
            } else {
                emit(Resource.error("Call not found"))
            }
        } catch (e: Exception) {
            emit(Resource.error("Failed to update call status: ${e.message}"))
        }
    }
    
    override fun answerCall(callId: String): Flow<Resource<Call>> = flow {
        emit(Resource.Loading)
        
        try {
            // Get the call
            val callDoc = callsCollection.document(callId).get().await()
            val call = callDoc.toObject(Call::class.java)
            
            if (call != null) {
                // Set status to ongoing and record start time
                val now = Date()
                val updatedCall = call.copy(status = CallStatus.ONGOING, startTime = now)
                
                // Update in Firestore
                callsCollection.document(callId).update(
                    mapOf(
                        "status" to CallStatus.ONGOING,
                        "startTime" to now
                    )
                ).await()
                
                // Update local cache
                callDao.updateCall(updatedCall)
                
                emit(Resource.success(updatedCall))
            } else {
                emit(Resource.error("Call not found"))
            }
        } catch (e: Exception) {
            emit(Resource.error("Failed to answer call: ${e.message}"))
        }
    }
    
    override fun declineCall(callId: String): Flow<Resource<Call>> = flow {
        emit(Resource.Loading)
        
        try {
            // Get the call
            val callDoc = callsCollection.document(callId).get().await()
            val call = callDoc.toObject(Call::class.java)
            
            if (call != null) {
                // Set status to declined
                val updatedCall = call.copy(status = CallStatus.DECLINED)
                
                // Update in Firestore
                callsCollection.document(callId).update("status", CallStatus.DECLINED).await()
                
                // Update local cache
                callDao.updateCall(updatedCall)
                
                // Send notification to caller
                notificationRepository.createNotification(
                    title = "Call declined",
                    message = "Your call was declined",
                    type = NotificationType.CALL_ENDED,
                    relatedId = callId,
                    actionType = NotificationActionType.NONE
                )
                
                emit(Resource.success(updatedCall))
            } else {
                emit(Resource.error("Call not found"))
            }
        } catch (e: Exception) {
            emit(Resource.error("Failed to decline call: ${e.message}"))
        }
    }
    
    override fun endCall(callId: String, endTime: Date, duration: Long): Flow<Resource<Call>> = flow {
        emit(Resource.Loading)
        
        try {
            // Get the call
            val callDoc = callsCollection.document(callId).get().await()
            val call = callDoc.toObject(Call::class.java)
            
            if (call != null) {
                // Update call with end time, duration, and status
                val updatedCall = call.copy(
                    status = CallStatus.ENDED,
                    endTime = endTime,
                    duration = duration
                )
                
                // Update in Firestore
                callsCollection.document(callId).update(
                    mapOf(
                        "status" to CallStatus.ENDED,
                        "endTime" to endTime,
                        "duration" to duration
                    )
                ).await()
                
                // Update local cache
                callDao.updateCall(updatedCall)
                
                emit(Resource.success(updatedCall))
            } else {
                emit(Resource.error("Call not found"))
            }
        } catch (e: Exception) {
            emit(Resource.error("Failed to end call: ${e.message}"))
        }
    }
    
    override fun getAgoraToken(callId: String, userId: String, channelName: String): Flow<Resource<String>> = flow {
        emit(Resource.Loading)
        
        try {
            // Call Firebase Function to get a token
            val data = hashMapOf(
                "uid" to userId,
                "channelName" to channelName,
                "role" to 1, // Publisher role
                "tokenType" to "rtc",
                "privilegeExpireTime" to 3600 // 1 hour in seconds
            )
            
            val result = functions
                .getHttpsCallable("generateRtcToken")
                .call(data)
                .await()
                .data as? Map<*, *>
            
            val token = result?.get("token") as? String
            
            if (token != null) {
                // Update the call with the token
                callsCollection.document(callId).update("tokenId", token).await()
                
                emit(Resource.success(token))
            } else {
                emit(Resource.error("Failed to generate token"))
            }
        } catch (e: Exception) {
            emit(Resource.error("Failed to get Agora token: ${e.message}"))
        }
    }
    
    override fun deleteCall(callId: String): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading)
        
        try {
            // Delete from Firestore
            callsCollection.document(callId).delete().await()
            
            // Delete from local storage
            callDao.deleteCall(callId)
            
            emit(Resource.success(Unit))
        } catch (e: Exception) {
            emit(Resource.error("Failed to delete call: ${e.message}"))
        }
    }
}