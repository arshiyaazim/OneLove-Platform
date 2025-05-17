package com.kilagee.onelove.ui.screens.payment

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.kilagee.onelove.data.model.BillingInterval
import com.kilagee.onelove.data.model.PaymentMethod
import com.kilagee.onelove.data.model.SubscriptionPlan
import com.kilagee.onelove.ui.components.SnackbarHostState
import com.stripe.android.model.CardParams
import kotlinx.coroutines.delay
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

/**
 * Payment screen for subscription management
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentScreen(
    onNavigateBack: () -> Unit,
    onPaymentSuccess: () -> Unit,
    snackbarHostState: SnackbarHostState,
    viewModel: PaymentViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val plans by viewModel.plans.collectAsState()
    val selectedPlan by viewModel.selectedPlan.collectAsState()
    val paymentMethods by viewModel.paymentMethods.collectAsState()
    val selectedPaymentMethod by viewModel.selectedPaymentMethod.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    // Track coupon code
    var couponCode by remember { mutableStateOf("") }
    var isValidatingCoupon by remember { mutableStateOf(false) }
    var couponDiscount by remember { mutableStateOf<Double?>(null) }
    
    // Card form state
    var isAddingCard by remember { mutableStateOf(false) }
    
    // Track payment confirmation
    var showConfirmationDialog by remember { mutableStateOf(false) }
    
    // Launch effect for events
    LaunchedEffect(key1 = true) {
        viewModel.events.collect { event ->
            when (event) {
                is PaymentEvent.Error -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                is PaymentEvent.CouponValidated -> {
                    isValidatingCoupon = false
                    couponDiscount = event.discountAmount
                    if (event.discountAmount != null) {
                        snackbarHostState.showSnackbar("Coupon applied: ${event.discountAmount}% discount")
                    } else {
                        snackbarHostState.showSnackbar("Invalid coupon code")
                    }
                }
                is PaymentEvent.PaymentSuccess -> {
                    snackbarHostState.showSnackbar("Payment successful!")
                    delay(1000) // Give time for the user to see the message
                    onPaymentSuccess()
                }
                is PaymentEvent.Success -> {
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }
    
    // Add card dialog
    if (isAddingCard) {
        AddCardDialog(
            onDismiss = { isAddingCard = false },
            onCardAdded = { cardParams ->
                viewModel.addPaymentMethod(cardParams)
                isAddingCard = false
            }
        )
    }
    
    // Confirmation dialog
    if (showConfirmationDialog) {
        PaymentConfirmationDialog(
            plan = selectedPlan,
            discount = couponDiscount,
            onConfirm = { 
                viewModel.processPayment(couponCode.takeIf { it.isNotBlank() })
                showConfirmationDialog = false
            },
            onDismiss = { showConfirmationDialog = false }
        )
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Subscribe to OneLove") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Loading state
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        strokeCap = StrokeCap.Round
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Plan selection section
                    item {
                        Text(
                            text = "Choose Your Plan",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    
                    // Plan options
                    items(plans) { plan ->
                        PlanCard(
                            plan = plan,
                            isSelected = plan.id == selectedPlan?.id,
                            onClick = { viewModel.selectPlan(plan) }
                        )
                    }
                    
                    // Payment method selection
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = "Payment Method",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Existing payment methods
                        if (paymentMethods.isNotEmpty()) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                paymentMethods.forEach { method ->
                                    PaymentMethodItem(
                                        paymentMethod = method,
                                        isSelected = method.id == selectedPaymentMethod?.id,
                                        onClick = { viewModel.selectPaymentMethod(method) }
                                    )
                                }
                            }
                        }
                        
                        // Add new payment method button
                        TextButton(
                            onClick = { isAddingCard = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Add New Payment Method")
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Coupon code
                        Text(
                            text = "Have a Coupon?",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = couponCode,
                                onValueChange = { 
                                    couponCode = it
                                    couponDiscount = null // Reset discount when code changes
                                },
                                label = { Text("Coupon Code") },
                                modifier = Modifier.weight(1f),
                                enabled = !isValidatingCoupon
                            )
                            
                            Button(
                                onClick = { 
                                    isValidatingCoupon = true
                                    viewModel.validateCoupon(couponCode)
                                },
                                enabled = couponCode.isNotBlank() && !isValidatingCoupon
                            ) {
                                if (isValidatingCoupon) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                } else {
                                    Text("Apply")
                                }
                            }
                        }
                        
                        // Show discount if applied
                        if (couponDiscount != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Coupon applied: ${couponDiscount}% discount",
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // Subscribe button
                        Button(
                            onClick = { showConfirmationDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = selectedPlan != null && selectedPaymentMethod != null
                        ) {
                            Text(
                                text = "Subscribe Now",
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        
                        // Terms and privacy
                        Text(
                            text = "By subscribing, you agree to our Terms of Service and Privacy Policy. " +
                                  "You can cancel your subscription at any time from your account settings.",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Plan card component
 */
@Composable
fun PlanCard(
    plan: SubscriptionPlan,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
    } else {
        MaterialTheme.colorScheme.surface
    }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(2.dp, borderColor),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Selection indicator
            RadioButton(
                selected = isSelected,
                onClick = onClick
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // Plan details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = plan.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    
                    if (plan.isPopular) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f),
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Text(
                                text = "Popular",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = plan.description,
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Features
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    plan.features.forEach { feature ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            
                            Spacer(modifier = Modifier.width(4.dp))
                            
                            Text(
                                text = feature,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // Price
            Column(
                horizontalAlignment = Alignment.End
            ) {
                val format = NumberFormat.getCurrencyInstance(Locale.US)
                format.currency = Currency.getInstance(plan.currency)
                
                val priceText = if (plan.discount > 0) {
                    val originalPrice = format.format(plan.price)
                    val discountedPrice = format.format(plan.price * (1 - plan.discount / 100))
                    "$discountedPrice\n"
                } else {
                    format.format(plan.price) + "\n"
                }
                
                Text(
                    text = priceText,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                // Show original price if discounted
                if (plan.discount > 0) {
                    val format = NumberFormat.getCurrencyInstance(Locale.US)
                    format.currency = Currency.getInstance(plan.currency)
                    
                    Text(
                        text = format.format(plan.price),
                        style = MaterialTheme.typography.bodySmall,
                        textDecoration = TextDecoration.LineThrough,
                        color = MaterialTheme.colorScheme.error
                    )
                    
                    Text(
                        text = "${plan.discount.toInt()}% off",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                
                Text(
                    text = getIntervalText(plan.interval, plan.intervalCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // Trial period if available
                if (plan.trialPeriodDays != null && plan.trialPeriodDays > 0) {
                    Text(
                        text = "${plan.trialPeriodDays}-day free trial",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
        }
    }
}

/**
 * Payment method item
 */
@Composable
fun PaymentMethodItem(
    paymentMethod: PaymentMethod,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick),
        color = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
        } else {
            MaterialTheme.colorScheme.surface
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Selection indicator
            RadioButton(
                selected = isSelected,
                onClick = onClick
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // Card icon
            Icon(
                imageVector = Icons.Default.CreditCard,
                contentDescription = null,
                tint = when (paymentMethod.brand?.lowercase()) {
                    "visa" -> Color(0xFF1A1F71)
                    "mastercard" -> Color(0xFFFF5F00)
                    "amex" -> Color(0xFF2E77BB)
                    "discover" -> Color(0xFFFF6000)
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Card details
            Column {
                Text(
                    text = getCardTypeName(paymentMethod),
                    style = MaterialTheme.typography.bodyLarge
                )
                
                Text(
                    text = "•••• ${paymentMethod.last4}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Expiry date
            if (paymentMethod.expiryMonth != null && paymentMethod.expiryYear != null) {
                Text(
                    text = "${paymentMethod.expiryMonth}/${paymentMethod.expiryYear % 100}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Default badge
            if (paymentMethod.isDefault) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(
                        text = "Default",
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

/**
 * Add card dialog
 */
@Composable
fun AddCardDialog(
    onDismiss: () -> Unit,
    onCardAdded: (CardParams) -> Unit
) {
    var cardNumber by remember { mutableStateOf("") }
    var expMonth by remember { mutableStateOf("") }
    var expYear by remember { mutableStateOf("") }
    var cvc by remember { mutableStateOf("") }
    var cardholderName by remember { mutableStateOf("") }
    
    var hasError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnClickOutside = true)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = "Add Payment Method",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Card input fields
                OutlinedTextField(
                    value = cardNumber,
                    onValueChange = { 
                        // Only allow numeric input and limit to 16 digits
                        if (it.length <= 16 && it.all { char -> char.isDigit() }) {
                            cardNumber = it
                        }
                    },
                    label = { Text("Card Number") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = hasError && cardNumber.length < 16,
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Expiry Month
                    OutlinedTextField(
                        value = expMonth,
                        onValueChange = { 
                            if (it.length <= 2 && it.all { char -> char.isDigit() }) {
                                expMonth = it
                            }
                        },
                        label = { Text("MM") },
                        modifier = Modifier.weight(1f),
                        isError = hasError && (expMonth.isEmpty() || expMonth.toIntOrNull() !in 1..12),
                        singleLine = true
                    )
                    
                    // Expiry Year
                    OutlinedTextField(
                        value = expYear,
                        onValueChange = { 
                            if (it.length <= 2 && it.all { char -> char.isDigit() }) {
                                expYear = it
                            }
                        },
                        label = { Text("YY") },
                        modifier = Modifier.weight(1f),
                        isError = hasError && expYear.length < 2,
                        singleLine = true
                    )
                    
                    // CVC
                    OutlinedTextField(
                        value = cvc,
                        onValueChange = { 
                            if (it.length <= 4 && it.all { char -> char.isDigit() }) {
                                cvc = it
                            }
                        },
                        label = { Text("CVC") },
                        modifier = Modifier.weight(1f),
                        isError = hasError && cvc.length < 3,
                        singleLine = true
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Cardholder name
                OutlinedTextField(
                    value = cardholderName,
                    onValueChange = { cardholderName = it },
                    label = { Text("Cardholder Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                // Error message
                if (hasError) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Security message
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Your payment information is secure and encrypted",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    
                    Button(
                        onClick = {
                            // Validate input
                            if (cardNumber.length < 16) {
                                hasError = true
                                errorMessage = "Please enter a valid card number"
                                return@Button
                            }
                            
                            val expMonthInt = expMonth.toIntOrNull()
                            if (expMonthInt == null || expMonthInt !in 1..12) {
                                hasError = true
                                errorMessage = "Please enter a valid expiry month"
                                return@Button
                            }
                            
                            if (expYear.length < 2) {
                                hasError = true
                                errorMessage = "Please enter a valid expiry year"
                                return@Button
                            }
                            
                            if (cvc.length < 3) {
                                hasError = true
                                errorMessage = "Please enter a valid CVC"
                                return@Button
                            }
                            
                            // Create card params
                            val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
                            val fullYear = currentYear / 100 * 100 + expYear.toInt()
                            
                            hasError = false
                            
                            val cardParams = CardParams(
                                number = cardNumber,
                                expMonth = expMonthInt,
                                expYear = fullYear,
                                cvc = cvc
                            )
                            
                            onCardAdded(cardParams)
                        }
                    ) {
                        Text("Add Card")
                    }
                }
            }
        }
    }
}

/**
 * Payment confirmation dialog
 */
@Composable
fun PaymentConfirmationDialog(
    plan: SubscriptionPlan?,
    discount: Double?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    var isProcessing by remember { mutableStateOf(false) }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnClickOutside = !isProcessing)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            if (plan == null) {
                // Error state
                Box(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No plan selected. Please select a subscription plan.")
                }
            } else {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth()
                ) {
                    Text(
                        text = "Confirm Subscription",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Plan details
                    Text(
                        text = "You are subscribing to:",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    
                    Text(
                        text = plan.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Order summary
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Subtotal",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                
                                val format = NumberFormat.getCurrencyInstance(Locale.US)
                                format.currency = Currency.getInstance(plan.currency)
                                
                                Text(
                                    text = format.format(plan.price),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            
                            // Discount if any
                            if (discount != null) {
                                Spacer(modifier = Modifier.height(4.dp))
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Discount ($discount%)",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    
                                    val format = NumberFormat.getCurrencyInstance(Locale.US)
                                    format.currency = Currency.getInstance(plan.currency)
                                    
                                    val discountAmount = plan.price * (discount / 100.0)
                                    
                                    Text(
                                        text = "-${format.format(discountAmount)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            
                            // Plan discount if any
                            if (plan.discount > 0) {
                                Spacer(modifier = Modifier.height(4.dp))
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Plan discount (${plan.discount.toInt()}%)",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    
                                    val format = NumberFormat.getCurrencyInstance(Locale.US)
                                    format.currency = Currency.getInstance(plan.currency)
                                    
                                    val discountAmount = plan.price * (plan.discount / 100.0)
                                    
                                    Text(
                                        text = "-${format.format(discountAmount)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            Divider()
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // Total
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Total",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                
                                val format = NumberFormat.getCurrencyInstance(Locale.US)
                                format.currency = Currency.getInstance(plan.currency)
                                
                                var finalPrice = plan.price
                                
                                // Apply plan discount
                                if (plan.discount > 0) {
                                    finalPrice *= (1 - plan.discount / 100.0)
                                }
                                
                                // Apply coupon discount
                                if (discount != null) {
                                    finalPrice *= (1 - discount / 100.0)
                                }
                                
                                Text(
                                    text = format.format(finalPrice),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            
                            // Billing frequency
                            Text(
                                text = "Billed ${getIntervalText(plan.interval, plan.intervalCount)}",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.align(Alignment.End)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Trial info if applicable
                    if (plan.trialPeriodDays != null && plan.trialPeriodDays > 0) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Help,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary
                                )
                                
                                Spacer(modifier = Modifier.width(8.dp))
                                
                                Text(
                                    text = "Your subscription will include a ${plan.trialPeriodDays}-day free trial. " +
                                          "You won't be charged until the trial period ends.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    
                    // Terms and cancellation
                    Text(
                        text = "By confirming this subscription, you agree to our Terms of Service. " +
                              "You can cancel anytime from your account settings.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            enabled = !isProcessing
                        ) {
                            Text("Cancel")
                        }
                        
                        Button(
                            onClick = {
                                isProcessing = true
                                onConfirm()
                            },
                            modifier = Modifier.weight(1f),
                            enabled = !isProcessing
                        ) {
                            if (isProcessing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Text("Confirm")
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Helper function to get interval text
 */
private fun getIntervalText(interval: BillingInterval, count: Int): String {
    return when (interval) {
        BillingInterval.DAY -> if (count == 1) "daily" else "every $count days"
        BillingInterval.WEEK -> if (count == 1) "weekly" else "every $count weeks"
        BillingInterval.MONTH -> if (count == 1) "monthly" else "every $count months"
        BillingInterval.YEAR -> if (count == 1) "yearly" else "every $count years"
    }
}

/**
 * Helper function to get card type name
 */
private fun getCardTypeName(paymentMethod: PaymentMethod): String {
    val brand = paymentMethod.brand?.lowercase() ?: return "Card"
    
    return when (brand) {
        "visa" -> "Visa"
        "mastercard" -> "Mastercard"
        "amex" -> "American Express"
        "discover" -> "Discover"
        "jcb" -> "JCB"
        "diners" -> "Diners Club"
        "unionpay" -> "UnionPay"
        else -> "Card"
    }
}