package com.kilagee.onelove.ui.screens.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.kilagee.onelove.data.model.Message
import com.kilagee.onelove.data.model.MessageReaction
import kotlinx.coroutines.delay

/**
 * Chat screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    chatId: String,
    onBackClick: () -> Unit,
    onCallClick: (String, Boolean) -> Unit,
    onProfileClick: (String) -> Unit,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val chatState by viewModel.chatState.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val chatPartner by viewModel.chatPartner.collectAsState()
    
    var messageText by remember { mutableStateOf("") }
    var showReactionPicker by remember { mutableStateOf<String?>(null) }
    
    val listState = rememberLazyListState()
    
    // Scroll to bottom on new messages
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { 
                            chatPartner?.let { onProfileClick(it.id) } 
                        }
                    ) {
                        // Profile picture
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            if (chatPartner?.profilePictureUrl != null) {
                                AsyncImage(
                                    model = chatPartner?.profilePictureUrl,
                                    contentDescription = "Profile picture",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Text(
                                    text = chatPartner?.name?.firstOrNull()?.toString() ?: "?",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            
                            // Online indicator
                            if (chatPartner?.isOnline == true) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(Color.Green)
                                        .align(Alignment.BottomEnd)
                                )
                            }
                        }
                        
                        // Name and status
                        Column(
                            modifier = Modifier.padding(start = 12.dp)
                        ) {
                            Text(
                                text = chatPartner?.name ?: "Loading...",
                                style = MaterialTheme.typography.titleMedium
                            )
                            if (chatPartner?.isOnline == true) {
                                Text(
                                    text = "Online",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
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
                    // Voice call button
                    IconButton(
                        onClick = { 
                            chatPartner?.let { onCallClick(it.id, false) } 
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "Voice Call"
                        )
                    }
                    
                    // Video call button
                    IconButton(
                        onClick = { 
                            chatPartner?.let { onCallClick(it.id, true) } 
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = "Video Call"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        when (chatState) {
            is ChatState.Loading -> {
                // Loading state
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            
            is ChatState.Error -> {
                // Error state
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Error: ${(chatState as ChatState.Error).message}",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
            
            is ChatState.Success -> {
                // Success state
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    // Messages list
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        state = listState,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        reverseLayout = false
                    ) {
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                        
                        items(messages) { message ->
                            val isFromCurrentUser = message.senderId == currentUser?.id
                            
                            // Show reaction picker for this message
                            val showReactions = showReactionPicker == message.id
                            
                            Column {
                                MessageItem(
                                    message = message,
                                    isFromCurrentUser = isFromCurrentUser,
                                    onClick = {
                                        showReactionPicker = if (showReactionPicker == message.id) null else message.id
                                    },
                                    onLongClick = {
                                        if (isFromCurrentUser) {
                                            viewModel.deleteMessage(message.id)
                                        }
                                    }
                                )
                                
                                // Reaction picker
                                AnimatedVisibility(
                                    visible = showReactions,
                                    enter = fadeIn() + slideInVertically(
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessLow
                                        ),
                                        initialOffsetY = { it }
                                    ),
                                    exit = fadeOut()
                                ) {
                                    ReactionPicker(
                                        currentReaction = message.reaction,
                                        onReactionSelected = { reaction ->
                                            if (message.reaction == reaction) {
                                                viewModel.removeReaction(message.id)
                                            } else {
                                                viewModel.addReaction(message.id, reaction)
                                            }
                                            showReactionPicker = null
                                        },
                                        onDismiss = { showReactionPicker = null },
                                        isFromCurrentUser = isFromCurrentUser
                                    )
                                }
                            }
                        }
                        
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                    
                    // Message input
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Attachment button
                        IconButton(
                            onClick = { /* TODO: Add attachment functionality */ }
                        ) {
                            Icon(
                                imageVector = Icons.Default.AttachFile,
                                contentDescription = "Attach"
                            )
                        }
                        
                        // Text input
                        TextField(
                            value = messageText,
                            onValueChange = { messageText = it },
                            placeholder = { Text("Type a message") },
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                disabledContainerColor = MaterialTheme.colorScheme.surface,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            shape = RoundedCornerShape(24.dp)
                        )
                        
                        // Send button
                        IconButton(
                            onClick = {
                                if (messageText.isNotBlank()) {
                                    viewModel.sendMessage(messageText)
                                    messageText = ""
                                }
                            },
                            enabled = messageText.isNotBlank()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "Send",
                                tint = if (messageText.isNotBlank()) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Individual message item
 */
@Composable
fun MessageItem(
    message: Message,
    isFromCurrentUser: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = if (isFromCurrentUser) Alignment.End else Alignment.Start
    ) {
        // Message bubble
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = if (isFromCurrentUser) Arrangement.End else Arrangement.Start,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Reaction
            if (message.reaction != null) {
                ReactionBubble(reaction = message.reaction)
            }
            
            // Message content
            Surface(
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isFromCurrentUser) 16.dp else 4.dp,
                    bottomEnd = if (isFromCurrentUser) 4.dp else 16.dp
                ),
                color = if (isFromCurrentUser) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .clickable(onClick = onClick)
                    .clickable(onLongClick = onLongClick)
            ) {
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isFromCurrentUser) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
        
        // Timestamp and read status
        Row(
            modifier = Modifier.padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Format timestamp
            val timeString = remember(message.timestamp) {
                val formatter = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault())
                formatter.format(message.timestamp)
            }
            
            Text(
                text = timeString,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
            
            // Read status for sender's messages
            if (isFromCurrentUser) {
                Spacer(modifier = Modifier.padding(start = 4.dp))
                if (message.isRead) {
                    Text(
                        text = "Read",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

/**
 * Reaction bubble
 */
@Composable
fun ReactionBubble(
    reaction: MessageReaction,
    modifier: Modifier = Modifier
) {
    val emoji = when (reaction) {
        MessageReaction.LOVE -> "❤️"
        MessageReaction.LAUGH -> "😂"
        MessageReaction.WOW -> "😮"
        MessageReaction.SAD -> "😢"
        MessageReaction.ANGRY -> "😡"
        MessageReaction.LIKE -> "👍"
        MessageReaction.DISLIKE -> "👎"
    }
    
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier
            .size(24.dp)
            .padding(horizontal = 4.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = emoji,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Reaction picker for messages
 */
@Composable
fun ReactionPicker(
    currentReaction: MessageReaction?,
    onReactionSelected: (MessageReaction) -> Unit,
    onDismiss: () -> Unit,
    isFromCurrentUser: Boolean,
    modifier: Modifier = Modifier
) {
    // Close the picker after a timeout
    LaunchedEffect(Unit) {
        delay(5000)
        onDismiss()
    }
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = if (isFromCurrentUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier.padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MessageReaction.values().forEach { reaction ->
                    val emoji = when (reaction) {
                        MessageReaction.LOVE -> "❤️"
                        MessageReaction.LAUGH -> "😂"
                        MessageReaction.WOW -> "😮"
                        MessageReaction.SAD -> "😢"
                        MessageReaction.ANGRY -> "😡"
                        MessageReaction.LIKE -> "👍"
                        MessageReaction.DISLIKE -> "👎"
                    }
                    
                    val isSelected = currentReaction == reaction
                    
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    Color.Transparent
                                }
                            )
                            .clickable { onReactionSelected(reaction) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = emoji,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        }
    }
}