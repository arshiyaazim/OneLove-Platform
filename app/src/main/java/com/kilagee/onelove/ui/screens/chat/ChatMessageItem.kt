package com.kilagee.onelove.ui.screens.chat

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.kilagee.onelove.data.model.ChatMessage
import com.kilagee.onelove.data.model.MessageStatus
import com.kilagee.onelove.data.model.MessageType
import com.kilagee.onelove.data.model.ReactionSummary
import com.kilagee.onelove.data.model.ReactionTargetType
import com.kilagee.onelove.data.model.ReactionType
import com.kilagee.onelove.ui.components.EmojiReactionBar
import com.kilagee.onelove.ui.components.ReactionSummaryBar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * A composable that displays a single chat message
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatMessageItem(
    message: ChatMessage,
    isFromCurrentUser: Boolean,
    showSenderInfo: Boolean = false,
    senderName: String = "",
    senderImage: String? = null,
    onLongClick: () -> Unit = {},
    onImageClick: (String) -> Unit = {},
    reactionSummary: ReactionSummary? = null,
    currentUserReaction: ReactionType? = null,
    popularEmojis: List<ReactionType> = ReactionType.values().take(6),
    onReactionSelected: (String, ReactionType) -> Unit = { _, _ -> },
    onReactionRemoved: (String) -> Unit = {},
    onReactionSummaryClicked: (ReactionType) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val bubbleShape = RoundedCornerShape(
        topStart = if (isFromCurrentUser) 16.dp else 0.dp,
        topEnd = if (isFromCurrentUser) 0.dp else 16.dp,
        bottomStart = 16.dp,
        bottomEnd = 16.dp
    )
    
    val bubbleColor = if (isFromCurrentUser) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    
    val textColor = if (isFromCurrentUser) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    
    val arrangement = if (isFromCurrentUser) {
        Arrangement.End
    } else {
        Arrangement.Start
    }
    
    val maxWidth = 0.75f // Maximum width as a fraction of the screen width
    
    // Show reaction controls
    var showReactionControls by remember { mutableStateOf(false) }
    
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = if (isFromCurrentUser) Alignment.End else Alignment.Start
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = arrangement,
            verticalAlignment = Alignment.Bottom
        ) {
            // Sender's profile image (only for received messages)
            if (!isFromCurrentUser && showSenderInfo) {
                if (senderImage != null) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = senderImage,
                            contentDescription = "Sender profile image",
                            modifier = Modifier.fillMaxWidth(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
            }
            
            // Message bubble
            Column(
                modifier = Modifier.widthIn(max = maxWidth * 9999.dp) // Using a large number for dp max
            ) {
                // Sender name (for received messages)
                if (!isFromCurrentUser && showSenderInfo && senderName.isNotEmpty()) {
                    Text(
                        text = senderName,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 12.dp, bottom = 4.dp)
                    )
                }
                
                // Message content
                Surface(
                    shape = bubbleShape,
                    color = bubbleColor,
                    modifier = Modifier
                        .animateContentSize()
                        .combinedClickable(
                            onClick = {},
                            onLongClick = {
                                showReactionControls = !showReactionControls
                                onLongClick()
                            }
                        )
                ) {
                    when (message.type) {
                        MessageType.TEXT -> {
                            Text(
                                text = message.content,
                                style = MaterialTheme.typography.bodyMedium,
                                color = textColor,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                        
                        MessageType.IMAGE -> {
                            Column {
                                AsyncImage(
                                    model = message.content,
                                    contentDescription = "Image message",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable(onClick = { onImageClick(message.content) }),
                                    contentScale = ContentScale.FillWidth
                                )
                                
                                if (message.caption != null) {
                                    Text(
                                        text = message.caption,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = textColor,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            }
                        }
                        
                        MessageType.AUDIO -> {
                            // Audio message UI
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter = painterResource(id = android.R.drawable.ic_media_play),
                                    contentDescription = "Play audio",
                                    tint = textColor,
                                    modifier = Modifier.size(24.dp)
                                )
                                
                                Spacer(modifier = Modifier.width(8.dp))
                                
                                Text(
                                    text = "Audio message",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = textColor
                                )
                            }
                        }
                        
                        MessageType.FILE -> {
                            // File message UI
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter = painterResource(id = android.R.drawable.ic_menu_save),
                                    contentDescription = "File attachment",
                                    tint = textColor,
                                    modifier = Modifier.size(24.dp)
                                )
                                
                                Spacer(modifier = Modifier.width(8.dp))
                                
                                Text(
                                    text = "File: ${message.content.substringAfterLast('/')}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = textColor
                                )
                            }
                        }
                        
                        MessageType.LOCATION -> {
                            // Location message UI
                            Column(
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Text(
                                    text = "📍 Location",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = textColor
                                )
                                
                                Spacer(modifier = Modifier.height(4.dp))
                                
                                Box(
                                    modifier = Modifier
                                        .height(120.dp)
                                        .fillMaxWidth()
                                        .background(
                                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                            shape = RoundedCornerShape(8.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Map preview",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = textColor.copy(alpha = 0.7f),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                        
                        else -> {
                            Text(
                                text = "Unsupported message type",
                                style = MaterialTheme.typography.bodyMedium,
                                color = textColor,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
                
                // Timestamp and status
                Row(
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatMessageTime(message.timestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    
                    if (isFromCurrentUser) {
                        Spacer(modifier = Modifier.width(4.dp))
                        
                        // Message status indicator
                        Icon(
                            painter = when (message.status) {
                                MessageStatus.SENT -> painterResource(id = android.R.drawable.ic_media_next)
                                MessageStatus.DELIVERED -> painterResource(id = android.R.drawable.ic_media_pause)
                                MessageStatus.READ -> painterResource(id = android.R.drawable.ic_media_play)
                                else -> painterResource(id = android.R.drawable.ic_popup_sync)
                            },
                            contentDescription = message.status.name,
                            modifier = Modifier.size(12.dp),
                            tint = when (message.status) {
                                MessageStatus.READ -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            }
                        )
                    }
                }
                
                // Reaction summary
                if (reactionSummary != null && reactionSummary.totalCount > 0) {
                    Box(
                        modifier = Modifier.align(
                            if (isFromCurrentUser) Alignment.Start else Alignment.End
                        )
                    ) {
                        ReactionSummaryBar(
                            summary = reactionSummary,
                            onReactionClicked = onReactionSummaryClicked
                        )
                    }
                }
            }
        }
        
        // Reaction controls
        if (showReactionControls) {
            EmojiReactionBar(
                selectedReaction = currentUserReaction,
                popularEmojis = popularEmojis,
                onReactionSelected = { reaction -> onReactionSelected(message.id, reaction) },
                onReactionRemoved = { onReactionRemoved(message.id) },
                modifier = Modifier
                    .padding(top = 8.dp)
                    .align(if (isFromCurrentUser) Alignment.Start else Alignment.End)
            )
        }
    }
}

/**
 * Format timestamp to a readable time string
 */
@Composable
fun formatMessageTime(timestamp: Date): String {
    val now = Date()
    val isToday = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(timestamp) ==
            SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(now)
    
    return if (isToday) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(timestamp)
    } else {
        SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(timestamp)
    }
}

/**
 * Clickable modifier
 */
@Composable
fun Modifier.clickable(onClick: () -> Unit): Modifier = this
    .combinedClickable(
        onClick = onClick,
        onLongClick = {}
    )