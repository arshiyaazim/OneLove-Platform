package com.kilagee.onelove.ui.notifications

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.kilagee.onelove.domain.model.Resource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(
    navController: NavController,
    viewModel: NotificationViewModel = hiltViewModel()
) {
    var enableMatchNotifications by remember { mutableStateOf(true) }
    var enableMessageNotifications by remember { mutableStateOf(true) }
    var enableOfferNotifications by remember { mutableStateOf(true) }
    
    val operationState by viewModel.operationState.collectAsState()
    
    // Effect to load settings from user preferences
    LaunchedEffect(Unit) {
        // In a real implementation, we would load these values from the repository
        // For now, we'll use default values
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notification Settings") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Notification Preferences",
                style = MaterialTheme.typography.titleLarge
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Match notifications toggle
            NotificationSettingItem(
                title = "Match Notifications",
                description = "Receive notifications when you get new matches",
                icon = Icons.Filled.Favorite,
                isEnabled = enableMatchNotifications,
                onToggleChange = { enableMatchNotifications = it }
            )
            
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            // Message notifications toggle
            NotificationSettingItem(
                title = "Message Notifications",
                description = "Receive notifications for new messages",
                icon = Icons.Filled.Chat,
                isEnabled = enableMessageNotifications,
                onToggleChange = { enableMessageNotifications = it }
            )
            
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            // Offer notifications toggle
            NotificationSettingItem(
                title = "Offer Notifications",
                description = "Receive notifications for offers and responses",
                icon = Icons.Filled.CardGiftcard,
                isEnabled = enableOfferNotifications,
                onToggleChange = { enableOfferNotifications = it }
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Button(
                onClick = {
                    viewModel.updateNotificationSettings(
                        enableMatchNotifications = enableMatchNotifications,
                        enableMessageNotifications = enableMessageNotifications,
                        enableOfferNotifications = enableOfferNotifications
                    )
                },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("Save Preferences")
            }
            
            // Status messages
            Spacer(modifier = Modifier.height(16.dp))
            
            when (val state = operationState) {
                is Resource.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
                is Resource.Success -> {
                    Text(
                        text = "Settings updated successfully",
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                    
                    // Clear the operation state after a short delay
                    LaunchedEffect(state) {
                        kotlinx.coroutines.delay(3000)
                        viewModel.clearOperationState()
                    }
                }
                is Resource.Error -> {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
                else -> {}
            }
        }
    }
}

@Composable
fun NotificationSettingItem(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isEnabled: Boolean,
    onToggleChange: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Switch(
            checked = isEnabled,
            onCheckedChange = onToggleChange
        )
    }
}