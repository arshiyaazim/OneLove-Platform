package com.kilagee.onelove.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kilagee.onelove.ui.theme.MessageIncomingShape
import com.kilagee.onelove.ui.theme.MessageOutgoingShape
import com.kilagee.onelove.ui.theme.OneLoveTheme
import com.kilagee.onelove.ui.theme.Purple40
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Chat list item component for displaying a chat in a list
 */
@Composable
fun ChatListItem(
    chatName: String,
    lastMessage: String?,
    time: Date,
    unreadCount: Int,
    avatarUrl: String?,
    isOnline: Boolean,
    isVerified: Boolean,
    isPinned: Boolean,
    onClick: () -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Avatar(
            imageUrl = avatarUrl,
            isOnline = isOnline,
            isVerified = isVerified,
            size = 56.dp
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = chatName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (unreadCount > 0) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                
                Text(
                    text = formatTimeForChat(time),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (unreadCount > 0) 
                        MaterialTheme.colorScheme.primary
                    else 
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.size(4.dp))
            
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (lastMessage != null) {
                    Text(
                        text = lastMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (unreadCount > 0) 
                            MaterialTheme.colorScheme.onBackground
                        else 
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Text(
                        text = "No messages yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
                
                if (unreadCount > 0) {
                    Spacer(modifier = Modifier.width(8.dp))
                    UnreadBadge(count = unreadCount)
                }
            }
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        
        IconButton(
            onClick = onMoreClick,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "More options",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Message bubble component for displaying a chat message
 */
@Composable
fun MessageBubble(
    message: String,
    isOutgoing: Boolean,
    time: Date,
    isRead: Boolean,
    hasMedia: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bubbleShape = if (isOutgoing) MessageOutgoingShape else MessageIncomingShape
    val bubbleColor = if (isOutgoing) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val textColor = if (isOutgoing) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = if (isOutgoing) Alignment.End else Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 320.dp)
                .clip(bubbleShape)
                .background(bubbleColor)
                .clickable { onClick() }
                .padding(12.dp)
        ) {
            Column {
                if (hasMedia) {
                    Box(
                        modifier = Modifier
                            .size(240.dp)
                            .clip(MaterialTheme.shapes.medium)
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = "Media",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                    Spacer(modifier = Modifier.size(8.dp))
                }
                
                Text(
                    text = message,
                    color = textColor,
                    style = MaterialTheme.typography.bodyLarge
                )
                
                Spacer(modifier = Modifier.size(4.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(
                        text = formatTimeForMessage(time),
                        style = MaterialTheme.typography.bodySmall,
                        color = textColor.copy(alpha = 0.7f)
                    )
                    
                    if (isOutgoing) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = if (isRead) Icons.Default.CheckCircle else Icons.Default.AccessTime,
                            contentDescription = if (isRead) "Read" else "Sent",
                            tint = if (isRead) Purple40 else textColor.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Unread message count badge
 */
@Composable
fun UnreadBadge(
    count: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.primary,
        shape = MaterialTheme.shapes.small,
        modifier = modifier
    ) {
        Text(
            text = if (count > 99) "99+" else count.toString(),
            color = MaterialTheme.colorScheme.onPrimary,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

/**
 * Format time for display in chat list
 */
private fun formatTimeForChat(date: Date): String {
    val now = Date()
    val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(now)
    val messageDay = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date)
    
    return when {
        today == messageDay -> SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
        else -> SimpleDateFormat("MM/dd", Locale.getDefault()).format(date)
    }
}

/**
 * Format time for display in message bubble
 */
private fun formatTimeForMessage(date: Date): String {
    return SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
}

@Preview(showBackground = true)
@Composable
fun ChatComponentsPreview() {
    OneLoveTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            ChatListItem(
                chatName = "Sarah Johnson",
                lastMessage = "Are we still meeting tomorrow?",
                time = Date(),
                unreadCount = 2,
                avatarUrl = null,
                isOnline = true,
                isVerified = true,
                isPinned = true,
                onClick = {},
                onMoreClick = {}
            )
            
            Spacer(modifier = Modifier.size(16.dp))
            
            MessageBubble(
                message = "Hey, how are you doing? I was wondering if you're free this weekend.",
                isOutgoing = false,
                time = Date(),
                isRead = true,
                onClick = {},
                onLongClick = {}
            )
            
            Spacer(modifier = Modifier.size(16.dp))
            
            MessageBubble(
                message = "I'm good, thanks! Yes, I'm free on Saturday afternoon.",
                isOutgoing = true,
                time = Date(),
                isRead = true,
                onClick = {},
                onLongClick = {}
            )
        }
    }
}