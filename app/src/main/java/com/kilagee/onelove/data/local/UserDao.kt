package com.kilagee.onelove.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.kilagee.onelove.data.model.User
import kotlinx.coroutines.flow.Flow

/**
 * Data access object for User
 */
@Dao
interface UserDao {
    
    /**
     * Insert a user
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)
    
    /**
     * Insert multiple users
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<User>)
    
    /**
     * Update a user
     */
    @Update
    suspend fun updateUser(user: User)
    
    /**
     * Delete a user
     */
    @Delete
    suspend fun deleteUser(user: User)
    
    /**
     * Get a user by ID
     */
    @Query("SELECT * FROM users WHERE id = :userId")
    suspend fun getUserById(userId: String): User?
    
    /**
     * Get a user by ID as a Flow
     */
    @Query("SELECT * FROM users WHERE id = :userId")
    fun getUserByIdFlow(userId: String): Flow<User?>
    
    /**
     * Get all users
     */
    @Query("SELECT * FROM users")
    fun getAllUsers(): Flow<List<User>>
    
    /**
     * Get users by age range
     */
    @Query("SELECT * FROM users WHERE age BETWEEN :minAge AND :maxAge")
    fun getUsersByAgeRange(minAge: Int, maxAge: Int): Flow<List<User>>
    
    /**
     * Delete all users
     */
    @Query("DELETE FROM users")
    suspend fun deleteAllUsers()
}