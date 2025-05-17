package com.kilagee.onelove.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.kilagee.onelove.navigation.Screen
import com.kilagee.onelove.util.PremiumAccessManager
import com.kilagee.onelove.util.PremiumFeature
import kotlinx.coroutines.launch

/**
 * Composable that wraps premium content and shows a premium overlay
 * if the user doesn't have access to the specified feature
 */
@Composable
fun PremiumFeatureGuard(
    feature: PremiumFeature,
    premiumAccessManager: PremiumAccessManager,
    navController: NavController,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var hasAccess by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    
    // Check if user has access to the feature
    LaunchedEffect(feature) {
        isLoading = true
        hasAccess = premiumAccessManager.isFeatureAvailable(feature)
        isLoading = false
    }
    
    Box(modifier = modifier) {
        // Content
        content()
        
        // Premium overlay if user doesn't have access
        if (!hasAccess && !isLoading) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                PremiumFeatureOverlay(
                    feature = feature,
                    onUpgradeClick = { navController.navigate(Screen.Subscriptions.route) }
                )
            }
        }
        
        // Loading indicator
        if (isLoading) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}

/**
 * Composable that conditionally shows premium content or free alternative
 * based on user's access to the specified feature
 */
@Composable
fun PremiumFeatureConditional(
    feature: PremiumFeature,
    premiumAccessManager: PremiumAccessManager,
    modifier: Modifier = Modifier,
    premiumContent: @Composable () -> Unit,
    freeContent: @Composable () -> Unit = { }
) {
    var hasAccess by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    
    // Check if user has access to the feature
    LaunchedEffect(feature) {
        isLoading = true
        hasAccess = premiumAccessManager.isFeatureAvailable(feature)
        isLoading = false
    }
    
    Box(modifier = modifier) {
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            }
        } else if (hasAccess) {
            premiumContent()
        } else {
            freeContent()
        }
    }
}

/**
 * Premium feature overlay content
 */
@Composable
fun PremiumFeatureOverlay(
    feature: PremiumFeature,
    onUpgradeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val requiredSubscription = remember(feature) {
        getPremiumFeatureRequiredSubscription(feature)
    }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Premium badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "PREMIUM FEATURE",
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Lock icon
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(64.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Feature title
            Text(
                text = getPremiumFeatureTitle(feature),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Feature description
            Text(
                text = getPremiumFeatureDescription(feature),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Required subscription
            Text(
                text = "Requires $requiredSubscription plan or higher",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Upgrade button
            Button(
                onClick = onUpgradeClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text("Upgrade to Premium")
            }
        }
    }
}

// Helper functions
private fun getPremiumFeatureTitle(feature: PremiumFeature): String {
    return when (feature) {
        PremiumFeature.UNLIMITED_OFFERS -> "Unlimited Offers"
        PremiumFeature.VIDEO_CALLS -> "Video Calls"
        PremiumFeature.AUDIO_CALLS -> "Audio Calls"
        PremiumFeature.SEE_PROFILE_VISITORS -> "Profile Visitors"
        PremiumFeature.PROFILE_BOOST -> "Profile Boost"
        PremiumFeature.SEE_WHO_LIKED -> "See Who Liked You"
        PremiumFeature.PRIORITY_MATCHING -> "Priority Matching"
        PremiumFeature.PREMIUM_BADGE -> "Premium Badge"
        PremiumFeature.AD_FREE -> "Ad-Free Experience"
        PremiumFeature.BACKGROUND_VERIFICATION -> "Background Verification"
        PremiumFeature.EXCLUSIVE_EVENTS -> "Exclusive Events"
        PremiumFeature.PRIORITY_SUPPORT -> "Priority Support"
    }
}

private fun getPremiumFeatureDescription(feature: PremiumFeature): String {
    return when (feature) {
        PremiumFeature.UNLIMITED_OFFERS -> "Send unlimited offers to matches and increase your chances of finding the perfect match."
        PremiumFeature.VIDEO_CALLS -> "Connect face-to-face with your matches through high-quality video calls."
        PremiumFeature.AUDIO_CALLS -> "Talk directly with your matches through private voice calls."
        PremiumFeature.SEE_PROFILE_VISITORS -> "See who's viewed your profile and discover potential matches who are interested in you."
        PremiumFeature.PROFILE_BOOST -> "Get more visibility with profile boosts and increase your chances of matching."
        PremiumFeature.SEE_WHO_LIKED -> "See users who have already liked your profile, making matching easier."
        PremiumFeature.PRIORITY_MATCHING -> "Get priority in the matching algorithm and be shown to more users."
        PremiumFeature.PREMIUM_BADGE -> "Stand out with a premium badge on your profile that shows you're serious about dating."
        PremiumFeature.AD_FREE -> "Enjoy an ad-free experience throughout the app."
        PremiumFeature.BACKGROUND_VERIFICATION -> "Verify your identity and build trust with potential matches through our background verification process."
        PremiumFeature.EXCLUSIVE_EVENTS -> "Access exclusive virtual and in-person events with other premium members."
        PremiumFeature.PRIORITY_SUPPORT -> "Get priority customer support for any issues or questions."
    }
}

private fun getPremiumFeatureRequiredSubscription(feature: PremiumFeature): String {
    return when (feature) {
        PremiumFeature.UNLIMITED_OFFERS, 
        PremiumFeature.PROFILE_BOOST,
        PremiumFeature.SEE_WHO_LIKED -> "Boost"
        
        PremiumFeature.VIDEO_CALLS,
        PremiumFeature.AUDIO_CALLS,
        PremiumFeature.SEE_PROFILE_VISITORS,
        PremiumFeature.PRIORITY_MATCHING -> "Unlimited"
        
        PremiumFeature.PREMIUM_BADGE,
        PremiumFeature.AD_FREE,
        PremiumFeature.BACKGROUND_VERIFICATION,
        PremiumFeature.EXCLUSIVE_EVENTS,
        PremiumFeature.PRIORITY_SUPPORT -> "Premium"
    }
}