package com.kilagee.onelove.ui.screens.payment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kilagee.onelove.data.model.PaymentMethod
import com.kilagee.onelove.data.model.SubscriptionPlan
import com.kilagee.onelove.domain.repository.SubscriptionRepository
import com.stripe.android.model.CardParams
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for the payment screen
 */
@HiltViewModel
class PaymentViewModel @Inject constructor(
    private val subscriptionRepository: SubscriptionRepository
) : ViewModel() {
    
    // UI state
    private val _uiState = MutableStateFlow<PaymentUIState>(PaymentUIState.Loading)
    val uiState: StateFlow<PaymentUIState> = _uiState.asStateFlow()
    
    // Subscription plans
    private val _plans = MutableStateFlow<List<SubscriptionPlan>>(emptyList())
    val plans: StateFlow<List<SubscriptionPlan>> = _plans.asStateFlow()
    
    // Selected plan
    private val _selectedPlan = MutableStateFlow<SubscriptionPlan?>(null)
    val selectedPlan: StateFlow<SubscriptionPlan?> = _selectedPlan.asStateFlow()
    
    // Payment methods
    private val _paymentMethods = MutableStateFlow<List<PaymentMethod>>(emptyList())
    val paymentMethods: StateFlow<List<PaymentMethod>> = _paymentMethods.asStateFlow()
    
    // Selected payment method
    private val _selectedPaymentMethod = MutableStateFlow<PaymentMethod?>(null)
    val selectedPaymentMethod: StateFlow<PaymentMethod?> = _selectedPaymentMethod.asStateFlow()
    
    // Loading state
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    // Events
    private val _events = MutableSharedFlow<PaymentEvent>()
    val events: SharedFlow<PaymentEvent> = _events.asSharedFlow()
    
    // Client secret for payment
    private var paymentClientSecret: String? = null
    
    init {
        loadSubscriptionPlans()
        loadPaymentMethods()
    }
    
    /**
     * Load subscription plans
     */
    private fun loadSubscriptionPlans() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                
                val result = subscriptionRepository.getSubscriptionPlans()
                
                if (result is com.kilagee.onelove.domain.util.Result.Success) {
                    val planList = result.data
                    _plans.value = planList
                    
                    // Select popular plan by default, or first plan if none is marked as popular
                    _selectedPlan.value = planList.find { it.isPopular } ?: planList.firstOrNull()
                    
                    _uiState.value = PaymentUIState.PlansLoaded
                } else if (result is com.kilagee.onelove.domain.util.Result.Error) {
                    _uiState.value = PaymentUIState.Error(result.message ?: "Failed to load subscription plans")
                    _events.emit(PaymentEvent.Error(result.message ?: "Failed to load subscription plans"))
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading subscription plans")
                _uiState.value = PaymentUIState.Error("An unexpected error occurred")
                _events.emit(PaymentEvent.Error("An unexpected error occurred: ${e.localizedMessage}"))
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Load payment methods
     */
    private fun loadPaymentMethods() {
        viewModelScope.launch {
            try {
                val result = subscriptionRepository.getPaymentMethods()
                
                if (result is com.kilagee.onelove.domain.util.Result.Success) {
                    val methodList = result.data
                    _paymentMethods.value = methodList
                    
                    // Select default payment method if available
                    _selectedPaymentMethod.value = methodList.find { it.isDefault } ?: methodList.firstOrNull()
                } else if (result is com.kilagee.onelove.domain.util.Result.Error) {
                    _events.emit(PaymentEvent.Error(result.message ?: "Failed to load payment methods"))
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading payment methods")
                _events.emit(PaymentEvent.Error("Failed to load payment methods: ${e.localizedMessage}"))
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
     * Select a payment method
     */
    fun selectPaymentMethod(paymentMethod: PaymentMethod) {
        _selectedPaymentMethod.value = paymentMethod
    }
    
    /**
     * Add a new payment method
     */
    fun addPaymentMethod(cardParams: CardParams) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                
                // In a real implementation, we would send this card to Stripe first
                // to get a payment method ID, then save it to our backend
                
                // For this example, we'll simulate getting a payment method ID
                val setupIntentResult = subscriptionRepository.getSetupIntent()
                
                if (setupIntentResult is com.kilagee.onelove.domain.util.Result.Success) {
                    // After confirming setup intent with Stripe SDK (not shown here)
                    // The result would be a payment method ID that we use to create a payment method
                    
                    // Simulating a payment method ID for this example
                    val paymentMethodId = "pm_" + System.currentTimeMillis()
                    
                    val result = subscriptionRepository.addPaymentMethod(
                        paymentMethodId = paymentMethodId,
                        isDefault = _paymentMethods.value.isEmpty() // Make default if this is the first one
                    )
                    
                    if (result is com.kilagee.onelove.domain.util.Result.Success) {
                        // Reload payment methods to include the new one
                        loadPaymentMethods()
                        _events.emit(PaymentEvent.Success("Payment method added successfully"))
                    } else if (result is com.kilagee.onelove.domain.util.Result.Error) {
                        _events.emit(PaymentEvent.Error(result.message ?: "Failed to add payment method"))
                    }
                } else if (setupIntentResult is com.kilagee.onelove.domain.util.Result.Error) {
                    _events.emit(PaymentEvent.Error(setupIntentResult.message ?: "Failed to set up payment"))
                }
            } catch (e: Exception) {
                Timber.e(e, "Error adding payment method")
                _events.emit(PaymentEvent.Error("Failed to add payment method: ${e.localizedMessage}"))
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Validate coupon code
     */
    fun validateCoupon(code: String) {
        if (code.isBlank()) {
            return
        }
        
        viewModelScope.launch {
            try {
                val result = subscriptionRepository.validateCoupon(code)
                
                if (result is com.kilagee.onelove.domain.util.Result.Success) {
                    _events.emit(PaymentEvent.CouponValidated(result.data))
                } else if (result is com.kilagee.onelove.domain.util.Result.Error) {
                    _events.emit(PaymentEvent.Error(result.message ?: "Invalid coupon code"))
                    _events.emit(PaymentEvent.CouponValidated(null))
                }
            } catch (e: Exception) {
                Timber.e(e, "Error validating coupon")
                _events.emit(PaymentEvent.Error("Failed to validate coupon: ${e.localizedMessage}"))
                _events.emit(PaymentEvent.CouponValidated(null))
            }
        }
    }
    
    /**
     * Process payment
     */
    fun processPayment(couponCode: String? = null) {
        val selectedPlan = _selectedPlan.value ?: return
        val selectedPaymentMethod = _selectedPaymentMethod.value ?: return
        
        viewModelScope.launch {
            try {
                _isLoading.value = true
                
                // First, create a payment intent
                val intentResult = subscriptionRepository.initiatePurchase(selectedPlan.id)
                
                if (intentResult is com.kilagee.onelove.domain.util.Result.Success) {
                    paymentClientSecret = intentResult.data
                    
                    // In a real app, we would confirm the payment with the Stripe SDK here
                    // Then proceed with subscription creation after successful payment
                    
                    // For this example, we'll simulate a successful payment
                    val subscriptionResult = subscriptionRepository.subscribe(
                        planId = selectedPlan.id,
                        paymentMethodId = selectedPaymentMethod.id
                    )
                    
                    if (subscriptionResult is com.kilagee.onelove.domain.util.Result.Success) {
                        // If coupon was provided, apply it to the subscription
                        if (!couponCode.isNullOrBlank()) {
                            subscriptionRepository.applyCoupon(
                                subscriptionId = subscriptionResult.data.id,
                                couponCode = couponCode
                            )
                        }
                        
                        _events.emit(PaymentEvent.PaymentSuccess)
                    } else if (subscriptionResult is com.kilagee.onelove.domain.util.Result.Error) {
                        _events.emit(
                            PaymentEvent.Error(
                                subscriptionResult.message ?: "Failed to create subscription"
                            )
                        )
                    }
                } else if (intentResult is com.kilagee.onelove.domain.util.Result.Error) {
                    _events.emit(
                        PaymentEvent.Error(
                            intentResult.message ?: "Failed to initiate payment"
                        )
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Error processing payment")
                _events.emit(PaymentEvent.Error("Payment failed: ${e.localizedMessage}"))
            } finally {
                _isLoading.value = false
            }
        }
    }
}

/**
 * UI state for the payment screen
 */
sealed class PaymentUIState {
    object Loading : PaymentUIState()
    object PlansLoaded : PaymentUIState()
    data class Error(val message: String) : PaymentUIState()
}

/**
 * Events emitted by the payment screen
 */
sealed class PaymentEvent {
    data class Error(val message: String) : PaymentEvent()
    data class CouponValidated(val discountAmount: Double?) : PaymentEvent()
    object PaymentSuccess : PaymentEvent()
    data class Success(val message: String) : PaymentEvent()
}