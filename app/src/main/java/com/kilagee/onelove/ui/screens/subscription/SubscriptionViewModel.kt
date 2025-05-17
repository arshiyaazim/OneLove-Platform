package com.kilagee.onelove.ui.screens.subscription

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kilagee.onelove.data.model.BillingPeriod
import com.kilagee.onelove.data.model.PaymentMethod
import com.kilagee.onelove.data.model.SubscriptionPlan
import com.kilagee.onelove.data.model.SubscriptionPurchaseResult
import com.kilagee.onelove.data.model.SubscriptionTier
import com.kilagee.onelove.data.model.UserSubscription
import com.kilagee.onelove.domain.repository.AuthRepository
import com.kilagee.onelove.domain.repository.SubscriptionRepository
import com.kilagee.onelove.domain.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for the subscription screen
 */
@HiltViewModel
class SubscriptionViewModel @Inject constructor(
    private val subscriptionRepository: SubscriptionRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<SubscriptionUiState>(SubscriptionUiState.Loading)
    val uiState: StateFlow<SubscriptionUiState> = _uiState.asStateFlow()
    
    private val _currentSubscription = MutableStateFlow<UserSubscription?>(null)
    val currentSubscription: StateFlow<UserSubscription?> = _currentSubscription.asStateFlow()
    
    private val _subscriptionPlans = MutableStateFlow<List<SubscriptionPlan>>(emptyList())
    val subscriptionPlans: StateFlow<List<SubscriptionPlan>> = _subscriptionPlans.asStateFlow()
    
    private val _selectedPlan = MutableStateFlow<SubscriptionPlan?>(null)
    val selectedPlan: StateFlow<SubscriptionPlan?> = _selectedPlan.asStateFlow()
    
    private val _selectedBillingPeriod = MutableStateFlow(BillingPeriod.MONTHLY)
    val selectedBillingPeriod: StateFlow<BillingPeriod> = _selectedBillingPeriod.asStateFlow()
    
    private val _paymentMethods = MutableStateFlow<List<PaymentMethod>>(emptyList())
    val paymentMethods: StateFlow<List<PaymentMethod>> = _paymentMethods.asStateFlow()
    
    private val _selectedPaymentMethod = MutableStateFlow<PaymentMethod?>(null)
    val selectedPaymentMethod: StateFlow<PaymentMethod?> = _selectedPaymentMethod.asStateFlow()
    
    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    private val _purchaseResult = MutableStateFlow<SubscriptionPurchaseResult?>(null)
    val purchaseResult: StateFlow<SubscriptionPurchaseResult?> = _purchaseResult.asStateFlow()
    
    /**
     * Initialize the ViewModel
     */
    init {
        loadSubscriptionData()
    }
    
    /**
     * Load subscription data
     */
    fun loadSubscriptionData() {
        viewModelScope.launch {
            _uiState.value = SubscriptionUiState.Loading
            
            try {
                val userId = authRepository.getCurrentUserId()
                if (userId == null) {
                    _uiState.value = SubscriptionUiState.Error("User not authenticated")
                    return@launch
                }
                
                // Load subscription plans
                subscriptionRepository.getSubscriptionPlans().collectLatest { result ->
                    when (result) {
                        is Result.Success -> {
                            _subscriptionPlans.value = result.data.sortedBy { it.displayOrder }
                            
                            // Select the first plan if none is selected
                            if (_selectedPlan.value == null && result.data.isNotEmpty()) {
                                _selectedPlan.value = result.data.first()
                            }
                            
                            loadCurrentSubscription(userId)
                        }
                        is Result.Error -> {
                            _errorMessage.value = result.message
                        }
                        is Result.Loading -> {
                            // Already handled
                        }
                    }
                }
                
                // Load payment methods
                loadPaymentMethods(userId)
                
            } catch (e: Exception) {
                Timber.e(e, "Error loading subscription data")
                _uiState.value = SubscriptionUiState.Error("Failed to load subscription data: ${e.message}")
                _errorMessage.value = "Failed to load subscription data: ${e.message}"
            }
        }
    }
    
    /**
     * Load current subscription
     */
    private suspend fun loadCurrentSubscription(userId: String) {
        subscriptionRepository.getUserSubscription(userId).collectLatest { result ->
            when (result) {
                is Result.Success -> {
                    _currentSubscription.value = result.data
                    _uiState.value = SubscriptionUiState.Success
                }
                is Result.Error -> {
                    _errorMessage.value = result.message
                    _uiState.value = SubscriptionUiState.Success // Still show UI even if subscription fetch fails
                }
                is Result.Loading -> {
                    // Already handled
                }
            }
        }
    }
    
    /**
     * Load payment methods
     */
    private fun loadPaymentMethods(userId: String) {
        viewModelScope.launch {
            subscriptionRepository.getUserPaymentMethods(userId).collectLatest { result ->
                when (result) {
                    is Result.Success -> {
                        _paymentMethods.value = result.data
                        
                        // Select default payment method if available
                        val defaultMethod = result.data.find { it.isDefault }
                        if (defaultMethod != null) {
                            _selectedPaymentMethod.value = defaultMethod
                        } else if (result.data.isNotEmpty()) {
                            _selectedPaymentMethod.value = result.data.first()
                        }
                    }
                    is Result.Error -> {
                        _errorMessage.value = result.message
                    }
                    is Result.Loading -> {
                        // Already handled
                    }
                }
            }
        }
    }
    
    /**
     * Select a subscription plan
     */
    fun selectPlan(plan: SubscriptionPlan) {
        _selectedPlan.value = plan
    }
    
    /**
     * Switch billing period
     */
    fun switchBillingPeriod(period: BillingPeriod) {
        _selectedBillingPeriod.value = period
    }
    
    /**
     * Select payment method
     */
    fun selectPaymentMethod(method: PaymentMethod) {
        _selectedPaymentMethod.value = method
    }
    
    /**
     * Subscribe to the selected plan
     */
    fun subscribe() {
        viewModelScope.launch {
            _isProcessing.value = true
            _purchaseResult.value = null
            
            try {
                val userId = authRepository.getCurrentUserId()
                if (userId == null) {
                    _errorMessage.value = "User not authenticated"
                    _isProcessing.value = false
                    return@launch
                }
                
                val plan = _selectedPlan.value
                if (plan == null) {
                    _errorMessage.value = "No plan selected"
                    _isProcessing.value = false
                    return@launch
                }
                
                val billingPeriod = _selectedBillingPeriod.value
                val paymentMethodId = _selectedPaymentMethod.value?.id
                
                val result = subscriptionRepository.subscribeToPlan(
                    userId = userId,
                    planId = plan.id,
                    paymentMethodId = paymentMethodId,
                    billingPeriod = billingPeriod
                )
                
                when (result) {
                    is Result.Success -> {
                        _purchaseResult.value = result.data
                        
                        if (result.data.requiresAction) {
                            _uiState.value = SubscriptionUiState.ActionRequired(result.data.actionUrl ?: "")
                        } else if (result.data.success) {
                            _uiState.value = SubscriptionUiState.Success
                            loadSubscriptionData() // Refresh data
                        } else {
                            _errorMessage.value = result.data.error ?: "Unknown error"
                        }
                    }
                    is Result.Error -> {
                        _errorMessage.value = result.message
                    }
                    is Result.Loading -> {
                        // Already handled
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error subscribing to plan")
                _errorMessage.value = "Failed to subscribe: ${e.message}"
            } finally {
                _isProcessing.value = false
            }
        }
    }
    
    /**
     * Cancel subscription
     */
    fun cancelSubscription() {
        viewModelScope.launch {
            _isProcessing.value = true
            
            try {
                val userId = authRepository.getCurrentUserId()
                if (userId == null) {
                    _errorMessage.value = "User not authenticated"
                    _isProcessing.value = false
                    return@launch
                }
                
                val result = subscriptionRepository.cancelSubscription(userId)
                
                when (result) {
                    is Result.Success -> {
                        loadSubscriptionData() // Refresh data
                    }
                    is Result.Error -> {
                        _errorMessage.value = result.message
                    }
                    is Result.Loading -> {
                        // Already handled
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error canceling subscription")
                _errorMessage.value = "Failed to cancel subscription: ${e.message}"
            } finally {
                _isProcessing.value = false
            }
        }
    }
    
    /**
     * Update auto-renew setting
     */
    fun updateAutoRenew(autoRenew: Boolean) {
        viewModelScope.launch {
            _isProcessing.value = true
            
            try {
                val userId = authRepository.getCurrentUserId()
                if (userId == null) {
                    _errorMessage.value = "User not authenticated"
                    _isProcessing.value = false
                    return@launch
                }
                
                val result = subscriptionRepository.updateAutoRenew(userId, autoRenew)
                
                when (result) {
                    is Result.Success -> {
                        loadSubscriptionData() // Refresh data
                    }
                    is Result.Error -> {
                        _errorMessage.value = result.message
                    }
                    is Result.Loading -> {
                        // Already handled
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error updating auto-renew")
                _errorMessage.value = "Failed to update auto-renew: ${e.message}"
            } finally {
                _isProcessing.value = false
            }
        }
    }
    
    /**
     * Add payment method
     */
    fun addPaymentMethod(
        cardNumber: String,
        expiryMonth: Int,
        expiryYear: Int,
        cvc: String,
        isDefault: Boolean
    ) {
        viewModelScope.launch {
            _isProcessing.value = true
            
            try {
                val userId = authRepository.getCurrentUserId()
                if (userId == null) {
                    _errorMessage.value = "User not authenticated"
                    _isProcessing.value = false
                    return@launch
                }
                
                val result = subscriptionRepository.addPaymentMethod(
                    userId = userId,
                    cardNumber = cardNumber,
                    expiryMonth = expiryMonth,
                    expiryYear = expiryYear,
                    cvc = cvc,
                    isDefault = isDefault
                )
                
                when (result) {
                    is Result.Success -> {
                        loadPaymentMethods(userId)
                        _selectedPaymentMethod.value = result.data
                    }
                    is Result.Error -> {
                        _errorMessage.value = result.message
                    }
                    is Result.Loading -> {
                        // Already handled
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error adding payment method")
                _errorMessage.value = "Failed to add payment method: ${e.message}"
            } finally {
                _isProcessing.value = false
            }
        }
    }
    
    /**
     * Remove payment method
     */
    fun removePaymentMethod(paymentMethodId: String) {
        viewModelScope.launch {
            _isProcessing.value = true
            
            try {
                val userId = authRepository.getCurrentUserId()
                if (userId == null) {
                    _errorMessage.value = "User not authenticated"
                    _isProcessing.value = false
                    return@launch
                }
                
                val result = subscriptionRepository.removePaymentMethod(userId, paymentMethodId)
                
                when (result) {
                    is Result.Success -> {
                        // Remove from selected
                        if (_selectedPaymentMethod.value?.id == paymentMethodId) {
                            _selectedPaymentMethod.value = null
                        }
                        loadPaymentMethods(userId)
                    }
                    is Result.Error -> {
                        _errorMessage.value = result.message
                    }
                    is Result.Loading -> {
                        // Already handled
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error removing payment method")
                _errorMessage.value = "Failed to remove payment method: ${e.message}"
            } finally {
                _isProcessing.value = false
            }
        }
    }
    
    /**
     * Set default payment method
     */
    fun setDefaultPaymentMethod(paymentMethodId: String) {
        viewModelScope.launch {
            _isProcessing.value = true
            
            try {
                val userId = authRepository.getCurrentUserId()
                if (userId == null) {
                    _errorMessage.value = "User not authenticated"
                    _isProcessing.value = false
                    return@launch
                }
                
                val result = subscriptionRepository.setDefaultPaymentMethod(userId, paymentMethodId)
                
                when (result) {
                    is Result.Success -> {
                        loadPaymentMethods(userId)
                    }
                    is Result.Error -> {
                        _errorMessage.value = result.message
                    }
                    is Result.Loading -> {
                        // Already handled
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error setting default payment method")
                _errorMessage.value = "Failed to set default payment method: ${e.message}"
            } finally {
                _isProcessing.value = false
            }
        }
    }
    
    /**
     * Complete Stripe payment action
     */
    fun completePaymentAction(actionUrl: String, subscriptionId: String) {
        viewModelScope.launch {
            _isProcessing.value = true
            
            try {
                val userId = authRepository.getCurrentUserId()
                if (userId == null) {
                    _errorMessage.value = "User not authenticated"
                    _isProcessing.value = false
                    return@launch
                }
                
                val result = subscriptionRepository.completeSubscription(
                    userId = userId,
                    subscriptionId = subscriptionId,
                    actionSecret = actionUrl
                )
                
                when (result) {
                    is Result.Success -> {
                        _purchaseResult.value = result.data
                        
                        if (result.data.success) {
                            _uiState.value = SubscriptionUiState.Success
                            loadSubscriptionData() // Refresh data
                        } else if (result.data.requiresAction) {
                            _uiState.value = SubscriptionUiState.ActionRequired(result.data.actionUrl ?: "")
                        } else {
                            _errorMessage.value = result.data.error ?: "Unknown error"
                        }
                    }
                    is Result.Error -> {
                        _errorMessage.value = result.message
                    }
                    is Result.Loading -> {
                        // Already handled
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error completing payment action")
                _errorMessage.value = "Failed to complete payment: ${e.message}"
            } finally {
                _isProcessing.value = false
            }
        }
    }
    
    /**
     * Check if user is subscribed to a specific tier
     */
    fun isSubscribedToTier(tier: SubscriptionTier, callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val userId = authRepository.getCurrentUserId()
                if (userId == null) {
                    callback(false)
                    return@launch
                }
                
                val result = subscriptionRepository.isSubscribedToTier(userId, tier)
                
                when (result) {
                    is Result.Success -> {
                        callback(result.data)
                    }
                    else -> {
                        callback(false)
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error checking subscription tier")
                callback(false)
            }
        }
    }
    
    /**
     * Clear error message
     */
    fun clearErrorMessage() {
        _errorMessage.value = null
    }
}

/**
 * UI state for the subscription screen
 */
sealed class SubscriptionUiState {
    object Loading : SubscriptionUiState()
    object Success : SubscriptionUiState()
    data class Error(val message: String) : SubscriptionUiState()
    data class ActionRequired(val actionUrl: String) : SubscriptionUiState()
}