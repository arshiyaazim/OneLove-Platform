package com.kilagee.onelove.ui.screens.subscription

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kilagee.onelove.data.model.BillingPeriod
import com.kilagee.onelove.data.model.PaymentMethod
import com.kilagee.onelove.data.model.SubscriptionPlan
import com.kilagee.onelove.data.model.SubscriptionStatus
import com.kilagee.onelove.data.model.SubscriptionTier
import com.kilagee.onelove.data.model.UserSubscription
import com.kilagee.onelove.ui.components.OneLoveActionButton
import com.kilagee.onelove.ui.components.OneLoveDivider
import com.kilagee.onelove.ui.components.OneLoveErrorDisplay
import com.kilagee.onelove.ui.components.OneLoveLoadingIndicator
import com.kilagee.onelove.ui.components.OneLoveTopBar
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Screen for managing subscriptions
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionScreen(
    navigateBack: () -> Unit,
    navigateToPayment: (String) -> Unit,
    viewModel: SubscriptionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentSubscription by viewModel.currentSubscription.collectAsState()
    val subscriptionPlans by viewModel.subscriptionPlans.collectAsState()
    val selectedPlan by viewModel.selectedPlan.collectAsState()
    val selectedBillingPeriod by viewModel.selectedBillingPeriod.collectAsState()
    val paymentMethods by viewModel.paymentMethods.collectAsState()
    val selectedPaymentMethod by viewModel.selectedPaymentMethod.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val purchaseResult by viewModel.purchaseResult.collectAsState()
    
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Show error in snackbar
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            scope.launch {
                snackbarHostState.showSnackbar(it)
                viewModel.clearErrorMessage()
            }
        }
    }
    
    // Handle payment action required
    LaunchedEffect(uiState) {
        if (uiState is SubscriptionUiState.ActionRequired) {
            val actionUrl = (uiState as SubscriptionUiState.ActionRequired).actionUrl
            purchaseResult?.subscription?.id?.let { subscriptionId ->
                navigateToPayment(actionUrl)
            }
        }
    }
    
    Scaffold(
        topBar = {
            OneLoveTopBar(
                title = "Membership",
                onBackClick = navigateBack
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (uiState) {
                is SubscriptionUiState.Loading -> {
                    OneLoveLoadingIndicator()
                }
                is SubscriptionUiState.Error -> {
                    OneLoveErrorDisplay(
                        message = (uiState as SubscriptionUiState.Error).message,
                        onRetry = { viewModel.loadSubscriptionData() }
                    )
                }
                else -> {
                    SubscriptionContent(
                        currentSubscription = currentSubscription,
                        subscriptionPlans = subscriptionPlans,
                        selectedPlan = selectedPlan,
                        selectedBillingPeriod = selectedBillingPeriod,
                        paymentMethods = paymentMethods,
                        selectedPaymentMethod = selectedPaymentMethod,
                        isProcessing = isProcessing,
                        onPlanSelected = { viewModel.selectPlan(it) },
                        onBillingPeriodChanged = { viewModel.switchBillingPeriod(it) },
                        onPaymentMethodSelected = { viewModel.selectPaymentMethod(it) },
                        onSubscribe = { viewModel.subscribe() },
                        onCancelSubscription = { viewModel.cancelSubscription() },
                        onUpdateAutoRenew = { viewModel.updateAutoRenew(it) }
                    )
                }
            }
            
            // Processing indicator
            if (isProcessing) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

/**
 * Main subscription content
 */
@Composable
private fun SubscriptionContent(
    currentSubscription: UserSubscription?,
    subscriptionPlans: List<SubscriptionPlan>,
    selectedPlan: SubscriptionPlan?,
    selectedBillingPeriod: BillingPeriod,
    paymentMethods: List<PaymentMethod>,
    selectedPaymentMethod: PaymentMethod?,
    isProcessing: Boolean,
    onPlanSelected: (SubscriptionPlan) -> Unit,
    onBillingPeriodChanged: (BillingPeriod) -> Unit,
    onPaymentMethodSelected: (PaymentMethod) -> Unit,
    onSubscribe: () -> Unit,
    onCancelSubscription: () -> Unit,
    onUpdateAutoRenew: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Current subscription section
        if (currentSubscription != null) {
            CurrentSubscriptionCard(
                subscription = currentSubscription,
                onCancelSubscription = onCancelSubscription,
                onUpdateAutoRenew = onUpdateAutoRenew
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
        
        // Subscription plans section
        Text(
            text = "Choose your plan",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        
        // Billing period toggle
        BillingPeriodToggle(
            selectedBillingPeriod = selectedBillingPeriod,
            onBillingPeriodChanged = onBillingPeriodChanged
        )
        Spacer(modifier = Modifier.height(16.dp))
        
        // Plan cards
        subscriptionPlans.forEach { plan ->
            SubscriptionPlanCard(
                plan = plan,
                isSelected = plan.id == selectedPlan?.id,
                billingPeriod = selectedBillingPeriod,
                onClick = { onPlanSelected(plan) }
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
        
        // Payment method section if not free plan
        if (selectedPlan?.tier != SubscriptionTier.FREE) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Payment Method",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            if (paymentMethods.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CreditCard,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No payment methods found",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { /* Navigate to add payment method */ },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text("Add Payment Method")
                        }
                    }
                }
            } else {
                // Payment methods
                paymentMethods.forEach { method ->
                    PaymentMethodCard(
                        paymentMethod = method,
                        isSelected = method.id == selectedPaymentMethod?.id,
                        onClick = { onPaymentMethodSelected(method) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                // Add payment method button
                TextButton(
                    onClick = { /* Navigate to add payment method */ },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("+ Add New Card")
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Subscribe button
        OneLoveActionButton(
            text = getSubscribeButtonText(selectedPlan, currentSubscription),
            onClick = onSubscribe,
            enabled = selectedPlan != null && !isProcessing,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Terms and privacy policy
        Text(
            text = "By continuing, you agree to our Terms of Service and Privacy Policy. " +
                    "Subscriptions auto-renew until canceled.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Card showing the current subscription
 */
@Composable
private fun CurrentSubscriptionCard(
    subscription: UserSubscription,
    onCancelSubscription: () -> Unit,
    onUpdateAutoRenew: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (subscription.tier) {
                SubscriptionTier.PREMIUM -> MaterialTheme.colorScheme.primaryContainer
                SubscriptionTier.GOLD -> Color(0xFFFFF8E1)
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = when (subscription.tier) {
                        SubscriptionTier.PREMIUM -> Icons.Filled.Star
                        SubscriptionTier.GOLD -> Icons.Filled.Star
                        else -> Icons.Outlined.Star
                    },
                    contentDescription = null,
                    tint = when (subscription.tier) {
                        SubscriptionTier.PREMIUM -> MaterialTheme.colorScheme.primary
                        SubscriptionTier.GOLD -> Color(0xFFFFB300)
                        else -> MaterialTheme.colorScheme.primary
                    },
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${subscription.tier.name.lowercase().capitalize()} Membership",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            when (subscription.status) {
                                SubscriptionStatus.ACTIVE -> Color(0xFF4CAF50).copy(alpha = 0.2f)
                                SubscriptionStatus.CANCELED -> Color(0xFFFF5722).copy(alpha = 0.2f)
                                else -> Color(0xFFFF9800).copy(alpha = 0.2f)
                            }
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = subscription.status.name.lowercase().capitalize(),
                        style = MaterialTheme.typography.bodySmall,
                        color = when (subscription.status) {
                            SubscriptionStatus.ACTIVE -> Color(0xFF1B5E20)
                            SubscriptionStatus.CANCELED -> Color(0xFFB71C1C)
                            else -> Color(0xFF33691E)
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            OneLoveDivider()
            Spacer(modifier = Modifier.height(16.dp))
            
            // Subscription details
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.DateRange,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Renewal Date",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatDate(subscription.endDate),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Payment method (if available)
            if (subscription.stripePaymentMethodId != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Payments,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Payment Method",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Card ****${subscription.stripePaymentMethodId.takeLast(4)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            // Auto-renew toggle
            if (subscription.status == SubscriptionStatus.ACTIVE) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Auto-Renew",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = if (subscription.autoRenew) "Your subscription will automatically renew" else "Your subscription will expire on ${formatDate(subscription.endDate)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = subscription.autoRenew,
                        onCheckedChange = { onUpdateAutoRenew(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                            uncheckedThumbColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Cancel subscription button
            if (subscription.status == SubscriptionStatus.ACTIVE) {
                OutlinedButton(
                    onClick = onCancelSubscription,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Cancel Subscription")
                }
            }
        }
    }
}

/**
 * Toggle for switching between monthly and yearly billing
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BillingPeriodToggle(
    selectedBillingPeriod: BillingPeriod,
    onBillingPeriodChanged: (BillingPeriod) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier.fillMaxWidth()
        ) {
            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                selected = selectedBillingPeriod == BillingPeriod.MONTHLY,
                onClick = { onBillingPeriodChanged(BillingPeriod.MONTHLY) },
                label = { Text("Monthly") }
            )
            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                selected = selectedBillingPeriod == BillingPeriod.YEARLY,
                onClick = { onBillingPeriodChanged(BillingPeriod.YEARLY) },
                label = { 
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Yearly")
                        Text(
                            text = "Save 20%",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        }
    }
}

/**
 * Card displaying a subscription plan
 */
@Composable
private fun SubscriptionPlanCard(
    plan: SubscriptionPlan,
    isSelected: Boolean,
    billingPeriod: BillingPeriod,
    onClick: () -> Unit
) {
    val price = if (billingPeriod == BillingPeriod.MONTHLY) {
        plan.priceMonthly
    } else {
        plan.priceYearly
    }
    
    val borderColor = if (isSelected) {
        when (plan.tier) {
            SubscriptionTier.PREMIUM -> MaterialTheme.colorScheme.primary
            SubscriptionTier.GOLD -> Color(0xFFFFB300)
            else -> MaterialTheme.colorScheme.primary
        }
    } else {
        MaterialTheme.colorScheme.outline
    }
    
    val backgroundColor = if (isSelected) {
        when (plan.tier) {
            SubscriptionTier.PREMIUM -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            SubscriptionTier.GOLD -> Color(0xFFFFF8E1)
            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        }
    } else {
        MaterialTheme.colorScheme.surface
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 4.dp else 0.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Selected indicator
            if (isSelected) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = "Selected",
                    tint = when (plan.tier) {
                        SubscriptionTier.PREMIUM -> MaterialTheme.colorScheme.primary
                        SubscriptionTier.GOLD -> Color(0xFFFFB300)
                        else -> MaterialTheme.colorScheme.primary
                    },
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
            
            // Plan info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Plan tier icon
                    when (plan.tier) {
                        SubscriptionTier.PREMIUM -> {
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        SubscriptionTier.GOLD -> {
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = null,
                                tint = Color(0xFFFFB300),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        else -> {
                            // No icon for free tier
                        }
                    }
                    
                    // Plan name
                    Text(
                        text = plan.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Plan description
                Text(
                    text = plan.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // Only show features for non-free plans
                if (plan.tier != SubscriptionTier.FREE && plan.features.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Features
                    var expanded by remember { mutableStateOf(false) }
                    
                    TextButton(
                        onClick = { expanded = !expanded },
                        contentPadding = ButtonDefaults.TextButtonWithIconContentPadding
                    ) {
                        Text(
                            text = if (expanded) "Hide Features" else "Show Features",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Icon(
                            imageVector = if (expanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                            contentDescription = if (expanded) "Hide" else "Show",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    AnimatedVisibility(
                        visible = expanded,
                        enter = fadeIn(animationSpec = tween(300)),
                        exit = fadeOut(animationSpec = tween(300))
                    ) {
                        Column(
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            plan.features.forEach { feature ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = feature,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Price
            Column(
                horizontalAlignment = Alignment.End
            ) {
                if (plan.tier == SubscriptionTier.FREE) {
                    Text(
                        text = "Free",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Text(
                        text = "$${String.format("%.2f", price)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (billingPeriod == BillingPeriod.MONTHLY) "per month" else "per year",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    // Show savings for yearly plan
                    if (billingPeriod == BillingPeriod.YEARLY && plan.tier != SubscriptionTier.FREE) {
                        val monthlyCost = plan.priceMonthly * 12
                        val yearlyCost = plan.priceYearly
                        val savingsPercent = ((monthlyCost - yearlyCost) / monthlyCost * 100).toInt()
                        
                        if (savingsPercent > 0) {
                            Text(
                                text = "Save $savingsPercent%",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                textDecoration = TextDecoration.Underline
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Card displaying a payment method
 */
@Composable
private fun PaymentMethodCard(
    paymentMethod: PaymentMethod,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Card icon
            Icon(
                imageVector = Icons.Outlined.CreditCard,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Card details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "${paymentMethod.brand} ••••${paymentMethod.last4}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Expires ${paymentMethod.expiryMonth}/${paymentMethod.expiryYear}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Default indicator
            if (paymentMethod.isDefault) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Default",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            // Selected indicator
            if (isSelected) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

/**
 * Get the text for the subscribe button based on the selected plan and current subscription
 */
private fun getSubscribeButtonText(
    selectedPlan: SubscriptionPlan?,
    currentSubscription: UserSubscription?
): String {
    if (selectedPlan == null) return "Choose a Plan"
    
    if (currentSubscription == null) {
        return if (selectedPlan.tier == SubscriptionTier.FREE) {
            "Continue with Free Plan"
        } else {
            "Subscribe Now"
        }
    }
    
    // User already has a subscription
    val selectedTierLevel = getTierLevel(selectedPlan.tier)
    val currentTierLevel = getTierLevel(SubscriptionTier.valueOf(currentSubscription.tier.name))
    
    return when {
        selectedTierLevel > currentTierLevel -> "Upgrade to ${selectedPlan.name}"
        selectedTierLevel < currentTierLevel -> "Downgrade to ${selectedPlan.name}"
        else -> "Change Billing Period"
    }
}

/**
 * Get the numeric level of a subscription tier for comparison
 */
private fun getTierLevel(tier: SubscriptionTier): Int {
    return when (tier) {
        SubscriptionTier.FREE -> 0
        SubscriptionTier.PREMIUM -> 1
        SubscriptionTier.GOLD -> 2
    }
}

/**
 * Format a date to a readable string
 */
private fun formatDate(date: Date): String {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return dateFormat.format(date)
}

/**
 * Extension function to capitalize first letter of a string
 */
private fun String.capitalize(): String {
    return if (this.isNotEmpty()) {
        this.substring(0, 1).uppercase() + this.substring(1)
    } else {
        this
    }
}