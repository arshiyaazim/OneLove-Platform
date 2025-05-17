package com.kilagee.onelove.ui.calls

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.kilagee.onelove.data.model.Call
import com.kilagee.onelove.data.model.CallType
import com.kilagee.onelove.domain.model.Resource
import com.kilagee.onelove.domain.model.User
import com.kilagee.onelove.navigation.Screen
import com.kilagee.onelove.ui.components.ErrorStateView
import com.kilagee.onelove.ui.components.LoadingStateView

@Composable
fun IncomingCallScreen(
    navController: NavController,
    callId: String,
    viewModel: CallViewModel = hiltViewModel()
) {
    // Load the call details
    LaunchedEffect(callId) {
        viewModel.getCall(callId)
    }
    
    val activeCallState by viewModel.activeCallState.collectAsState()
    
    Box(modifier = Modifier.fillMaxSize()) {
        when (val state = activeCallState) {
            is Resource.Loading -> {
                LoadingStateView()
            }
            is Resource.Success -> {
                val call = state.data
                IncomingCallContent(
                    call = call,
                    onAnswerClick = {
                        viewModel.answerCall(callId)
                        navController.navigate(Screen.OngoingCall.createRoute(callId))
                    },
                    onDeclineClick = {
                        viewModel.declineCall(callId)
                        navController.popBackStack()
                    },
                    navController = navController
                )
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
fun IncomingCallContent(
    call: Call,
    onAnswerClick: () -> Unit,
    onDeclineClick: () -> Unit,
    navController: NavController,
    userViewModel: UserViewModel = hiltViewModel()
) {
    // Load caller info
    val callerState = remember { mutableStateOf<Resource<User>?>(Resource.Loading) }
    
    LaunchedEffect(call.callerId) {
        userViewModel.getUserById(call.callerId)
            .collect { callerState.value = it }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Call type indicator
            Text(
                text = if (call.type == CallType.AUDIO) "Incoming Audio Call" else "Incoming Video Call",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Caller info
            when (val state = callerState.value) {
                is Resource.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.size(100.dp))
                }
                is Resource.Success -> {
                    val caller = state.data
                    
                    // Profile image
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(caller.profilePhotoUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Caller photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(150.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Caller name
                    Text(
                        text = caller.displayName,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    // If verified or premium, show badge
                    if (caller.isVerified || caller.isPremium) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            if (caller.isVerified) {
                                Icon(
                                    imageVector = Icons.Default.Verified,
                                    contentDescription = "Verified",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Verified",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            
                            if (caller.isPremium) {
                                if (caller.isVerified) Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "Premium",
                                    tint = Color(0xFFFFD700),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Premium",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFFFFD700)
                                )
                            }
                        }
                    }
                }
                is Resource.Error -> {
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
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Unknown Caller",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                null -> {}
            }
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Call actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Decline button
                FloatingActionButton(
                    onClick = onDeclineClick,
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                    modifier = Modifier.size(70.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CallEnd,
                        contentDescription = "Decline",
                        modifier = Modifier.size(32.dp)
                    )
                }
                
                // Answer button
                FloatingActionButton(
                    onClick = onAnswerClick,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(70.dp)
                ) {
                    Icon(
                        imageVector = if (call.type == CallType.AUDIO) Icons.Default.Call else Icons.Default.Videocam,
                        contentDescription = "Answer",
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}