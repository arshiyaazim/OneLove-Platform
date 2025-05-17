package com.kilagee.onelove.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.kilagee.onelove.data.model.Offer
import com.kilagee.onelove.data.model.OfferStatus
import java.util.Date

/**
 * Data Access Object for the Offers table
 */
@Dao
interface OfferDao {
    /**
     * Insert an offer into the database
     * 
     * @param offer The offer to insert
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOffer(offer: Offer)
    
    /**
     * Insert multiple offers into the database
     * 
     * @param offers The offers to insert
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOffers(offers: List<Offer>)
    
    /**
     * Get all offers
     * 
     * @return List of all offers
     */
    @Query("SELECT * FROM offers ORDER BY created_at DESC")
    suspend fun getAllOffers(): List<Offer>
    
    /**
     * Get offers sent by a user
     * 
     * @param userId The ID of the sender
     * @return List of offers sent by the user
     */
    @Query("SELECT * FROM offers WHERE sender_id = :userId ORDER BY created_at DESC")
    suspend fun getOffersBySenderId(userId: String): List<Offer>
    
    /**
     * Get offers received by a user
     * 
     * @param userId The ID of the receiver
     * @return List of offers received by the user
     */
    @Query("SELECT * FROM offers WHERE receiver_id = :userId ORDER BY created_at DESC")
    suspend fun getOffersByReceiverId(userId: String): List<Offer>
    
    /**
     * Get offers by status
     * 
     * @param status The status to filter by
     * @return List of offers with the specified status
     */
    @Query("SELECT * FROM offers WHERE status = :status ORDER BY created_at DESC")
    suspend fun getOffersByStatus(status: OfferStatus): List<Offer>
    
    /**
     * Get offers by sender and status
     * 
     * @param userId The ID of the sender
     * @param status The status to filter by
     * @return List of offers sent by the user with the specified status
     */
    @Query("SELECT * FROM offers WHERE sender_id = :userId AND status = :status ORDER BY created_at DESC")
    suspend fun getOffersBySenderIdAndStatus(userId: String, status: OfferStatus): List<Offer>
    
    /**
     * Get offers by receiver and status
     * 
     * @param userId The ID of the receiver
     * @param status The status to filter by
     * @return List of offers received by the user with the specified status
     */
    @Query("SELECT * FROM offers WHERE receiver_id = :userId AND status = :status ORDER BY created_at DESC")
    suspend fun getOffersByReceiverIdAndStatus(userId: String, status: OfferStatus): List<Offer>
    
    /**
     * Get an offer by ID
     * 
     * @param offerId The ID of the offer
     * @return The offer with the specified ID, or null if not found
     */
    @Query("SELECT * FROM offers WHERE id = :offerId LIMIT 1")
    suspend fun getOfferById(offerId: String): Offer?
    
    /**
     * Update the status of an offer
     * 
     * @param offerId The ID of the offer
     * @param status The new status
     * @param updatedAt The timestamp of the update
     * @return The number of rows updated
     */
    @Query("UPDATE offers SET status = :status, updated_at = :updatedAt WHERE id = :offerId")
    suspend fun updateOfferStatus(offerId: String, status: OfferStatus, updatedAt: Date): Int
    
    /**
     * Delete an offer by ID
     * 
     * @param offerId The ID of the offer to delete
     * @return The number of rows deleted
     */
    @Query("DELETE FROM offers WHERE id = :offerId")
    suspend fun deleteOffer(offerId: String): Int
    
    /**
     * Count pending offers for a user
     * 
     * @param userId The ID of the receiver
     * @return The number of pending offers
     */
    @Query("SELECT COUNT(*) FROM offers WHERE receiver_id = :userId AND status = 'PENDING'")
    suspend fun countPendingOffers(userId: String): Int
}