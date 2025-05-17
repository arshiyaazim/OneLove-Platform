package com.kilagee.onelove.ui.calls

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.firebase.auth.FirebaseAuth
import com.kilagee.onelove.data.model.Call
import com.kilagee.onelove.data.model.CallStatus
import com.kilagee.onelove.data.model.CallType
import com.kilagee.onelove.domain.model.Resource
import com.kilagee.onelove.domain.model.User
import com.kilagee.onelove.ui.components.ErrorStateView
import com.kilagee.onelove.ui.components.LoadingStateView
import io.agora.rtc.IRtcEngineEventHandler
import io.agora.rtc.RtcEngine
import io.agora.rtc.video.VideoCanvas
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*

@Composable
fun OngoingCallScreen(
    navController: NavController,
    callId: String,
    viewModel: CallViewModel = hiltViewModel(),
    userViewModel: UserViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val activeCallState by viewModel.activeCallState.collectAsState()
    val tokenState by viewModel.tokenState.collectAsState()
    val scope = rememberCoroutineScope()
    val currentUser = remember { FirebaseAuth.getInstance().currentUser }
    
    // Variables for Agora RTC
    var agoraAppId by remember { mutableStateOf("") } // Will be retrieved from Firebase Remote Config
    var rtcEngine by remember { mutableStateOf<RtcEngine?>(null) }
    var callDuration by remember { mutableStateOf(0L) }
    var callStartTime by remember { mutableStateOf(0L) }
    var remotePeerJoined by remember { mutableStateOf(false) }
    var isMuted by remember { mutableStateOf(false) }
    var isVideoEnabled by remember { mutableStateOf(true) }
    var isSpeakerOn by remember { mutableStateOf(true) }
    var isFrontCamera by remember { mutableStateOf(true) }
    var otherUser by remember { mutableStateOf<User?>(null) }
    
    // Permission launchers
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                // Audio permission granted, proceed with call
                if (activeCallState is Resource.Success) {
                    setupAgoraEngine((activeCallState as Resource.Success<Call>).data)
                }
            } else {
                // Permission denied, end call
                if (activeCallState is Resource.Success) {
                    val call = (activeCallState as Resource.Success<Call>).data
                    viewModel.endCall(call.id, 0)
                    navController.popBackStack()
                }
            }
        }
    )
    
    val videoPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                // Video permission granted, proceed with call
                if (activeCallState is Resource.Success) {
                    val call = (activeCallState as Resource.Success<Call>).data
                    if (call.type == CallType.VIDEO) {
                        isVideoEnabled = true
                        rtcEngine?.enableVideo()
                    }
                }
            } else {
                // Video permission denied, continue with audio only
                isVideoEnabled = false
                rtcEngine?.disableVideo()
            }
        }
    )
    
    // Load call details
    LaunchedEffect(callId) {
        viewModel.getCall(callId)
    }
    
    // Setup Agora engine when call is loaded
    fun setupAgoraEngine(call: Call) {
        // Load Agora App ID from Firebase Remote Config
        agoraAppId = "your_agora_app_id" // TODO: Replace with actual app ID from Remote Config or ask user for secrets
        
        try {
            // Initialize Agora engine
            rtcEngine = RtcEngine.create(context, agoraAppId, object : IRtcEngineEventHandler() {
                override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
                    // Update UI when successfully joined channel
                    scope.launch {
                        callStartTime = System.currentTimeMillis()
                    }
                }
                
                override fun onUserJoined(uid: Int, elapsed: Int) {
                    // Remote user joined
                    scope.launch {
                        remotePeerJoined = true
                    }
                }
                
                override fun onUserOffline(uid: Int, reason: Int) {
                    // Remote user left
                    scope.launch {
                        remotePeerJoined = false
                        // If call was ongoing, end it
                        if (activeCallState is Resource.Success) {
                            val duration = (System.currentTimeMillis() - callStartTime) / 1000
                            viewModel.endCall(call.id, duration)
                            delay(1000) // Give some time for the call to end properly
                            navController.popBackStack()
                        }
                    }
                }
                
                override fun onError(err: Int) {
                    // Handle errors
                    scope.launch {
                        if (activeCallState is Resource.Success) {
                            viewModel.endCall(call.id, 0)
                            navController.popBackStack()
                        }
                    }
                }
            })
            
            // Configure the Agora engine based on call type
            if (call.type == CallType.VIDEO) {
                rtcEngine?.enableVideo()
                isVideoEnabled = true
            } else {
                rtcEngine?.disableVideo()
                isVideoEnabled = false
            }
            
            // Set audio profile
            rtcEngine?.setAudioProfile(
                io.agora.rtc.Constants.AUDIO_PROFILE_DEFAULT,
                io.agora.rtc.Constants.AUDIO_SCENARIO_DEFAULT
            )
            
            // Get token for secure connection
            if (currentUser != null) {
                viewModel.getAgoraToken(call.id, currentUser.uid, call.channelName)
            }
        } catch (e: Exception) {
            // Handle initialization error
            scope.launch {
                if (activeCallState is Resource.Success) {
                    viewModel.endCall(call.id, 0)
                    navController.popBackStack()
                }
            }
        }
    }
    
    // Join channel when token is available
    LaunchedEffect(tokenState) {
        if (tokenState is Resource.Success && rtcEngine != null) {
            val token = (tokenState as Resource.Success<String>).data
            rtcEngine?.joinChannel(token, (activeCallState as? Resource.Success<Call>)?.data?.channelName ?: "", null, 0)
        }
    }
    
    // Track call duration
    LaunchedEffect(callStartTime) {
        if (callStartTime > 0) {
            while (true) {
                delay(1000) // Update every second
                callDuration = (System.currentTimeMillis() - callStartTime) / 1000
            }
        }
    }
    
    // Load other user info
    LaunchedEffect(activeCallState) {
        if (activeCallState is Resource.Success) {
            val call = (activeCallState as Resource.Success<Call>).data
            val otherId = if (call.callerId == currentUser?.uid) call.receiverId else call.callerId
            
            userViewModel.getUserById(otherId).collect { userResource ->
                if (userResource is Resource.Success) {
                    otherUser = userResource.data
                }
            }
        }
    }
    
    // Cleanup when leaving the screen
    DisposableEffect(Unit) {
        onDispose {
            // End call if not already ended
            if (activeCallState is Resource.Success) {
                val call = (activeCallState as Resource.Success<Call>).data
                if (call.status == CallStatus.ONGOING) {
                    val duration = (System.currentTimeMillis() - callStartTime) / 1000
                    viewModel.endCall(call.id, duration)
                }
            }
            
            // Release Agora resources
            rtcEngine?.leaveChannel()
            RtcEngine.destroy()
        }
    }
    
    // Check and request permissions
    LaunchedEffect(activeCallState) {
        if (activeCallState is Resource.Success) {
            val call = (activeCallState as Resource.Success<Call>).data
            
            // Check audio permission
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.RECORD_AUDIO
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            } else if (call.type == CallType.VIDEO && ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.CAMERA
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // Check camera permission for video calls
                videoPermissionLauncher.launch(Manifest.permission.CAMERA)
            } else {
                // All permissions granted, proceed with call
                setupAgoraEngine(call)
            }
        }
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        when (val state = activeCallState) {
            is Resource.Loading -> {
                LoadingStateView()
            }
            is Resource.Success -> {
                val call = state.data
                
                // Main call UI
                OngoingCallContent(
                    call = call,
                    otherUser = otherUser,
                    rtcEngine = rtcEngine,
                    isMuted = isMuted,
                    isVideoEnabled = isVideoEnabled,
                    isSpeakerOn = isSpeakerOn,
                    isFrontCamera = isFrontCamera,
                    callDuration = callDuration,
                    onMuteToggle = {
                        isMuted = !isMuted
                        rtcEngine?.muteLocalAudioStream(isMuted)
                    },
                    onVideoToggle = {
                        isVideoEnabled = !isVideoEnabled
                        if (isVideoEnabled) {
                            rtcEngine?.enableVideo()
                        } else {
                            rtcEngine?.disableVideo()
                        }
                    },
                    onSpeakerToggle = {
                        isSpeakerOn = !isSpeakerOn
                        rtcEngine?.setEnableSpeakerphone(isSpeakerOn)
                    },
                    onCameraSwitch = {
                        isFrontCamera = !isFrontCamera
                        rtcEngine?.switchCamera()
                    },
                    onEndCall = {
                        val duration = (System.currentTimeMillis() - callStartTime) / 1000
                        viewModel.endCall(call.id, duration)
                        navController.popBackStack()
                    }
                )
                
                // Connecting indicator if remote peer not joined
                if (!remotePeerJoined) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.7f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(50.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Connecting...",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
            is Resource.Error -> {
                ErrorStateView(
                    message = state.message,
                    onRetryClick = { viewModel.getCall(callId) }
                )
            }
            null -> {
                // Initial state, do nothing
            }
        }
    }
}

@Composable
fun OngoingCallContent(
    call: Call,
    otherUser: User?,
    rtcEngine: RtcEngine?,
    isMuted: Boolean,
    isVideoEnabled: Boolean,
    isSpeakerOn: Boolean,
    isFrontCamera: Boolean,
    callDuration: Long,
    onMuteToggle: () -> Unit,
    onVideoToggle: () -> Unit,
    onSpeakerToggle: () -> Unit,
    onCameraSwitch: () -> Unit,
    onEndCall: () -> Unit
) {
    val isVideoCall = call.type == CallType.VIDEO
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isVideoCall) Color.Black else MaterialTheme.colorScheme.background)
    ) {
        // Video content (only for video calls)
        if (isVideoCall && isVideoEnabled && rtcEngine != null) {
            // Remote video (fills the screen)
            AndroidView(
                factory = { context ->
                    val surfaceView = io.agora.rtc.video.SurfaceView(context)
                    rtcEngine.setupRemoteVideo(
                        VideoCanvas(
                            surfaceView,
                            io.agora.rtc.Constants.RENDER_MODE_HIDDEN,
                            0
                        )
                    )
                    surfaceView
                },
                modifier = Modifier.fillMaxSize()
            )
            
            // Local video (small overlay)
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .size(width = 120.dp, height = 160.dp)
                    .clip(RoundedCornerShape(8.dp))
            ) {
                AndroidView(
                    factory = { context ->
                        val surfaceView = io.agora.rtc.video.SurfaceView(context)
                        rtcEngine.setupLocalVideo(
                            VideoCanvas(
                                surfaceView,
                                io.agora.rtc.Constants.RENDER_MODE_HIDDEN,
                                0
                            )
                        )
                        rtcEngine.startPreview()
                        surfaceView
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else if (!isVideoCall || !isVideoEnabled) {
            // Audio call UI or video disabled UI
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Profile image or placeholder
                if (otherUser != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(otherUser.profilePhotoUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Caller photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(150.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(150.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Unknown caller",
                            modifier = Modifier.size(80.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Name
                Text(
                    text = otherUser?.displayName ?: "Unknown User",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isVideoCall) Color.White else MaterialTheme.colorScheme.onBackground
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Call status
                val hours = callDuration / 3600
                val minutes = (callDuration % 3600) / 60
                val seconds = callDuration % 60
                val formattedDuration = if (hours > 0) {
                    String.format("%02d:%02d:%02d", hours, minutes, seconds)
                } else {
                    String.format("%02d:%02d", minutes, seconds)
                }
                
                Text(
                    text = formattedDuration,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isVideoCall) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                )
            }
        }
        
        // Call controls at the bottom
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(
                    color = if (isVideoCall) Color.Black.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface.copy(
                        alpha = 0.9f
                    )
                )
                .padding(vertical = 16.dp)
        ) {
            // Call actions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mute button
                CallControlButton(
                    icon = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                    contentDescription = if (isMuted) "Unmute" else "Mute",
                    label = if (isMuted) "Unmute" else "Mute",
                    onClick = onMuteToggle,
                    isActive = isMuted,
                    isVideoCall = isVideoCall
                )
                
                // Video toggle (only for video calls)
                if (isVideoCall) {
                    CallControlButton(
                        icon = if (isVideoEnabled) Icons.Default.Videocam else Icons.Default.VideocamOff,
                        contentDescription = if (isVideoEnabled) "Disable Video" else "Enable Video",
                        label = if (isVideoEnabled) "Video Off" else "Video On",
                        onClick = onVideoToggle,
                        isActive = !isVideoEnabled,
                        isVideoCall = isVideoCall
                    )
                }
                
                // Speaker button
                CallControlButton(
                    icon = if (isSpeakerOn) Icons.Default.VolumeUp else Icons.Default.VolumeDown,
                    contentDescription = if (isSpeakerOn) "Speaker Off" else "Speaker On",
                    label = if (isSpeakerOn) "Speaker" else "Earpiece",
                    onClick = onSpeakerToggle,
                    isActive = isSpeakerOn,
                    isVideoCall = isVideoCall
                )
                
                // Camera switch (only for video calls)
                if (isVideoCall && isVideoEnabled) {
                    CallControlButton(
                        icon = Icons.Default.FlipCameraAndroid,
                        contentDescription = "Switch Camera",
                        label = "Flip",
                        onClick = onCameraSwitch,
                        isActive = false,
                        isVideoCall = isVideoCall
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // End call button
            Box(
                modifier = Modifier
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                FloatingActionButton(
                    onClick = onEndCall,
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                    modifier = Modifier.size(70.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CallEnd,
                        contentDescription = "End Call",
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CallControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    label: String,
    onClick: () -> Unit,
    isActive: Boolean,
    isVideoCall: Boolean
) {
    val backgroundColor = if (isActive) {
        MaterialTheme.colorScheme.primary
    } else {
        if (isVideoCall) Color.White.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant
    }
    
    val contentColor = if (isActive) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        if (isVideoCall) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
    }
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(50.dp)
                .background(
                    color = backgroundColor,
                    shape = CircleShape
                )
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = contentColor,
                modifier = Modifier.size(24.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = if (isVideoCall) Color.White else MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
    }
}