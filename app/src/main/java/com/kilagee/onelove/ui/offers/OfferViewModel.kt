package com.kilagee.onelove.ui.offers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kilagee.onelove.data.model.Offer
import com.kilagee.onelove.data.model.OfferStatus
import com.kilagee.onelove.data.model.OfferType
import com.kilagee.onelove.domain.model.Resource
import com.kilagee.onelove.domain.repository.OfferRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class OfferViewModel @Inject constructor(
    private val offerRepository: OfferRepository
) : ViewModel() {
    
    // State for all offers
    private val _allOffersState = MutableStateFlow<Resource<List<Offer>>>(Resource.Loading)
    val allOffersState: StateFlow<Resource<List<Offer>>> = _allOffersState
    
    // State for received offers
    private val _receivedOffersState = MutableStateFlow<Resource<List<Offer>>>(Resource.Loading)
    val receivedOffersState: StateFlow<Resource<List<Offer>>> = _receivedOffersState
    
    // State for sent offers
    private val _sentOffersState = MutableStateFlow<Resource<List<Offer>>>(Resource.Loading)
    val sentOffersState: StateFlow<Resource<List<Offer>>> = _sentOffersState
    
    // State for pending offers count
    private val _pendingOffersCountState = MutableStateFlow<Resource<Int>>(Resource.Loading)
    val pendingOffersCountState: StateFlow<Resource<Int>> = _pendingOffersCountState
    
    // State for offer operations (create, update status)
    private val _operationState = MutableStateFlow<Resource<Any>?>(null)
    val operationState: StateFlow<Resource<Any>?> = _operationState
    
    // State for current offer details
    private val _currentOfferState = MutableStateFlow<Resource<Offer>?>(null)
    val currentOfferState: StateFlow<Resource<Offer>?> = _currentOfferState
    
    init {
        loadAllData()
    }
    
    /**
     * Load all offers data
     */
    fun loadAllData() {
        loadAllOffers()
        loadReceivedOffers()
        loadSentOffers()
        loadPendingOffersCount()
    }
    
    /**
     * Load all offers
     */
    fun loadAllOffers() {
        viewModelScope.launch {
            offerRepository.getAllOffers()
                .onEach { resource ->
                    _allOffersState.value = resource
                }
                .catch { e ->
                    _allOffersState.value = Resource.error("Failed to load offers: ${e.message}")
                }
                .launchIn(viewModelScope)
        }
    }
    
    /**
     * Load received offers
     */
    fun loadReceivedOffers() {
        viewModelScope.launch {
            offerRepository.getReceivedOffers()
                .onEach { resource ->
                    _receivedOffersState.value = resource
                }
                .catch { e ->
                    _receivedOffersState.value = Resource.error("Failed to load received offers: ${e.message}")
                }
                .launchIn(viewModelScope)
        }
    }
    
    /**
     * Load sent offers
     */
    fun loadSentOffers() {
        viewModelScope.launch {
            offerRepository.getSentOffers()
                .onEach { resource ->
                    _sentOffersState.value = resource
                }
                .catch { e ->
                    _sentOffersState.value = Resource.error("Failed to load sent offers: ${e.message}")
                }
                .launchIn(viewModelScope)
        }
    }
    
    /**
     * Load pending offers count
     */
    fun loadPendingOffersCount() {
        viewModelScope.launch {
            offerRepository.getPendingOffersCount()
                .onEach { resource ->
                    _pendingOffersCountState.value = resource
                }
                .catch { e ->
                    _pendingOffersCountState.value = Resource.error("Failed to get pending count: ${e.message}")
                }
                .launchIn(viewModelScope)
        }
    }
    
    /**
     * Create a new offer
     */
    fun createOffer(
        receiverId: String,
        type: OfferType,
        title: String,
        description: String = "",
        location: String = "",
        proposedTime: Date? = null,
        pointsOffered: Int = 0
    ) {
        viewModelScope.launch {
            _operationState.value = Resource.Loading
            
            offerRepository.createOffer(
                receiverId = receiverId,
                type = type,
                title = title,
                description = description,
                location = location,
                proposedTime = proposedTime,
                pointsOffered = pointsOffered
            )
                .onEach { resource ->
                    _operationState.value = resource
                    
                    // Refresh offers if successful
                    if (resource is Resource.Success) {
                        loadAllData()
                    }
                }
                .catch { e ->
                    _operationState.value = Resource.error("Failed to create offer: ${e.message}")
                }
                .launchIn(viewModelScope)
        }
    }
    
    /**
     * Update an offer's status
     */
    fun updateOfferStatus(offerId: String, status: OfferStatus) {
        viewModelScope.launch {
            _operationState.value = Resource.Loading
            
            offerRepository.updateOfferStatus(offerId, status)
                .onEach { resource ->
                    _operationState.value = resource
                    
                    // Refresh offers if successful
                    if (resource is Resource.Success) {
                        loadAllData()
                        
                        // If we're viewing offer details, update that as well
                        _currentOfferState.value?.let { currentOffer ->
                            if (currentOffer is Resource.Success && currentOffer.data.id == offerId) {
                                loadOfferDetails(offerId)
                            }
                        }
                    }
                }
                .catch { e ->
                    _operationState.value = Resource.error("Failed to update offer: ${e.message}")
                }
                .launchIn(viewModelScope)
        }
    }
    
    /**
     * Load the details of a specific offer
     */
    fun loadOfferDetails(offerId: String) {
        viewModelScope.launch {
            _currentOfferState.value = Resource.Loading
            
            offerRepository.getOfferById(offerId)
                .onEach { resource ->
                    _currentOfferState.value = resource
                }
                .catch { e ->
                    _currentOfferState.value = Resource.error("Failed to load offer details: ${e.message}")
                }
                .launchIn(viewModelScope)
        }
    }
    
    /**
     * Clear operation state when it's been handled
     */
    fun clearOperationState() {
        _operationState.value = null
    }
    
    /**
     * Clear current offer state when navigating away
     */
    fun clearCurrentOfferState() {
        _currentOfferState.value = null
    }
    
    /**
     * Get offers filtered by status
     */
    fun loadOffersByStatus(status: OfferStatus, sent: Boolean = false) {
        viewModelScope.launch {
            val stateFlow = if (sent) _sentOffersState else _receivedOffersState
            stateFlow.value = Resource.Loading
            
            offerRepository.getOffersByStatus(status, sent)
                .onEach { resource ->
                    stateFlow.value = resource
                }
                .catch { e ->
                    stateFlow.value = Resource.error("Failed to load offers: ${e.message}")
                }
                .launchIn(viewModelScope)
        }
    }
    
    /**
     * Accept an offer
     */
    fun acceptOffer(offerId: String) {
        updateOfferStatus(offerId, OfferStatus.ACCEPTED)
    }
    
    /**
     * Decline an offer
     */
    fun declineOffer(offerId: String) {
        updateOfferStatus(offerId, OfferStatus.DECLINED)
    }
    
    /**
     * Cancel an offer
     */
    fun cancelOffer(offerId: String) {
        updateOfferStatus(offerId, OfferStatus.CANCELLED)
    }
    
    /**
     * Mark an offer as completed
     */
    fun completeOffer(offerId: String) {
        updateOfferStatus(offerId, OfferStatus.COMPLETED)
    }
}