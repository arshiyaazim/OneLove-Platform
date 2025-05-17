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
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.kilagee.onelove.data.model.User
import com.kilagee.onelove.data.model.VerificationStatus
import com.kilagee.onelove.ui.theme.VerifiedColor

@Composable
fun SuggestionsColumn(
    userSuggestions: List<User>,
    aiProfiles: List<User>,
    onNavigateToProfile: (String) -> Unit,
    onSendOffer: (String) -> Unit,
    onNavigateToChat: (String) -> Unit,
    onNavigateToAIChat: (String) -> Unit,
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
            Text(
                text = "People Near You",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
            
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Normal user suggestions
                items(userSuggestions) { user ->
                    UserSuggestionItem(
                        user = user,
                        onProfileClick = { onNavigateToProfile(user.id) },
                        onChatClick = { onNavigateToChat(user.id) },
                        onOfferClick = { onSendOffer(user.id) }
                    )
                }
                
                item {
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    Text(
                        text = "AI Friends",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                
                // AI profiles
                items(aiProfiles) { aiUser ->
                    UserSuggestionItem(
                        user = aiUser,
                        onProfileClick = { onNavigateToProfile(aiUser.id) },
                        onChatClick = { onNavigateToAIChat(aiUser.id) },
                        onOfferClick = { /* No offers for AI */ },
                        isAI = true
                    )
                }
            }
        }
    }
}

@Composable
fun UserSuggestionItem(
    user: User,
    onProfileClick: () -> Unit,
    onChatClick: () -> Unit,
    onOfferClick: () -> Unit,
    isAI: Boolean = false
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Profile Image with Verification Badge
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .clickable { onProfileClick() }
            ) {
                // Use a placeholder if URL is empty
                val imageUrl = if (user.profilePictureUrl.isNotEmpty()) {
                    user.profilePictureUrl
                } else {
                    if (isAI) {
                        "https://images.unsplash.com/photo-1531243501393-a8996d8f527b" // AI profile placeholder
                    } else {
                        listOf(
                            "https://images.unsplash.com/photo-1499557354967-2b2d8910bcca",
                            "https://images.unsplash.com/photo-1521931961826-fe48677230a5",
                            "https://images.unsplash.com/photo-1586103516265-d0e1e1fd69de",
                            "https://images.unsplash.com/photo-1585282263861-f55e341878f8"
                        ).random()
                    }
                }
                
                Image(
                    painter = rememberAsyncImagePainter(model = imageUrl),
                    contentDescription = "Profile Picture",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                
                // Verification badge if verified
                if (user.verificationStatus == VerificationStatus.FULLY_VERIFIED) {
                    Badge(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(24.dp)
                            .padding(2.dp)
                            .background(VerifiedColor, CircleShape),
                        containerColor = VerifiedColor
                    ) {
                        Icon(
                            imageVector = Icons.Default.SmartToy,
                            contentDescription = "Verified",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                
                // AI badge
                if (isAI) {
                    Badge(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(2.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape),
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(
                            imageVector = Icons.Default.SmartToy,
                            contentDescription = "AI Profile",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // User name and location
            Text(
                text = if (isAI) "AI ${user.firstName}" else "${user.firstName}, ${user.age}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            Text(
                text = user.location,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Chat button
                IconButton(
                    onClick = onChatClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Chat,
                            contentDescription = "Chat",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Chat",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                
                // Offer button (not shown for AI profiles)
                if (!isAI) {
                    IconButton(
                        onClick = onOfferClick,
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.LocalOffer,
                                contentDescription = "Send Offer",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Offer",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}
