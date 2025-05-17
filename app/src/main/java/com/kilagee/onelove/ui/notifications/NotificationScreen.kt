package com.kilagee.onelove.ui.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.kilagee.onelove.data.model.Notification
import com.kilagee.onelove.data.model.NotificationActionType
import com.kilagee.onelove.data.model.NotificationType
import com.kilagee.onelove.domain.model.Resource
import com.kilagee.onelove.navigation.Screen
import com.kilagee.onelove.ui.components.EmptyStateView
import com.kilagee.onelove.ui.components.ErrorStateView
import com.kilagee.onelove.ui.components.LoadingStateView
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(
    navController: NavController,
    viewModel: NotificationViewModel = hiltViewModel()
) {
    val notificationsState by viewModel.notificationsState.collectAsState()
    val operationState by viewModel.operationState.collectAsState()
    var showDeleteAllDialog by remember { mutableStateOf(false) }
    
    // Effect to reload notifications when screen is shown
    LaunchedEffect(Unit) {
        viewModel.loadNotifications()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notifications") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Mark all as read button
                    IconButton(onClick = { viewModel.markAllAsRead() }) {
                        Icon(Icons.Filled.DoneAll, contentDescription = "Mark All as Read")
                    }
                    
                    // Delete all button
                    IconButton(onClick = { showDeleteAllDialog = true }) {
                        Icon(Icons.Filled.DeleteSweep, contentDescription = "Delete All")
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
            when (val state = notificationsState) {
                is Resource.Loading -> {
                    LoadingStateView()
                }
                is Resource.Success -> {
                    val notifications = state.data
                    if (notifications.isEmpty()) {
                        EmptyStateView(
                            icon = Icons.Filled.Notifications,
                            message = "No notifications yet",
                            actionText = null,
                            onActionClick = null
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(notifications) { notification ->
                                NotificationItem(
                                    notification = notification,
                                    onClick = {
                                        // Mark as read
                                        if (!notification.isRead) {
                                            viewModel.markAsRead(notification.id)
                                        }
                                        
                                        // Handle action
                                        handleNotificationAction(notification, navController)
                                    },
                                    onDelete = {
                                        viewModel.deleteNotification(notification.id)
                                    }
                                )
                            }
                        }
                    }
                }
                is Resource.Error -> {
                    ErrorStateView(
                        message = state.message,
                        onRetryClick = { viewModel.loadNotifications() }
                    )
                }
            }
            
            // Delete all confirmation dialog
            if (showDeleteAllDialog) {
                AlertDialog(
                    onDismissRequest = { showDeleteAllDialog = false },
                    title = { Text("Delete All Notifications") },
                    text = { Text("Are you sure you want to delete all notifications? This action cannot be undone.") },
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.deleteAllNotifications()
                                showDeleteAllDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            )
                        ) {
                            Text("Delete All")
                        }
                    },
                    dismissButton = {
                        OutlinedButton(
                            onClick = { showDeleteAllDialog = false }
                        ) {
                            Text("Cancel")
                        }
                    }
                )
            }
            
            // Operation loading indicator
            if (operationState is Resource.Loading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
            
            // Operation error dialog
            if (operationState is Resource.Error) {
                AlertDialog(
                    onDismissRequest = { viewModel.clearOperationState() },
                    title = { Text("Error") },
                    text = { Text((operationState as Resource.Error).message) },
                    confirmButton = {
                        Button(onClick = { viewModel.clearOperationState() }) {
                            Text("OK")
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationItem(
    notification: Notification,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (notification.isRead) {
                MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon based on notification type
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(getNotificationTypeColor(notification.type).copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (notification.imageUrl != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(notification.imageUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = null,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = getNotificationTypeIcon(notification.type),
                            contentDescription = null,
                            tint = getNotificationTypeColor(notification.type),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    // Title
                    Text(
                        text = notification.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (!notification.isRead) FontWeight.Bold else FontWeight.Normal
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // Message (expand/collapse)
                    Text(
                        text = notification.message,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = if (expanded) Int.MAX_VALUE else 2,
                        overflow = if (expanded) TextOverflow.Clip else TextOverflow.Ellipsis,
                        modifier = Modifier.clickable { expanded = !expanded }
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // Date
                    val dateFormat = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault())
                    Text(
                        text = dateFormat.format(notification.createdAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                // Delete button
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
            
            // Action button if available
            if (notification.actionType != NotificationActionType.NONE) {
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = onClick,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(getActionButtonText(notification.actionType))
                }
            }
        }
    }
}

@Composable
private fun getNotificationTypeIcon(type: NotificationType): ImageVector {
    return when (type) {
        NotificationType.MATCH -> Icons.Filled.Favorite
        NotificationType.MESSAGE -> Icons.Filled.Chat
        NotificationType.OFFER -> Icons.Filled.CardGiftcard
        NotificationType.OFFER_ACCEPTED -> Icons.Filled.Check
        NotificationType.OFFER_DECLINED -> Icons.Filled.Close
        NotificationType.CALL_MISSED -> Icons.Filled.CallMissed
        NotificationType.CALL_ENDED -> Icons.Filled.Call
        NotificationType.VERIFICATION -> Icons.Filled.Verified
        NotificationType.PAYMENT -> Icons.Filled.CreditCard
        NotificationType.SYSTEM -> Icons.Filled.Notifications
    }
}

@Composable
private fun getNotificationTypeColor(type: NotificationType): Color {
    return when (type) {
        NotificationType.MATCH -> Color(0xFFE91E63) // Pink
        NotificationType.MESSAGE -> Color(0xFF2196F3) // Blue
        NotificationType.OFFER, NotificationType.OFFER_ACCEPTED -> Color(0xFF4CAF50) // Green
        NotificationType.OFFER_DECLINED -> Color(0xFFF44336) // Red
        NotificationType.CALL_MISSED, NotificationType.CALL_ENDED -> Color(0xFF9C27B0) // Purple
        NotificationType.VERIFICATION -> Color(0xFF00BFA5) // Teal
        NotificationType.PAYMENT -> Color(0xFFFF9800) // Orange
        NotificationType.SYSTEM -> Color(0xFF607D8B) // Blue Grey
    }
}

private fun getActionButtonText(actionType: NotificationActionType): String {
    return when (actionType) {
        NotificationActionType.OPEN_PROFILE -> "View Profile"
        NotificationActionType.OPEN_CHAT -> "Open Chat"
        NotificationActionType.OPEN_OFFER -> "View Offer"
        NotificationActionType.OPEN_CALL -> "Call Back"
        NotificationActionType.OPEN_PAYMENT -> "View Payment"
        NotificationActionType.OPEN_SETTINGS -> "Open Settings"
        NotificationActionType.NONE -> ""
    }
}

private fun handleNotificationAction(
    notification: Notification,
    navController: NavController
) {
    // Handle action based on type and data
    when (notification.actionType) {
        NotificationActionType.OPEN_PROFILE -> {
            // Navigate to profile
            notification.relatedId?.let { userId ->
                // For now, we just navigate to profile screen
                // In future, we'll need to add a parameter for the user ID
                navController.navigate(Screen.Profile.route)
            }
        }
        NotificationActionType.OPEN_CHAT -> {
            // Navigate to chat
            notification.relatedId?.let { chatId ->
                navController.navigate(Screen.ChatDetail.createRoute(chatId))
            }
        }
        NotificationActionType.OPEN_OFFER -> {
            // Navigate to offer detail
            notification.relatedId?.let { offerId ->
                navController.navigate(Screen.OfferDetail.createRoute(offerId))
            }
        }
        NotificationActionType.OPEN_CALL -> {
            // Handle call action
            // This will be implemented when we create the call functionality
        }
        NotificationActionType.OPEN_PAYMENT -> {
            // Navigate to payment screen or transaction detail
            navController.navigate(Screen.Wallet.route)
        }
        NotificationActionType.OPEN_SETTINGS -> {
            // Navigate to settings
            navController.navigate(Screen.Settings.route)
        }
        NotificationActionType.NONE -> {
            // No action
        }
    }
}