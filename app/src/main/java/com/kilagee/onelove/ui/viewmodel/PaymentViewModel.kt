package com.kilagee.onelove.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kilagee.onelove.data.model.PaymentMethod
import com.kilagee.onelove.data.model.SubscriptionPlan
import com.kilagee.onelove.data.model.SubscriptionStatus
import com.kilagee.onelove.data.model.Transaction
import com.kilagee.onelove.domain.repository.PaymentRepository
import com.kilagee.onelove.domain.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Date
import javax.inject.Inject

/**
 * Payment view model
 */
@HiltViewModel
class PaymentViewModel @Inject constructor(
    private val paymentRepository: PaymentRepository
) : ViewModel() {
    
    private val _subscriptionPlans = MutableStateFlow<Result<List<SubscriptionPlan>>>(Result.Loading)
    val subscriptionPlans: StateFlow<Result<List<SubscriptionPlan>>> = _subscriptionPlans
    
    private val _currentSubscription = MutableStateFlow<Result<SubscriptionStatus?>>(Result.Loading)
    val currentSubscription: StateFlow<Result<SubscriptionStatus?>> = _currentSubscription
    
    private val _subscriptionHistory = MutableStateFlow<Result<List<SubscriptionStatus>>>(Result.Loading)
    val subscriptionHistory: StateFlow<Result<List<SubscriptionStatus>>> = _subscriptionHistory
    
    private val _paymentMethods = MutableStateFlow<Result<List<PaymentMethod>>>(Result.Loading)
    val paymentMethods: StateFlow<Result<List<PaymentMethod>>> = _paymentMethods
    
    private val _defaultPaymentMethod = MutableStateFlow<Result<PaymentMethod?>>(Result.Loading)
    val defaultPaymentMethod: StateFlow<Result<PaymentMethod?>> = _defaultPaymentMethod
    
    private val _transactionHistory = MutableStateFlow<Result<List<Transaction>>>(Result.Loading)
    val transactionHistory: StateFlow<Result<List<Transaction>>> = _transactionHistory
    
    private val _paymentActionState = MutableStateFlow<PaymentActionState>(PaymentActionState.Idle)
    val paymentActionState: StateFlow<PaymentActionState> = _paymentActionState
    
    private val _selectedPlanId = MutableStateFlow<String?>(null)
    val selectedPlanId: StateFlow<String?> = _selectedPlanId
    
    init {
        loadSubscriptionPlans()
        loadCurrentSubscription()
        loadPaymentMethods()
        loadDefaultPaymentMethod()
        loadTransactionHistory()
    }
    
    /**
     * Load available subscription plans
     */
    fun loadSubscriptionPlans() {
        viewModelScope.launch {
            paymentRepository.getSubscriptionPlans()
                .catch { e ->
                    Timber.e(e, "Error loading subscription plans")
                    _subscriptionPlans.value = Result.Error("Failed to load subscription plans: ${e.message}")
                }
                .collect { result ->
                    _subscriptionPlans.value = result
                }
        }
    }
    
    /**
     * Load current subscription status
     */
    fun loadCurrentSubscription() {
        viewModelScope.launch {
            paymentRepository.getCurrentSubscription()
                .catch { e ->
                    Timber.e(e, "Error loading current subscription")
                    _currentSubscription.value = Result.Error("Failed to load subscription: ${e.message}")
                }
                .collect { result ->
                    _currentSubscription.value = result
                }
        }
    }
    
    /**
     * Load subscription history
     */
    fun loadSubscriptionHistory() {
        viewModelScope.launch {
            paymentRepository.getSubscriptionHistory()
                .catch { e ->
                    Timber.e(e, "Error loading subscription history")
                    _subscriptionHistory.value = Result.Error("Failed to load subscription history: ${e.message}")
                }
                .collect { result ->
                    _subscriptionHistory.value = result
                }
        }
    }
    
    /**
     * Load payment methods
     */
    fun loadPaymentMethods() {
        viewModelScope.launch {
            paymentRepository.getPaymentMethods()
                .catch { e ->
                    Timber.e(e, "Error loading payment methods")
                    _paymentMethods.value = Result.Error("Failed to load payment methods: ${e.message}")
                }
                .collect { result ->
                    _paymentMethods.value = result
                }
        }
    }
    
    /**
     * Load default payment method
     */
    fun loadDefaultPaymentMethod() {
        viewModelScope.launch {
            paymentRepository.getDefaultPaymentMethod()
                .catch { e ->
                    Timber.e(e, "Error loading default payment method")
                    _defaultPaymentMethod.value = Result.Error("Failed to load default payment method: ${e.message}")
                }
                .collect { result ->
                    _defaultPaymentMethod.value = result
                }
        }
    }
    
    /**
     * Load transaction history
     */
    fun loadTransactionHistory() {
        viewModelScope.launch {
            paymentRepository.getTransactionHistory()
                .catch { e ->
                    Timber.e(e, "Error loading transaction history")
                    _transactionHistory.value = Result.Error("Failed to load transaction history: ${e.message}")
                }
                .collect { result ->
                    _transactionHistory.value = result
                }
        }
    }
    
    /**
     * Select a subscription plan
     * 
     * @param planId ID of the plan to select
     */
    fun selectPlan(planId: String) {
        _selectedPlanId.value = planId
    }
    
    /**
     * Subscribe to a plan
     * 
     * @param paymentMethodId ID of the payment method to use
     */
    fun subscribeToPlan(paymentMethodId: String) {
        val planId = _selectedPlanId.value ?: return
        
        viewModelScope.launch {
            try {
                _paymentActionState.value = PaymentActionState.Processing
                val result = paymentRepository.subscribeToPlan(planId, paymentMethodId)
                
                when (result) {
                    is Result.Success -> {
                        _paymentActionState.value = PaymentActionState.SubscriptionCreated(result.data)
                        // Refresh subscription status
                        loadCurrentSubscription()
                    }
                    is Result.Error -> {
                        _paymentActionState.value = PaymentActionState.Error(result.message)
                    }
                    is Result.Loading -> {
                        _paymentActionState.value = PaymentActionState.Processing
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error subscribing to plan")
                _paymentActionState.value = PaymentActionState.Error(e.message ?: "Failed to subscribe to plan")
            } finally {
                // Reset state after a delay
                kotlinx.coroutines.delay(2000)
                _paymentActionState.value = PaymentActionState.Idle
            }
        }
    }
    
    /**
     * Update auto-renew setting
     * 
     * @param subscriptionId ID of the subscription
     * @param autoRenew Whether auto-renew should be enabled
     */
    fun updateAutoRenew(subscriptionId: String, autoRenew: Boolean) {
        viewModelScope.launch {
            try {
                _paymentActionState.value = PaymentActionState.Processing
                val result = paymentRepository.updateAutoRenew(subscriptionId, autoRenew)
                
                when (result) {
                    is Result.Success -> {
                        _paymentActionState.value = PaymentActionState.SubscriptionUpdated(result.data)
                        // Refresh subscription status
                        loadCurrentSubscription()
                    }
                    is Result.Error -> {
                        _paymentActionState.value = PaymentActionState.Error(result.message)
                    }
                    is Result.Loading -> {
                        _paymentActionState.value = PaymentActionState.Processing
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error updating auto-renew setting")
                _paymentActionState.value = PaymentActionState.Error(e.message ?: "Failed to update auto-renew setting")
            } finally {
                // Reset state after a delay
                kotlinx.coroutines.delay(2000)
                _paymentActionState.value = PaymentActionState.Idle
            }
        }
    }
    
    /**
     * Cancel a subscription
     * 
     * @param subscriptionId ID of the subscription
     * @param reason Optional reason for cancellation
     */
    fun cancelSubscription(subscriptionId: String, reason: String? = null) {
        viewModelScope.launch {
            try {
                _paymentActionState.value = PaymentActionState.Processing
                val result = paymentRepository.cancelSubscription(subscriptionId, reason)
                
                when (result) {
                    is Result.Success -> {
                        _paymentActionState.value = PaymentActionState.SubscriptionCancelled
                        // Refresh subscription status
                        loadCurrentSubscription()
                    }
                    is Result.Error -> {
                        _paymentActionState.value = PaymentActionState.Error(result.message)
                    }
                    is Result.Loading -> {
                        _paymentActionState.value = PaymentActionState.Processing
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error cancelling subscription")
                _paymentActionState.value = PaymentActionState.Error(e.message ?: "Failed to cancel subscription")
            } finally {
                // Reset state after a delay
                kotlinx.coroutines.delay(2000)
                _paymentActionState.value = PaymentActionState.Idle
            }
        }
    }
    
    /**
     * Add a payment method
     * 
     * @param paymentMethodId Stripe payment method ID
     * @param makeDefault Whether to make this the default payment method
     */
    fun addPaymentMethod(paymentMethodId: String, makeDefault: Boolean = false) {
        viewModelScope.launch {
            try {
                _paymentActionState.value = PaymentActionState.Processing
                val result = paymentRepository.addPaymentMethod(paymentMethodId, makeDefault)
                
                when (result) {
                    is Result.Success -> {
                        _paymentActionState.value = PaymentActionState.PaymentMethodAdded(result.data)
                        // Refresh payment methods
                        loadPaymentMethods()
                        loadDefaultPaymentMethod()
                    }
                    is Result.Error -> {
                        _paymentActionState.value = PaymentActionState.Error(result.message)
                    }
                    is Result.Loading -> {
                        _paymentActionState.value = PaymentActionState.Processing
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error adding payment method")
                _paymentActionState.value = PaymentActionState.Error(e.message ?: "Failed to add payment method")
            } finally {
                // Reset state after a delay
                kotlinx.coroutines.delay(2000)
                _paymentActionState.value = PaymentActionState.Idle
            }
        }
    }
    
    /**
     * Set a payment method as default
     * 
     * @param paymentMethodId ID of the payment method
     */
    fun setDefaultPaymentMethod(paymentMethodId: String) {
        viewModelScope.launch {
            try {
                _paymentActionState.value = PaymentActionState.Processing
                val result = paymentRepository.setDefaultPaymentMethod(paymentMethodId)
                
                when (result) {
                    is Result.Success -> {
                        _paymentActionState.value = PaymentActionState.DefaultPaymentMethodSet
                        // Refresh payment methods
                        loadPaymentMethods()
                        loadDefaultPaymentMethod()
                    }
                    is Result.Error -> {
                        _paymentActionState.value = PaymentActionState.Error(result.message)
                    }
                    is Result.Loading -> {
                        _paymentActionState.value = PaymentActionState.Processing
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error setting default payment method")
                _paymentActionState.value = PaymentActionState.Error(e.message ?: "Failed to set default payment method")
            } finally {
                // Reset state after a delay
                kotlinx.coroutines.delay(2000)
                _paymentActionState.value = PaymentActionState.Idle
            }
        }
    }
    
    /**
     * Delete a payment method
     * 
     * @param paymentMethodId ID of the payment method
     */
    fun deletePaymentMethod(paymentMethodId: String) {
        viewModelScope.launch {
            try {
                _paymentActionState.value = PaymentActionState.Processing
                val result = paymentRepository.deletePaymentMethod(paymentMethodId)
                
                when (result) {
                    is Result.Success -> {
                        _paymentActionState.value = PaymentActionState.PaymentMethodDeleted
                        // Refresh payment methods
                        loadPaymentMethods()
                        loadDefaultPaymentMethod()
                    }
                    is Result.Error -> {
                        _paymentActionState.value = PaymentActionState.Error(result.message)
                    }
                    is Result.Loading -> {
                        _paymentActionState.value = PaymentActionState.Processing
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error deleting payment method")
                _paymentActionState.value = PaymentActionState.Error(e.message ?: "Failed to delete payment method")
            } finally {
                // Reset state after a delay
                kotlinx.coroutines.delay(2000)
                _paymentActionState.value = PaymentActionState.Idle
            }
        }
    }
    
    /**
     * Get spending summary for a date range
     * 
     * @param startDate Start date for the summary
     * @param endDate End date for the summary
     */
    fun getSpendingSummary(startDate: Date, endDate: Date) {
        viewModelScope.launch {
            try {
                val result = paymentRepository.getSpendingSummary(startDate, endDate)
                // Handle result as needed
            } catch (e: Exception) {
                Timber.e(e, "Error getting spending summary")
            }
        }
    }
    
    /**
     * Purchase coins
     * 
     * @param coinPackageId ID of the coin package
     * @param paymentMethodId ID of the payment method
     */
    fun purchaseCoins(coinPackageId: String, paymentMethodId: String) {
        viewModelScope.launch {
            try {
                _paymentActionState.value = PaymentActionState.Processing
                val result = paymentRepository.purchaseCoins(coinPackageId, paymentMethodId)
                
                when (result) {
                    is Result.Success -> {
                        _paymentActionState.value = PaymentActionState.CoinsPurchased(result.data)
                        // Refresh transaction history
                        loadTransactionHistory()
                    }
                    is Result.Error -> {
                        _paymentActionState.value = PaymentActionState.Error(result.message)
                    }
                    is Result.Loading -> {
                        _paymentActionState.value = PaymentActionState.Processing
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error purchasing coins")
                _paymentActionState.value = PaymentActionState.Error(e.message ?: "Failed to purchase coins")
            } finally {
                // Reset state after a delay
                kotlinx.coroutines.delay(2000)
                _paymentActionState.value = PaymentActionState.Idle
            }
        }
    }
}

/**
 * Payment action state
 */
sealed class PaymentActionState {
    object Idle : PaymentActionState()
    object Processing : PaymentActionState()
    data class SubscriptionCreated(val subscription: SubscriptionStatus) : PaymentActionState()
    data class SubscriptionUpdated(val subscription: SubscriptionStatus) : PaymentActionState()
    object SubscriptionCancelled : PaymentActionState()
    data class PaymentMethodAdded(val paymentMethod: PaymentMethod) : PaymentActionState()
    object DefaultPaymentMethodSet : PaymentActionState()
    object PaymentMethodDeleted : PaymentActionState()
    data class CoinsPurchased(val transaction: Transaction) : PaymentActionState()
    data class Error(val message: String) : PaymentActionState()
}