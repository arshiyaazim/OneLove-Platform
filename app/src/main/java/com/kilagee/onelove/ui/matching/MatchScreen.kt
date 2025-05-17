package com.kilagee.onelove.ui.matching

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.kilagee.onelove.R
import com.kilagee.onelove.domain.matching.MatchEngine
import com.kilagee.onelove.domain.model.Resource
import com.kilagee.onelove.navigation.Screen
import com.kilagee.onelove.ui.theme.Verified
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchScreen(
    navController: NavController,
    viewModel: MatchViewModel = hiltViewModel()
) {
    val potentialMatchesState by viewModel.potentialMatchesState.collectAsState()
    val matchesState by viewModel.matchesState.collectAsState()
    val likeState by viewModel.likeState.collectAsState()
    
    // State for the current card position
    val offsetX = remember { mutableStateOf(0f) }
    val offsetY = remember { mutableStateOf(0f) }
    val rotation = remember { mutableStateOf(0f) }
    var isCardVisible by remember { mutableStateOf(true) }
    
    // Handle match result
    LaunchedEffect(likeState) {
        if (likeState is Resource.Success) {
            val isMatch = (likeState as Resource.Success<Boolean>).data
            if (isMatch) {
                // Show match dialog or animation
                // For now, we'll just clear the state
                viewModel.clearLikeState()
            }
        }
    }
    
    // Current user to display
    val currentMatch = remember { mutableStateOf<MatchEngine.MatchResult?>(null) }
    
    // Update current match when potential matches change
    LaunchedEffect(potentialMatchesState) {
        if (potentialMatchesState is Resource.Success) {
            currentMatch.value = viewModel.getCurrentPotentialMatch()
        }
    }
    
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Discover") },
                actions = {
                    IconButton(onClick = { navController.navigate(Screen.Chat.route) }) {
                        Icon(Icons.Filled.Chat, contentDescription = "Messages")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = true,
                    onClick = { /* Already on Home */ },
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
                    selected = false,
                    onClick = { navController.navigate(Screen.Offers.route) },
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (potentialMatchesState) {
                is Resource.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is Resource.Success -> {
                    val matches = (potentialMatchesState as Resource.Success<List<MatchEngine.MatchResult>>).data
                    
                    if (matches.isEmpty()) {
                        // No matches found
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.SearchOff,
                                contentDescription = null,
                                modifier = Modifier.size(72.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Text(
                                text = "No potential matches found",
                                style = MaterialTheme.typography.headlineSmall,
                                textAlign = TextAlign.Center
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                text = "Try adjusting your search preferences or check back later",
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            Button(onClick = { viewModel.loadPotentialMatches(minMatchPercentage = 30) }) {
                                Text("Expand Search")
                            }
                        }
                    } else {
                        // Current match to display
                        val current = currentMatch.value
                        
                        if (current != null) {
                            // Card with profile
                            val density = LocalDensity.current
                            
                            AnimatedVisibility(
                                visible = isCardVisible,
                                exit = slideOutHorizontally(
                                    targetOffsetX = { if (offsetX.value > 0) it else -it },
                                    animationSpec = tween(300)
                                ) + fadeOut(animationSpec = tween(300))
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth(0.9f)
                                            .fillMaxHeight(0.7f)
                                            .offset { IntOffset(offsetX.value.roundToInt(), offsetY.value.roundToInt()) }
                                            .pointerInput(Unit) {
                                                detectDragGestures(
                                                    onDragEnd = {
                                                        // If dragged far enough, like or dislike
                                                        val screenWidth = with(density) { size.width.toPx() }
                                                        if (offsetX.value > screenWidth / 4) {
                                                            // Swiped right - Like
                                                            isCardVisible = false
                                                            viewModel.likeCurrentUser()
                                                        } else if (offsetX.value < -screenWidth / 4) {
                                                            // Swiped left - Dislike
                                                            isCardVisible = false
                                                            viewModel.skipCurrentUser()
                                                        } else {
                                                            // Not dragged far enough, reset position
                                                            offsetX.value = 0f
                                                            offsetY.value = 0f
                                                            rotation.value = 0f
                                                        }
                                                    },
                                                    onDrag = { change, dragAmount ->
                                                        change.consume()
                                                        offsetX.value += dragAmount.x
                                                        offsetY.value += dragAmount.y
                                                        
                                                        // Calculate rotation based on horizontal drag
                                                        val screenWidth = with(density) { size.width.toPx() }
                                                        rotation.value = (offsetX.value / screenWidth) * 15f // max 15 degrees
                                                    }
                                                )
                                            }
                                    ) {
                                        Box(
                                            modifier = Modifier.fillMaxSize()
                                        ) {
                                            // Profile Image
                                            AsyncImage(
                                                model = ImageRequest.Builder(LocalContext.current)
                                                    .data(current.user.profilePictureUrl)
                                                    .crossfade(true)
                                                    .placeholder(R.drawable.ic_launcher_foreground)
                                                    .build(),
                                                contentDescription = "Profile Picture",
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                            
                                            // Gradient overlay for text readability
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(
                                                        androidx.compose.ui.graphics.Brush.verticalGradient(
                                                            colors = listOf(
                                                                Color.Transparent,
                                                                Color.Black.copy(alpha = 0.7f)
                                                            ),
                                                            startY = 300f,
                                                            endY = 900f
                                                        )
                                                    )
                                            )
                                            
                                            // Like/Dislike indicators
                                            AnimatedVisibility(
                                                visible = offsetX.value > 100,
                                                enter = fadeIn(),
                                                exit = fadeOut()
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .padding(16.dp)
                                                        .align(Alignment.TopStart)
                                                ) {
                                                    Surface(
                                                        shape = RoundedCornerShape(8.dp),
                                                        color = Verified.copy(alpha = 0.8f)
                                                    ) {
                                                        Text(
                                                            text = "LIKE",
                                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                                            color = Color.White,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                            }
                                            
                                            AnimatedVisibility(
                                                visible = offsetX.value < -100,
                                                enter = fadeIn(),
                                                exit = fadeOut()
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .padding(16.dp)
                                                        .align(Alignment.TopEnd)
                                                ) {
                                                    Surface(
                                                        shape = RoundedCornerShape(8.dp),
                                                        color = Color.Red.copy(alpha = 0.8f)
                                                    ) {
                                                        Text(
                                                            text = "NOPE",
                                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                                            color = Color.White,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                            }
                                            
                                            // User Info at bottom
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(16.dp)
                                                    .align(Alignment.BottomStart)
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = "${current.user.firstName}, ${current.user.age}",
                                                        style = MaterialTheme.typography.headlineMedium,
                                                        color = Color.White,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    
                                                    if (current.user.verificationStatus == com.kilagee.onelove.data.model.VerificationStatus.FULLY_VERIFIED) {
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Icon(
                                                            imageVector = Icons.Filled.Verified,
                                                            contentDescription = "Verified",
                                                            tint = Verified,
                                                            modifier = Modifier.size(24.dp)
                                                        )
                                                    }
                                                    
                                                    Spacer(modifier = Modifier.weight(1f))
                                                    
                                                    // Match percentage
                                                    Text(
                                                        text = "${current.matchPercentage}% Match",
                                                        style = MaterialTheme.typography.titleMedium,
                                                        color = Color.White
                                                    )
                                                }
                                                
                                                Spacer(modifier = Modifier.height(4.dp))
                                                
                                                Text(
                                                    text = current.user.bio,
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    color = Color.White.copy(alpha = 0.9f),
                                                    maxLines = 3,
                                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                                )
                                                
                                                Spacer(modifier = Modifier.height(8.dp))
                                                
                                                // Interests
                                                Row(
                                                    modifier = Modifier.horizontalScroll(rememberScrollState())
                                                ) {
                                                    current.user.interests.take(5).forEach { interest ->
                                                        Surface(
                                                            shape = RoundedCornerShape(16.dp),
                                                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f),
                                                            modifier = Modifier.padding(end = 8.dp)
                                                        ) {
                                                            Text(
                                                                text = interest,
                                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                                            )
                                                        }
                                                    }
                                                }
                                                
                                                Spacer(modifier = Modifier.height(16.dp))
                                                
                                                // Location
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.LocationOn,
                                                        contentDescription = "Location",
                                                        tint = Color.White
                                                    )
                                                    
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    
                                                    Text(
                                                        text = "${current.user.city}, ${current.user.country}",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = Color.White
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            
                            // When animation completes, reset the card
                            LaunchedEffect(isCardVisible) {
                                if (!isCardVisible) {
                                    // Short delay to complete animation
                                    kotlinx.coroutines.delay(300)
                                    // Reset position
                                    offsetX.value = 0f
                                    offsetY.value = 0f
                                    rotation.value = 0f
                                    // Update current match
                                    currentMatch.value = viewModel.getCurrentPotentialMatch()
                                    // Show new card
                                    isCardVisible = true
                                }
                            }
                        }
                        
                        // Action buttons at bottom
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .align(Alignment.BottomCenter),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            // Dislike button
                            FloatingActionButton(
                                onClick = {
                                    isCardVisible = false
                                    viewModel.skipCurrentUser()
                                },
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Dislike",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                            
                            // Like button
                            FloatingActionButton(
                                onClick = {
                                    isCardVisible = false
                                    viewModel.likeCurrentUser()
                                },
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Favorite,
                                    contentDescription = "Like",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
                is Resource.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Error,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = (potentialMatchesState as Resource.Error).message,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Button(onClick = { viewModel.loadPotentialMatches() }) {
                            Text("Retry")
                        }
                    }
                }
                else -> {}
            }
            
            // Show Match Modal
            if (likeState is Resource.Success && (likeState as Resource.Success<Boolean>).data) {
                MatchDialog(
                    onDismiss = { viewModel.clearLikeState() },
                    onViewMatches = {
                        viewModel.clearLikeState()
                        navController.navigate(Screen.Chat.route)
                    }
                )
            }
        }
    }
}

@Composable
fun MatchDialog(
    onDismiss: () -> Unit,
    onViewMatches: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "It's a Match!",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
            }
        },
        text = {
            Text(
                text = "You both liked each other! Start a conversation now.",
                textAlign = TextAlign.Center
            )
        },
        confirmButton = {
            Button(
                onClick = onViewMatches,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Send a Message")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Continue Browsing")
            }
        }
    )
}