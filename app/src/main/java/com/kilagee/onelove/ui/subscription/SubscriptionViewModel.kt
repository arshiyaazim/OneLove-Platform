package com.kilagee.onelove.ui.subscription

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kilagee.onelove.data.model.Subscription
import com.kilagee.onelove.data.model.SubscriptionStatus
import com.kilagee.onelove.data.model.SubscriptionType
import com.kilagee.onelove.domain.model.Resource
import com.kilagee.onelove.domain.repository.PaymentMethod
import com.kilagee.onelove.domain.repository.PaymentMethodDetails
import com.kilagee.onelove.domain.repository.PaymentRepository
import com.kilagee.onelove.domain.repository.SubscriptionPlan
import com.kilagee.onelove.domain.repository.SubscriptionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SubscriptionViewModel @Inject constructor(
    private val subscriptionRepository: SubscriptionRepository,
    private val paymentRepository: PaymentRepository
) : ViewModel() {
    
    // State for subscription plans
    private val _subscriptionPlansState = MutableStateFlow<Resource<List<SubscriptionPlan>>>(Resource.Loading)
    val subscriptionPlansState: StateFlow<Resource<List<SubscriptionPlan>>> = _subscriptionPlansState
    
    // State for user subscriptions
    private val _userSubscriptionsState = MutableStateFlow<Resource<List<Subscription>>>(Resource.Loading)
    val userSubscriptionsState: StateFlow<Resource<List<Subscription>>> = _userSubscriptionsState
    
    // State for current active subscription
    private val _activeSubscriptionState = MutableStateFlow<Resource<Subscription?>>(Resource.Loading)
    val activeSubscriptionState: StateFlow<Resource<Subscription?>> = _activeSubscriptionState
    
    // State for subscription creation
    private val _createSubscriptionState = MutableStateFlow<Resource<Subscription>?>(null)
    val createSubscriptionState: StateFlow<Resource<Subscription>?> = _createSubscriptionState
    
    // State for payment methods
    private val _paymentMethodsState = MutableStateFlow<Resource<List<PaymentMethod>>>(Resource.Loading)
    val paymentMethodsState: StateFlow<Resource<List<PaymentMethod>>> = _paymentMethodsState
    
    // State for new payment method
    private val _newPaymentMethodState = MutableStateFlow<Resource<String>?>(null)
    val newPaymentMethodState: StateFlow<Resource<String>?> = _newPaymentMethodState
    
    // State for premium status
    private val _isPremiumState = MutableStateFlow<Resource<Boolean>>(Resource.Loading)
    val isPremiumState: StateFlow<Resource<Boolean>> = _isPremiumState
    
    init {
        loadSubscriptionPlans()
        loadUserSubscriptions()
        loadActiveSubscription()
        loadPaymentMethods()
        checkPremiumStatus()
    }
    
    /**
     * Load available subscription plans
     */
    fun loadSubscriptionPlans() {
        _subscriptionPlansState.value = Resource.Loading
        
        subscriptionRepository.getSubscriptionPlans()
            .onEach { resource ->
                _subscriptionPlansState.value = resource
            }
            .catch { e ->
                _subscriptionPlansState.value = Resource.error("Failed to load subscription plans: ${e.message}")
            }
            .launchIn(viewModelScope)
    }
    
    /**
     * Load user's subscriptions
     */
    fun loadUserSubscriptions() {
        _userSubscriptionsState.value = Resource.Loading
        
        subscriptionRepository.getUserSubscriptions()
            .onEach { resource ->
                _userSubscriptionsState.value = resource
            }
            .catch { e ->
                _userSubscriptionsState.value = Resource.error("Failed to load subscriptions: ${e.message}")
            }
            .launchIn(viewModelScope)
    }
    
    /**
     * Load user's active subscription
     */
    fun loadActiveSubscription() {
        _activeSubscriptionState.value = Resource.Loading
        
        subscriptionRepository.getCurrentActiveSubscription()
            .onEach { resource ->
                _activeSubscriptionState.value = resource
            }
            .catch { e ->
                _activeSubscriptionState.value = Resource.error("Failed to load active subscription: ${e.message}")
            }
            .launchIn(viewModelScope)
    }
    
    /**
     * Create a new subscription
     */
    fun createSubscription(type: SubscriptionType, paymentMethodId: String, autoRenew: Boolean = true) {
        _createSubscriptionState.value = Resource.Loading
        
        subscriptionRepository.createSubscription(type, paymentMethodId, autoRenew)
            .onEach { resource ->
                _createSubscriptionState.value = resource
                
                // Reload user subscriptions and active subscription on success
                if (resource is Resource.Success) {
                    loadUserSubscriptions()
                    loadActiveSubscription()
                    checkPremiumStatus()
                }
            }
            .catch { e ->
                _createSubscriptionState.value = Resource.error("Failed to create subscription: ${e.message}")
            }
            .launchIn(viewModelScope)
    }
    
    /**
     * Cancel a subscription
     */
    fun cancelSubscription(subscriptionId: String, cancelImmediately: Boolean = false) {
        viewModelScope.launch {
            subscriptionRepository.cancelSubscription(subscriptionId, cancelImmediately)
                .onEach { resource ->
                    // Reload subscriptions on success
                    if (resource is Resource.Success) {
                        loadUserSubscriptions()
                        loadActiveSubscription()
                        checkPremiumStatus()
                    }
                }
                .catch { e ->
                    // Error handling
                }
                .launchIn(viewModelScope)
        }
    }
    
    /**
     * Toggle auto-renewal for a subscription
     */
    fun toggleAutoRenew(subscriptionId: String, autoRenew: Boolean) {
        viewModelScope.launch {
            subscriptionRepository.updateSubscription(subscriptionId, null, autoRenew)
                .onEach { resource ->
                    // Reload subscriptions on success
                    if (resource is Resource.Success) {
                        loadUserSubscriptions()
                        loadActiveSubscription()
                    }
                }
                .catch { e ->
                    // Error handling
                }
                .launchIn(viewModelScope)
        }
    }
    
    /**
     * Sync subscriptions with the payment provider
     */
    fun syncSubscriptions() {
        viewModelScope.launch {
            subscriptionRepository.syncSubscriptions()
                .onEach { resource ->
                    // Reload subscriptions on success
                    if (resource is Resource.Success) {
                        loadUserSubscriptions()
                        loadActiveSubscription()
                        checkPremiumStatus()
                    }
                }
                .catch { e ->
                    // Error handling
                }
                .launchIn(viewModelScope)
        }
    }
    
    /**
     * Load saved payment methods
     */
    fun loadPaymentMethods() {
        _paymentMethodsState.value = Resource.Loading
        
        paymentRepository.getSavedPaymentMethods()
            .onEach { resource ->
                _paymentMethodsState.value = resource
            }
            .catch { e ->
                _paymentMethodsState.value = Resource.error("Failed to load payment methods: ${e.message}")
            }
            .launchIn(viewModelScope)
    }
    
    /**
     * Save a new payment method
     */
    fun savePaymentMethod(paymentMethodDetails: PaymentMethodDetails) {
        _newPaymentMethodState.value = Resource.Loading
        
        paymentRepository.savePaymentMethod(paymentMethodDetails)
            .onEach { resource ->
                _newPaymentMethodState.value = resource
                
                // Reload payment methods on success
                if (resource is Resource.Success) {
                    loadPaymentMethods()
                }
            }
            .catch { e ->
                _newPaymentMethodState.value = Resource.error("Failed to save payment method: ${e.message}")
            }
            .launchIn(viewModelScope)
    }
    
    /**
     * Delete a payment method
     */
    fun deletePaymentMethod(paymentMethodId: String) {
        viewModelScope.launch {
            paymentRepository.deletePaymentMethod(paymentMethodId)
                .onEach { resource ->
                    // Reload payment methods on success
                    if (resource is Resource.Success) {
                        loadPaymentMethods()
                    }
                }
                .catch { e ->
                    // Error handling
                }
                .launchIn(viewModelScope)
        }
    }
    
    /**
     * Set a payment method as default
     */
    fun setDefaultPaymentMethod(paymentMethodId: String) {
        viewModelScope.launch {
            paymentRepository.setDefaultPaymentMethod(paymentMethodId)
                .onEach { resource ->
                    // Reload payment methods on success
                    if (resource is Resource.Success) {
                        loadPaymentMethods()
                    }
                }
                .catch { e ->
                    // Error handling
                }
                .launchIn(viewModelScope)
        }
    }
    
    /**
     * Update subscription payment method
     */
    fun updateSubscriptionPaymentMethod(subscriptionId: String, paymentMethodId: String) {
        viewModelScope.launch {
            subscriptionRepository.updateSubscriptionPaymentMethod(subscriptionId, paymentMethodId)
                .onEach { resource ->
                    // Reload subscriptions on success
                    if (resource is Resource.Success) {
                        loadUserSubscriptions()
                        loadActiveSubscription()
                    }
                }
                .catch { e ->
                    // Error handling
                }
                .launchIn(viewModelScope)
        }
    }
    
    /**
     * Check premium status
     */
    fun checkPremiumStatus() {
        _isPremiumState.value = Resource.Loading
        
        viewModelScope.launch {
            subscriptionRepository.hasActiveSubscription()
                .onEach { resource ->
                    if (resource is Resource.Success) {
                        // Only premium if there's an active non-basic subscription
                        if (resource.data) {
                            val activeSubscription = subscriptionRepository.getCurrentActiveSubscription().await()
                            if (activeSubscription is Resource.Success && 
                                activeSubscription.data?.type != SubscriptionType.BASIC) {
                                _isPremiumState.value = Resource.success(true)
                            } else {
                                _isPremiumState.value = Resource.success(false)
                            }
                        } else {
                            _isPremiumState.value = Resource.success(false)
                        }
                    } else if (resource is Resource.Error) {
                        _isPremiumState.value = resource
                    }
                }
                .catch { e ->
                    _isPremiumState.value = Resource.error("Failed to check premium status: ${e.message}")
                }
                .launchIn(viewModelScope)
        }
    }
    
    /**
     * Clear create subscription state
     */
    fun clearCreateSubscriptionState() {
        _createSubscriptionState.value = null
    }
    
    /**
     * Clear new payment method state
     */
    fun clearNewPaymentMethodState() {
        _newPaymentMethodState.value = null
    }
    
    // Extension function to get first emission from a Flow
    private suspend fun <T> kotlinx.coroutines.flow.Flow<T>.await(): T? {
        var result: T? = null
        this.collect { result = it }
        return result
    }
}