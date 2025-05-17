package com.kilagee.onelove.domain.repository

import com.kilagee.onelove.data.model.User
import com.kilagee.onelove.domain.util.Result
import kotlinx.coroutines.flow.Flow
import org.webrtc.MediaStream
import org.webrtc.SessionDescription

/**
 * Call history item
 */
data class CallHistoryItem(
    val id: String,
    val userId: String,
    val userName: String,
    val userProfileUrl: String?,
    val timestamp: Long,
    val durationSeconds: Int,
    val isOutgoing: Boolean,
    val isVideoCall: Boolean,
    val isMissed: Boolean
)

/**
 * Repository interface for call-related operations
 */
interface CallRepository {
    
    /**
     * Get call history
     * 
     * @param limit Maximum number of items to retrieve
     * @return Flow of a list of call history items
     */
    fun getCallHistory(limit: Int = 20): Flow<Result<List<CallHistoryItem>>>
    
    /**
     * Get call history with a specific user
     * 
     * @param userId ID of the user
     * @param limit Maximum number of items to retrieve
     * @return Flow of a list of call history items
     */
    fun getCallHistoryWithUser(userId: String, limit: Int = 20): Flow<Result<List<CallHistoryItem>>>
    
    /**
     * Initialize WebRTC
     * 
     * @return Result of the initialization
     */
    suspend fun initializeWebRTC(): Result<Unit>
    
    /**
     * Start a call with a user
     * 
     * @param user User to call
     * @param isVideoCall Whether this is a video call
     * @return Result of the operation
     */
    suspend fun startCall(user: User, isVideoCall: Boolean): Result<String>
    
    /**
     * Answer an incoming call
     * 
     * @param callId ID of the call
     * @param withVideo Whether to answer with video
     * @return Result of the operation
     */
    suspend fun answerCall(callId: String, withVideo: Boolean): Result<Unit>
    
    /**
     * End the current call
     * 
     * @return Result of the operation
     */
    suspend fun endCall(): Result<Unit>
    
    /**
     * Decline an incoming call
     * 
     * @param callId ID of the call
     * @return Result of the operation
     */
    suspend fun declineCall(callId: String): Result<Unit>
    
    /**
     * Create an offer to start a call
     * 
     * @param userId ID of the user to call
     * @return Result containing the session description
     */
    suspend fun createOffer(userId: String): Result<SessionDescription>
    
    /**
     * Create an answer to accept a call
     * 
     * @param callId ID of the call
     * @param offer Remote session description
     * @return Result containing the session description
     */
    suspend fun createAnswer(callId: String, offer: SessionDescription): Result<SessionDescription>
    
    /**
     * Add ICE candidate
     * 
     * @param callId ID of the call
     * @param candidate ICE candidate
     * @return Result of the operation
     */
    suspend fun addIceCandidate(callId: String, candidate: org.webrtc.IceCandidate): Result<Unit>
    
    /**
     * Set remote description
     * 
     * @param callId ID of the call
     * @param description Remote session description
     * @return Result of the operation
     */
    suspend fun setRemoteDescription(callId: String, description: SessionDescription): Result<Unit>
    
    /**
     * Toggle audio mute
     * 
     * @param isMuted Whether audio should be muted
     * @return Result of the operation
     */
    suspend fun toggleAudioMute(isMuted: Boolean): Result<Unit>
    
    /**
     * Toggle video
     * 
     * @param isEnabled Whether video should be enabled
     * @return Result of the operation
     */
    suspend fun toggleVideo(isEnabled: Boolean): Result<Unit>
    
    /**
     * Toggle speaker
     * 
     * @param isSpeakerOn Whether speaker should be on
     * @return Result of the operation
     */
    suspend fun toggleSpeaker(isSpeakerOn: Boolean): Result<Unit>
    
    /**
     * Switch camera
     * 
     * @return Result of the operation
     */
    suspend fun switchCamera(): Result<Unit>
    
    /**
     * Get local media stream
     * 
     * @return Flow of the local media stream
     */
    fun getLocalStream(): Flow<MediaStream>
    
    /**
     * Get remote media stream
     * 
     * @return Flow of the remote media stream
     */
    fun getRemoteStream(): Flow<MediaStream>
    
    /**
     * Get call state
     * 
     * @return Flow of the call state
     */
    fun getCallState(): Flow<CallState>
    
    /**
     * Clean up WebRTC resources
     */
    fun cleanup()
}

/**
 * Call state
 */
enum class CallState {
    IDLE,
    CONNECTING,
    CONNECTED,
    RECONNECTING,
    DISCONNECTED,
    INCOMING_CALL
}