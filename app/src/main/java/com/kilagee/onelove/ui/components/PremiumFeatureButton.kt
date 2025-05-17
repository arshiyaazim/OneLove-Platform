package com.kilagee.onelove.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.kilagee.onelove.data.model.SubscriptionType
import com.kilagee.onelove.navigation.Screen
import com.kilagee.onelove.util.PremiumAccessManager
import com.kilagee.onelove.util.PremiumFeature
import kotlinx.coroutines.launch

/**
 * Button for premium features that shows a lock icon and redirects to subscription screen
 * when the feature is premium but user doesn't have access
 */
@Composable
fun PremiumFeatureIconButton(
    icon: ImageVector,
    contentDescription: String,
    feature: PremiumFeature,
    premiumAccessManager: PremiumAccessManager,
    navController: NavController,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    requiresPremium: Boolean = true,
    tint: Color = LocalContentColor.current,
    enabled: Boolean = true
) {
    val coroutineScope = rememberCoroutineScope()
    var showPremiumDialog by remember { mutableStateOf(false) }
    var hasAccess by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    
    // Check if user has access to the feature
    LaunchedEffect(feature) {
        isLoading = true
        hasAccess = !requiresPremium || premiumAccessManager.isFeatureAvailable(feature)
        isLoading = false
    }
    
    IconButton(
        onClick = {
            if (isLoading) return@IconButton
            
            if (hasAccess) {
                onClick()
            } else {
                // Show premium dialog
                showPremiumDialog = true
            }
        },
        modifier = modifier,
        enabled = enabled && !isLoading
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = tint
            )
            
            // Show lock badge when premium required
            if (!hasAccess && !isLoading) {
                Badge(
                    containerColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.BottomEnd)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Premium Feature",
                        modifier = Modifier.size(8.dp)
                    )
                }
            }
            
            // Loading indicator
            AnimatedVisibility(
                visible = isLoading,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            }
        }
    }
    
    // Premium feature dialog
    if (showPremiumDialog) {
        PremiumFeatureDialog(
            feature = feature,
            premiumAccessManager = premiumAccessManager,
            onDismiss = { showPremiumDialog = false },
            onUpgradeClick = {
                showPremiumDialog = false
                navController.navigate(Screen.Subscriptions.route)
            }
        )
    }
}

/**
 * Button for premium features that shows a lock icon and redirects to subscription screen
 * when the feature is premium but user doesn't have access
 */
@Composable
fun PremiumFeatureButton(
    text: String,
    feature: PremiumFeature,
    premiumAccessManager: PremiumAccessManager,
    navController: NavController,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    requiresPremium: Boolean = true,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.buttonColors()
) {
    val coroutineScope = rememberCoroutineScope()
    var showPremiumDialog by remember { mutableStateOf(false) }
    var hasAccess by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    
    // Check if user has access to the feature
    LaunchedEffect(feature) {
        isLoading = true
        hasAccess = !requiresPremium || premiumAccessManager.isFeatureAvailable(feature)
        isLoading = false
    }
    
    Button(
        onClick = {
            if (isLoading) return@Button
            
            if (hasAccess) {
                onClick()
            } else {
                // Show premium dialog
                showPremiumDialog = true
            }
        },
        modifier = modifier,
        enabled = enabled && !isLoading,
        colors = colors
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = colors.contentColor.value,
                strokeWidth = 2.dp
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                }
                
                Text(text = text)
                
                if (!hasAccess) {
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Premium Feature",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
    
    // Premium feature dialog
    if (showPremiumDialog) {
        PremiumFeatureDialog(
            feature = feature,
            premiumAccessManager = premiumAccessManager,
            onDismiss = { showPremiumDialog = false },
            onUpgradeClick = {
                showPremiumDialog = false
                navController.navigate(Screen.Subscriptions.route)
            }
        )
    }
}

/**
 * Dialog that shows when a user tries to access a premium feature without the proper subscription
 */
@Composable
fun PremiumFeatureDialog(
    feature: PremiumFeature,
    premiumAccessManager: PremiumAccessManager,
    onDismiss: () -> Unit,
    onUpgradeClick: () -> Unit
) {
    val requiredSubscription = premiumAccessManager.getSubscriptionTypeForFeature(feature)
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Premium Feature",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = getFeatureDescription(feature),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Upgrade to ${getSubscriptionName(requiredSubscription)} to unlock this feature.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = onUpgradeClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Upgrade Now")
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Maybe Later")
                }
            }
        }
    }
}

/**
 * Get readable name for subscription type
 */
private fun getSubscriptionName(type: SubscriptionType): String {
    return when (type) {
        SubscriptionType.BASIC -> "Basic"
        SubscriptionType.BOOST -> "Boost"
        SubscriptionType.UNLIMITED -> "Unlimited"
        SubscriptionType.PREMIUM -> "Premium"
    }
}

/**
 * Get description for premium feature
 */
private fun getFeatureDescription(feature: PremiumFeature): String {
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