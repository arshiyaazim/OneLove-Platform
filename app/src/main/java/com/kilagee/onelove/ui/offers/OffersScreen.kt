package com.kilagee.onelove.ui.offers

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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.kilagee.onelove.data.model.Offer
import com.kilagee.onelove.data.model.OfferStatus
import com.kilagee.onelove.domain.model.Resource
import com.kilagee.onelove.navigation.Screen
import com.kilagee.onelove.ui.components.EmptyStateView
import com.kilagee.onelove.ui.components.ErrorStateView
import com.kilagee.onelove.ui.components.LoadingStateView
import com.kilagee.onelove.ui.theme.PrimaryLight
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OffersScreen(
    navController: NavController,
    viewModel: OfferViewModel = hiltViewModel()
) {
    val allOffersState by viewModel.allOffersState.collectAsState()
    val sentOffersState by viewModel.sentOffersState.collectAsState()
    val receivedOffersState by viewModel.receivedOffersState.collectAsState()
    val pendingOffersCountState by viewModel.pendingOffersCountState.collectAsState()
    
    // Tab selection state
    var selectedTabIndex by remember { mutableStateOf(0) }
    
    // Refresh data when screen is shown
    LaunchedEffect(Unit) {
        viewModel.loadAllData()
    }
    
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Offers") },
                actions = {
                    // Create new offer button
                    IconButton(onClick = { navController.navigate(Screen.CreateOffer.route) }) {
                        Icon(Icons.Filled.Add, contentDescription = "Create Offer")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = false,
                    onClick = { navController.navigate(Screen.Home.route) { popUpTo(Screen.Home.route) { inclusive = true } } },
                    icon = { Icon(Icons.Filled.Home, contentDescription = "Home") },
                    label = { Text("Home") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { navController.navigate(Screen.Chat.route) },
                    icon = { Icon(Icons.Filled.Chat, contentDescription = "Chat") },
                    label = { Text("Chat") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { navController.navigate(Screen.Profile.route) },
                    icon = { Icon(Icons.Filled.Person, contentDescription = "Profile") },
                    label = { Text("Profile") }
                )
                NavigationBarItem(
                    selected = true,
                    onClick = { /* Already on Offers */ },
                    icon = { Icon(Icons.Filled.List, contentDescription = "Offers") },
                    label = { Text("Offers") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { navController.navigate(Screen.Wallet.route) },
                    icon = { Icon(Icons.Filled.Wallet, contentDescription = "Wallet") },
                    label = { Text("Wallet") }
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tabs for filtering offers
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.background
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text("All") }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Received")
                            if (pendingOffersCountState is Resource.Success && pendingOffersCountState.data > 0) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Badge {
                                    Text(text = pendingOffersCountState.data.toString())
                                }
                            }
                        }
                    }
                )
                Tab(
                    selected = selectedTabIndex == 2,
                    onClick = { selectedTabIndex = 2 },
                    text = { Text("Sent") }
                )
            }
            
            // Content based on selected tab
            when (selectedTabIndex) {
                0 -> OffersContent(
                    offersState = allOffersState,
                    navController = navController,
                    emptyMessage = "No offers found"
                )
                1 -> OffersContent(
                    offersState = receivedOffersState,
                    navController = navController,
                    emptyMessage = "No received offers"
                )
                2 -> OffersContent(
                    offersState = sentOffersState,
                    navController = navController,
                    emptyMessage = "No sent offers"
                )
            }
        }
    }
}

@Composable
fun OffersContent(
    offersState: Resource<List<Offer>>,
    navController: NavController,
    emptyMessage: String
) {
    when (offersState) {
        is Resource.Loading -> {
            LoadingStateView()
        }
        is Resource.Success -> {
            val offers = offersState.data
            if (offers.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Filled.List,
                    message = emptyMessage,
                    actionText = "Create an Offer",
                    onActionClick = { navController.navigate(Screen.CreateOffer.route) }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(offers) { offer ->
                        OfferItem(
                            offer = offer,
                            onClick = { navController.navigate(Screen.OfferDetail.createRoute(offer.id)) }
                        )
                    }
                }
            }
        }
        is Resource.Error -> {
            ErrorStateView(
                message = offersState.message,
                onRetryClick = { /* Reload data */ }
            )
        }
    }
}

@Composable
fun OfferItem(
    offer: Offer,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Profile image of sender/receiver
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(if (offer.senderProfileImageUrl.isNullOrEmpty()) null else offer.senderProfileImageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "Profile Picture",
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(Color.LightGray),
                contentScale = ContentScale.Crop
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Offer title
                Text(
                    text = offer.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Date and type
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Offer type icon
                    val typeIcon = when (offer.type) {
                        com.kilagee.onelove.data.model.OfferType.COFFEE -> Icons.Filled.Coffee
                        com.kilagee.onelove.data.model.OfferType.DINNER -> Icons.Filled.Restaurant
                        com.kilagee.onelove.data.model.OfferType.MOVIE -> Icons.Filled.Movie
                        com.kilagee.onelove.data.model.OfferType.DRINKS -> Icons.Filled.LocalBar
                        com.kilagee.onelove.data.model.OfferType.WALK -> Icons.Filled.DirectionsWalk
                        com.kilagee.onelove.data.model.OfferType.VIDEO_CALL -> Icons.Filled.VideoCall
                        com.kilagee.onelove.data.model.OfferType.TRAVEL -> Icons.Filled.Flight
                        else -> Icons.Filled.Event
                    }
                    
                    Icon(
                        imageVector = typeIcon,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    
                    Spacer(modifier = Modifier.width(4.dp))
                    
                    // Offer type
                    Text(
                        text = offer.type.name.lowercase().replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    // Date
                    val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                    Text(
                        text = dateFormatter.format(offer.createdAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // From/To
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (offer.senderId == offer.receiverId) {
                        // Self offer
                        Text(
                            text = "Self offer",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        val isFromMe = offer.senderName.isNullOrEmpty()
                        Text(
                            text = if (isFromMe) "To: ${offer.receiverName ?: "Unknown"}" else "From: ${offer.senderName}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Status indicator
            StatusBadge(status = offer.status)
        }
    }
}

@Composable
fun StatusBadge(status: OfferStatus) {
    val (backgroundColor, textColor, statusText) = when (status) {
        OfferStatus.PENDING -> Triple(
            Color(0xFFFFF9C4),  // Light yellow
            Color(0xFF8D6E63),  // Brown text
            "Pending"
        )
        OfferStatus.ACCEPTED -> Triple(
            Color(0xFFC8E6C9),  // Light green
            Color(0xFF2E7D32),  // Dark green text
            "Accepted"
        )
        OfferStatus.DECLINED -> Triple(
            Color(0xFFFFCDD2),  // Light red
            Color(0xFFD32F2F),  // Red text
            "Declined"
        )
        OfferStatus.CANCELLED -> Triple(
            Color(0xFFE0E0E0),  // Light grey
            Color(0xFF616161),  // Grey text
            "Cancelled"
        )
        OfferStatus.COMPLETED -> Triple(
            Color(0xFFBBDEFB),  // Light blue
            Color(0xFF1976D2),  // Blue text
            "Completed"
        )
    }
    
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor
    ) {
        Text(
            text = statusText,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.bodySmall,
            color = textColor
        )
    }
}