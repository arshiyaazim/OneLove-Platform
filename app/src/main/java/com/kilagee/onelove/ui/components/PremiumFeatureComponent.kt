package com.kilagee.onelove.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kilagee.onelove.util.PremiumAccessManager
import com.kilagee.onelove.util.PremiumFeature

/**
 * Component that displays a premium feature lock screen with upgrade option
 */
@Composable
fun PremiumFeatureComponent(
    feature: PremiumFeature,
    premiumAccessManager: PremiumAccessManager? = null,
    navigateToSubscription: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (() -> Unit)? = null
) {
    val featureTitle = when (feature) {
        PremiumFeature.VIDEO_CALLS -> "Video Calls"
        PremiumFeature.AUDIO_CALLS -> "Audio Calls"
        PremiumFeature.UNLIMITED_MATCHES -> "Unlimited Matches"
        PremiumFeature.UNLIMITED_OFFERS -> "Unlimited Offers"
        PremiumFeature.PROFILE_BOOST -> "Profile Boost"
        PremiumFeature.AI_CHAT -> "AI Chat"
        PremiumFeature.USER_VERIFICATION -> "User Verification"
        PremiumFeature.AD_FREE -> "Ad-Free Experience"
        PremiumFeature.CUSTOM_THEMES -> "Custom Themes"
        PremiumFeature.UNDO_SWIPE -> "Undo Swipe"
    }
    
    val featureDescription = when (feature) {
        PremiumFeature.VIDEO_CALLS -> "Connect face-to-face with your matches through high-quality video calls."
        PremiumFeature.AUDIO_CALLS -> "Talk with your matches through crystal-clear audio calls."
        PremiumFeature.UNLIMITED_MATCHES -> "Remove limits and match with as many people as you want."
        PremiumFeature.UNLIMITED_OFFERS -> "Send unlimited special offers to your matches."
        PremiumFeature.PROFILE_BOOST -> "Get your profile seen by more people and increase your matches."
        PremiumFeature.AI_CHAT -> "Practice your dating skills with AI companions who respond like real people."
        PremiumFeature.USER_VERIFICATION -> "Get verified and show others you're genuine."
        PremiumFeature.AD_FREE -> "Enjoy OneLove without any advertisements."
        PremiumFeature.CUSTOM_THEMES -> "Personalize your OneLove experience with custom themes."
        PremiumFeature.UNDO_SWIPE -> "Made a mistake? Go back and give them another chance."
    }
    
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.tertiary
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Premium Feature",
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "$featureTitle is a Premium Feature",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = featureDescription,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = navigateToSubscription,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text("Upgrade to Premium")
                }
            }
        }
    }
}