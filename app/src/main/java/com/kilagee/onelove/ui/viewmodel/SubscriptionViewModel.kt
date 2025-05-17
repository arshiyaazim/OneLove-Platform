package com.kilagee.onelove.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kilagee.onelove.data.model.PaymentMethod
import com.kilagee.onelove.data.model.PremiumFeature
import com.kilagee.onelove.data.model.Result
import com.kilagee.onelove.data.model.Subscription
import com.kilagee.onelove.data.model.SubscriptionPlan
import com.kilagee.onelove.data.model.SubscriptionStatus
import com.kilagee.onelove.data.model.SubscriptionTier
import com.kilagee.onelove.data.repository.AuthRepository
import com.kilagee.onelove.data.repository.SubscriptionRepository
import com.kilagee.onelove.data.repository.UserRepository
import com.kilagee.onelove.util.AppError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Date
import javax.inject.Inject

/**
 * Sealed class representing subscription UI state
 */
sealed class SubscriptionUiState {
    object Initial : SubscriptionUiState()
    object Loading : SubscriptionUiState()
    data class Error(val error: AppError, val message: String) : SubscriptionUiState()
    data class Success<T>(val data: T) : SubscriptionUiState()
}

/**
 * ViewModel for subscription and payment operations
 */
@HiltViewModel
class SubscriptionViewModel @Inject constructor(
    private val subscriptionRepository: SubscriptionRepository,
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) : ViewModel() {
    
    // Current subscription UI state
    private val _subscriptionUiState = MutableStateFlow<SubscriptionUiState>(SubscriptionUiState.Initial)
    val subscriptionUiState: StateFlow<SubscriptionUiState> = _subscriptionUiState.asStateFlow()
    
    // Available subscription plans
    private val _subscriptionPlans = MutableStateFlow<List<SubscriptionPlan>>(emptyList())
    val subscriptionPlans: StateFlow<List<SubscriptionPlan>> = _subscriptionPlans.asStateFlow()
    
    // Current subscription for the user
    private val _currentSubscription = MutableStateFlow<Subscription?>(null)
    val currentSubscription: StateFlow<Subscription?> = _currentSubscription.asStateFlow()
    
    // User's subscription tier
    private val _subscriptionTier = MutableStateFlow<SubscriptionTier>(SubscriptionTier.FREE)
    val subscriptionTier: StateFlow<SubscriptionTier> = _subscriptionTier.asStateFlow()
    
    // Currently selected plan
    private val _selectedPlan = MutableStateFlow<SubscriptionPlan?>(null)
    val selectedPlan: StateFlow<SubscriptionPlan?> = _selectedPlan.asStateFlow()
    
    // Subscription history
    private val _subscriptionHistory = MutableStateFlow<List<Subscription>>(emptyList())
    val subscriptionHistory: StateFlow<List<Subscription>> = _subscriptionHistory.asStateFlow()
    
    // User payment methods
    private val _paymentMethods = MutableStateFlow<List<Map<String, Any>>>(emptyList())
    val paymentMethods: StateFlow<List<Map<String, Any>>> = _paymentMethods.asStateFlow()
    
    // Promo code details
    private val _promoApplied = MutableStateFlow<Map<String, Any>?>(null)
    val promoApplied: StateFlow<Map<String, Any>?> = _promoApplied.asStateFlow()
    
    // Payment intent
    private val _paymentIntent = MutableStateFlow<Map<String, Any>?>(null)
    val paymentIntent: StateFlow<Map<String, Any>?> = _paymentIntent.asStateFlow()
    
    // Has active subscription
    private val _hasActiveSubscription = MutableStateFlow(false)
    val hasActiveSubscription: StateFlow<Boolean> = _hasActiveSubscription.asStateFlow()
    
    // Subscription expiry date
    private val _subscriptionExpiryDate = MutableStateFlow<Date?>(null)
    val subscriptionExpiryDate: StateFlow<Date?> = _subscriptionExpiryDate.asStateFlow()
    
    init {
        // Load initial subscription data
        loadSubscriptionPlans()
        
        val userId = authRepository.getCurrentUserId()
        if (userId != null) {
            loadCurrentSubscription(userId)
            loadSubscriptionTier(userId)
            checkSubscriptionStatus(userId)
        }
    }
    
    /**
     * Load all available subscription plans
     */
    fun loadSubscriptionPlans() {
        viewModelScope.launch {
            _subscriptionUiState.value = SubscriptionUiState.Loading
            
            when (val result = subscriptionRepository.getSubscriptionPlans()) {
                is Result.Success -> {
                    _subscriptionPlans.value = result.data
                    _subscriptionUiState.value = SubscriptionUiState.Success(result.data)
                }
                is Result.Error -> {
                    _subscriptionUiState.value = SubscriptionUiState.Error(result.error, "Failed to load subscription plans")
                }
            }
        }
    }
    
    /**
     * Load current user subscription
     */
    fun loadCurrentSubscription(userId: String = authRepository.getCurrentUserId() ?: "") {
        if (userId.isEmpty()) return
        
        viewModelScope.launch {
            _subscriptionUiState.value = SubscriptionUiState.Loading
            
            when (val result = subscriptionRepository.getCurrentSubscription(userId)) {
                is Result.Success -> {
                    _currentSubscription.value = result.data
                    _subscriptionUiState.value = SubscriptionUiState.Success(result.data)
                }
                is Result.Error -> {
                    _subscriptionUiState.value = SubscriptionUiState.Error(result.error, "Failed to load current subscription")
                }
            }
        }
    }
    
    /**
     * Load subscription plan by ID
     */
    fun loadSubscriptionPlanById(planId: String) {
        viewModelScope.launch {
            _subscriptionUiState.value = SubscriptionUiState.Loading
            
            when (val result = subscriptionRepository.getSubscriptionPlanById(planId)) {
                is Result.Success -> {
                    _selectedPlan.value = result.data
                    _subscriptionUiState.value = SubscriptionUiState.Success(result.data)
                }
                is Result.Error -> {
                    _subscriptionUiState.value = SubscriptionUiState.Error(result.error, "Failed to load subscription plan")
                }
            }
        }
    }
    
    /**
     * Set selected plan
     */
    fun selectPlan(plan: SubscriptionPlan) {
        _selectedPlan.value = plan
        
        // Clear any previously applied promo
        _promoApplied.value = null
    }
    
    /**
     * Create a payment intent for Stripe
     */
    fun createPaymentIntent(
        planId: String,
        amount: Double,
        currency: String = "USD",
        metadata: Map<String, String> = emptyMap()
    ) {
        viewModelScope.launch {
            _subscriptionUiState.value = SubscriptionUiState.Loading
            
            val userId = authRepository.getCurrentUserId() ?: return@launch
            
            when (val result = subscriptionRepository.createPaymentIntent(userId, planId, amount, currency, metadata)) {
                is Result.Success -> {
                    _paymentIntent.value = result.data
                    _subscriptionUiState.value = SubscriptionUiState.Success(result.data)
                }
                is Result.Error -> {
                    _subscriptionUiState.value = SubscriptionUiState.Error(result.error, "Failed to create payment intent")
                }
            }
        }
    }
    
    /**
     * Confirm a payment intent
     */
    fun confirmPaymentIntent(paymentIntentId: String, paymentMethodId: String) {
        viewModelScope.launch {
            _subscriptionUiState.value = SubscriptionUiState.Loading
            
            when (val result = subscriptionRepository.confirmPaymentIntent(paymentIntentId, paymentMethodId)) {
                is Result.Success -> {
                    _subscriptionUiState.value = SubscriptionUiState.Success(result.data)
                }
                is Result.Error -> {
                    _subscriptionUiState.value = SubscriptionUiState.Error(result.error, "Failed to confirm payment")
                }
            }
        }
    }
    
    /**
     * Subscribe with Stripe
     */
    fun subscribeWithStripe(planId: String, paymentMethodId: String, promoCode: String? = null) {
        viewModelScope.launch {
            _subscriptionUiState.value = SubscriptionUiState.Loading
            
            val userId = authRepository.getCurrentUserId() ?: return@launch
            
            when (val result = subscriptionRepository.subscribeWithStripe(userId, planId, paymentMethodId, promoCode)) {
                is Result.Success -> {
                    _currentSubscription.value = result.data
                    _subscriptionUiState.value = SubscriptionUiState.Success(result.data)
                    
                    // Update subscription status
                    checkSubscriptionStatus(userId)
                    loadSubscriptionTier(userId)
                }
                is Result.Error -> {
                    _subscriptionUiState.value = SubscriptionUiState.Error(result.error, "Failed to subscribe")
                }
            }
        }
    }
    
    /**
     * Subscribe with local payment method (bKash/Nagad)
     */
    fun subscribeWithLocalPayment(
        planId: String,
        paymentMethod: PaymentMethod,
        transactionId: String,
        amount: Double,
        currency: String = "BDT",
        promoCode: String? = null
    ) {
        viewModelScope.launch {
            _subscriptionUiState.value = SubscriptionUiState.Loading
            
            val userId = authRepository.getCurrentUserId() ?: return@launch
            
            when (val result = subscriptionRepository.subscribeWithLocalPayment(
                userId, planId, paymentMethod, transactionId, amount, currency, promoCode
            )) {
                is Result.Success -> {
                    _currentSubscription.value = result.data
                    _subscriptionUiState.value = SubscriptionUiState.Success(result.data)
                    
                    // Update subscription status
                    checkSubscriptionStatus(userId)
                    loadSubscriptionTier(userId)
                }
                is Result.Error -> {
                    _subscriptionUiState.value = SubscriptionUiState.Error(result.error, "Failed to subscribe")
                }
            }
        }
    }
    
    /**
     * Cancel subscription
     */
    fun cancelSubscription(subscriptionId: String, cancelReason: String? = null) {
        viewModelScope.launch {
            _subscriptionUiState.value = SubscriptionUiState.Loading
            
            val userId = authRepository.getCurrentUserId() ?: return@launch
            
            when (val result = subscriptionRepository.cancelSubscription(userId, subscriptionId, cancelReason)) {
                is Result.Success -> {
                    _currentSubscription.value = result.data
                    _subscriptionUiState.value = SubscriptionUiState.Success(result.data)
                    
                    // Update subscription status
                    checkSubscriptionStatus(userId)
                }
                is Result.Error -> {
                    _subscriptionUiState.value = SubscriptionUiState.Error(result.error, "Failed to cancel subscription")
                }
            }
        }
    }
    
    /**
     * Update auto-renew settings
     */
    fun updateAutoRenew(subscriptionId: String, autoRenew: Boolean) {
        viewModelScope.launch {
            _subscriptionUiState.value = SubscriptionUiState.Loading
            
            val userId = authRepository.getCurrentUserId() ?: return@launch
            
            when (val result = subscriptionRepository.updateAutoRenew(userId, subscriptionId, autoRenew)) {
                is Result.Success -> {
                    _currentSubscription.value = result.data
                    _subscriptionUiState.value = SubscriptionUiState.Success(result.data)
                }
                is Result.Error -> {
                    _subscriptionUiState.value = SubscriptionUiState.Error(result.error, "Failed to update auto-renew settings")
                }
            }
        }
    }
    
    /**
     * Load subscription history
     */
    fun loadSubscriptionHistory() {
        viewModelScope.launch {
            _subscriptionUiState.value = SubscriptionUiState.Loading
            
            val userId = authRepository.getCurrentUserId() ?: return@launch
            
            when (val result = subscriptionRepository.getSubscriptionHistory(userId)) {
                is Result.Success -> {
                    _subscriptionHistory.value = result.data
                    _subscriptionUiState.value = SubscriptionUiState.Success(result.data)
                }
                is Result.Error -> {
                    _subscriptionUiState.value = SubscriptionUiState.Error(result.error, "Failed to load subscription history")
                }
            }
        }
    }
    
    /**
     * Apply promotional code
     */
    fun applyPromoCode(promoCode: String) {
        viewModelScope.launch {
            _subscriptionUiState.value = SubscriptionUiState.Loading
            
            val planId = _selectedPlan.value?.id ?: return@launch
            
            when (val result = subscriptionRepository.applyPromoCode(promoCode, planId)) {
                is Result.Success -> {
                    _promoApplied.value = result.data
                    _subscriptionUiState.value = SubscriptionUiState.Success(result.data)
                }
                is Result.Error -> {
                    _subscriptionUiState.value = SubscriptionUiState.Error(result.error, "Failed to apply promo code")
                }
            }
        }
    }
    
    /**
     * Verify subscription receipt (for App Store purchases)
     */
    fun verifySubscriptionReceipt(receipt: String, planId: String) {
        viewModelScope.launch {
            _subscriptionUiState.value = SubscriptionUiState.Loading
            
            val userId = authRepository.getCurrentUserId() ?: return@launch
            
            when (val result = subscriptionRepository.verifySubscriptionReceipt(userId, receipt, planId)) {
                is Result.Success -> {
                    _currentSubscription.value = result.data
                    _subscriptionUiState.value = SubscriptionUiState.Success(result.data)
                    
                    // Update subscription status
                    checkSubscriptionStatus(userId)
                    loadSubscriptionTier(userId)
                }
                is Result.Error -> {
                    _subscriptionUiState.value = SubscriptionUiState.Error(result.error, "Failed to verify receipt")
                }
            }
        }
    }
    
    /**
     * Verify Google Play purchase
     */
    fun verifyGooglePlayPurchase(purchaseToken: String, productId: String, planId: String) {
        viewModelScope.launch {
            _subscriptionUiState.value = SubscriptionUiState.Loading
            
            val userId = authRepository.getCurrentUserId() ?: return@launch
            
            when (val result = subscriptionRepository.verifyGooglePlayPurchase(userId, purchaseToken, productId, planId)) {
                is Result.Success -> {
                    _currentSubscription.value = result.data
                    _subscriptionUiState.value = SubscriptionUiState.Success(result.data)
                    
                    // Update subscription status
                    checkSubscriptionStatus(userId)
                    loadSubscriptionTier(userId)
                }
                is Result.Error -> {
                    _subscriptionUiState.value = SubscriptionUiState.Error(result.error, "Failed to verify purchase")
                }
            }
        }
    }
    
    /**
     * Validate premium features access
     */
    fun validatePremiumAccess(feature: PremiumFeature): Boolean {
        if (feature == PremiumFeature.BASIC_MATCHING || feature == PremiumFeature.LIMITED_MESSAGES) {
            return true // These features are available to all users
        }
        
        // Check local cache first
        if (_hasActiveSubscription.value) {
            val tier = _subscriptionTier.value
            return tier.getFeatures().contains(feature)
        }
        
        return false
    }
    
    /**
     * Load subscription tier
     */
    private fun loadSubscriptionTier(userId: String) {
        viewModelScope.launch {
            when (val result = subscriptionRepository.getUserSubscriptionTier(userId)) {
                is Result.Success -> {
                    _subscriptionTier.value = result.data
                }
                is Result.Error -> {
                    Timber.e(result.error.throwable, "Error loading subscription tier")
                }
            }
        }
    }
    
    /**
     * Check subscription status
     */
    private fun checkSubscriptionStatus(userId: String) {
        viewModelScope.launch {
            when (val result = subscriptionRepository.hasActiveSubscription(userId)) {
                is Result.Success -> {
                    _hasActiveSubscription.value = result.data
                    
                    // Load expiry date if subscription is active
                    if (result.data) {
                        loadSubscriptionExpiryDate(userId)
                    }
                }
                is Result.Error -> {
                    Timber.e(result.error.throwable, "Error checking subscription status")
                }
            }
        }
    }
    
    /**
     * Load subscription expiry date
     */
    private fun loadSubscriptionExpiryDate(userId: String) {
        viewModelScope.launch {
            when (val result = subscriptionRepository.getSubscriptionExpiryDate(userId)) {
                is Result.Success -> {
                    _subscriptionExpiryDate.value = result.data
                }
                is Result.Error -> {
                    Timber.e(result.error.throwable, "Error loading subscription expiry date")
                }
            }
        }
    }
    
    /**
     * Load user payment methods
     */
    fun loadUserPaymentMethods() {
        viewModelScope.launch {
            _subscriptionUiState.value = SubscriptionUiState.Loading
            
            val userId = authRepository.getCurrentUserId() ?: return@launch
            
            when (val result = subscriptionRepository.getUserPaymentMethods(userId)) {
                is Result.Success -> {
                    _paymentMethods.value = result.data
                    _subscriptionUiState.value = SubscriptionUiState.Success(result.data)
                }
                is Result.Error -> {
                    _subscriptionUiState.value = SubscriptionUiState.Error(result.error, "Failed to load payment methods")
                }
            }
        }
    }
    
    /**
     * Add payment method
     */
    fun addPaymentMethod(paymentMethodId: String) {
        viewModelScope.launch {
            _subscriptionUiState.value = SubscriptionUiState.Loading
            
            val userId = authRepository.getCurrentUserId() ?: return@launch
            
            when (val result = subscriptionRepository.addPaymentMethod(userId, paymentMethodId)) {
                is Result.Success -> {
                    _subscriptionUiState.value = SubscriptionUiState.Success(result.data)
                    loadUserPaymentMethods()
                }
                is Result.Error -> {
                    _subscriptionUiState.value = SubscriptionUiState.Error(result.error, "Failed to add payment method")
                }
            }
        }
    }
    
    /**
     * Remove payment method
     */
    fun removePaymentMethod(paymentMethodId: String) {
        viewModelScope.launch {
            _subscriptionUiState.value = SubscriptionUiState.Loading
            
            val userId = authRepository.getCurrentUserId() ?: return@launch
            
            when (val result = subscriptionRepository.removePaymentMethod(userId, paymentMethodId)) {
                is Result.Success -> {
                    _subscriptionUiState.value = SubscriptionUiState.Success(Unit)
                    loadUserPaymentMethods()
                }
                is Result.Error -> {
                    _subscriptionUiState.value = SubscriptionUiState.Error(result.error, "Failed to remove payment method")
                }
            }
        }
    }
    
    /**
     * Set default payment method
     */
    fun setDefaultPaymentMethod(paymentMethodId: String) {
        viewModelScope.launch {
            _subscriptionUiState.value = SubscriptionUiState.Loading
            
            val userId = authRepository.getCurrentUserId() ?: return@launch
            
            when (val result = subscriptionRepository.setDefaultPaymentMethod(userId, paymentMethodId)) {
                is Result.Success -> {
                    _subscriptionUiState.value = SubscriptionUiState.Success(Unit)
                    loadUserPaymentMethods()
                }
                is Result.Error -> {
                    _subscriptionUiState.value = SubscriptionUiState.Error(result.error, "Failed to set default payment method")
                }
            }
        }
    }
    
    /**
     * Sync subscriptions with Stripe
     */
    fun syncSubscriptions() {
        viewModelScope.launch {
            val userId = authRepository.getCurrentUserId() ?: return@launch
            
            when (val result = subscriptionRepository.syncSubscriptions(userId)) {
                is Result.Success -> {
                    // Refresh current subscription
                    loadCurrentSubscription(userId)
                }
                is Result.Error -> {
                    Timber.e(result.error.throwable, "Error syncing subscriptions")
                }
            }
        }
    }
    
    /**
     * Reset subscription UI state
     */
    fun resetSubscriptionState() {
        _subscriptionUiState.value = SubscriptionUiState.Initial
    }
}