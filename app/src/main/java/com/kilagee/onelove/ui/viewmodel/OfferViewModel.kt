package com.kilagee.onelove.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kilagee.onelove.data.model.Offer
import com.kilagee.onelove.data.model.OfferContent
import com.kilagee.onelove.data.model.OfferStatus
import com.kilagee.onelove.data.model.OfferType
import com.kilagee.onelove.data.model.Result
import com.kilagee.onelove.data.repository.AuthRepository
import com.kilagee.onelove.data.repository.OfferRepository
import com.kilagee.onelove.data.repository.UserRepository
import com.kilagee.onelove.util.AppError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Date
import javax.inject.Inject

/**
 * Sealed class representing offer UI state
 */
sealed class OfferUiState {
    object Initial : OfferUiState()
    object Loading : OfferUiState()
    data class Error(val error: AppError, val message: String) : OfferUiState()
    data class Success<T>(val data: T) : OfferUiState()
}

/**
 * Offer operation types
 */
enum class OfferOperation {
    NONE,
    FETCH_OFFERS,
    CREATE_OFFER,
    UPDATE_OFFER,
    ACCEPT_OFFER,
    DECLINE_OFFER,
    CANCEL_OFFER,
    COMPLETE_OFFER,
    RATE_OFFER,
    SEARCH_OFFERS
}

/**
 * ViewModel for offer operations
 */
@HiltViewModel
class OfferViewModel @Inject constructor(
    private val offerRepository: OfferRepository,
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) : ViewModel() {
    
    // Current offer UI state
    private val _offerUiState = MutableStateFlow<OfferUiState>(OfferUiState.Initial)
    val offerUiState: StateFlow<OfferUiState> = _offerUiState.asStateFlow()
    
    // Current offer operation
    private val _currentOperation = MutableStateFlow(OfferOperation.NONE)
    val currentOperation: StateFlow<OfferOperation> = _currentOperation.asStateFlow()
    
    // Current viewed offer
    private val _currentOffer = MutableStateFlow<Offer?>(null)
    val currentOffer: StateFlow<Offer?> = _currentOffer.asStateFlow()
    
    // All offers for current user
    private val _offers = MutableStateFlow<List<Offer>>(emptyList())
    val offers: StateFlow<List<Offer>> = _offers.asStateFlow()
    
    // Pending offers for current user
    private val _pendingOffers = MutableStateFlow<List<Offer>>(emptyList())
    val pendingOffers: StateFlow<List<Offer>> = _pendingOffers.asStateFlow()
    
    // Accepted offers for current user
    private val _acceptedOffers = MutableStateFlow<List<Offer>>(emptyList())
    val acceptedOffers: StateFlow<List<Offer>> = _acceptedOffers.asStateFlow()
    
    // Offers for current match
    private val _matchOffers = MutableStateFlow<List<Offer>>(emptyList())
    val matchOffers: StateFlow<List<Offer>> = _matchOffers.asStateFlow()
    
    // Media upload progress
    private val _uploadProgress = MutableStateFlow(0f)
    val uploadProgress: StateFlow<Float> = _uploadProgress.asStateFlow()
    
    // Offer counts
    private val _incomingOfferCount = MutableStateFlow(0)
    val incomingOfferCount: StateFlow<Int> = _incomingOfferCount.asStateFlow()
    
    private val _pendingOfferCount = MutableStateFlow(0)
    val pendingOfferCount: StateFlow<Int> = _pendingOfferCount.asStateFlow()
    
    private val _remainingDailyOffers = MutableStateFlow(0)
    val remainingDailyOffers: StateFlow<Int> = _remainingDailyOffers.asStateFlow()
    
    // Popular offer types
    private val _popularOfferTypes = MutableStateFlow<List<OfferType>>(emptyList())
    val popularOfferTypes: StateFlow<List<OfferType>> = _popularOfferTypes.asStateFlow()
    
    // Search results
    private val _searchResults = MutableStateFlow<List<Offer>>(emptyList())
    val searchResults: StateFlow<List<Offer>> = _searchResults.asStateFlow()
    
    /**
     * Load offers for current user
     */
    fun loadOffersForUser() {
        viewModelScope.launch {
            _currentOperation.value = OfferOperation.FETCH_OFFERS
            _offerUiState.value = OfferUiState.Loading
            
            val userId = authRepository.getCurrentUserId() ?: return@launch
            
            when (val result = offerRepository.getOffersForUser(userId)) {
                is Result.Success -> {
                    _offers.value = result.data
                    _offerUiState.value = OfferUiState.Success(result.data)
                    
                    // Get pending and accepted offers
                    filterPendingAndAcceptedOffers(result.data)
                }
                is Result.Error -> {
                    _offerUiState.value = OfferUiState.Error(result.error, "Failed to load offers")
                }
            }
            
            // Get offer counts
            loadOfferCounts(userId)
            
            // Get popular offer types
            loadPopularOfferTypes()
            
            _currentOperation.value = OfferOperation.NONE
        }
    }
    
    /**
     * Filter pending and accepted offers from all offers
     */
    private fun filterPendingAndAcceptedOffers(allOffers: List<Offer>) {
        val userId = authRepository.getCurrentUserId() ?: return
        
        // Filter pending offers
        _pendingOffers.value = allOffers.filter { 
            it.status == OfferStatus.PENDING && 
            (it.receiverId == userId || it.senderId == userId)
        }
        
        // Filter accepted offers
        _acceptedOffers.value = allOffers.filter {
            it.status == OfferStatus.ACCEPTED &&
            (it.receiverId == userId || it.senderId == userId)
        }
    }
    
    /**
     * Load offers for a specific match
     */
    fun loadOffersForMatch(matchId: String) {
        viewModelScope.launch {
            _currentOperation.value = OfferOperation.FETCH_OFFERS
            _offerUiState.value = OfferUiState.Loading
            
            when (val result = offerRepository.getOffersForMatch(matchId)) {
                is Result.Success -> {
                    _matchOffers.value = result.data
                    _offerUiState.value = OfferUiState.Success(result.data)
                }
                is Result.Error -> {
                    _offerUiState.value = OfferUiState.Error(result.error, "Failed to load match offers")
                }
            }
            
            _currentOperation.value = OfferOperation.NONE
        }
    }
    
    /**
     * Load offer by ID
     */
    fun loadOfferById(offerId: String) {
        viewModelScope.launch {
            _currentOperation.value = OfferOperation.FETCH_OFFERS
            _offerUiState.value = OfferUiState.Loading
            
            when (val result = offerRepository.getOfferById(offerId)) {
                is Result.Success -> {
                    _currentOffer.value = result.data
                    _offerUiState.value = OfferUiState.Success(result.data)
                    
                    // Mark as viewed if current user is the receiver
                    val userId = authRepository.getCurrentUserId()
                    if (userId != null && result.data.receiverId == userId && !result.data.isViewed) {
                        markOfferAsViewed(offerId)
                    }
                }
                is Result.Error -> {
                    _offerUiState.value = OfferUiState.Error(result.error, "Failed to load offer")
                }
            }
            
            _currentOperation.value = OfferOperation.NONE
        }
    }
    
    /**
     * Create a new offer
     */
    fun createOffer(
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
        metadata: Map<String, Any> = emptyMap()
    ) {
        viewModelScope.launch {
            _currentOperation.value = OfferOperation.CREATE_OFFER
            _offerUiState.value = OfferUiState.Loading
            _uploadProgress.value = 0f
            
            val userId = authRepository.getCurrentUserId() ?: return@launch
            
            // Check if user can send an offer
            when (val canSendResult = offerRepository.canSendOffer(userId, receiverId)) {
                is Result.Success -> {
                    if (!canSendResult.data) {
                        _offerUiState.value = OfferUiState.Error(
                            AppError.ValidationError.LimitExceeded("You've reached your offer limit"),
                            "You've reached your offer limit"
                        )
                        _currentOperation.value = OfferOperation.NONE
                        return@launch
                    }
                }
                is Result.Error -> {
                    _offerUiState.value = OfferUiState.Error(canSendResult.error, "Failed to check offer limits")
                    _currentOperation.value = OfferOperation.NONE
                    return@launch
                }
            }
            
            // Create the offer
            when (val result = offerRepository.createOffer(
                senderId = userId,
                receiverId = receiverId,
                title = title,
                description = description,
                offerType = offerType,
                content = content,
                mediaUri = mediaUri,
                expiryDate = expiryDate,
                matchId = matchId,
                chatId = chatId,
                pointsCost = pointsCost,
                pointsReward = pointsReward,
                isPremiumOnly = isPremiumOnly,
                metadata = metadata,
                onProgress = { progress -> _uploadProgress.value = progress }
            )) {
                is Result.Success -> {
                    val newOffer = result.data
                    _currentOffer.value = newOffer
                    _offerUiState.value = OfferUiState.Success(newOffer)
                    
                    // Update offers list
                    val updatedOffers = _offers.value.toMutableList().apply {
                        add(newOffer)
                    }
                    _offers.value = updatedOffers
                    
                    // Update pending offers
                    val updatedPendingOffers = _pendingOffers.value.toMutableList().apply {
                        add(newOffer)
                    }
                    _pendingOffers.value = updatedPendingOffers
                    
                    // Update match offers if applicable
                    if (matchId != null) {
                        val updatedMatchOffers = _matchOffers.value.toMutableList().apply {
                            add(newOffer)
                        }
                        _matchOffers.value = updatedMatchOffers
                    }
                    
                    // Update remaining offers count
                    loadOfferCounts(userId)
                }
                is Result.Error -> {
                    _offerUiState.value = OfferUiState.Error(result.error, "Failed to create offer")
                }
            }
            
            _currentOperation.value = OfferOperation.NONE
        }
    }
    
    /**
     * Accept an offer
     */
    fun acceptOffer(offerId: String) {
        viewModelScope.launch {
            _currentOperation.value = OfferOperation.ACCEPT_OFFER
            _offerUiState.value = OfferUiState.Loading
            
            when (val result = offerRepository.acceptOffer(offerId)) {
                is Result.Success -> {
                    val updatedOffer = result.data
                    _currentOffer.value = updatedOffer
                    _offerUiState.value = OfferUiState.Success(updatedOffer)
                    
                    // Update offers lists
                    updateOfferInLists(updatedOffer)
                    
                    // Update offer counts
                    val userId = authRepository.getCurrentUserId()
                    if (userId != null) {
                        loadOfferCounts(userId)
                    }
                }
                is Result.Error -> {
                    _offerUiState.value = OfferUiState.Error(result.error, "Failed to accept offer")
                }
            }
            
            _currentOperation.value = OfferOperation.NONE
        }
    }
    
    /**
     * Decline an offer
     */
    fun declineOffer(offerId: String) {
        viewModelScope.launch {
            _currentOperation.value = OfferOperation.DECLINE_OFFER
            _offerUiState.value = OfferUiState.Loading
            
            when (val result = offerRepository.declineOffer(offerId)) {
                is Result.Success -> {
                    val updatedOffer = result.data
                    _currentOffer.value = updatedOffer
                    _offerUiState.value = OfferUiState.Success(updatedOffer)
                    
                    // Update offers lists
                    updateOfferInLists(updatedOffer)
                    
                    // Update offer counts
                    val userId = authRepository.getCurrentUserId()
                    if (userId != null) {
                        loadOfferCounts(userId)
                    }
                }
                is Result.Error -> {
                    _offerUiState.value = OfferUiState.Error(result.error, "Failed to decline offer")
                }
            }
            
            _currentOperation.value = OfferOperation.NONE
        }
    }
    
    /**
     * Cancel an offer
     */
    fun cancelOffer(offerId: String) {
        viewModelScope.launch {
            _currentOperation.value = OfferOperation.CANCEL_OFFER
            _offerUiState.value = OfferUiState.Loading
            
            when (val result = offerRepository.cancelOffer(offerId)) {
                is Result.Success -> {
                    val updatedOffer = result.data
                    _currentOffer.value = updatedOffer
                    _offerUiState.value = OfferUiState.Success(updatedOffer)
                    
                    // Update offers lists
                    updateOfferInLists(updatedOffer)
                    
                    // Update offer counts
                    val userId = authRepository.getCurrentUserId()
                    if (userId != null) {
                        loadOfferCounts(userId)
                    }
                }
                is Result.Error -> {
                    _offerUiState.value = OfferUiState.Error(result.error, "Failed to cancel offer")
                }
            }
            
            _currentOperation.value = OfferOperation.NONE
        }
    }
    
    /**
     * Mark offer as completed
     */
    fun completeOffer(offerId: String) {
        viewModelScope.launch {
            _currentOperation.value = OfferOperation.COMPLETE_OFFER
            _offerUiState.value = OfferUiState.Loading
            
            when (val result = offerRepository.completeOffer(offerId)) {
                is Result.Success -> {
                    val updatedOffer = result.data
                    _currentOffer.value = updatedOffer
                    _offerUiState.value = OfferUiState.Success(updatedOffer)
                    
                    // Update offers lists
                    updateOfferInLists(updatedOffer)
                }
                is Result.Error -> {
                    _offerUiState.value = OfferUiState.Error(result.error, "Failed to complete offer")
                }
            }
            
            _currentOperation.value = OfferOperation.NONE
        }
    }
    
    /**
     * Rate an offer
     */
    fun rateOffer(offerId: String, rating: Int, feedback: String? = null) {
        viewModelScope.launch {
            _currentOperation.value = OfferOperation.RATE_OFFER
            _offerUiState.value = OfferUiState.Loading
            
            val userId = authRepository.getCurrentUserId() ?: return@launch
            
            when (val result = offerRepository.rateOffer(offerId, userId, rating, feedback)) {
                is Result.Success -> {
                    val updatedOffer = result.data
                    _currentOffer.value = updatedOffer
                    _offerUiState.value = OfferUiState.Success(updatedOffer)
                    
                    // Update offers lists
                    updateOfferInLists(updatedOffer)
                }
                is Result.Error -> {
                    _offerUiState.value = OfferUiState.Error(result.error, "Failed to rate offer")
                }
            }
            
            _currentOperation.value = OfferOperation.NONE
        }
    }
    
    /**
     * Mark offer as viewed
     */
    private fun markOfferAsViewed(offerId: String) {
        viewModelScope.launch {
            offerRepository.markOfferAsViewed(offerId)
            
            // Update offer counts
            val userId = authRepository.getCurrentUserId()
            if (userId != null) {
                loadOfferCounts(userId)
            }
        }
    }
    
    /**
     * Report an offer
     */
    fun reportOffer(offerId: String, reason: String, details: String? = null) {
        viewModelScope.launch {
            _offerUiState.value = OfferUiState.Loading
            
            val userId = authRepository.getCurrentUserId() ?: return@launch
            
            when (val result = offerRepository.reportOffer(offerId, userId, reason, details)) {
                is Result.Success -> {
                    _offerUiState.value = OfferUiState.Success(Unit)
                }
                is Result.Error -> {
                    _offerUiState.value = OfferUiState.Error(result.error, "Failed to report offer")
                }
            }
        }
    }
    
    /**
     * Search offers
     */
    fun searchOffers(
        query: String,
        offerType: OfferType? = null,
        status: OfferStatus? = null,
        limit: Int = 20
    ) {
        viewModelScope.launch {
            _currentOperation.value = OfferOperation.SEARCH_OFFERS
            _offerUiState.value = OfferUiState.Loading
            
            val userId = authRepository.getCurrentUserId() ?: return@launch
            
            when (val result = offerRepository.searchOffers(userId, query, offerType, status, limit)) {
                is Result.Success -> {
                    _searchResults.value = result.data
                    _offerUiState.value = OfferUiState.Success(result.data)
                }
                is Result.Error -> {
                    _offerUiState.value = OfferUiState.Error(result.error, "Failed to search offers")
                }
            }
            
            _currentOperation.value = OfferOperation.NONE
        }
    }
    
    /**
     * Load offer counts for current user
     */
    private fun loadOfferCounts(userId: String) {
        viewModelScope.launch {
            // Load incoming offers count
            when (val result = offerRepository.getIncomingOfferCount(userId)) {
                is Result.Success -> {
                    _incomingOfferCount.value = result.data
                }
                is Result.Error -> {
                    Timber.e(result.error.throwable, "Error loading incoming offer count")
                }
            }
            
            // Load pending offers count
            when (val result = offerRepository.getPendingOfferCount(userId)) {
                is Result.Success -> {
                    _pendingOfferCount.value = result.data
                }
                is Result.Error -> {
                    Timber.e(result.error.throwable, "Error loading pending offer count")
                }
            }
            
            // Load remaining daily offers
            when (val result = offerRepository.getRemainingDailyOffers(userId)) {
                is Result.Success -> {
                    _remainingDailyOffers.value = result.data
                }
                is Result.Error -> {
                    Timber.e(result.error.throwable, "Error loading remaining daily offers")
                }
            }
        }
    }
    
    /**
     * Load popular offer types
     */
    private fun loadPopularOfferTypes() {
        viewModelScope.launch {
            when (val result = offerRepository.getPopularOfferTypes()) {
                is Result.Success -> {
                    _popularOfferTypes.value = result.data
                }
                is Result.Error -> {
                    Timber.e(result.error.throwable, "Error loading popular offer types")
                }
            }
        }
    }
    
    /**
     * Update an offer in all lists
     */
    private fun updateOfferInLists(updatedOffer: Offer) {
        // Update in all offers list
        val currentOffers = _offers.value.toMutableList()
        val offerIndex = currentOffers.indexOfFirst { it.id == updatedOffer.id }
        if (offerIndex != -1) {
            currentOffers[offerIndex] = updatedOffer
            _offers.value = currentOffers
        }
        
        // Update in pending offers list if status is still pending
        if (updatedOffer.status == OfferStatus.PENDING) {
            val currentPendingOffers = _pendingOffers.value.toMutableList()
            val pendingIndex = currentPendingOffers.indexOfFirst { it.id == updatedOffer.id }
            if (pendingIndex != -1) {
                currentPendingOffers[pendingIndex] = updatedOffer
                _pendingOffers.value = currentPendingOffers
            }
        } else {
            // Remove from pending if status changed
            _pendingOffers.value = _pendingOffers.value.filter { it.id != updatedOffer.id }
        }
        
        // Update in accepted offers list if status is accepted
        if (updatedOffer.status == OfferStatus.ACCEPTED) {
            val currentAcceptedOffers = _acceptedOffers.value.toMutableList()
            val acceptedIndex = currentAcceptedOffers.indexOfFirst { it.id == updatedOffer.id }
            if (acceptedIndex != -1) {
                currentAcceptedOffers[acceptedIndex] = updatedOffer
            } else {
                currentAcceptedOffers.add(updatedOffer)
            }
            _acceptedOffers.value = currentAcceptedOffers
        } else {
            // Remove from accepted if status changed
            _acceptedOffers.value = _acceptedOffers.value.filter { it.id != updatedOffer.id }
        }
        
        // Update in match offers if applicable
        if (updatedOffer.matchId != null) {
            val currentMatchOffers = _matchOffers.value.toMutableList()
            val matchIndex = currentMatchOffers.indexOfFirst { it.id == updatedOffer.id }
            if (matchIndex != -1) {
                currentMatchOffers[matchIndex] = updatedOffer
                _matchOffers.value = currentMatchOffers
            }
        }
    }
    
    /**
     * Reset offer UI state
     */
    fun resetOfferState() {
        _offerUiState.value = OfferUiState.Initial
        _currentOperation.value = OfferOperation.NONE
    }
}