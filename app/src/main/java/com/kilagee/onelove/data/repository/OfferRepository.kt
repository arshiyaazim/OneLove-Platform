package com.kilagee.onelove.data.repository

import android.net.Uri
import com.kilagee.onelove.data.model.Offer
import com.kilagee.onelove.data.model.OfferContent
import com.kilagee.onelove.data.model.OfferStatus
import com.kilagee.onelove.data.model.OfferType
import com.kilagee.onelove.data.model.Result
import kotlinx.coroutines.flow.Flow
import java.util.Date

/**
 * Repository interface for offer operations
 */
interface OfferRepository {
    /**
     * Get offer by ID
     */
    suspend fun getOfferById(offerId: String): Result<Offer>
    
    /**
     * Get offer by ID as Flow
     */
    fun getOfferByIdFlow(offerId: String): Flow<Offer?>
    
    /**
     * Get offers for user (sent and received)
     */
    suspend fun getOffersForUser(userId: String, limit: Int = 50): Result<List<Offer>>
    
    /**
     * Get offers for user as Flow
     */
    fun getOffersForUserFlow(userId: String): Flow<List<Offer>>
    
    /**
     * Get pending offers for user
     */
    suspend fun getPendingOffersForUser(userId: String): Result<List<Offer>>
    
    /**
     * Get accepted offers for user
     */
    suspend fun getAcceptedOffersForUser(userId: String): Result<List<Offer>>
    
    /**
     * Get offers for match
     */
    suspend fun getOffersForMatch(matchId: String): Result<List<Offer>>
    
    /**
     * Get offers for match as Flow
     */
    fun getOffersForMatchFlow(matchId: String): Flow<List<Offer>>
    
    /**
     * Create a new offer
     */
    suspend fun createOffer(
        senderId: String,
        receiverId: String,
        title: String,
        description: String,
        offerType: OfferType,
        content: OfferContent,
        mediaUri: Uri? = null,
        expiryDate: Date? = null,
        matchId: String? = null,
        chatId: String? = null,
        pointsCost: Int = 0,
        pointsReward: Int = 0,
        isPremiumOnly: Boolean = false,
        metadata: Map<String, Any> = emptyMap(),
        onProgress: ((Float) -> Unit)? = null
    ): Result<Offer>
    
    /**
     * Update offer status
     */
    suspend fun updateOfferStatus(offerId: String, status: OfferStatus): Result<Offer>
    
    /**
     * Accept offer
     */
    suspend fun acceptOffer(offerId: String): Result<Offer>
    
    /**
     * Decline offer
     */
    suspend fun declineOffer(offerId: String): Result<Offer>
    
    /**
     * Cancel offer
     */
    suspend fun cancelOffer(offerId: String): Result<Offer>
    
    /**
     * Mark offer as completed
     */
    suspend fun completeOffer(offerId: String): Result<Offer>
    
    /**
     * Mark offer as viewed
     */
    suspend fun markOfferAsViewed(offerId: String): Result<Offer>
    
    /**
     * Add rating to offer
     */
    suspend fun rateOffer(
        offerId: String,
        userId: String,
        rating: Int,
        feedback: String? = null
    ): Result<Offer>
    
    /**
     * Get incoming offer count for user
     */
    suspend fun getIncomingOfferCount(userId: String): Result<Int>
    
    /**
     * Get pending offer count for user
     */
    suspend fun getPendingOfferCount(userId: String): Result<Int>
    
    /**
     * Check if user can send an offer
     * (based on premium status, daily limits, etc.)
     */
    suspend fun canSendOffer(userId: String, receiverId: String): Result<Boolean>
    
    /**
     * Get daily offer quota for user
     */
    suspend fun getDailyOfferQuota(userId: String): Result<Int>
    
    /**
     * Get remaining daily offers for user
     */
    suspend fun getRemainingDailyOffers(userId: String): Result<Int>
    
    /**
     * Delete offer
     */
    suspend fun deleteOffer(offerId: String): Result<Unit>
    
    /**
     * Report offer
     */
    suspend fun reportOffer(
        offerId: String,
        userId: String,
        reason: String,
        details: String? = null
    ): Result<Unit>
    
    /**
     * Get popular offer types
     */
    suspend fun getPopularOfferTypes(): Result<List<OfferType>>
    
    /**
     * Search offers
     */
    suspend fun searchOffers(
        userId: String,
        query: String,
        offerType: OfferType? = null,
        status: OfferStatus? = null,
        limit: Int = 20
    ): Result<List<Offer>>
}