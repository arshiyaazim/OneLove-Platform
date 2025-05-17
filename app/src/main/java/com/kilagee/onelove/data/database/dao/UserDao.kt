package com.kilagee.onelove.data.database.dao

import androidx.room.*
import com.kilagee.onelove.data.model.User
import com.kilagee.onelove.data.model.VerificationStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)
    
    @Update
    suspend fun updateUser(user: User)
    
    @Delete
    suspend fun deleteUser(user: User)
    
    @Query("SELECT * FROM users WHERE id = :userId")
    fun getUserById(userId: String): Flow<User?>
    
    @Query("SELECT * FROM users WHERE email = :email")
    fun getUserByEmail(email: String): Flow<User?>
    
    @Query("SELECT * FROM users WHERE username = :username")
    fun getUserByUsername(username: String): Flow<User?>
    
    @Query("SELECT * FROM users WHERE verification_status = :status")
    fun getUsersByVerificationStatus(status: VerificationStatus): Flow<List<User>>
    
    @Query("SELECT * FROM users WHERE country = :country AND city = :city")
    fun getUsersByLocation(country: String, city: String): Flow<List<User>>
    
    @Query("UPDATE users SET wallet_balance = wallet_balance + :amount WHERE id = :userId")
    suspend fun addToWalletBalance(userId: String, amount: Double)
    
    @Query("UPDATE users SET points = points + :points WHERE id = :userId")
    suspend fun addPoints(userId: String, points: Int)
    
    @Query("UPDATE users SET verification_status = :status WHERE id = :userId")
    suspend fun updateVerificationStatus(userId: String, status: VerificationStatus)
    
    @Query("UPDATE users SET is_online = :isOnline WHERE id = :userId")
    suspend fun updateOnlineStatus(userId: String, isOnline: Boolean)
}