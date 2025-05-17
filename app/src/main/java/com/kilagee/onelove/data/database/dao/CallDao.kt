package com.kilagee.onelove.data.database.dao

import androidx.room.*
import com.kilagee.onelove.data.model.Call
import com.kilagee.onelove.data.model.CallStatus
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Call entities
 */
@Dao
interface CallDao {
    
    /**
     * Insert a call record
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCall(call: Call)
    
    /**
     * Insert multiple call records
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCalls(calls: List<Call>)
    
    /**
     * Update an existing call record
     */
    @Update
    suspend fun updateCall(call: Call)
    
    /**
     * Get a call by ID
     */
    @Query("SELECT * FROM calls WHERE id = :callId")
    suspend fun getCallById(callId: String): Call?
    
    /**
     * Get all calls for current user (as caller or receiver)
     */
    @Query("SELECT * FROM calls WHERE callerId = :userId OR receiverId = :userId OR isGroupCall = 1 ORDER BY createdAt DESC")
    fun getCallsForUser(userId: String): Flow<List<Call>>
    
    /**
     * Get calls between two specific users
     */
    @Query("SELECT * FROM calls WHERE (callerId = :userId1 AND receiverId = :userId2) OR (callerId = :userId2 AND receiverId = :userId1) ORDER BY createdAt DESC")
    fun getCallsBetweenUsers(userId1: String, userId2: String): Flow<List<Call>>
    
    /**
     * Get all group calls
     */
    @Query("SELECT * FROM calls WHERE isGroupCall = 1 ORDER BY createdAt DESC")
    fun getGroupCalls(): Flow<List<Call>>
    
    /**
     * Get all missed calls for a user
     */
    @Query("SELECT * FROM calls WHERE receiverId = :userId AND status = 'MISSED' ORDER BY createdAt DESC")
    fun getMissedCalls(userId: String): Flow<List<Call>>
    
    /**
     * Get call counts by status for a user
     */
    @Query("SELECT COUNT(*) FROM calls WHERE (callerId = :userId OR receiverId = :userId) AND status = :status")
    suspend fun getCallCountByStatus(userId: String, status: CallStatus): Int
    
    /**
     * Delete a call record by ID
     */
    @Query("DELETE FROM calls WHERE id = :callId")
    suspend fun deleteCall(callId: String)
    
    /**
     * Delete all calls older than a specific timestamp
     */
    @Query("DELETE FROM calls WHERE createdAt < :timestamp")
    suspend fun deleteOldCalls(timestamp: Long)
}