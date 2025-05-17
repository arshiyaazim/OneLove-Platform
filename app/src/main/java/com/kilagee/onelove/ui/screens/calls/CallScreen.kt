package com.kilagee.onelove.ui.screens.calls

import android.Manifest
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.SpeakerOff
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import coil.compose.AsyncImage
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import io.agora.rtc.RtcEngine
import io.agora.rtc.video.VideoCanvas
import java.util.concurrent.TimeUnit

/**
 * Call screen
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CallScreen(
    callId: String,
    isVideo: Boolean,
    onEndCall: () -> Unit,
    viewModel: CallViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    
    val callState by viewModel.callState.collectAsState()
    val otherUser by viewModel.otherUser.collectAsState()
    val callDuration by viewModel.callDuration.collectAsState()
    val isMuted by viewModel.isMuted.collectAsState()
    val isSpeakerOn by viewModel.isSpeakerOn.collectAsState()
    val isCameraOn by viewModel.isCameraOn.collectAsState()
    
    // Format call duration as mm:ss
    val formattedDuration = remember(callDuration) {
        val minutes = TimeUnit.SECONDS.toMinutes(callDuration.toLong())
        val seconds = callDuration - TimeUnit.MINUTES.toSeconds(minutes)
        String.format("%02d:%02d", minutes, seconds)
    }
    
    // Define required permissions based on call type
    val permissionsToRequest = if (isVideo) {
        listOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA
        )
    } else {
        listOf(Manifest.permission.RECORD_AUDIO)
    }
    
    // Request required permissions
    val permissionsState = rememberMultiplePermissionsState(permissions = permissionsToRequest)
    
    LaunchedEffect(Unit) {
        permissionsState.launchMultiplePermissionRequest()
    }
    
    // Observe lifecycle events to handle app minimizing and returning
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    // When app is minimized, mute audio/video
                    viewModel.toggleMute()
                    if (isVideo) {
                        viewModel.toggleCamera()
                    }
                }
                Lifecycle.Event.ON_RESUME -> {
                    // When returning to app, check permissions again
                    if (permissionsState.permissions.all { it.status.isGranted }) {
                        // Permissions granted, unmute if needed
                    }
                }
                else -> {}
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
        }
    }
    
    // Handle call state changes
    LaunchedEffect(callState) {
        when (callState) {
            is CallState.Ended -> {
                onEndCall()
            }
            is CallState.Error -> {
                // Handle error
                onEndCall()
            }
            else -> {}
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (isVideo && permissionsState.permissions.all { it.status.isGranted }) {
            // Video call UI - Remote video view
            if (callState is CallState.Connected) {
                AndroidView(
                    factory = { ctx ->
                        val surfaceView = RtcEngine.CreateRendererView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                        }
                        viewModel.setRemoteVideoView(surfaceView)
                        surfaceView
                    },
                    modifier = Modifier.fillMaxSize()
                )
                
                // Local video view (PIP)
                if (isCameraOn) {
                    Box(
                        modifier = Modifier
                            .padding(16.dp)
                            .size(120.dp, 160.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .align(Alignment.TopEnd)
                    ) {
                        AndroidView(
                            factory = { ctx ->
                                val surfaceView = RtcEngine.CreateRendererView(ctx).apply {
                                    layoutParams = ViewGroup.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT
                                    )
                                }
                                viewModel.setLocalVideoView(surfaceView)
                                surfaceView
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        } else {
            // Audio call UI or video call with camera off
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(16.dp)
                ) {
                    // Profile picture
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        if (otherUser?.profilePictureUrl != null) {
                            AsyncImage(
                                model = otherUser?.profilePictureUrl,
                                contentDescription = "Profile picture",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Text(
                                text = otherUser?.name?.firstOrNull()?.toString() ?: "?",
                                style = MaterialTheme.typography.headlineLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.padding(16.dp))
                    
                    // User name
                    Text(
                        text = otherUser?.name ?: "Unknown",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.padding(8.dp))
                    
                    // Call state or duration
                    val statusText = when (callState) {
                        is CallState.Connecting -> "Connecting..."
                        is CallState.Connected -> formattedDuration
                        is CallState.Ended -> "Call ended"
                        is CallState.Error -> "Call failed"
                    }
                    
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        }
        
        // Call controls
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom,
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 48.dp)
        ) {
            // Call duration for video calls
            if (isVideo && callState is CallState.Connected) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = formattedDuration,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                }
            }
            
            // Control buttons
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                // Mute button
                FloatingActionButton(
                    onClick = { viewModel.toggleMute() },
                    modifier = Modifier.size(56.dp),
                    containerColor = if (isMuted) 
                        MaterialTheme.colorScheme.error.copy(alpha = 0.8f) 
                    else 
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                ) {
                    Icon(
                        imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = if (isMuted) "Unmute" else "Mute",
                        tint = if (isMuted) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                }
                
                // Speaker button
                FloatingActionButton(
                    onClick = { viewModel.toggleSpeaker() },
                    modifier = Modifier.size(56.dp),
                    containerColor = if (isSpeakerOn) 
                        MaterialTheme.colorScheme.primaryContainer 
                    else 
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                ) {
                    Icon(
                        imageVector = if (isSpeakerOn) Icons.Default.Speaker else Icons.Default.SpeakerOff,
                        contentDescription = if (isSpeakerOn) "Speaker Off" else "Speaker On",
                        tint = if (isSpeakerOn) 
                            MaterialTheme.colorScheme.onPrimaryContainer 
                        else 
                            MaterialTheme.colorScheme.onSurface
                    )
                }
                
                // Video button (only for video calls)
                if (isVideo) {
                    FloatingActionButton(
                        onClick = { viewModel.toggleCamera() },
                        modifier = Modifier.size(56.dp),
                        containerColor = if (!isCameraOn) 
                            MaterialTheme.colorScheme.error.copy(alpha = 0.8f) 
                        else 
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                    ) {
                        Icon(
                            imageVector = if (isCameraOn) Icons.Default.Videocam else Icons.Default.VideocamOff,
                            contentDescription = if (isCameraOn) "Turn Camera Off" else "Turn Camera On",
                            tint = if (!isCameraOn) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    
                    // Flip camera button (only for video calls)
                    FloatingActionButton(
                        onClick = { viewModel.switchCamera() },
                        modifier = Modifier.size(56.dp),
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FlipCameraAndroid,
                            contentDescription = "Flip Camera",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.padding(16.dp))
            
            // End call button
            FloatingActionButton(
                onClick = { 
                    viewModel.endCall()
                    onEndCall()
                },
                modifier = Modifier.size(72.dp),
                containerColor = MaterialTheme.colorScheme.error
            ) {
                Icon(
                    imageVector = Icons.Default.CallEnd,
                    contentDescription = "End Call",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}