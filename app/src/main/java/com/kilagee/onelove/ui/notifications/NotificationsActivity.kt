package com.kilagee.onelove.ui.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import com.kilagee.onelove.data.model.Notification
import com.kilagee.onelove.data.model.NotificationType
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class NotificationsActivity : ComponentActivity() {
    
    private lateinit var viewModel: NotificationsViewModel
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Create notification channels
        createNotificationChannels()
        
        viewModel = ViewModelProvider(this)[NotificationsViewModel::class.java]
        
        // Handle notification if opened from notification
        intent.extras?.let { extras ->
            extras.getString("notification_id")?.let { notificationId ->
                viewModel.markNotificationAsRead(notificationId)
            }
        }
        
        setContent {
            NotificationsScreen(
                onBackClick = { finish() },
                viewModel = viewModel
            )
        }
    }
    
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            // Create Messages channel
            val messagesChannel = NotificationChannel(
                "messages",
                "Messages",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for new messages and matches"
                enableVibration(true)
                enableLights(true)
                lightColor = android.graphics.Color.BLUE
            }
            
            // Create Calls channel
            val callsChannel = NotificationChannel(
                "calls",
                "Calls",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for incoming calls"
                enableVibration(true)
                enableLights(true)
                lightColor = android.graphics.Color.GREEN
            }
            
            // Create Payments channel
            val paymentsChannel = NotificationChannel(
                "payments",
                "Payments",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for payment updates"
            }
            
            // Create Subscriptions channel
            val subscriptionsChannel = NotificationChannel(
                "subscriptions",
                "Subscriptions",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for subscription updates"
            }
            
            // Create Offers channel
            val offersChannel = NotificationChannel(
                "offers",
                "Offers",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for offers and points"
            }
            
            // Register all channels
            notificationManager.createNotificationChannels(listOf(
                messagesChannel,
                callsChannel,
                paymentsChannel,
                subscriptionsChannel,
                offersChannel
            ))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    onBackClick: () -> Unit,
    viewModel: NotificationsViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.loadNotifications()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notifications") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.markAllAsRead() }) {
                        Icon(Icons.Default.DoneAll, contentDescription = "Mark all as read")
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
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (uiState.notifications.isEmpty()) {
                EmptyNotificationsView()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(uiState.notifications) { notification ->
                        NotificationItem(
                            notification = notification,
                            onNotificationClick = { 
                                viewModel.handleNotificationClick(notification)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationItem(
    notification: Notification,
    onNotificationClick: () -> Unit
) {
    val backgroundColor = if (!notification.read) {
        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
    } else {
        Color.Transparent
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (!notification.read) 1.dp else 0.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onNotificationClick)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Surface(
                modifier = Modifier.size(48.dp),
                color = getNotificationColor(notification.type),
                shape = MaterialTheme.shapes.medium
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getNotificationIcon(notification.type),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = notification.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (!notification.read) FontWeight.Bold else FontWeight.Normal
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = notification.body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = formatTimestamp(notification.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun EmptyNotificationsView() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Notifications,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "No Notifications",
            style = MaterialTheme.typography.titleLarge
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "You'll see notifications about messages, matches, calls, and payments here.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// Helper functions
private fun getNotificationIcon(type: NotificationType): ImageVector {
    return when (type) {
        NotificationType.MESSAGE -> Icons.Default.Chat
        NotificationType.MATCH -> Icons.Default.Favorite
        NotificationType.CALL -> Icons.Default.Call
        NotificationType.PAYMENT -> Icons.Default.Payments
        NotificationType.SUBSCRIPTION -> Icons.Default.Subscriptions
        NotificationType.OFFER -> Icons.Default.LocalOffer
        NotificationType.SYSTEM -> Icons.Default.Notifications
    }
}

private fun getNotificationColor(type: NotificationType): Color {
    return when (type) {
        NotificationType.MESSAGE -> Color(0xFF2196F3) // Blue
        NotificationType.MATCH -> Color(0xFFE91E63) // Pink
        NotificationType.CALL -> Color(0xFF4CAF50) // Green
        NotificationType.PAYMENT -> Color(0xFF9C27B0) // Purple
        NotificationType.SUBSCRIPTION -> Color(0xFFFF9800) // Orange
        NotificationType.OFFER -> Color(0xFFFF5722) // Deep Orange
        NotificationType.SYSTEM -> Color(0xFF607D8B) // Blue Grey
    }
}

private fun formatTimestamp(timestamp: Date): String {
    val now = Calendar.getInstance()
    val notificationTime = Calendar.getInstance().apply { time = timestamp }
    
    return when {
        // Today
        now.get(Calendar.DATE) == notificationTime.get(Calendar.DATE) -> {
            SimpleDateFormat("h:mm a", Locale.getDefault()).format(timestamp)
        }
        // Yesterday
        now.get(Calendar.DATE) - notificationTime.get(Calendar.DATE) == 1 -> {
            "Yesterday, ${SimpleDateFormat("h:mm a", Locale.getDefault()).format(timestamp)}"
        }
        // This week
        now.get(Calendar.WEEK_OF_YEAR) == notificationTime.get(Calendar.WEEK_OF_YEAR) -> {
            SimpleDateFormat("EEEE", Locale.getDefault()).format(timestamp)
        }
        // Older
        else -> {
            SimpleDateFormat("MMM d", Locale.getDefault()).format(timestamp)
        }
    }
}