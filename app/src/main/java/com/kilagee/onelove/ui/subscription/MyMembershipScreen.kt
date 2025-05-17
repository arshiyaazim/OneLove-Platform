package com.kilagee.onelove.ui.subscription

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.kilagee.onelove.data.model.Subscription
import com.kilagee.onelove.data.model.SubscriptionStatus
import com.kilagee.onelove.data.model.SubscriptionType
import com.kilagee.onelove.domain.model.Resource
import com.kilagee.onelove.navigation.Screen
import com.kilagee.onelove.ui.components.ErrorStateView
import com.kilagee.onelove.ui.components.LoadingStateView
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyMembershipScreen(
    navController: NavController,
    viewModel: SubscriptionViewModel = hiltViewModel()
) {
    val activeSubscriptionState by viewModel.activeSubscriptionState.collectAsState()
    val userSubscriptionsState by viewModel.userSubscriptionsState.collectAsState()
    
    // Show confirmation dialog
    var showCancelDialog by remember { mutableStateOf(false) }
    var subscriptionToCancel by remember { mutableStateOf<Subscription?>(null) }
    
    // Refresh on appear
    LaunchedEffect(Unit) {
        viewModel.loadActiveSubscription()
        viewModel.loadUserSubscriptions()
        viewModel.syncSubscriptions()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Membership") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.syncSubscriptions() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate(Screen.Subscriptions.route) },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Upgrade",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val active = activeSubscriptionState) {
                is Resource.Loading -> {
                    LoadingStateView()
                }
                is Resource.Success -> {
                    val activeSubscription = active.data
                    
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (activeSubscription != null && activeSubscription.status == SubscriptionStatus.ACTIVE) {
                            // Active subscription card
                            item {
                                ActiveSubscriptionCard(
                                    subscription = activeSubscription,
                                    onCancelClick = {
                                        subscriptionToCancel = activeSubscription
                                        showCancelDialog = true
                                    },
                                    onRenewClick = { autoRenew ->
                                        viewModel.toggleAutoRenew(activeSubscription.id, autoRenew)
                                    }
                                )
                            }
                            
                            // Membership features
                            item {
                                MembershipFeaturesCard(subscription = activeSubscription)
                            }
                        } else {
                            // No active subscription
                            item {
                                NoActiveSubscriptionCard(
                                    onUpgradeClick = { navController.navigate(Screen.Subscriptions.route) }
                                )
                            }
                        }
                        
                        // Past subscriptions
                        when (val userSubs = userSubscriptionsState) {
                            is Resource.Success -> {
                                val pastSubscriptions = userSubs.data.filter { 
                                    it.status != SubscriptionStatus.ACTIVE || 
                                    it.currentPeriodEnd.before(Date()) 
                                }
                                
                                if (pastSubscriptions.isNotEmpty()) {
                                    item {
                                        Text(
                                            text = "Subscription History",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(vertical = 8.dp)
                                        )
                                    }
                                    
                                    pastSubscriptions.forEach { subscription ->
                                        item {
                                            PastSubscriptionItem(subscription = subscription)
                                        }
                                    }
                                }
                            }
                            else -> { /* Do nothing */ }
                        }
                    }
                }
                is Resource.Error -> {
                    ErrorStateView(
                        message = active.message,
                        onRetryClick = { 
                            viewModel.loadActiveSubscription()
                            viewModel.loadUserSubscriptions()
                        }
                    )
                }
            }
        }
    }
    
    // Cancel subscription confirmation dialog
    if (showCancelDialog && subscriptionToCancel != null) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text("Cancel Subscription") },
            text = {
                Column {
                    Text("Are you sure you want to cancel your ${subscriptionToCancel!!.type.name.lowercase().capitalize()} subscription?")
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        "You will lose access to premium features after the current billing period ends on ${formatDate(subscriptionToCancel!!.currentPeriodEnd)}.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = false,
                            onCheckedChange = { /* Cancel immediately not recommended */ }
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Text(
                            "Cancel immediately (not recommended)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.cancelSubscription(subscriptionToCancel!!.id, false)
                        showCancelDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Cancel Subscription")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) {
                    Text("Keep Subscription")
                }
            }
        )
    }
}

@Composable
fun ActiveSubscriptionCard(
    subscription: Subscription,
    onCancelClick: () -> Unit,
    onRenewClick: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Badge
            Box(
                modifier = Modifier
                    .align(Alignment.Start)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "ACTIVE",
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Subscription details
            Text(
                text = getSubscriptionTypeName(subscription.type),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Active until ${formatDate(subscription.currentPeriodEnd)}",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = "$${String.format("%.2f", subscription.priceUsd)}/month",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Auto-renew toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Auto-renew subscription",
                    style = MaterialTheme.typography.bodyLarge
                )
                
                Switch(
                    checked = subscription.autoRenew,
                    onCheckedChange = onRenewClick
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Cancel button
            OutlinedButton(
                onClick = onCancelClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Cancel Subscription")
            }
        }
    }
}

@Composable
fun MembershipFeaturesCard(subscription: Subscription) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Your Premium Features",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Features
            val features = subscription.features.split(",")
            
            features.forEach { feature ->
                Row(
                    modifier = Modifier.padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Text(
                        text = feature.trim(),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}

@Composable
fun NoActiveSubscriptionCard(onUpgradeClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "No Active Subscription",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Upgrade to premium to unlock exclusive features and enhance your OneLove experience.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = onUpgradeClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Upgrade to Premium")
            }
        }
    }
}

@Composable
fun PastSubscriptionItem(subscription: Subscription) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status icon
            val (icon, tint) = when (subscription.status) {
                SubscriptionStatus.EXPIRED -> Icons.Default.AccessTime to MaterialTheme.colorScheme.onSurfaceVariant
                SubscriptionStatus.CANCELED -> Icons.Default.Cancel to MaterialTheme.colorScheme.error
                else -> Icons.Default.Info to MaterialTheme.colorScheme.onSurfaceVariant
            }
            
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column {
                Text(
                    text = getSubscriptionTypeName(subscription.type),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = "${formatDate(subscription.startDate)} - ${formatDate(subscription.currentPeriodEnd)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Text(
                    text = subscription.status.name.capitalize(),
                    style = MaterialTheme.typography.bodySmall,
                    color = when (subscription.status) {
                        SubscriptionStatus.CANCELED -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            Text(
                text = "$${String.format("%.2f", subscription.priceUsd)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// Helper functions
private fun formatDate(date: Date): String {
    val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return formatter.format(date)
}

private fun getSubscriptionTypeName(type: SubscriptionType): String {
    return when (type) {
        SubscriptionType.BASIC -> "Basic"
        SubscriptionType.BOOST -> "Boost"
        SubscriptionType.UNLIMITED -> "Unlimited"
        SubscriptionType.PREMIUM -> "Premium"
    }
}

private fun String.capitalize(): String {
    return this.lowercase().replaceFirstChar { it.uppercase() }
}