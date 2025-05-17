package com.kilagee.onelove.ui.ai

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.kilagee.onelove.data.model.AIProfile
import com.kilagee.onelove.data.model.Gender
import com.kilagee.onelove.data.model.PersonalityType
import com.kilagee.onelove.util.UIEvent
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIProfileDetailScreen(
    navController: NavController,
    profileId: String,
    viewModel: AIProfileDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showPhotoDialog by remember { mutableStateOf(false) }
    var selectedPhotoUrl by remember { mutableStateOf("") }
    
    // Load profile
    LaunchedEffect(profileId) {
        viewModel.loadProfile(profileId)
    }
    
    // Collect UI events
    LaunchedEffect(Unit) {
        viewModel.uiEvent.collectLatest { event ->
            when (event) {
                is UIEvent.ShowToast -> {
                    Toast.makeText(context, event.message, android.widget.Toast.LENGTH_SHORT).show()
                }
                is UIEvent.Navigate -> {
                    navController.navigate(event.route)
                }
                else -> {}
            }
        }
    }
    
    val profile = uiState.profile
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(profile?.name ?: "Profile") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            if (profile != null) {
                FloatingActionButton(
                    onClick = { viewModel.startConversation() },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        imageVector = Icons.Default.Chat,
                        contentDescription = "Start Chat",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoading) {
                // Loading state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (uiState.error != null) {
                // Error state
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(48.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Error Loading Profile",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = uiState.error ?: "Unknown error occurred",
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(onClick = { viewModel.loadProfile(profileId) }) {
                        Text("Try Again")
                    }
                }
            } else if (profile != null) {
                // Profile details
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    // Profile header with image
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                    ) {
                        // Main profile image
                        AsyncImage(
                            model = profile.photoUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
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
                                        startY = 300f * 0.6f // Start gradient at 60% of the height
                                    )
                                )
                        )
                        
                        // Profile info overlay
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomStart)
                                .padding(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${profile.name}, ${profile.age}",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                
                                Spacer(modifier = Modifier.width(8.dp))
                                
                                Icon(
                                    imageVector = if (profile.gender == Gender.FEMALE) {
                                        Icons.Default.Female
                                    } else {
                                        Icons.Default.Male
                                    },
                                    contentDescription = null,
                                    tint = if (profile.gender == Gender.FEMALE) {
                                        Color(0xFFE91E63)
                                    } else {
                                        Color(0xFF2196F3)
                                    },
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            Text(
                                text = "${profile.city}, ${profile.country}",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // Personality badge
                            ElevatedAssistChip(
                                onClick = { },
                                colors = AssistChipDefaults.elevatedAssistChipColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                ),
                                label = { 
                                    Text(
                                        text = getPersonalityName(profile.personalityType),
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = getPersonalityIcon(profile.personalityType),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            )
                        }
                    }
                    
                    // Content
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        // About section
                        SectionTitle("About Me")
                        
                        Text(
                            text = profile.bio,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // Basic info
                        SectionTitle("Basic Info")
                        
                        ProfileInfoItem(
                            icon = Icons.Default.Work,
                            label = "Occupation",
                            value = profile.occupation
                        )
                        
                        if (profile.education != null) {
                            ProfileInfoItem(
                                icon = Icons.Default.School,
                                label = "Education",
                                value = profile.education
                            )
                        }
                        
                        if (profile.height != null) {
                            ProfileInfoItem(
                                icon = Icons.Default.Height,
                                label = "Height",
                                value = "${profile.height} cm"
                            )
                        }
                        
                        if (profile.relationshipStatus != null) {
                            ProfileInfoItem(
                                icon = Icons.Default.Favorite,
                                label = "Relationship Status",
                                value = profile.relationshipStatus.name.replace("_", " ")
                                    .split(" ")
                                    .joinToString(" ") { it.capitalize() }
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // Interests
                        if (profile.interests.isNotEmpty()) {
                            SectionTitle("Interests")
                            
                            LazyHorizontalGrid(
                                rows = GridCells.Fixed(2),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp),
                                contentPadding = PaddingValues(vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(profile.interests) { interest ->
                                    SuggestionChip(
                                        onClick = { },
                                        label = { Text(interest) }
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                        
                        // Photos
                        if (profile.photoUrls.isNotEmpty()) {
                            SectionTitle("Photos")
                            
                            LazyHorizontalGrid(
                                rows = GridCells.Fixed(2),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(240.dp),
                                contentPadding = PaddingValues(vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(profile.photoUrls) { photoUrl ->
                                    AsyncImage(
                                        model = photoUrl,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(120.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                selectedPhotoUrl = photoUrl
                                                showPhotoDialog = true
                                            },
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                        
                        // More traits
                        if (profile.drinking != null || profile.smoking != null || 
                            profile.children != null || profile.religion != null || 
                            profile.zodiacSign != null) {
                                
                            SectionTitle("More About Me")
                            
                            if (profile.drinking != null) {
                                ProfileInfoItem(
                                    icon = Icons.Default.LocalBar,
                                    label = "Drinking",
                                    value = profile.drinking.name.capitalize()
                                )
                            }
                            
                            if (profile.smoking != null) {
                                ProfileInfoItem(
                                    icon = Icons.Default.SmokingRooms,
                                    label = "Smoking",
                                    value = profile.smoking.name.capitalize()
                                )
                            }
                            
                            if (profile.children != null) {
                                ProfileInfoItem(
                                    icon = Icons.Default.ChildCare,
                                    label = "Children",
                                    value = profile.children.name.replace("_", " ")
                                        .split(" ")
                                        .joinToString(" ") { it.capitalize() }
                                )
                            }
                            
                            if (profile.religion != null) {
                                ProfileInfoItem(
                                    icon = Icons.Default.ChurchTemple,
                                    label = "Religion",
                                    value = profile.religion.name.capitalize()
                                )
                            }
                            
                            if (profile.zodiacSign != null) {
                                ProfileInfoItem(
                                    icon = Icons.Default.Stars,
                                    label = "Zodiac Sign",
                                    value = profile.zodiacSign.name.capitalize()
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                        
                        // Start chat button
                        Button(
                            onClick = { viewModel.startConversation() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Chat, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Start Conversation")
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }
    
    // Photo dialog
    if (showPhotoDialog) {
        Dialog(onDismissRequest = { showPhotoDialog = false }) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = selectedPhotoUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Fit
                )
                
                IconButton(
                    onClick = { showPhotoDialog = false },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(40.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
fun ProfileInfoItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
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
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
fun LazyHorizontalGrid(
    rows: GridCells,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable () -> Unit
) {
    // This is a simplified version of LazyHorizontalGrid since we're not
    // actually using the lazy functionality
    Column(
        modifier = modifier.padding(contentPadding),
        verticalArrangement = verticalArrangement
    ) {
        content()
    }
}

fun getPersonalityIcon(personalityType: PersonalityType): androidx.compose.ui.graphics.vector.ImageVector {
    return when (personalityType) {
        PersonalityType.ROMANTIC -> Icons.Default.Favorite
        PersonalityType.FLIRTY -> Icons.Default.Wink
        PersonalityType.INTELLECTUAL -> Icons.Default.Psychology
        PersonalityType.ADVENTUROUS -> Icons.Default.Explore
        PersonalityType.SHY -> Icons.Default.VisibilityOff
        PersonalityType.CONFIDENT -> Icons.Default.Star
        PersonalityType.HUMOROUS -> Icons.Default.EmojiEmotions
        PersonalityType.MYSTERIOUS -> Icons.Default.QuestionMark
        PersonalityType.CARING -> Icons.Default.Favorite
        PersonalityType.CREATIVE -> Icons.Default.Brush
        PersonalityType.AMBITIOUS -> Icons.Default.TrendingUp
        PersonalityType.SPIRITUAL -> Icons.Default.Cloud
        PersonalityType.PRACTICAL -> Icons.Default.HandyMan
        PersonalityType.OUTGOING -> Icons.Default.People
        PersonalityType.RESERVED -> Icons.Default.Person
    }
}

fun getPersonalityName(personalityType: PersonalityType): String {
    return when (personalityType) {
        PersonalityType.ROMANTIC -> "Romantic"
        PersonalityType.FLIRTY -> "Flirty"
        PersonalityType.INTELLECTUAL -> "Intellectual"
        PersonalityType.ADVENTUROUS -> "Adventurous"
        PersonalityType.SHY -> "Shy"
        PersonalityType.CONFIDENT -> "Confident"
        PersonalityType.HUMOROUS -> "Humorous"
        PersonalityType.MYSTERIOUS -> "Mysterious"
        PersonalityType.CARING -> "Caring"
        PersonalityType.CREATIVE -> "Creative"
        PersonalityType.AMBITIOUS -> "Ambitious"
        PersonalityType.SPIRITUAL -> "Spiritual"
        PersonalityType.PRACTICAL -> "Practical"
        PersonalityType.OUTGOING -> "Outgoing"
        PersonalityType.RESERVED -> "Reserved"
    }
}

private fun String.capitalize(): String {
    return this.lowercase().replaceFirstChar { it.uppercase() }
}