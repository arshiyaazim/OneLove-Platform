package com.kilagee.onelove.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.kilagee.onelove.R
import com.kilagee.onelove.data.model.Message
import com.kilagee.onelove.domain.model.Resource
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    navController: NavController,
    chatId: String,
    viewModel: ChatDetailViewModel = hiltViewModel()
) {
    val chatState by viewModel.chatState.collectAsState()
    val messagesState by viewModel.messagesState.collectAsState()
    val messageText = remember { mutableStateOf("") }
    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    
    // Effect to load chat and messages
    LaunchedEffect(chatId) {
        viewModel.loadChat(chatId)
        viewModel.loadMessages(chatId)
    }
    
    // Effect to scroll to bottom when new messages arrive
    LaunchedEffect(messagesState) {
        if (messagesState is Resource.Success) {
            val messages = (messagesState as Resource.Success<List<Message>>).data
            if (messages.isNotEmpty()) {
                coroutineScope.launch {
                    lazyListState.scrollToItem(messages.size - 1)
                }
            }
        }
    }
    
    Scaffold(
        topBar = {
            if (chatState is Resource.Success) {
                val chat = (chatState as Resource.Success).data
                CenterAlignedTopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(chat.profileImageUrl)
                                    .crossfade(true)
                                    .placeholder(R.drawable.ic_launcher_foreground)
                                    .build(),
                                contentDescription = chat.username,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                            
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            Column {
                                Text(
                                    text = chat.username ?: "",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                
                                if (chat.isOnline == true) {
                                    Text(
                                        text = "Online",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                } else {
                                    Text(
                                        text = "Offline",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { /* Open call screen */ }) {
                            Icon(Icons.Filled.Call, contentDescription = "Call")
                        }
                        IconButton(onClick = { /* Open video call screen */ }) {
                            Icon(Icons.Filled.VideoCall, contentDescription = "Video Call")
                        }
                        IconButton(onClick = { /* Open chat options */ }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "More Options")
                        }
                    }
                )
            } else {
                CenterAlignedTopAppBar(
                    title = { Text("Chat") },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { /* Open gallery */ }) {
                        Icon(Icons.Filled.Photo, contentDescription = "Attach Photo")
                    }
                    
                    OutlinedTextField(
                        value = messageText.value,
                        onValueChange = { messageText.value = it },
                        placeholder = { Text("Type a message...") },
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        ),
                        trailingIcon = {
                            IconButton(onClick = { /* Record voice message */ }) {
                                Icon(Icons.Filled.Mic, contentDescription = "Voice Message")
                            }
                        }
                    )
                    
                    FloatingActionButton(
                        onClick = {
                            if (messageText.value.isNotBlank()) {
                                viewModel.sendMessage(chatId, messageText.value)
                                messageText.value = ""
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Filled.Send, contentDescription = "Send Message")
                    }
                }
            }
        }
    ) { paddingValues ->
        // Messages area
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (messagesState) {
                is Resource.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is Resource.Success -> {
                    val messages = (messagesState as Resource.Success<List<Message>>).data
                    val currentUserId = viewModel.getCurrentUserId()
                    
                    if (messages.isEmpty()) {
                        // No messages yet
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            if (chatState is Resource.Success) {
                                val chat = (chatState as Resource.Success).data
                                
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(chat.profileImageUrl)
                                        .crossfade(true)
                                        .placeholder(R.drawable.ic_launcher_foreground)
                                        .build(),
                                    contentDescription = chat.username,
                                    modifier = Modifier
                                        .size(120.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                Text(
                                    text = "This is the beginning of your conversation with ${chat.username}",
                                    style = MaterialTheme.typography.bodyLarge,
                                    textAlign = TextAlign.Center
                                )
                            } else {
                                Text(
                                    text = "No messages yet. Start the conversation!",
                                    style = MaterialTheme.typography.bodyLarge,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        // Show messages
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            state = lazyListState,
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            items(messages) { message ->
                                val isCurrentUser = message.senderId == currentUserId
                                MessageItem(
                                    message = message,
                                    isCurrentUser = isCurrentUser
                                )
                            }
                        }
                    }
                }
                is Resource.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(48.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = (messagesState as Resource.Error).message,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Button(onClick = { viewModel.loadMessages(chatId) }) {
                            Text("Retry")
                        }
                    }
                }
                else -> {}
            }
        }
    }
}

@Composable
fun MessageItem(message: Message, isCurrentUser: Boolean) {
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val dateFormat = remember { SimpleDateFormat("MMM dd", Locale.getDefault()) }
    
    val formattedTime = remember(message.sentAt) {
        message.sentAt?.let { date ->
            val calendar = Calendar.getInstance()
            val today = Calendar.getInstance()
            
            calendar.time = date
            
            if (calendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                calendar.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
            ) {
                // Today
                timeFormat.format(date)
            } else {
                // Not today
                "${dateFormat.format(date)} ${timeFormat.format(date)}"
            }
        } ?: ""
    }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = if (isCurrentUser) Alignment.End else Alignment.Start
    ) {
        Surface(
            color = if (isCurrentUser) 
                MaterialTheme.colorScheme.primary 
            else 
                MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isCurrentUser) 16.dp else 4.dp,
                bottomEnd = if (isCurrentUser) 4.dp else 16.dp
            ),
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = message.text ?: "",
                    color = if (isCurrentUser) 
                        MaterialTheme.colorScheme.onPrimary 
                    else 
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Text(
                    text = formattedTime,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isCurrentUser) 
                        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f) 
                    else 
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
        
        // Read receipt
        if (isCurrentUser && message.readAt != null) {
            Text(
                text = "Read",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 2.dp, end = 4.dp)
            )
        }
    }
}