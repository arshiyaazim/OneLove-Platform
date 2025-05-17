package com.kilagee.onelove.domain.repository

import com.kilagee.onelove.data.model.Offer
import com.kilagee.onelove.data.model.OfferLocation
import com.kilagee.onelove.data.model.OfferStatus
import com.kilagee.onelove.data.model.OfferType
import com.kilagee.onelove.domain.util.Result
import kotlinx.coroutines.flow.Flow
import java.io.File
import java.util.Date

/**
 * Repository interface for offer operations
 */
interface OfferRepository {
    
    /**
     * Create a new offer
     */
    suspend fun createOffer(
        matchId: String,
        senderId: String,
        receiverId: String,
        title: String,
        description: String,
        date: Date?,
        location: OfferLocation?,
        type: OfferType,
        pointsCost: Int,
        pointsReward: Int,
        activities: List<String>,
        photos: List<File> = listOf()
    ): Result<Offer>
    
    /**
     * Get an offer by ID
     */
    suspend fun getOfferById(offerId: String): Result<Offer>
    
    /**
     * Get offer as a flow for real-time updates
     */
    fun getOfferFlow(offerId: String): Flow<Result<Offer>>
    
    /**
     * Get offers for a match
     */
    suspend fun getOffersForMatch(matchId: String): Result<List<Offer>>
    
    /**
     * Get offers for a match as a flow
     */
    fun getOffersForMatchFlow(matchId: String): Flow<Result<List<Offer>>>
    
    /**
     * Get all offers sent by a user
     */
    suspend fun getOffersSentByUser(userId: String): Result<List<Offer>>
    
    /**
     * Get all offers received by a user
     */
    suspend fun getOffersReceivedByUser(userId: String): Result<List<Offer>>
    
    /**
     * Get pending offers for a user
     */
    suspend fun getPendingOffersForUser(userId: String): Result<List<Offer>>
    
    /**
     * Get pending offers for a user as a flow
     */
    fun getPendingOffersForUserFlow(userId: String): Flow<Result<List<Offer>>>
    
    /**
     * Accept an offer
     */
    suspend fun acceptOffer(offerId: String, userId: String): Result<Unit>
    
    /**
     * Decline an offer
     */
    suspend fun declineOffer(offerId: String, userId: String): Result<Unit>
    
    /**
     * Cancel an offer
     */
    suspend fun cancelOffer(offerId: String, userId: String): Result<Unit>
    
    /**
     * Complete an offer
     */
    suspend fun completeOffer(offerId: String, userId: String): Result<Unit>
    
    /**
     * Update offer status
     */
    suspend fun updateOfferStatus(offerId: String, status: OfferStatus): Result<Unit>
    
    /**
     * Update offer details
     */
    suspend fun updateOffer(offer: Offer): Result<Unit>
}