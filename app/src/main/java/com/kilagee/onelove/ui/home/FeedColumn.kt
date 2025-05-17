package com.kilagee.onelove.ui.home

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Comment
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.SendTimeExtension
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.kilagee.onelove.data.model.Comment
import com.kilagee.onelove.data.model.MediaType
import com.kilagee.onelove.data.model.Post
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FeedColumn(
    posts: List<Post>,
    onLikePost: (String) -> Unit,
    onCommentPost: (String, String) -> Unit,
    onSendOffer: (String) -> Unit,
    onNavigateToProfile: (String) -> Unit,
    onCreatePost: () -> Unit,
    postContent: String,
    onPostContentChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.padding(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            // Create post section
            CreatePostSection(
                content = postContent,
                onContentChanged = onPostContentChanged,
                onCreatePost = onCreatePost
            )
            
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            // Posts feed
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                items(posts) { post ->
                    PostItem(
                        post = post,
                        onLikeClick = { onLikePost(post.id) },
                        onCommentClick = { comment -> onCommentPost(post.id, comment) },
                        onSendOfferClick = { onSendOffer(post.userId) },
                        onProfileClick = { onNavigateToProfile(post.userId) }
                    )
                    
                    Divider(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePostSection(
    content: String,
    onContentChanged: (String) -> Unit,
    onCreatePost: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Text(
            text = "Share something...",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        OutlinedTextField(
            value = content,
            onValueChange = onContentChanged,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("What's on your mind?") },
            minLines = 3,
            maxLines = 5
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(
                onClick = onCreatePost,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send Post"
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Post")
            }
        }
    }
}

@Composable
fun PostItem(
    post: Post,
    onLikeClick: () -> Unit,
    onCommentClick: (String) -> Unit,
    onSendOfferClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy · h:mm a", Locale.getDefault())
    val formattedDate = dateFormat.format(Date(post.createdAt.seconds * 1000))
    
    var showComments by remember { mutableStateOf(false) }
    var commentText by remember { mutableStateOf("") }
    val isLiked = false // This should be based on whether current user has liked the post
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        // Post header with profile info
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Profile picture
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable { onProfileClick() }
            ) {
                Image(
                    painter = rememberAsyncImagePainter(
                        model = post.userProfilePictureUrl.ifEmpty {
                            "https://images.unsplash.com/photo-1517292987719-0369a794ec0f"
                        }
                    ),
                    contentDescription = "Profile Picture",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Column {
                Text(
                    text = post.username,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // Post content
        Text(
            text = post.content,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        
        // Post media (if any)
        if (post.mediaType != MediaType.NONE && post.mediaUrl.isNotEmpty()) {
            when (post.mediaType) {
                MediaType.IMAGE -> {
                    Image(
                        painter = rememberAsyncImagePainter(model = post.mediaUrl),
                        contentDescription = "Post Image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(MaterialTheme.shapes.medium),
                        contentScale = ContentScale.Crop
                    )
                }
                MediaType.VIDEO -> {
                    // Video player would go here
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(MaterialTheme.shapes.medium)
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Video Content",
                            color = Color.White
                        )
                    }
                }
                else -> { /* Do nothing */ }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
        }
        
        // Interaction stats
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${post.likes.size} likes",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Text(
                text = "${post.comments.size} comments",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.clickable { showComments = !showComments }
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Text(
                text = "${post.offers.size} offers",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        // Interaction buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Like button
            IconButton(onClick = onLikeClick) {
                Icon(
                    imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Like",
                    tint = if (isLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }
            
            // Comment button
            IconButton(onClick = { showComments = !showComments }) {
                Icon(
                    imageVector = Icons.Default.Comment,
                    contentDescription = "Comment"
                )
            }
            
            // Send offer button
            IconButton(onClick = onSendOfferClick) {
                Icon(
                    imageVector = Icons.Outlined.LocalOffer,
                    contentDescription = "Send Offer"
                )
            }
        }
        
        // Comments section (if expanded)
        if (showComments) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                // Comment input field
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = commentText,
                        onValueChange = { commentText = it },
                        placeholder = { Text("Add a comment...") },
                        modifier = Modifier.weight(1f),
                        maxLines = 2
                    )
                    
                    IconButton(
                        onClick = {
                            if (commentText.isNotEmpty()) {
                                onCommentClick(commentText)
                                commentText = ""
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.SendTimeExtension,
                            contentDescription = "Post Comment"
                        )
                    }
                }
                
                // Comment list
                if (post.comments.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    post.comments.forEach { comment ->
                        CommentItem(comment = comment)
                    }
                }
            }
        }
    }
}

@Composable
fun CommentItem(comment: Comment) {
    val dateFormat = SimpleDateFormat("MMM dd · h:mm a", Locale.getDefault())
    val formattedDate = dateFormat.format(Date(comment.createdAt.seconds * 1000))
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        // Profile picture
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
        ) {
            Image(
                painter = rememberAsyncImagePainter(
                    model = comment.userProfilePictureUrl.ifEmpty {
                        "https://images.unsplash.com/photo-1586103516265-d0e1e1fd69de"
                    }
                ),
                contentDescription = "Commenter Picture",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = comment.username,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Text(
                text = comment.content,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
