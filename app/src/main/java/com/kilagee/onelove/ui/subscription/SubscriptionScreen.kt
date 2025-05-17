package com.kilagee.onelove.ui.subscription

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.kilagee.onelove.data.model.SubscriptionType
import com.kilagee.onelove.domain.model.Resource
import com.kilagee.onelove.domain.repository.SubscriptionPlan
import com.kilagee.onelove.navigation.Screen
import com.kilagee.onelove.ui.components.ErrorStateView
import com.kilagee.onelove.ui.components.LoadingStateView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionScreen(
    navController: NavController,
    viewModel: SubscriptionViewModel = hiltViewModel()
) {
    val subscriptionPlansState by viewModel.subscriptionPlansState.collectAsState()
    val paymentMethodsState by viewModel.paymentMethodsState.collectAsState()
    val createSubscriptionState by viewModel.createSubscriptionState.collectAsState()
    
    // Selected plan
    var selectedPlan by remember { mutableStateOf<SubscriptionPlan?>(null) }
    
    // Selected payment method
    var selectedPaymentMethodId by remember { mutableStateOf<String?>(null) }
    
    // Show payment method dialog
    var showPaymentMethodDialog by remember { mutableStateOf(false) }
    
    // Show add payment method dialog
    var showAddPaymentMethodDialog by remember { mutableStateOf(false) }
    
    // Effect to auto-select the first payment method when loaded
    LaunchedEffect(paymentMethodsState) {
        if (paymentMethodsState is Resource.Success) {
            val paymentMethods = (paymentMethodsState as Resource.Success).data
            val defaultMethod = paymentMethods.find { it.isDefault }
            if (defaultMethod != null) {
                selectedPaymentMethodId = defaultMethod.id
            } else if (paymentMethods.isNotEmpty()) {
                selectedPaymentMethodId = paymentMethods.first().id
            }
        }
    }
    
    // Observe create subscription state
    LaunchedEffect(createSubscriptionState) {
        if (createSubscriptionState is Resource.Success) {
            // Navigate to my membership screen
            navController.navigate(Screen.MyMembership.route) {
                popUpTo(Screen.Subscriptions.route) { inclusive = true }
            }
            
            // Clear state
            viewModel.clearCreateSubscriptionState()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Premium Plans") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = subscriptionPlansState) {
                is Resource.Loading -> {
                    LoadingStateView()
                }
                is Resource.Success -> {
                    val plans = state.data.filter { it.type != SubscriptionType.BASIC } // Filter out free plan
                    
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            Text(
                                text = "Choose a Premium Plan",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                            
                            Text(
                                text = "Upgrade your OneLove experience with premium features",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                        }
                        
                        items(plans) { plan ->
                            SubscriptionPlanCard(
                                plan = plan,
                                isSelected = selectedPlan?.type == plan.type,
                                onClick = { selectedPlan = plan }
                            )
                        }
                        
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Button(
                                onClick = { 
                                    if (selectedPlan != null) {
                                        // If payment methods available, show dialog
                                        if (paymentMethodsState is Resource.Success && 
                                            (paymentMethodsState as Resource.Success).data.isNotEmpty()) {
                                            showPaymentMethodDialog = true
                                        } else {
                                            // Else show add payment method dialog
                                            showAddPaymentMethodDialog = true
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                enabled = selectedPlan != null,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                if (createSubscriptionState is Resource.Loading) {
                                    CircularProgressIndicator(
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                } else {
                                    Text("Continue to Payment")
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            if (createSubscriptionState is Resource.Error) {
                                Text(
                                    text = (createSubscriptionState as Resource.Error).message,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                            
                            TextButton(
                                onClick = { navController.navigate(Screen.MyMembership.route) },
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            ) {
                                Text("I already have a subscription")
                            }
                        }
                    }
                }
                is Resource.Error -> {
                    ErrorStateView(
                        message = state.message,
                        onRetryClick = { viewModel.loadSubscriptionPlans() }
                    )
                }
            }
        }
    }
    
    // Payment method selection dialog
    if (showPaymentMethodDialog && paymentMethodsState is Resource.Success) {
        val paymentMethods = (paymentMethodsState as Resource.Success).data
        
        AlertDialog(
            onDismissRequest = { showPaymentMethodDialog = false },
            title = { Text("Select Payment Method") },
            text = {
                LazyColumn {
                    items(paymentMethods) { method ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedPaymentMethodId = method.id }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedPaymentMethodId == method.id,
                                onClick = { selectedPaymentMethodId = method.id }
                            )
                            
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            val icon = when (method.brand?.lowercase()) {
                                "visa" -> Icons.Default.CreditCard
                                "mastercard" -> Icons.Default.CreditCard
                                "amex" -> Icons.Default.CreditCard
                                else -> Icons.Default.CreditCard
                            }
                            
                            Icon(icon, contentDescription = method.brand)
                            
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            Column {
                                Text(
                                    text = method.brand ?: "Card",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                
                                Text(
                                    text = "•••• ${method.last4}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    
                    item {
                        TextButton(
                            onClick = {
                                showPaymentMethodDialog = false
                                showAddPaymentMethodDialog = true
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Add, contentDescription = "Add")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Add new payment method")
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPaymentMethodDialog = false
                        
                        // Create subscription
                        if (selectedPlan != null && selectedPaymentMethodId != null) {
                            viewModel.createSubscription(
                                type = selectedPlan!!.type,
                                paymentMethodId = selectedPaymentMethodId!!,
                                autoRenew = true
                            )
                        }
                    },
                    enabled = selectedPaymentMethodId != null
                ) {
                    Text("Subscribe")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPaymentMethodDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Add payment method dialog
    if (showAddPaymentMethodDialog) {
        // In a real app, this would be a more complex form
        // For simplicity, we'll just navigate to a payment method screen
        showAddPaymentMethodDialog = false
        navController.navigate(Screen.AddPaymentMethod.route)
    }
}

@Composable
fun SubscriptionPlanCard(
    plan: SubscriptionPlan,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
    val borderWidth = if (isSelected) 2.dp else 1.dp
    val backgroundColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .border(
                width = borderWidth,
                color = borderColor,
                shape = RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 4.dp else 1.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Popular tag
            if (plan.popular) {
                Box(
                    modifier = Modifier
                        .align(Alignment.End)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "POPULAR",
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            // Plan name and price
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = plan.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = plan.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "$${String.format("%.2f", plan.priceUsd)}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text(
                        text = "/${if (plan.durationMonths == 1) "month" else "${plan.durationMonths} months"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Features
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                plan.features.forEach { feature ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Text(
                            text = feature,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}