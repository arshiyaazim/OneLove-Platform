package com.kilagee.onelove.data.repository

import com.kilagee.onelove.data.model.Call
import com.kilagee.onelove.data.model.CallStatus
import com.kilagee.onelove.data.model.CallType
import com.kilagee.onelove.data.model.Result
import com.kilagee.onelove.data.model.User
import kotlinx.coroutines.flow.Flow
import java.util.Date

/**
 * Repository interface for calls
 */
interface CallRepository {
    
    /**
     * Get call by ID
     * @param callId Call ID
     * @return Result containing the call or error
     */
    suspend fun getCallById(callId: String): Result<Call>
    
    /**
     * Get call by ID as Flow
     * @param callId Call ID
     * @return Flow emitting Result containing the call or error
     */
    fun getCallByIdFlow(callId: String): Flow<Result<Call>>
    
    /**
     * Initiate call
     * @param callerId Caller user ID
     * @param receiverId Receiver user ID
     * @param callType Call type
     * @param isScheduled Whether this is a scheduled call
     * @param scheduledTime Optional scheduled time for the call
     * @return Result containing the call ID and call token or error
     */
    suspend fun initiateCall(
        callerId: String,
        receiverId: String,
        callType: CallType,
        isScheduled: Boolean = false,
        scheduledTime: Date? = null
    ): Result<Pair<String, String>>
    
    /**
     * Accept call
     * @param callId Call ID
     * @return Result containing the call token or error
     */
    suspend fun acceptCall(callId: String): Result<String>
    
    /**
     * Reject call
     * @param callId Call ID
     * @return Result indicating success or error
     */
    suspend fun rejectCall(callId: String): Result<Unit>
    
    /**
     * End call
     * @param callId Call ID
     * @param duration Call duration in seconds
     * @param callQuality Optional call quality rating (0-5)
     * @param connectionIssues Whether there were connection issues
     * @return Result containing the updated call or error
     */
    suspend fun endCall(
        callId: String,
        duration: Long,
        callQuality: Int? = null,
        connectionIssues: Boolean = false
    ): Result<Call>
    
    /**
     * Cancel call
     * @param callId Call ID
     * @return Result indicating success or error
     */
    suspend fun cancelCall(callId: String): Result<Unit>
    
    /**
     * Get user call history
     * @param userId User ID
     * @param limit Maximum number of results
     * @return Result containing list of calls or error
     */
    suspend fun getUserCallHistory(userId: String, limit: Int = 50): Result<List<Call>>
    
    /**
     * Get user call history as Flow
     * @param userId User ID
     * @return Flow emitting Result containing list of calls or error
     */
    fun getUserCallHistoryFlow(userId: String): Flow<Result<List<Call>>>
    
    /**
     * Get calls between users
     * @param userId1 First user ID
     * @param userId2 Second user ID
     * @param limit Maximum number of results
     * @return Result containing list of calls or error
     */
    suspend fun getCallsBetweenUsers(
        userId1: String,
        userId2: String,
        limit: Int = 20
    ): Result<List<Call>>
    
    /**
     * Schedule call
     * @param callerId Caller user ID
     * @param receiverId Receiver user ID
     * @param callType Call type
     * @param scheduledTime Scheduled time
     * @return Result containing the call ID or error
     */
    suspend fun scheduleCall(
        callerId: String,
        receiverId: String,
        callType: CallType,
        scheduledTime: Date
    ): Result<String>
    
    /**
     * Get scheduled calls for user
     * @param userId User ID
     * @return Result containing list of scheduled calls or error
     */
    suspend fun getScheduledCallsForUser(userId: String): Result<List<Call>>
    
    /**
     * Get scheduled calls for user as Flow
     * @param userId User ID
     * @return Flow emitting Result containing list of scheduled calls or error
     */
    fun getScheduledCallsForUserFlow(userId: String): Flow<Result<List<Call>>>
    
    /**
     * Cancel scheduled call
     * @param callId Call ID
     * @return Result indicating success or error
     */
    suspend fun cancelScheduledCall(callId: String): Result<Unit>
    
    /**
     * Update call note
     * @param callId Call ID
     * @param note Call note
     * @return Result containing the updated call or error
     */
    suspend fun updateCallNote(callId: String, note: String): Result<Call>
    
    /**
     * Update call quality rating
     * @param callId Call ID
     * @param rating Quality rating (0-5)
     * @return Result containing the updated call or error
     */
    suspend fun updateCallQualityRating(callId: String, rating: Int): Result<Call>
    
    /**
     * Get upcoming scheduled calls
     * @param userId User ID
     * @param hours Hours from now
     * @return Result containing list of upcoming calls or error
     */
    suspend fun getUpcomingScheduledCalls(
        userId: String,
        hours: Int = 24
    ): Result<List<Call>>
    
    /**
     * Initiate AI call
     * @param userId User ID
     * @param aiProfileId AI profile ID
     * @param callType Call type
     * @return Result containing the call ID and call token or error
     */
    suspend fun initiateAICall(
        userId: String,
        aiProfileId: String,
        callType: CallType
    ): Result<Pair<String, String>>
    
    /**
     * Get frequent call contacts
     * @param userId User ID
     * @param limit Maximum number of results
     * @return Result containing list of users or error
     */
    suspend fun getFrequentCallContacts(userId: String, limit: Int = 5): Result<List<User>>
    
    /**
     * Get call statistics for user
     * @param userId User ID
     * @return Result containing call statistics or error
     */
    suspend fun getCallStatistics(userId: String): Result<Map<String, Any>>
}