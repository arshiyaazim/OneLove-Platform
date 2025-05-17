package com.kilagee.onelove.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.kilagee.onelove.data.database.dao.OfferDao
import com.kilagee.onelove.data.model.Offer
import com.kilagee.onelove.data.model.OfferStatus
import com.kilagee.onelove.data.model.OfferType
import com.kilagee.onelove.domain.model.Resource
import com.kilagee.onelove.domain.repository.OfferRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseOfferRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val offerDao: OfferDao
) : OfferRepository {
    
    private val offersCollection = firestore.collection("offers")
    private val usersCollection = firestore.collection("users")
    
    override fun createOffer(
        receiverId: String,
        type: OfferType,
        title: String,
        description: String,
        location: String,
        proposedTime: Date?,
        pointsOffered: Int
    ): Flow<Resource<Offer>> = flow {
        emit(Resource.Loading)
        
        try {
            val currentUserId = auth.currentUser?.uid ?: run {
                emit(Resource.error("User not authenticated"))
                return@flow
            }
            
            // Get sender and receiver info for UI display
            val senderInfo = getUserBasicInfo(currentUserId)
            val receiverInfo = getUserBasicInfo(receiverId)
            
            // Create the offer
            val newOffer = Offer(
                id = "", // Will be set by Firestore
                senderId = currentUserId,
                receiverId = receiverId,
                type = type,
                title = title,
                description = description,
                location = location,
                proposedTime = proposedTime,
                status = OfferStatus.PENDING,
                pointsOffered = pointsOffered,
                createdAt = Date(),
                updatedAt = Date(),
                senderName = senderInfo?.first,
                senderProfileImageUrl = senderInfo?.second,
                receiverName = receiverInfo?.first,
                receiverProfileImageUrl = receiverInfo?.second
            )
            
            // Add to Firestore
            val docRef = offersCollection.add(newOffer).await()
            
            // Get the offer with the generated ID
            val addedOffer = newOffer.copy(id = docRef.id)
            
            // Save to local database
            offerDao.insertOffer(addedOffer)
            
            emit(Resource.success(addedOffer))
        } catch (e: Exception) {
            emit(Resource.error("Failed to create offer: ${e.message}"))
        }
    }
    
    override fun getSentOffers(): Flow<Resource<List<Offer>>> = flow {
        emit(Resource.Loading)
        
        try {
            val currentUserId = auth.currentUser?.uid ?: run {
                emit(Resource.error("User not authenticated"))
                return@flow
            }
            
            // Get sent offers from Firestore
            val query = offersCollection
                .whereEqualTo("senderId", currentUserId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
            
            val querySnapshot = query.get().await()
            val offers = querySnapshot.documents.mapNotNull { doc ->
                val offer = doc.toObject(Offer::class.java)
                offer?.copy(id = doc.id)
            }
            
            // Save to local database
            offers.forEach { offer ->
                offerDao.insertOffer(offer)
            }
            
            emit(Resource.success(offers))
        } catch (e: Exception) {
            emit(Resource.error("Failed to get sent offers: ${e.message}"))
        }
    }
    
    override fun getReceivedOffers(): Flow<Resource<List<Offer>>> = flow {
        emit(Resource.Loading)
        
        try {
            val currentUserId = auth.currentUser?.uid ?: run {
                emit(Resource.error("User not authenticated"))
                return@flow
            }
            
            // Get received offers from Firestore
            val query = offersCollection
                .whereEqualTo("receiverId", currentUserId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
            
            val querySnapshot = query.get().await()
            val offers = querySnapshot.documents.mapNotNull { doc ->
                val offer = doc.toObject(Offer::class.java)
                offer?.copy(id = doc.id)
            }
            
            // Save to local database
            offers.forEach { offer ->
                offerDao.insertOffer(offer)
            }
            
            emit(Resource.success(offers))
        } catch (e: Exception) {
            emit(Resource.error("Failed to get received offers: ${e.message}"))
        }
    }
    
    override fun getAllOffers(): Flow<Resource<List<Offer>>> = flow {
        emit(Resource.Loading)
        
        try {
            val currentUserId = auth.currentUser?.uid ?: run {
                emit(Resource.error("User not authenticated"))
                return@flow
            }
            
            // Get all offers where user is either sender or receiver
            val sentQuery = offersCollection.whereEqualTo("senderId", currentUserId)
            val receivedQuery = offersCollection.whereEqualTo("receiverId", currentUserId)
            
            val sentOffers = sentQuery.get().await().documents.mapNotNull { doc ->
                val offer = doc.toObject(Offer::class.java)
                offer?.copy(id = doc.id)
            }
            
            val receivedOffers = receivedQuery.get().await().documents.mapNotNull { doc ->
                val offer = doc.toObject(Offer::class.java)
                offer?.copy(id = doc.id)
            }
            
            // Combine and sort by created date (descending)
            val allOffers = (sentOffers + receivedOffers).sortedByDescending { it.createdAt }
            
            // Save to local database
            allOffers.forEach { offer ->
                offerDao.insertOffer(offer)
            }
            
            emit(Resource.success(allOffers))
        } catch (e: Exception) {
            emit(Resource.error("Failed to get all offers: ${e.message}"))
        }
    }
    
    override fun getOffersByStatus(status: OfferStatus, sent: Boolean): Flow<Resource<List<Offer>>> = flow {
        emit(Resource.Loading)
        
        try {
            val currentUserId = auth.currentUser?.uid ?: run {
                emit(Resource.error("User not authenticated"))
                return@flow
            }
            
            // Define the query based on whether we want sent or received offers
            val query = if (sent) {
                offersCollection
                    .whereEqualTo("senderId", currentUserId)
                    .whereEqualTo("status", status)
            } else {
                offersCollection
                    .whereEqualTo("receiverId", currentUserId)
                    .whereEqualTo("status", status)
            }
            
            // Execute the query
            val querySnapshot = query.get().await()
            val offers = querySnapshot.documents.mapNotNull { doc ->
                val offer = doc.toObject(Offer::class.java)
                offer?.copy(id = doc.id)
            }
            
            // Save to local database
            offers.forEach { offer ->
                offerDao.insertOffer(offer)
            }
            
            emit(Resource.success(offers))
        } catch (e: Exception) {
            emit(Resource.error("Failed to get offers by status: ${e.message}"))
        }
    }
    
    override fun getOfferById(offerId: String): Flow<Resource<Offer>> = flow {
        emit(Resource.Loading)
        
        try {
            // Try local database first
            val localOffer = offerDao.getOfferById(offerId)
            if (localOffer != null) {
                emit(Resource.success(localOffer))
                return@flow
            }
            
            // If not in local database, try Firestore
            val docSnapshot = offersCollection.document(offerId).get().await()
            if (docSnapshot.exists()) {
                val offer = docSnapshot.toObject(Offer::class.java)
                if (offer != null) {
                    val offerWithId = offer.copy(id = docSnapshot.id)
                    
                    // Save to local database
                    offerDao.insertOffer(offerWithId)
                    
                    emit(Resource.success(offerWithId))
                } else {
                    emit(Resource.error("Failed to parse offer"))
                }
            } else {
                emit(Resource.error("Offer not found"))
            }
        } catch (e: Exception) {
            emit(Resource.error("Failed to get offer: ${e.message}"))
        }
    }
    
    override fun updateOfferStatus(offerId: String, status: OfferStatus): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading)
        
        try {
            val currentUserId = auth.currentUser?.uid ?: run {
                emit(Resource.error("User not authenticated"))
                return@flow
            }
            
            // Get the offer
            val offerDoc = offersCollection.document(offerId).get().await()
            if (!offerDoc.exists()) {
                emit(Resource.error("Offer not found"))
                return@flow
            }
            
            val offer = offerDoc.toObject(Offer::class.java)
            if (offer == null) {
                emit(Resource.error("Failed to parse offer"))
                return@flow
            }
            
            // Check if user is authorized to update the offer
            if (offer.senderId != currentUserId && offer.receiverId != currentUserId) {
                emit(Resource.error("Not authorized to update this offer"))
                return@flow
            }
            
            // Update the status
            offersCollection.document(offerId).update(
                mapOf(
                    "status" to status,
                    "updatedAt" to Date()
                )
            ).await()
            
            // Update in local database
            offerDao.updateOfferStatus(offerId, status, Date())
            
            emit(Resource.success(Unit))
        } catch (e: Exception) {
            emit(Resource.error("Failed to update offer status: ${e.message}"))
        }
    }
    
    override fun getPendingOffersCount(): Flow<Resource<Int>> = flow {
        emit(Resource.Loading)
        
        try {
            val currentUserId = auth.currentUser?.uid ?: run {
                emit(Resource.error("User not authenticated"))
                return@flow
            }
            
            // Query for pending offers received by the current user
            val query = offersCollection
                .whereEqualTo("receiverId", currentUserId)
                .whereEqualTo("status", OfferStatus.PENDING)
            
            val querySnapshot = query.get().await()
            val count = querySnapshot.size()
            
            emit(Resource.success(count))
        } catch (e: Exception) {
            emit(Resource.error("Failed to get pending offers count: ${e.message}"))
        }
    }
    
    // Helper method to get basic user info (name and profile image)
    private suspend fun getUserBasicInfo(userId: String): Pair<String, String>? {
        return try {
            val userDoc = usersCollection.document(userId).get().await()
            if (userDoc.exists()) {
                val name = userDoc.getString("first_name") ?: "Unknown"
                val profileImageUrl = userDoc.getString("profile_picture_url") ?: ""
                Pair(name, profileImageUrl)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}