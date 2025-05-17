package com.kilagee.onelove.ui.screens.discover

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kilagee.onelove.data.model.User
import com.kilagee.onelove.ui.components.EmptyProfileCard
import com.kilagee.onelove.ui.components.ProfileCard
import kotlin.math.roundToInt

/**
 * Discover screen for browsing potential matches
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverScreen(
    onProfileClick: (String) -> Unit,
    onChatClick: () -> Unit,
    onMyProfileClick: () -> Unit,
    viewModel: DiscoverViewModel = hiltViewModel()
) {
    val discoverState by viewModel.discoverState.collectAsState()
    val potentialMatches by viewModel.potentialMatches.collectAsState()
    val currentUser by viewModel.currentUserProfile.collectAsState()
    
    val snackbarHostState = remember { SnackbarHostState() }
    
    val density = LocalDensity.current
    
    // Current profile being viewed
    val currentProfile = potentialMatches.firstOrNull()
    
    // Animation state
    var offsetX by remember { mutableStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    
    // Reset offset when new profile is loaded
    LaunchedEffect(currentProfile) {
        offsetX = 0f
    }
    
    // Show snackbar on error
    LaunchedEffect(discoverState) {
        if (discoverState is DiscoverState.Error) {
            snackbarHostState.showSnackbar(
                (discoverState as DiscoverState.Error).message
            )
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "OneLove",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.primary
                ),
                actions = {
                    androidx.compose.material3.IconButton(
                        onClick = onChatClick
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChatBubbleOutline,
                            contentDescription = "Messages"
                        )
                    }
                    
                    androidx.compose.material3.IconButton(
                        onClick = onMyProfileClick
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "My Profile"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            if (currentUser != null && discoverState !is DiscoverState.Loading) {
                ExtendedFloatingActionButton(
                    onClick = { onMyProfileClick() },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings"
                        )
                    },
                    text = { Text("Discovery Settings") }
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            when (discoverState) {
                is DiscoverState.Loading -> {
                    // Loading state
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                        Text(
                            text = "Finding your perfect matches...",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(top = 16.dp)
                        )
                    }
                }
                
                is DiscoverState.Empty -> {
                    // No matches found
                    EmptyProfileCard(
                        message = "No more profiles to show right now.\nCheck back later!",
                        onRefresh = { viewModel.refreshMatches() }
                    )
                }
                
                is DiscoverState.Error -> {
                    // Error state
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Error",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        Text(
                            text = "Something went wrong",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = (discoverState as DiscoverState.Error).message,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
                        )
                        androidx.compose.material3.Button(
                            onClick = { viewModel.refreshMatches() }
                        ) {
                            Text("Try Again")
                        }
                    }
                }
                
                else -> {
                    // Display profile cards
                    AnimatedVisibility(
                        visible = currentProfile != null,
                        enter = fadeIn(),
                        exit = fadeOut(animationSpec = tween(durationMillis = 300))
                    ) {
                        if (currentProfile != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .pointerInput(Unit) {
                                        detectDragGestures(
                                            onDragStart = { isDragging = true },
                                            onDragEnd = {
                                                isDragging = false
                                                if (offsetX > 150) {
                                                    // Swiped right (like)
                                                    viewModel.likeUser(currentProfile)
                                                } else if (offsetX < -150) {
                                                    // Swiped left (dislike)
                                                    viewModel.dislikeUser(currentProfile)
                                                }
                                                offsetX = 0f
                                            },
                                            onDragCancel = {
                                                isDragging = false
                                                offsetX = 0f
                                            },
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                offsetX += dragAmount.x
                                                
                                                // Limit drag distance
                                                offsetX = offsetX.coerceIn(-400f, 400f)
                                            }
                                        )
                                    }
                            ) {
                                val rotationAngle = (offsetX / 20)
                                
                                Box(
                                    modifier = Modifier.offset {
                                        IntOffset(offsetX.roundToInt(), 0)
                                    }
                                ) {
                                    ProfileCard(
                                        user = currentProfile,
                                        onLike = { viewModel.likeUser(currentProfile) },
                                        onDislike = { viewModel.dislikeUser(currentProfile) },
                                        onInfoClick = { onProfileClick(currentProfile.id) }
                                    )
                                }
                                
                                // Like overlay
                                AnimatedVisibility(
                                    visible = offsetX > 80,
                                    enter = fadeIn() + slideInHorizontally { it },
                                    exit = fadeOut() + slideOutHorizontally { it }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(24.dp),
                                        contentAlignment = Alignment.TopStart
                                    ) {
                                        androidx.compose.material3.Surface(
                                            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                        ) {
                                            Text(
                                                text = "LIKE",
                                                style = MaterialTheme.typography.bodyLarge.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White
                                                ),
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                            )
                                        }
                                    }
                                }
                                
                                // Dislike overlay
                                AnimatedVisibility(
                                    visible = offsetX < -80,
                                    enter = fadeIn() + slideInHorizontally { -it },
                                    exit = fadeOut() + slideOutHorizontally { -it }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(24.dp),
                                        contentAlignment = Alignment.TopEnd
                                    ) {
                                        androidx.compose.material3.Surface(
                                            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                                            color = Color.Red.copy(alpha = 0.8f)
                                        ) {
                                            Text(
                                                text = "PASS",
                                                style = MaterialTheme.typography.bodyLarge.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White
                                                ),
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    // No profiles
                    AnimatedVisibility(
                        visible = potentialMatches.isEmpty() && discoverState !is DiscoverState.Loading,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        EmptyProfileCard(
                            message = "No more profiles to show right now.\nCheck back later!",
                            onRefresh = { viewModel.refreshMatches() }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Profile details screen
 */
@Composable
fun ProfileDetailScreen(
    userId: String,
    onBackClick: () -> Unit,
    onChatClick: (String) -> Unit,
    onCallClick: (String, Boolean) -> Unit,
    viewModel: ProfileDetailViewModel = hiltViewModel()
) {
    // TODO: Implement profile detail screen
    Text(text = "Profile Detail Screen for user $userId")
}