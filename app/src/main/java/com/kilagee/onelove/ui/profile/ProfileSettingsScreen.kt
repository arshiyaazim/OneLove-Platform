package com.kilagee.onelove.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.kilagee.onelove.navigation.Screen
import com.kilagee.onelove.ui.components.PremiumFeatureButton
import com.kilagee.onelove.ui.components.PremiumFeatureConditional
import com.kilagee.onelove.ui.components.PremiumFeatureGuard
import com.kilagee.onelove.ui.subscription.SubscriptionViewModel
import com.kilagee.onelove.util.PremiumAccessManager
import com.kilagee.onelove.util.PremiumFeature
import javax.inject.Inject

/**
 * Profile settings screen that demonstrates premium feature integration
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSettingsScreen(
    navController: NavController,
    premiumAccessManager: PremiumAccessManager,
    viewModel: SubscriptionViewModel = hiltViewModel()
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile Settings") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate(Screen.MyMembership.route) }) {
                        Icon(Icons.Default.Star, contentDescription = "Premium")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // Basic account settings section
            SettingsSectionHeader(title = "Account Settings")
            
            SettingsItem(
                icon = Icons.Default.Person,
                title = "Edit Profile",
                onClick = { /* Navigate to edit profile */ }
            )
            
            SettingsItem(
                icon = Icons.Default.Notifications,
                title = "Notification Preferences",
                onClick = { /* Navigate to notification settings */ }
            )
            
            SettingsItem(
                icon = Icons.Default.Lock,
                title = "Privacy Settings",
                onClick = { /* Navigate to privacy settings */ }
            )
            
            // Premium features section
            SettingsSectionHeader(title = "Premium Features")
            
            // Profile boost (requires Boost plan)
            PremiumFeatureButton(
                text = "Boost My Profile",
                feature = PremiumFeature.PROFILE_BOOST,
                premiumAccessManager = premiumAccessManager,
                navController = navController,
                icon = Icons.Default.TrendingUp,
                onClick = { /* Activate profile boost */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
            
            // Background verification (requires Premium plan)
            PremiumFeatureButton(
                text = "Verify My Identity",
                feature = PremiumFeature.BACKGROUND_VERIFICATION,
                premiumAccessManager = premiumAccessManager,
                navController = navController,
                icon = Icons.Default.VerifiedUser,
                onClick = { /* Start verification process */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
            
            // See who liked you (requires Boost plan)
            SettingsItem(
                icon = Icons.Default.Favorite,
                title = "See Who Liked Me",
                onClick = { /* Navigate to likes screen with premium check */ },
                rightContent = {
                    PremiumFeatureIconButton(
                        icon = Icons.Default.ArrowForward,
                        contentDescription = "View Likes",
                        feature = PremiumFeature.SEE_WHO_LIKED,
                        premiumAccessManager = premiumAccessManager,
                        navController = navController,
                        onClick = { /* Navigate to likes screen */ }
                    )
                }
            )
            
            // See profile visitors (requires Unlimited plan)
            SettingsItem(
                icon = Icons.Default.Visibility,
                title = "See Profile Visitors",
                onClick = { /* Navigate to visitors screen with premium check */ },
                rightContent = {
                    PremiumFeatureIconButton(
                        icon = Icons.Default.ArrowForward,
                        contentDescription = "View Visitors",
                        feature = PremiumFeature.SEE_PROFILE_VISITORS,
                        premiumAccessManager = premiumAccessManager,
                        navController = navController,
                        onClick = { /* Navigate to visitors screen */ }
                    )
                }
            )
            
            // Exclusive events (requires Premium plan)
            SettingsItem(
                icon = Icons.Default.Event,
                title = "Exclusive Events",
                onClick = { /* Navigate to events screen with premium check */ },
                rightContent = {
                    PremiumFeatureIconButton(
                        icon = Icons.Default.ArrowForward,
                        contentDescription = "View Events",
                        feature = PremiumFeature.EXCLUSIVE_EVENTS,
                        premiumAccessManager = premiumAccessManager,
                        navController = navController,
                        onClick = { /* Navigate to events screen */ }
                    )
                }
            )
            
            // Ad-free toggle (requires Premium plan)
            SettingsItem(
                icon = Icons.Default.Block,
                title = "Ad-Free Experience",
                rightContent = {
                    PremiumFeatureConditional(
                        feature = PremiumFeature.AD_FREE,
                        premiumAccessManager = premiumAccessManager,
                        premiumContent = {
                            Switch(
                                checked = true, // Always enabled for premium
                                onCheckedChange = { /* Can't change */ }
                            )
                        },
                        freeContent = {
                            TextButton(
                                onClick = { navController.navigate(Screen.Subscriptions.route) }
                            ) {
                                Text("Upgrade")
                            }
                        }
                    )
                }
            )
            
            Divider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            
            // Premium membership status
            SettingsItem(
                icon = Icons.Default.Star,
                title = "Premium Membership",
                subtitle = "Manage your subscription",
                onClick = { navController.navigate(Screen.MyMembership.route) }
            )
            
            // Support section (with priority support for Premium)
            SettingsSectionHeader(title = "Support")
            
            PremiumFeatureConditional(
                feature = PremiumFeature.PRIORITY_SUPPORT,
                premiumAccessManager = premiumAccessManager,
                premiumContent = {
                    SettingsItem(
                        icon = Icons.Default.SupportAgent,
                        title = "Priority Support",
                        subtitle = "Get faster assistance as a Premium member",
                        onClick = { /* Contact priority support */ }
                    )
                },
                freeContent = {
                    SettingsItem(
                        icon = Icons.Default.Help,
                        title = "Contact Support",
                        onClick = { /* Contact standard support */ }
                    )
                }
            )
            
            SettingsItem(
                icon = Icons.Default.Info,
                title = "About OneLove",
                onClick = { /* Show about screen */ }
            )
            
            Spacer(modifier = Modifier.height(72.dp)) // Bottom padding
        }
    }
}

@Composable
fun SettingsSectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 8.dp)
    )
}

@Composable
fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onClick: () -> Unit,
    rightContent: @Composable (() -> Unit)? = null
) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge
                )
                
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            if (rightContent != null) {
                rightContent()
            } else {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}