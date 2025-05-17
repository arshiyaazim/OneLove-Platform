package com.kilagee.onelove.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.kilagee.onelove.data.model.User
import com.kilagee.onelove.domain.util.Result
import com.kilagee.onelove.ui.components.PremiumBadge
import com.kilagee.onelove.ui.components.VerificationBadge
import com.kilagee.onelove.ui.navigation.OneLoveBottomNavigation
import com.kilagee.onelove.ui.navigation.Routes
import com.kilagee.onelove.ui.theme.OneLoveTheme
import com.kilagee.onelove.ui.viewmodel.MatchViewModel
import com.kilagee.onelove.ui.viewmodel.ProfileViewModel
import kotlinx.coroutines.launch

/**
 * Profile screen
 * 
 * @param navController Navigation controller
 * @param userId ID of the user to display (null means current user)
 * @param viewModel Profile view model
 * @param matchViewModel Match view model for match actions
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    userId: String? = null,
    viewModel: ProfileViewModel = hiltViewModel(),
    matchViewModel: MatchViewModel = hiltViewModel()
) {
    val isCurrentUser = userId == null
    val userProfile by if (isCurrentUser) {
        viewModel.currentUserProfile.collectAsState()
    } else {
        viewModel.viewedUserProfile.collectAsState()
    }
    
    val matchActionState by matchViewModel.matchActionState.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    var isReportDialogVisible by remember { mutableStateOf(false) }
    
    // Load the profile
    LaunchedEffect(userId) {
        if (userId != null) {
            viewModel.loadUserProfile(userId)
        } else {
            viewModel.loadCurrentUserProfile()
        }
    }
    
    // Handle match action state
    LaunchedEffect(matchActionState) {
        // Handle match action results here
    }
    
    Scaffold(
        topBar = {
            ProfileTopBar(
                isCurrentUser = isCurrentUser,
                onBackClick = { navController.popBackStack() },
                onEditClick = { navController.navigate(Routes.PROFILE_EDIT) },
                onSettingsClick = { navController.navigate(Routes.SETTINGS) },
                onReportClick = { isReportDialogVisible = true }
            )
        },
        bottomBar = {
            if (isCurrentUser) {
                OneLoveBottomNavigation(navController = navController)
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (userProfile) {
                is Result.Loading -> {
                    LoadingState()
                }
                is Result.Error -> {
                    ErrorState(
                        message = (userProfile as Result.Error).message,
                        onRetry = {
                            if (isCurrentUser) {
                                viewModel.loadCurrentUserProfile()
                            } else if (userId != null) {
                                viewModel.loadUserProfile(userId)
                            }
                        }
                    )
                }
                is Result.Success -> {
                    val user = if (isCurrentUser) {
                        (userProfile as Result.Success<User>).data
                    } else {
                        (userProfile as Result.Success<User?>).data
                    }
                    
                    if (user != null) {
                        ProfileContent(
                            user = user,
                            isCurrentUser = isCurrentUser,
                            onSendMessage = { navController.navigate("${Routes.CHAT}/${user.id}") },
                            onLike = { matchViewModel.likeUser(user.id) },
                            onUnlike = { /* Unlike user */ },
                            onSendMatchRequest = { matchViewModel.sendMatchRequest(user.id) }
                        )
                    } else {
                        // User not found
                        ErrorState(
                            message = "User not found",
                            onRetry = {
                                if (userId != null) {
                                    viewModel.loadUserProfile(userId)
                                }
                            }
                        )
                    }
                }
            }
            
            if (isReportDialogVisible) {
                ReportDialog(
                    onDismiss = { isReportDialogVisible = false },
                    onSubmit = { reason ->
                        // Handle report submission
                        isReportDialogVisible = false
                    }
                )
            }
        }
    }
}

/**
 * Profile top bar
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileTopBar(
    isCurrentUser: Boolean,
    onBackClick: () -> Unit,
    onEditClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onReportClick: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                text = if (isCurrentUser) "My Profile" else "Profile",
                fontWeight = FontWeight.Bold
            )
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back"
                )
            }
        },
        actions = {
            if (isCurrentUser) {
                IconButton(onClick = onEditClick) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Profile"
                    )
                }
                IconButton(onClick = onSettingsClick) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Settings"
                    )
                }
            } else {
                IconButton(onClick = onReportClick) {
                    Icon(
                        imageVector = Icons.Default.Report,
                        contentDescription = "Report"
                    )
                }
                IconButton(onClick = { /* Share profile */ }) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share"
                    )
                }
            }
        }
    )
}

/**
 * Profile content
 */
@Composable
fun ProfileContent(
    user: User,
    isCurrentUser: Boolean,
    onSendMessage: () -> Unit,
    onLike: () -> Unit,
    onUnlike: () -> Unit,
    onSendMatchRequest: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Profile header with image
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
        ) {
            // Profile image
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(user.profilePictureUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "${user.name}'s profile picture",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            
            // Gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.7f)
                            ),
                            startY = 300f
                        )
                    )
            )
            
            // User info at bottom
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${user.name}, ${user.age}",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    if (user.isVerified) {
                        VerificationBadge()
                    }
                    
                    if (user.isPremium) {
                        Spacer(modifier = Modifier.width(8.dp))
                        PremiumBadge()
                    }
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                if (user.location != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Location",
                            tint = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.size(16.dp)
                        )
                        
                        Spacer(modifier = Modifier.width(4.dp))
                        
                        Text(
                            text = user.location,
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Action buttons for other users
                if (!isCurrentUser) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Like button
                        IconButton(
                            onClick = onLike,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.9f))
                        ) {
                            Icon(
                                imageVector = if (user.isLikedByMe == true) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Like",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        
                        // Message button
                        IconButton(
                            onClick = onSendMessage,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.9f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Message,
                                contentDescription = "Message",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
        
        // Profile details
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // About me
                Text(
                    text = "About Me",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = user.bio ?: "No bio provided",
                    style = MaterialTheme.typography.bodyLarge
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Basic info
                Text(
                    text = "Basic Info",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                InfoItem(
                    icon = Icons.Default.Person,
                    label = "Gender",
                    value = user.gender ?: "Not specified"
                )
                
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                
                InfoItem(
                    icon = Icons.Default.LocationOn,
                    label = "Location",
                    value = user.location ?: "Not specified"
                )
                
                // Add more basic info here as needed
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Interests
                Text(
                    text = "Interests",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                if (user.interests.isNullOrEmpty()) {
                    Text(
                        text = "No interests specified",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                } else {
                    InterestTags(interests = user.interests)
                }
            }
        }
        
        // Match actions
        if (!isCurrentUser) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Interested in ${user.name}?",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Button(
                        onClick = onSendMatchRequest,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Send Match Request")
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}

/**
 * Info item
 */
@Composable
fun InfoItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

/**
 * Interest tags
 */
@Composable
fun InterestTags(interests: List<String>) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(interests) { interest ->
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text = interest,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

/**
 * Loading state
 */
@Composable
fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

/**
 * Error state
 */
@Composable
fun ErrorState(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Error loading profile",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.error
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(onClick = onRetry) {
            Text("Retry")
        }
    }
}

/**
 * Report dialog
 */
@Composable
fun ReportDialog(
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit
) {
    // This would be a full dialog with report options
    // For brevity, we're just showing a placeholder
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Report user") },
        text = { Text("Report options would go here") },
        confirmButton = {
            Button(
                onClick = { onSubmit("Inappropriate content") }
            ) {
                Text("Submit")
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(
                onClick = onDismiss
            ) {
                Text("Cancel")
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun ProfileScreenPreview() {
    OneLoveTheme {
        ProfileScreen(
            navController = rememberNavController()
        )
    }
}