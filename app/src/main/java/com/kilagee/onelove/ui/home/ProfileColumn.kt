package com.kilagee.onelove.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
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
import com.kilagee.onelove.data.model.MembershipTier
import com.kilagee.onelove.data.model.User
import com.kilagee.onelove.data.model.VerificationStatus
import com.kilagee.onelove.ui.theme.NotVerifiedColor
import com.kilagee.onelove.ui.theme.TempApprovedColor
import com.kilagee.onelove.ui.theme.VerifiedColor

@Composable
fun ProfileColumn(
    currentUser: User?,
    onNavigateToProfile: () -> Unit,
    onNavigateToEditProfile: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    
    Card(
        modifier = modifier.padding(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Profile Image and Basic Info
            if (currentUser != null) {
                ProfileHeader(user = currentUser, onNavigateToProfile = onNavigateToProfile)
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Verification Badge
                VerificationBadge(status = currentUser.verificationStatus)
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Membership Status
                MembershipStatus(tier = currentUser.membershipTier)
                
                Spacer(modifier = Modifier.height(8.dp))
                Divider()
                Spacer(modifier = Modifier.height(8.dp))
                
                // Points & Wallet
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = currentUser.points.toString(),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Points",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$${String.format("%.2f", currentUser.wallet)}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Wallet",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                Divider()
                Spacer(modifier = Modifier.height(8.dp))
                
                // Offers Section
                Text(
                    text = "Offers",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Start)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Offers Stats
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OfferStat(
                        count = currentUser.offers.sent.size,
                        label = "Sent"
                    )
                    OfferStat(
                        count = currentUser.offers.received.size,
                        label = "Received"
                    )
                    OfferStat(
                        count = currentUser.offers.declined.size,
                        label = "Declined"
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Upload Options
                Text(
                    text = "Upload",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Start)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    UploadButton(
                        icon = Icons.Default.Photo,
                        label = "Photo",
                        onClick = { /* Photo upload */ }
                    )
                    
                    UploadButton(
                        icon = Icons.Default.VideoLibrary,
                        label = "Video",
                        onClick = { /* Video upload */ }
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Edit Profile Button
                Button(
                    onClick = onNavigateToEditProfile,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Profile"
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("Edit Profile")
                }
            } else {
                // Loading or not logged in state
                Text(
                    text = "Loading profile...",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

@Composable
fun ProfileHeader(
    user: User,
    onNavigateToProfile: () -> Unit
) {
    // Profile Image (use a placeholder if URL is empty)
    val imageUrl = if (user.profilePictureUrl.isNotEmpty()) {
        user.profilePictureUrl
    } else {
        "https://images.unsplash.com/photo-1517292987719-0369a794ec0f"
    }
    
    Box(
        modifier = Modifier
            .size(120.dp)
            .clip(CircleShape)
            .clickable { onNavigateToProfile() }
            .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
    ) {
        Image(
            painter = rememberAsyncImagePainter(model = imageUrl),
            contentDescription = "Profile Picture",
            modifier = Modifier.fillMaxWidth(),
            contentScale = ContentScale.Crop
        )
    }
    
    Spacer(modifier = Modifier.height(8.dp))
    
    // User name
    Text(
        text = "${user.firstName} ${user.lastName}",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
    
    // Username
    Text(
        text = "@${user.username}",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.primary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
    
    // Location
    Text(
        text = "${user.location}, ${user.country}",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
fun VerificationBadge(status: VerificationStatus) {
    val (backgroundColor, textColor, text) = when (status) {
        VerificationStatus.FULLY_VERIFIED -> Triple(
            VerifiedColor.copy(alpha = 0.1f),
            VerifiedColor,
            "Fully Verified"
        )
        VerificationStatus.TEMPORARILY_APPROVED -> Triple(
            TempApprovedColor.copy(alpha = 0.1f),
            TempApprovedColor,
            "Temporarily Approved"
        )
        VerificationStatus.NOT_VERIFIED -> Triple(
            NotVerifiedColor.copy(alpha = 0.1f),
            NotVerifiedColor,
            "Not Verified"
        )
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(backgroundColor)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (status == VerificationStatus.FULLY_VERIFIED) {
            Icon(
                imageVector = Icons.Default.Verified,
                contentDescription = "Verified",
                tint = textColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.size(4.dp))
        }
        
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = textColor,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun MembershipStatus(tier: MembershipTier) {
    val (backgroundColor, textColor, text) = when (tier) {
        MembershipTier.VIP -> Triple(
            Color(0xFFFFD700).copy(alpha = 0.1f),
            Color(0xFFFFD700),
            "VIP Member"
        )
        MembershipTier.PREMIUM -> Triple(
            Color(0xFF9370DB).copy(alpha = 0.1f),
            Color(0xFF9370DB),
            "Premium Member"
        )
        MembershipTier.BASIC -> Triple(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
            MaterialTheme.colorScheme.primary,
            "Basic Member"
        )
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(backgroundColor)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = textColor,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun OfferStat(count: Int, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
fun UploadButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall
        )
    }
}
