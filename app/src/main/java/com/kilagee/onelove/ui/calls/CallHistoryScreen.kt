package com.kilagee.onelove.ui.calls

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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
import com.kilagee.onelove.navigation.Screen
import com.kilagee.onelove.ui.components.EmptyStateView
import com.kilagee.onelove.ui.components.ErrorStateView
import com.kilagee.onelove.ui.components.LoadingStateView
import kotlinx.coroutines.flow.collectLatest
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallHistoryScreen(
    navController: NavController,
    viewModel: CallViewModel = hiltViewModel(),
    userViewModel: UserViewModel = hiltViewModel()
) {
    val callHistoryState by viewModel.callHistoryState.collectAsState()
    val currentUser = remember { FirebaseAuth.getInstance().currentUser }
    val context = LocalContext.current
    
    // Load call history on screen launch
    LaunchedEffect(Unit) {
        viewModel.loadCallHistory()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Call History") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    // Navigate to contacts or create call screen
                    navController.navigate(Screen.Contacts.route)
                }
            ) {
                Icon(Icons.Default.Call, contentDescription = "New Call")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = callHistoryState) {
                is Resource.Loading -> {
                    LoadingStateView()
                }
                is Resource.Success -> {
                    val calls = state.data
                    if (calls.isEmpty()) {
                        EmptyStateView(
                            icon = Icons.Default.Call,
                            message = "No call history yet",
                            actionText = "Make a call",
                            onActionClick = { navController.navigate(Screen.Contacts.route) }
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(calls) { call ->
                                CallHistoryItem(
                                    call = call,
                                    currentUserId = currentUser?.uid ?: "",
                                    userViewModel = userViewModel,
                                    onItemClick = {
                                        // Navigate to user profile or create new call
                                        val otherUserId = if (call.callerId == currentUser?.uid) {
                                            call.receiverId
                                        } else {
                                            call.callerId
                                        }
                                        
                                        if (call.isGroupCall) {
                                            // Navigate to group info
                                        } else {
                                            navController.navigate(Screen.UserProfile.createRoute(otherUserId))
                                        }
                                    },
                                    onCallClick = { userId, isVideo ->
                                        if (isVideo) {
                                            viewModel.initiateCall(userId, CallType.VIDEO)
                                        } else {
                                            viewModel.initiateCall(userId, CallType.AUDIO)
                                        }
                                    }
                                )
                                
                                Divider(modifier = Modifier.padding(horizontal = 16.dp))
                            }
                        }
                    }
                }
                is Resource.Error -> {
                    ErrorStateView(
                        message = state.message,
                        onRetryClick = { viewModel.loadCallHistory() }
                    )
                }
            }
        }
    }
    
    // Observe active call state to navigate to call screen
    LaunchedEffect(viewModel.activeCallState) {
        viewModel.activeCallState.collectLatest { state ->
            if (state is Resource.Success) {
                val call = state.data
                navController.navigate(Screen.OngoingCall.createRoute(call.id))
            }
        }
    }
}

@Composable
fun CallHistoryItem(
    call: Call,
    currentUserId: String,
    userViewModel: UserViewModel,
    onItemClick: () -> Unit,
    onCallClick: (String, Boolean) -> Unit
) {
    val otherUserId = if (call.callerId == currentUserId) call.receiverId else call.callerId
    val isOutgoing = call.callerId == currentUserId
    
    // Load other user's info
    var otherUser by remember { mutableStateOf<User?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    
    // Load user info for the other party
    LaunchedEffect(otherUserId) {
        userViewModel.getUserById(otherUserId).collect { resource ->
            isLoading = resource is Resource.Loading
            if (resource is Resource.Success) {
                otherUser = resource.data
            }
        }
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(onClick = onItemClick),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Call direction icon with background
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        when (call.status) {
                            CallStatus.MISSED, CallStatus.DECLINED -> Color(0xFFE57373) // Light red
                            CallStatus.ENDED -> if (isOutgoing) Color(0xFF81C784) else Color(0xFF64B5F6) // Green for outgoing, blue for incoming
                            else -> Color(0xFFFFB74D) // Orange for other states
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getCallStatusIcon(call.status, isOutgoing),
                    contentDescription = "Call status",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // User info and call details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                if (isLoading) {
                    // Placeholder while loading
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth(0.5f))
                } else {
                    // User name
                    Text(
                        text = otherUser?.displayName ?: "Unknown User",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // Call type and status
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (call.type == CallType.AUDIO) Icons.Default.Call else Icons.Default.Videocam,
                            contentDescription = if (call.type == CallType.AUDIO) "Audio call" else "Video call",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Spacer(modifier = Modifier.width(4.dp))
                        
                        Text(
                            text = getCallStatusText(call, isOutgoing),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // Call time and duration
                    val dateFormat = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault())
                    val timeText = dateFormat.format(call.createdAt)
                    val durationText = if (call.duration != null && call.duration > 0) {
                        " • ${formatDuration(call.duration)}"
                    } else {
                        ""
                    }
                    
                    Text(
                        text = "$timeText$durationText",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // Quick call buttons
            if (otherUser != null) {
                Row {
                    // Audio call button
                    IconButton(
                        onClick = { onCallClick(otherUserId, false) }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "Audio call",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    // Video call button
                    IconButton(
                        onClick = { onCallClick(otherUserId, true) }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = "Video call",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun getCallStatusIcon(status: CallStatus, isOutgoing: Boolean): ImageVector {
    return when (status) {
        CallStatus.INITIATED, CallStatus.ONGOING -> {
            if (isOutgoing) Icons.Default.CallMade else Icons.Default.CallReceived
        }
        CallStatus.MISSED -> Icons.Default.CallMissed
        CallStatus.DECLINED -> Icons.Default.CallEnd
        CallStatus.ENDED -> {
            if (isOutgoing) Icons.Default.CallMade else Icons.Default.CallReceived
        }
        CallStatus.FAILED -> Icons.Default.Error
    }
}

fun getCallStatusText(call: Call, isOutgoing: Boolean): String {
    return when (call.status) {
        CallStatus.INITIATED -> if (isOutgoing) "Outgoing" else "Incoming"
        CallStatus.ONGOING -> "Ongoing"
        CallStatus.MISSED -> "Missed"
        CallStatus.DECLINED -> "Declined"
        CallStatus.ENDED -> if (isOutgoing) "Outgoing" else "Incoming"
        CallStatus.FAILED -> "Failed"
    }
}

fun formatDuration(seconds: Long): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60
    
    return if (hours > 0) {
        String.format("%d h %d min", hours, minutes)
    } else if (minutes > 0) {
        String.format("%d min %d sec", minutes, secs)
    } else {
        String.format("%d sec", secs)
    }
}