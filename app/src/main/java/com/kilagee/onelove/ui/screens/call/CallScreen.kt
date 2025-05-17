package com.kilagee.onelove.ui.screens.call

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.SpeakerOff
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.kilagee.onelove.data.model.CallType
import com.kilagee.onelove.data.model.User
import com.kilagee.onelove.ui.LocalSnackbarHostState
import com.kilagee.onelove.ui.components.LoadingStateView
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/**
 * Call screen for audio/video calls
 */
@Composable
fun CallScreen(
    viewModel: CallViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val remoteUser by viewModel.remoteUser.collectAsState()
    val callDuration by viewModel.callDuration.collectAsState()
    val isMuted by viewModel.isMuted.collectAsState()
    val isVideoEnabled by viewModel.isVideoEnabled.collectAsState()
    val isSpeakerOn by viewModel.isSpeakerOn.collectAsState()
    val useFrontCamera by viewModel.useFrontCamera.collectAsState()
    val call by viewModel.call.collectAsState()
    
    val snackbarHostState = LocalSnackbarHostState.current
    
    // Update timer every second when connected
    LaunchedEffect(uiState) {
        if (uiState is CallUiState.Connected) {
            while (true) {
                delay(1000)
                // The timer is updated in the ViewModel through the call flow
            }
        }
    }
    
    // Handle one-time events
    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is CallEvent.Error -> {
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }
    
    // Auto-navigate back when call ends
    LaunchedEffect(uiState) {
        if (uiState is CallUiState.Ended) {
            delay(2000) // Show ended state for 2 seconds
            onNavigateBack()
        }
    }
    
    // Clean up when leaving the screen
    DisposableEffect(Unit) {
        onDispose {
            // End call if still active
            if (uiState is CallUiState.Connected || 
                uiState is CallUiState.Connecting || 
                uiState is CallUiState.Calling || 
                uiState is CallUiState.Ringing) {
                viewModel.endCall()
            }
        }
    }
    
    // Main content based on call type
    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        val isVideo = call?.type == CallType.VIDEO
        
        // Background - blurred user image for audio call, video feed for video call
        CallBackground(
            user = remoteUser,
            isVideoCall = isVideo,
            isVideoEnabled = isVideoEnabled
        )
        
        // Content based on call state
        when (uiState) {
            is CallUiState.Initializing -> {
                LoadingStateView("Initializing call...")
            }
            is CallUiState.Calling -> {
                CallingContent(
                    user = remoteUser,
                    onCancel = viewModel::endCall
                )
            }
            is CallUiState.Ringing -> {
                RingingContent(
                    user = remoteUser,
                    onAccept = viewModel::answerCall,
                    onReject = viewModel::rejectCall,
                    isVideoCall = isVideo
                )
            }
            is CallUiState.Connecting -> {
                ConnectingContent(
                    user = remoteUser,
                    onCancel = viewModel::endCall
                )
            }
            is CallUiState.Connected -> {
                ConnectedContent(
                    user = remoteUser,
                    duration = callDuration,
                    isVideoCall = isVideo,
                    isMuted = isMuted,
                    isVideoEnabled = isVideoEnabled,
                    isSpeakerOn = isSpeakerOn,
                    onEndCall = viewModel::endCall,
                    onToggleMute = viewModel::toggleMute,
                    onToggleVideo = viewModel::toggleVideo,
                    onToggleSpeaker = viewModel::toggleSpeaker,
                    onSwitchCamera = viewModel::switchCamera
                )
            }
            is CallUiState.Ended -> {
                val reason = (uiState as CallUiState.Ended).reason
                EndedContent(
                    user = remoteUser,
                    reason = reason
                )
            }
            is CallUiState.Error -> {
                val message = (uiState as CallUiState.Error).message
                ErrorContent(
                    message = message,
                    onBackClick = onNavigateBack
                )
            }
        }
    }
}

@Composable
fun CallBackground(
    user: User?,
    isVideoCall: Boolean,
    isVideoEnabled: Boolean
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // For now, we'll just show a blurred profile image
        // In a real app, this would be a video surface for video calls
        user?.let {
            AsyncImage(
                model = it.profileImageUrls.firstOrNull(),
                contentDescription = "Profile picture of ${it.name}",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(20.dp)
            )
        }
        
        // Darken the background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
        )
    }
}

@Composable
fun CallingContent(
    user: User?,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.height(64.dp))
        
        // User info
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Profile image
            UserProfileImage(user = user, size = 140.dp)
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Name
            Text(
                text = user?.name ?: "Unknown User",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Status text
            Text(
                text = "Calling...",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.8f)
            )
        }
        
        // Call controls
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 64.dp),
            contentAlignment = Alignment.Center
        ) {
            // End call button
            FilledIconButton(
                onClick = onCancel,
                modifier = Modifier.size(72.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(
                    imageVector = Icons.Default.CallEnd,
                    contentDescription = "End Call",
                    modifier = Modifier.size(36.dp),
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
fun RingingContent(
    user: User?,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    isVideoCall: Boolean
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.height(64.dp))
        
        // User info
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Profile image
            UserProfileImage(user = user, size = 140.dp)
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Name
            Text(
                text = user?.name ?: "Unknown User",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Status text
            Text(
                text = if (isVideoCall) "Video call incoming..." else "Audio call incoming...",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha =.8f)
            )
        }
        
        // Call controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 64.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Reject button
            FilledIconButton(
                onClick = onReject,
                modifier = Modifier.size(72.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(
                    imageVector = Icons.Default.CallEnd,
                    contentDescription = "Reject Call",
                    modifier = Modifier.size(36.dp),
                    tint = Color.White
                )
            }
            
            // Accept button
            FilledIconButton(
                onClick = onAccept,
                modifier = Modifier.size(72.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Call,
                    contentDescription = "Accept Call",
                    modifier = Modifier.size(36.dp),
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
fun ConnectingContent(
    user: User?,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.height(64.dp))
        
        // User info
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Profile image
            UserProfileImage(user = user, size = 140.dp)
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Name
            Text(
                text = user?.name ?: "Unknown User",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Status text
            Text(
                text = "Connecting...",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.8f)
            )
        }
        
        // Call controls
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 64.dp),
            contentAlignment = Alignment.Center
        ) {
            // End call button
            FilledIconButton(
                onClick = onCancel,
                modifier = Modifier.size(72.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(
                    imageVector = Icons.Default.CallEnd,
                    contentDescription = "End Call",
                    modifier = Modifier.size(36.dp),
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
fun ConnectedContent(
    user: User?,
    duration: Long,
    isVideoCall: Boolean,
    isMuted: Boolean,
    isVideoEnabled: Boolean,
    isSpeakerOn: Boolean,
    onEndCall: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleVideo: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onSwitchCamera: () -> Unit
) {
    val formattedDuration = formatCallDuration(duration)
    var showControls by remember { mutableStateOf(true) }
    
    // Auto-hide controls after 5 seconds
    LaunchedEffect(Unit) {
        delay(5000)
        showControls = false
    }
    
    // Toggle controls on tap for video calls
    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .background(Color.Transparent)
    ) {
        // User info at the top
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = user?.name ?: "Unknown User",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = formattedDuration,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }
        
        // In a video call, put a small user avatar in the corner
        if (isVideoCall) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .size(120.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.5f))
            ) {
                // Self video preview would go here
                // In a real app, this would be a camera preview surface
                
                // Placeholder profile image for self view
                UserProfileImage(
                    user = null, // Use current user here in a real app
                    size = 120.dp
                )
            }
        } else {
            // For audio calls, show the user profile in the center
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(16.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    UserProfileImage(user = user, size = 180.dp)
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = formattedDuration,
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White
                    )
                }
            }
        }
        
        // Call controls at the bottom
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Control row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Mute button
                    CallControlButton(
                        icon = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                        description = if (isMuted) "Unmute" else "Mute",
                        onClick = onToggleMute,
                        isActive = !isMuted
                    )
                    
                    // Video button (only for video calls)
                    if (isVideoCall) {
                        CallControlButton(
                            icon = if (isVideoEnabled) Icons.Default.Videocam else Icons.Default.VideocamOff,
                            description = if (isVideoEnabled) "Disable Video" else "Enable Video",
                            onClick = onToggleVideo,
                            isActive = isVideoEnabled
                        )
                    }
                    
                    // Speaker button
                    CallControlButton(
                        icon = if (isSpeakerOn) Icons.Default.Speaker else Icons.Default.SpeakerOff,
                        description = if (isSpeakerOn) "Disable Speaker" else "Enable Speaker",
                        onClick = onToggleSpeaker,
                        isActive = isSpeakerOn
                    )
                    
                    // Flip camera button (only for video calls)
                    if (isVideoCall) {
                        CallControlButton(
                            icon = Icons.Default.FlipCameraAndroid,
                            description = "Switch Camera",
                            onClick = onSwitchCamera,
                            isActive = true
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // End call button
                FilledIconButton(
                    onClick = onEndCall,
                    modifier = Modifier.size(72.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.CallEnd,
                        contentDescription = "End Call",
                        modifier = Modifier.size(36.dp),
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun EndedContent(
    user: User?,
    reason: String
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // User profile
        UserProfileImage(user = user, size = 140.dp)
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Name
        Text(
            text = user?.name ?: "Unknown User",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Status text
        val statusText = when (reason) {
            "ENDED" -> "Call ended"
            "MISSED" -> "Call missed"
            "REJECTED" -> "Call rejected"
            "BUSY" -> "User is busy"
            "FAILED" -> "Call failed"
            else -> "Call ended"
        }
        
        Text(
            text = statusText,
            style = MaterialTheme.typography.titleLarge,
            color = Color.White.copy(alpha = 0.8f)
        )
    }
}

@Composable
fun ErrorContent(
    message: String,
    onBackClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Error",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.8f),
            modifier = Modifier.padding(horizontal = 32.dp)
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        FilledIconButton(
            onClick = onBackClick,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                text = "Go Back",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
fun UserProfileImage(
    user: User?,
    size: Double,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = CircleShape,
        modifier = modifier.size(size.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
    ) {
        user?.let {
            AsyncImage(
                model = it.profileImageUrls.firstOrNull(),
                contentDescription = "Profile picture of ${it.name}",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } ?: Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            // Placeholder if no user or image
            Text(
                text = user?.name?.first().toString().uppercase(),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Bold,
                fontSize = (size / 2).sp
            )
        }
    }
}

@Composable
fun CallControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit,
    isActive: Boolean
) {
    val alpha by animateFloatAsState(
        targetValue = if (isActive) 1f else 0.6f,
        label = "alpha"
    )
    
    FilledIconButton(
        onClick = onClick,
        modifier = Modifier.size(56.dp),
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = if (isActive) {
                MaterialTheme.colorScheme.secondary
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            modifier = Modifier
                .size(24.dp)
                .alpha(alpha),
            tint = if (isActive) {
                MaterialTheme.colorScheme.onSecondary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}

// Helper function to format call duration
private fun formatCallDuration(durationSeconds: Long): String {
    val minutes = durationSeconds / 60
    val seconds = durationSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}