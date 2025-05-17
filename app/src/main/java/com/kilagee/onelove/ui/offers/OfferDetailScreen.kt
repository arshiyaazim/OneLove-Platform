package com.kilagee.onelove.ui.offers

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.kilagee.onelove.data.model.Offer
import com.kilagee.onelove.data.model.OfferStatus
import com.kilagee.onelove.data.model.OfferType
import com.kilagee.onelove.domain.model.Resource
import com.kilagee.onelove.navigation.Screen
import com.kilagee.onelove.ui.components.ErrorStateView
import com.kilagee.onelove.ui.components.LoadingStateView
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfferDetailScreen(
    navController: NavController,
    offerId: String,
    viewModel: OfferViewModel = hiltViewModel()
) {
    val currentOfferState by viewModel.currentOfferState.collectAsState()
    val operationState by viewModel.operationState.collectAsState()
    
    // Load offer details when screen is shown
    LaunchedEffect(offerId) {
        viewModel.loadOfferDetails(offerId)
    }
    
    // Handle operation result
    LaunchedEffect(operationState) {
        if (operationState is Resource.Success) {
            // Action completed successfully
            viewModel.loadOfferDetails(offerId)
            viewModel.clearOperationState()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Offer Details") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
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
            when (val state = currentOfferState) {
                is Resource.Loading -> {
                    LoadingStateView()
                }
                is Resource.Success -> {
                    val offer = state.data
                    OfferDetailContent(
                        offer = offer,
                        navController = navController,
                        onAccept = { viewModel.acceptOffer(offer.id) },
                        onDecline = { viewModel.declineOffer(offer.id) },
                        onCancel = { viewModel.cancelOffer(offer.id) },
                        onComplete = { viewModel.completeOffer(offer.id) }
                    )
                }
                is Resource.Error -> {
                    ErrorStateView(
                        message = state.message,
                        onRetryClick = { viewModel.loadOfferDetails(offerId) }
                    )
                }
                else -> {
                    // Empty state
                }
            }
            
            // Operation state overlay
            when (val state = operationState) {
                is Resource.Loading -> {
                    LoadingStateView()
                }
                is Resource.Error -> {
                    ErrorStateView(
                        message = state.message,
                        onRetryClick = { viewModel.clearOperationState() }
                    )
                }
                else -> {}
            }
        }
    }
}

@Composable
fun OfferDetailContent(
    offer: Offer,
    navController: NavController,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    onCancel: () -> Unit,
    onComplete: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Status badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            StatusBadge(status = offer.status)
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // User info
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Profile image
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(offer.senderProfileImageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "Profile Picture",
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(Color.LightGray),
                contentScale = ContentScale.Crop
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column {
                // Name
                Text(
                    text = offer.senderName ?: "Unknown",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                // Offer direction
                Text(
                    text = "Offering to: ${offer.receiverName ?: "Unknown"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Title
        Text(
            text = offer.title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Details card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Offer type
                DetailItem(
                    icon = when (offer.type) {
                        OfferType.COFFEE -> Icons.Filled.Coffee
                        OfferType.DINNER -> Icons.Filled.Restaurant
                        OfferType.MOVIE -> Icons.Filled.Movie
                        OfferType.DRINKS -> Icons.Filled.LocalBar
                        OfferType.WALK -> Icons.Filled.DirectionsWalk
                        OfferType.VIDEO_CALL -> Icons.Filled.VideoCall
                        OfferType.TRAVEL -> Icons.Filled.Flight
                        else -> Icons.Filled.Event
                    },
                    label = "Type",
                    value = offer.type.name.lowercase().replaceFirstChar { it.uppercase() }
                )
                
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                
                // Date
                if (offer.proposedTime != null) {
                    val dateFormatter = SimpleDateFormat("EEEE, MMMM dd, yyyy 'at' h:mm a", Locale.getDefault())
                    DetailItem(
                        icon = Icons.Filled.CalendarToday,
                        label = "Date & Time",
                        value = dateFormatter.format(offer.proposedTime)
                    )
                    
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                }
                
                // Location
                if (offer.location.isNotBlank()) {
                    DetailItem(
                        icon = Icons.Filled.LocationOn,
                        label = "Location",
                        value = offer.location
                    )
                    
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                }
                
                // Points
                if (offer.pointsOffered > 0) {
                    DetailItem(
                        icon = Icons.Filled.Star,
                        label = "Points Offered",
                        value = "${offer.pointsOffered} points"
                    )
                    
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                }
                
                // Created date
                val createdDateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                DetailItem(
                    icon = Icons.Filled.Schedule,
                    label = "Created",
                    value = createdDateFormatter.format(offer.createdAt)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Description
        if (offer.description.isNotBlank()) {
            Text(
                text = "Description",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = offer.description,
                style = MaterialTheme.typography.bodyLarge
            )
            
            Spacer(modifier = Modifier.height(24.dp))
        }
        
        // Action buttons based on offer status
        when (offer.status) {
            OfferStatus.PENDING -> {
                // For receiver: Accept/Decline
                // For sender: Cancel
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Check if current user is receiver or sender
                    val isReceiver = offer.receiverName.isNullOrBlank()
                    
                    if (isReceiver) {
                        // Receiver actions
                        OutlinedButton(
                            onClick = onDecline,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Decline")
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        Button(
                            onClick = onAccept,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.Check, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Accept")
                        }
                    } else {
                        // Sender actions
                        OutlinedButton(
                            onClick = onCancel,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(Icons.Filled.Cancel, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Cancel Offer")
                        }
                    }
                }
            }
            OfferStatus.ACCEPTED -> {
                // For both: Mark as completed
                Button(
                    onClick = onComplete,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Done, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Mark as Completed")
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Message button
                OutlinedButton(
                    onClick = { navController.navigate(Screen.Chat.route) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Chat, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Send Message")
                }
            }
            else -> {
                // No actions for completed, declined, or cancelled offers
            }
        }
    }
}

@Composable
fun DetailItem(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}