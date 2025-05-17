package com.kilagee.onelove.ui.screens.notifications

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kilagee.onelove.data.model.Notification
import com.kilagee.onelove.data.model.NotificationActionType
import com.kilagee.onelove.data.model.NotificationType
import com.kilagee.onelove.data.model.PushNotificationSettings
import com.kilagee.onelove.ui.LocalSnackbarHostState
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Notifications screen component
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToProfile: (String) -> Unit,
    onNavigateToChat: (String) -> Unit,
    onNavigateToMatch: (String) -> Unit,
    onNavigateToSubscription: () -> Unit,
    onNavigateToAIChat: (String, String?) -> Unit,
    viewModel: NotificationsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val notifications by viewModel.notifications.collectAsState()
    val unreadCount by viewModel.unreadCount.collectAsState()
    val notificationSettings by viewModel.notificationSettings.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val isSettingsOpen by viewModel.isSettingsOpen.collectAsState()
    
    val scope = rememberCoroutineScope()
    val snackbarHostState = LocalSnackbarHostState.current
    val context = LocalContext.current
    
    // Filter menu state
    var showFilterMenu by remember { mutableStateOf(false) }
    
    // Confirmation dialog state
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    
    // Collect one-time events
    LaunchedEffect(key1 = true) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is NotificationEvent.Error -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                is NotificationEvent.AllMarkedAsRead -> {
                    snackbarHostState.showSnackbar("All notifications marked as read")
                }
                is NotificationEvent.NotificationDeleted -> {
                    snackbarHostState.showSnackbar("Notification deleted")
                }
                is NotificationEvent.AllNotificationsDeleted -> {
                    snackbarHostState.showSnackbar("All notifications deleted")
                    showDeleteConfirmation = false
                }
                is NotificationEvent.SettingsUpdated -> {
                    snackbarHostState.showSnackbar("Notification settings updated")
                }
                is NotificationEvent.TestNotificationSent -> {
                    snackbarHostState.showSnackbar("Test notification sent")
                }
            }
        }
    }
    
    // Handle notification click
    val handleNotificationClick = { notification: Notification ->
        // Mark as read
        if (!notification.read) {
            viewModel.markAsRead(notification.id)
        }
        
        // Handle navigation based on action type
        when (notification.actionType) {
            NotificationActionType.VIEW_PROFILE -> {
                notification.getTargetId()?.let { userId ->
                    onNavigateToProfile(userId)
                }
            }
            NotificationActionType.OPEN_CHAT -> {
                notification.getTargetId()?.let { matchId ->
                    onNavigateToChat(matchId)
                }
            }
            NotificationActionType.OPEN_MATCH -> {
                notification.getTargetId()?.let { matchId ->
                    onNavigateToMatch(matchId)
                }
            }
            NotificationActionType.OPEN_SUBSCRIPTION -> {
                onNavigateToSubscription()
            }
            NotificationActionType.OPEN_AI_CHAT -> {
                val profileId = notification.data["profileId"] ?: return@handleNotificationClick
                val interactionId = notification.data["interactionId"]
                onNavigateToAIChat(profileId, interactionId)
            }
            else -> {
                // Default action - do nothing
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notifications") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    // Filter button
                    IconButton(onClick = { showFilterMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "Filter"
                        )
                    }
                    
                    // Filter dropdown menu
                    DropdownMenu(
                        expanded = showFilterMenu,
                        onDismissRequest = { showFilterMenu = false }
                    ) {
                        // Smart prioritization toggle
                        val prioritizationEnabled by viewModel.prioritizationEnabled.collectAsState()
                        DropdownMenuItem(
                            text = { Text("Smart Prioritization") },
                            onClick = { viewModel.togglePrioritization() },
                            leadingIcon = {
                                if (prioritizationEnabled) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null
                                    )
                                }
                            }
                        )
                        
                        Divider()
                        
                        // Filter options
                        DropdownMenuItem(
                            text = { Text("All") },
                            onClick = {
                                viewModel.setFilter(null)
                                showFilterMenu = false
                            },
                            leadingIcon = {
                                if (selectedFilter == null) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null
                                    )
                                }
                            }
                        )
                        
                        NotificationType.values().forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.name.lowercase().replaceFirstChar { it.uppercase() }.replace("_", " ")) },
                                onClick = {
                                    viewModel.setFilter(type)
                                    showFilterMenu = false
                                },
                                leadingIcon = {
                                    if (selectedFilter == type) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null
                                        )
                                    }
                                }
                            )
                        }
                    }
                    
                    // Notification settings button
                    IconButton(onClick = { viewModel.toggleSettings() }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Notification Settings"
                        )
                    }
                    
                    // Delete all button
                    IconButton(onClick = { showDeleteConfirmation = true }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete All"
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
            // Main content
            when (uiState) {
                is NotificationsUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                
                is NotificationsUiState.Empty -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = null,
                                modifier = Modifier.size(72.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No notifications",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "You don't have any notifications yet",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
                
                is NotificationsUiState.Error -> {
                    val error = (uiState as NotificationsUiState.Error).message
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Error loading notifications",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = error,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            TextButton(onClick = { viewModel.clearErrors() }) {
                                Text("Try Again")
                            }
                        }
                    }
                }
                
                is NotificationsUiState.Success -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        if (selectedFilter != null) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                ) {
                                    FilterChip(
                                        selected = true,
                                        onClick = { viewModel.setFilter(null) },
                                        label = {
                                            Text(
                                                "Filter: ${
                                                    selectedFilter.name.lowercase().replaceFirstChar { it.uppercase() }
                                                        .replace("_", " ")
                                                }"
                                            )
                                        },
                                        modifier = Modifier.padding(end = 8.dp)
                                    )
                                }
                                Divider(modifier = Modifier.padding(vertical = 8.dp))
                            }
                        }
                        
                        items(notifications) { notification ->
                            NotificationItem(
                                notification = notification,
                                onClick = { handleNotificationClick(notification) },
                                onDelete = { viewModel.deleteNotification(notification.id) }
                            )
                        }
                    }
                }
            }
            
            // Settings dialog
            if (isSettingsOpen) {
                notificationSettings?.let { settings ->
                    NotificationSettingsDialog(
                        settings = settings,
                        onDismiss = { viewModel.toggleSettings() },
                        onSettingToggle = { settingName -> viewModel.toggleSetting(settingName) },
                        onSendTestNotification = { type -> viewModel.sendTestNotification(type) }
                    )
                }
            }
            
            // Delete confirmation dialog
            if (showDeleteConfirmation) {
                AlertDialog(
                    onDismissRequest = { showDeleteConfirmation = false },
                    title = { Text("Delete All Notifications") },
                    text = { Text("Are you sure you want to delete all notifications? This action cannot be undone.") },
                    confirmButton = {
                        TextButton(onClick = { viewModel.deleteAllNotifications() }) {
                            Text("Delete All")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteConfirmation = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}

/**
 * Individual notification item
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationItem(
    notification: Notification,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val animatedVisibility = remember { MutableTransitionState(false).apply { targetState = true } }
    
    AnimatedVisibility(
        visibleState = animatedVisibility,
        enter = fadeIn(animationSpec = tween(300)) + 
                slideInVertically(animationSpec = tween(300)) { it / 2 },
        exit = fadeOut(animationSpec = tween(300))
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            onClick = onClick,
            colors = CardDefaults.cardColors(
                containerColor = if (!notification.read) {
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
                // Notification type indicator
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(getNotificationTypeColor(notification.type))
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getNotificationTypeIcon(notification.type),
                        contentDescription = null,
                        tint = Color.White
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                // Notification content
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
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = formatDate(notification.createdAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                
                // Delete button
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Notification"
                    )
                }
            }
        }
    }
}

/**
 * Settings dialog for notification preferences
 */
@Composable
fun NotificationSettingsDialog(
    settings: PushNotificationSettings,
    onDismiss: () -> Unit,
    onSettingToggle: (String) -> Unit,
    onSendTestNotification: (NotificationType) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Notification Settings") },
        text = {
            LazyColumn {
                item {
                    NotificationSettingToggle(
                        title = "Push Notifications",
                        description = "Enable all push notifications",
                        isChecked = settings.enabled,
                        onToggle = { onSettingToggle("enabled") }
                    )
                    
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    Text(
                        text = "Notification Types",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                
                item {
                    NotificationSettingToggle(
                        title = "Matches",
                        description = "Get notified when you have a new match",
                        isChecked = settings.matchEnabled,
                        onToggle = { onSettingToggle("matchEnabled") },
                        enabled = settings.enabled
                    )
                }
                
                item {
                    NotificationSettingToggle(
                        title = "Messages",
                        description = "Get notified of new messages",
                        isChecked = settings.messageEnabled,
                        onToggle = { onSettingToggle("messageEnabled") },
                        enabled = settings.enabled
                    )
                }
                
                item {
                    NotificationSettingToggle(
                        title = "Likes",
                        description = "Get notified when someone likes your profile",
                        isChecked = settings.likeEnabled,
                        onToggle = { onSettingToggle("likeEnabled") },
                        enabled = settings.enabled
                    )
                }
                
                item {
                    NotificationSettingToggle(
                        title = "Super Likes",
                        description = "Get notified when someone super likes your profile",
                        isChecked = settings.superLikeEnabled,
                        onToggle = { onSettingToggle("superLikeEnabled") },
                        enabled = settings.enabled
                    )
                }
                
                item {
                    NotificationSettingToggle(
                        title = "Profile Visits",
                        description = "Get notified when someone visits your profile",
                        isChecked = settings.visitEnabled,
                        onToggle = { onSettingToggle("visitEnabled") },
                        enabled = settings.enabled
                    )
                }
                
                item {
                    NotificationSettingToggle(
                        title = "Subscriptions",
                        description = "Get notified about subscription updates",
                        isChecked = settings.subscriptionEnabled,
                        onToggle = { onSettingToggle("subscriptionEnabled") },
                        enabled = settings.enabled
                    )
                }
                
                item {
                    NotificationSettingToggle(
                        title = "Payments",
                        description = "Get notified about payment updates",
                        isChecked = settings.paymentEnabled,
                        onToggle = { onSettingToggle("paymentEnabled") },
                        enabled = settings.enabled
                    )
                }
                
                item {
                    NotificationSettingToggle(
                        title = "System Updates",
                        description = "Get notified about system updates",
                        isChecked = settings.systemEnabled,
                        onToggle = { onSettingToggle("systemEnabled") },
                        enabled = settings.enabled
                    )
                }
                
                item {
                    NotificationSettingToggle(
                        title = "Offers",
                        description = "Get notified about offers from matches",
                        isChecked = settings.offerEnabled,
                        onToggle = { onSettingToggle("offerEnabled") },
                        enabled = settings.enabled
                    )
                }
                
                item {
                    NotificationSettingToggle(
                        title = "Events",
                        description = "Get notified about events",
                        isChecked = settings.eventEnabled,
                        onToggle = { onSettingToggle("eventEnabled") },
                        enabled = settings.enabled
                    )
                }
                
                item {
                    NotificationSettingToggle(
                        title = "Verification",
                        description = "Get notified about verification updates",
                        isChecked = settings.verificationEnabled,
                        onToggle = { onSettingToggle("verificationEnabled") },
                        enabled = settings.enabled
                    )
                }
                
                item {
                    NotificationSettingToggle(
                        title = "Points & Rewards",
                        description = "Get notified about points and rewards",
                        isChecked = settings.pointsEnabled,
                        onToggle = { onSettingToggle("pointsEnabled") },
                        enabled = settings.enabled
                    )
                }
                
                item {
                    NotificationSettingToggle(
                        title = "Calls",
                        description = "Get notified about audio and video calls",
                        isChecked = settings.callEnabled,
                        onToggle = { onSettingToggle("callEnabled") },
                        enabled = settings.enabled
                    )
                }
                
                item {
                    NotificationSettingToggle(
                        title = "AI Interactions",
                        description = "Get notified about AI interactions",
                        isChecked = settings.aiInteractionEnabled,
                        onToggle = { onSettingToggle("aiInteractionEnabled") },
                        enabled = settings.enabled
                    )
                }
                
                item {
                    NotificationSettingToggle(
                        title = "Profile Updates",
                        description = "Get notified about profile updates",
                        isChecked = settings.profileUpdateEnabled,
                        onToggle = { onSettingToggle("profileUpdateEnabled") },
                        enabled = settings.enabled
                    )
                }
                
                item {
                    NotificationSettingToggle(
                        title = "Admin Messages",
                        description = "Get notified about messages from administrators",
                        isChecked = settings.adminMessageEnabled,
                        onToggle = { onSettingToggle("adminMessageEnabled") },
                        enabled = settings.enabled
                    )
                }
                
                item {
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    Text(
                        text = "Alert Options",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                
                item {
                    NotificationSettingToggle(
                        title = "Vibrate",
                        description = "Vibrate on notification",
                        isChecked = settings.vibrate,
                        onToggle = { onSettingToggle("vibrate") },
                        enabled = settings.enabled
                    )
                }
                
                item {
                    NotificationSettingToggle(
                        title = "Sound",
                        description = "Play sound on notification",
                        isChecked = settings.sound,
                        onToggle = { onSettingToggle("sound") },
                        enabled = settings.enabled
                    )
                }
                
                item {
                    NotificationSettingToggle(
                        title = "Quiet Hours",
                        description = "Don't disturb during specified hours",
                        isChecked = settings.quietHoursEnabled,
                        onToggle = { onSettingToggle("quietHoursEnabled") },
                        enabled = settings.enabled
                    )
                }
                
                item {
                    // Time range selector for quiet hours
                    if (settings.quietHoursEnabled && settings.enabled) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Quiet from ${settings.quietHoursStart}:00 to ${settings.quietHoursEnd}:00",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                
                item {
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    Text(
                        text = "Test Notifications",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    
                    TextButton(
                        onClick = { onSendTestNotification(NotificationType.MESSAGE) },
                        enabled = settings.enabled && settings.messageEnabled
                    ) {
                        Text("Send Test Message Notification")
                    }
                    
                    TextButton(
                        onClick = { onSendTestNotification(NotificationType.MATCH) },
                        enabled = settings.enabled && settings.matchEnabled
                    ) {
                        Text("Send Test Match Notification")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

/**
 * Toggle switch for notification settings
 */
@Composable
fun NotificationSettingToggle(
    title: String,
    description: String,
    isChecked: Boolean,
    onToggle: () -> Unit,
    enabled: Boolean = true
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        onClick = if (enabled) onToggle else { {} },
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
            
            Switch(
                checked = isChecked,
                onCheckedChange = { onToggle() },
                enabled = enabled
            )
        }
    }
}

/**
 * Get color for notification type
 */
@Composable
fun getNotificationTypeColor(type: NotificationType): Color {
    return when (type) {
        NotificationType.MATCH -> MaterialTheme.colorScheme.tertiary
        NotificationType.MESSAGE -> MaterialTheme.colorScheme.primary
        NotificationType.LIKE -> Color(0xFFE91E63)
        NotificationType.SUPER_LIKE -> Color(0xFF9C27B0)
        NotificationType.VISIT -> Color(0xFF2196F3)
        NotificationType.SUBSCRIPTION -> Color(0xFF4CAF50)
        NotificationType.PAYMENT -> Color(0xFFFF9800)
        NotificationType.SYSTEM -> Color(0xFF607D8B)
        NotificationType.OFFER -> Color(0xFF00BCD4)
        NotificationType.EVENT -> Color(0xFFFF5722)
        NotificationType.VERIFICATION -> Color(0xFF3F51B5)
        NotificationType.POINTS -> Color(0xFFFFEB3B)
        NotificationType.CALL -> Color(0xFF009688)
        NotificationType.AI_INTERACTION -> Color(0xFF673AB7)
        NotificationType.PROFILE_UPDATE -> Color(0xFF8BC34A)
        NotificationType.ADMIN_MESSAGE -> Color(0xFFF44336)
    }
}

/**
 * Get icon for notification type
 */
@Composable
fun getNotificationTypeIcon(type: NotificationType): ImageVector {
    // In a real app, you would have different icons for each type
    // For now, just use the notification icon for all types
    return Icons.Default.Notifications
}

/**
 * Format date for display
 */
fun formatDate(date: Date): String {
    val now = Date()
    val diff = now.time - date.time
    
    return when {
        diff < 60 * 1000 -> "Just now"
        diff < 60 * 60 * 1000 -> "${diff / (60 * 1000)} minutes ago"
        diff < 24 * 60 * 60 * 1000 -> "${diff / (60 * 60 * 1000)} hours ago"
        diff < 48 * 60 * 60 * 1000 -> "Yesterday"
        else -> SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(date)
    }
}