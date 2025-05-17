package com.kilagee.onelove.ui.screens.ai

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.StarRate
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.kilagee.onelove.data.model.AIMessage
import com.kilagee.onelove.data.model.AIProfile
import com.kilagee.onelove.data.model.MessageType
import com.kilagee.onelove.data.model.ReactionSummary
import com.kilagee.onelove.data.model.ReactionTargetType
import com.kilagee.onelove.data.model.ReactionType
import com.kilagee.onelove.ui.LocalSnackbarHostState
import com.kilagee.onelove.ui.components.EmojiReactionBar
import com.kilagee.onelove.ui.components.ReactionSummaryBar
import com.kilagee.onelove.ui.screens.chat.TextButton
import com.kilagee.onelove.ui.screens.chat.formatMessageTime
import java.util.Date

/**
 * AI Chat screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIChatScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSubscription: () -> Unit,
    viewModel: AIChatViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val aiProfile by viewModel.aiProfile.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val isSubscribed by viewModel.isSubscribed.collectAsState()
    val interactionCount by viewModel.interactionCount.collectAsState()
    val interactionLimit by viewModel.interactionLimit.collectAsState()
    
    // Message input state
    var messageText by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    
    // Reaction states
    val messageReactions = remember { mutableStateMapOf<String, ReactionType>() }
    
    // Snackbar
    val snackbarHostState = LocalSnackbarHostState.current
    val context = LocalContext.current
    
    // Lazy list state for scrolling
    val listState = rememberLazyListState()
    
    // Limit reached dialog
    var showLimitReachedDialog by remember { mutableStateOf(false) }
    
    // Collect events
    LaunchedEffect(key1 = true) {
        viewModel.events.collect { event ->
            when (event) {
                is AIChatEvent.Error -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                is AIChatEvent.MessageSent -> {
                    // Auto-scroll to bottom when a new message is sent
                    if (listState.layoutInfo.totalItemsCount > 0) {
                        listState.animateScrollToItem(listState.layoutInfo.totalItemsCount - 1)
                    }
                }
                is AIChatEvent.LimitReached -> {
                    showLimitReachedDialog = true
                }
            }
        }
    }
    
    // Limit reached dialog
    if (showLimitReachedDialog) {
        LimitReachedDialog(
            onDismiss = { showLimitReachedDialog = false },
            onSubscribe = { 
                showLimitReachedDialog = false
                onNavigateToSubscription()
            }
        )
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // AI avatar
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            if (aiProfile?.imageUrl != null) {
                                AsyncImage(
                                    model = aiProfile?.imageUrl,
                                    contentDescription = "AI profile image",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        // AI name and personality
                        Column {
                            Text(
                                text = aiProfile?.name ?: "AI Companion",
                                style = MaterialTheme.typography.titleMedium
                            )
                            
                            Text(
                                text = aiProfile?.personalityType ?: "AI Assistant",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    // Show subscription status
                    if (isSubscribed) {
                        Icon(
                            imageVector = Icons.Default.StarRate,
                            contentDescription = "Premium",
                            tint = Color(0xFFFFD700)
                        )
                    } else {
                        // Show remaining interactions
                        Text(
                            text = "$interactionCount/$interactionLimit",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    
                    // Info button
                    IconButton(onClick = { 
                        /* Show AI profile info */ 
                    }) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Info"
                        )
                    }
                }
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                // Message input
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    // Message input field
                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 40.dp)
                            .focusRequester(focusRequester),
                        placeholder = { Text("Type a message") },
                        shape = RoundedCornerShape(24.dp),
                        maxLines = 5,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Send
                        ),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                if (messageText.isNotBlank()) {
                                    viewModel.sendMessage(messageText)
                                    messageText = ""
                                }
                            }
                        ),
                        enabled = !isGenerating && (isSubscribed || interactionCount < interactionLimit)
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    // Send button
                    FloatingActionButton(
                        onClick = {
                            if (messageText.isNotBlank()) {
                                viewModel.sendMessage(messageText)
                                messageText = ""
                            }
                        },
                        modifier = Modifier.size(48.dp),
                        containerColor = MaterialTheme.colorScheme.primary,
                        enabled = !isGenerating && (isSubscribed || interactionCount < interactionLimit)
                    ) {
                        if (isGenerating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "Send",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
                
                // Show limit warning
                if (!isSubscribed && interactionCount >= interactionLimit * 0.8) {
                    Divider()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Running low on free messages: $interactionCount/$interactionLimit",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                        
                        Button(
                            onClick = onNavigateToSubscription,
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("Upgrade")
                        }
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (uiState) {
                is AIChatUIState.Loading -> {
                    // Loading state
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                
                is AIChatUIState.Error -> {
                    // Error state
                    val error = (uiState as AIChatUIState.Error).message
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Error loading AI chat",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = error,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            TextButton(onClick = { viewModel.loadConversation() }) {
                                Text("Try Again")
                            }
                        }
                    }
                }
                
                is AIChatUIState.Empty -> {
                    // Empty state - no messages yet
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Text(
                                text = "Start chatting with ${aiProfile?.name ?: "AI Companion"}",
                                style = MaterialTheme.typography.titleLarge,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = aiProfile?.description ?: "I'm an AI companion ready to chat with you. What would you like to talk about?",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
                
                is AIChatUIState.Success -> {
                    // Chat message list
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        state = listState,
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Message items
                        items(messages) { message ->
                            val isFromUser = message.isFromUser
                            
                            AIMessageItem(
                                message = message,
                                isFromUser = isFromUser,
                                aiName = aiProfile?.name ?: "AI",
                                aiImage = aiProfile?.imageUrl,
                                userReaction = messageReactions[message.id],
                                onReactionSelected = { reaction ->
                                    messageReactions[message.id] = reaction
                                    viewModel.reactToMessage(message.id, reaction)
                                },
                                onReactionRemoved = {
                                    messageReactions.remove(message.id)
                                    viewModel.removeReaction(message.id)
                                }
                            )
                        }
                        
                        // "AI is typing" indicator
                        if (isGenerating) {
                            item {
                                Row(
                                    modifier = Modifier.padding(start = 16.dp, top = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // AI avatar
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (aiProfile?.imageUrl != null) {
                                            AsyncImage(
                                                model = aiProfile?.imageUrl,
                                                contentDescription = "AI profile image",
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.Person,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.width(8.dp))
                                    
                                    Text(
                                        text = "Typing...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                    }
                    
                    // Scroll to bottom initially and when new messages arrive
                    LaunchedEffect(messages.size, isGenerating) {
                        if (messages.isNotEmpty() && listState.layoutInfo.totalItemsCount > 0) {
                            listState.animateScrollToItem(listState.layoutInfo.totalItemsCount - 1)
                        }
                    }
                }
            }
        }
    }
}

/**
 * AI Message item
 */
@Composable
fun AIMessageItem(
    message: AIMessage,
    isFromUser: Boolean,
    aiName: String,
    aiImage: String?,
    userReaction: ReactionType? = null,
    onReactionSelected: (ReactionType) -> Unit,
    onReactionRemoved: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bubbleShape = RoundedCornerShape(
        topStart = if (isFromUser) 16.dp else 0.dp,
        topEnd = if (isFromUser) 0.dp else 16.dp,
        bottomStart = 16.dp,
        bottomEnd = 16.dp
    )
    
    val bubbleColor = if (isFromUser) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    
    val textColor = if (isFromUser) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    
    val arrangement = if (isFromUser) {
        Arrangement.End
    } else {
        Arrangement.Start
    }
    
    // Show reaction controls
    var showReactionControls by remember { mutableStateOf(false) }
    
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = if (isFromUser) Alignment.End else Alignment.Start
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = arrangement,
            verticalAlignment = Alignment.Bottom
        ) {
            // AI profile image (only for AI messages)
            if (!isFromUser) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    if (aiImage != null) {
                        AsyncImage(
                            model = aiImage,
                            contentDescription = "AI profile image",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
            }
            
            // Message bubble
            Column(
                modifier = Modifier.widthIn(max = 0.75f * 9999.dp)  // Using a large number for dp max
            ) {
                // AI name (for AI messages)
                if (!isFromUser) {
                    Text(
                        text = aiName,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier.padding(start = 12.dp, bottom = 4.dp)
                    )
                }
                
                // Message content
                Card(
                    shape = bubbleShape,
                    colors = androidx.compose.material3.CardDefaults.cardColors(
                        containerColor = bubbleColor
                    ),
                    modifier = Modifier.clickable(onClick = { showReactionControls = !showReactionControls })
                ) {
                    Text(
                        text = message.content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor,
                        modifier = Modifier.padding(12.dp)
                    )
                }
                
                // Timestamp
                Text(
                    text = formatMessageTime(message.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
        }
        
        // Reaction controls
        if (showReactionControls) {
            EmojiReactionBar(
                selectedReaction = userReaction,
                onReactionSelected = onReactionSelected,
                onReactionRemoved = onReactionRemoved,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .align(if (isFromUser) Alignment.Start else Alignment.End)
            )
        }
    }
}

/**
 * Limit reached dialog
 */
@Composable
fun LimitReachedDialog(
    onDismiss: () -> Unit,
    onSubscribe: () -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Message Limit Reached") },
        text = { 
            Text(
                "You've reached your free message limit for this AI companion. " +
                "Upgrade to premium to enjoy unlimited conversations with all AI companions."
            ) 
        },
        confirmButton = { 
            TextButton(onClick = onSubscribe) {
                Text("Upgrade to Premium")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Maybe Later")
            }
        }
    )
}

/**
 * Clickable modifier
 */
@Composable
fun Modifier.clickable(onClick: () -> Unit): Modifier = this
    .androidx.compose.foundation.clickable(onClick = onClick)