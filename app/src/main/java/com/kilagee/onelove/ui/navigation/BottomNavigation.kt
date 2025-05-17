package com.kilagee.onelove.ui.navigation

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState

/**
 * Bottom navigation items
 */
enum class BottomNavItem(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    HOME(NavRoutes.HOME, "Home", Icons.Filled.Home, Icons.Outlined.Home),
    MATCHES(NavRoutes.MATCHES, "Matches", Icons.Filled.Favorite, Icons.Outlined.Favorite),
    CHAT(NavRoutes.CHAT, "Chat", Icons.Filled.Chat, Icons.Outlined.Chat),
    AI_CHAT(NavRoutes.AI_CHAT, "AI Chat", Icons.Filled.SmartToy, Icons.Outlined.SmartToy),
    OFFERS(NavRoutes.OFFERS, "Offers", Icons.Filled.CardGiftcard, Icons.Outlined.CardGiftcard),
    PROFILE(NavRoutes.PROFILE, "Profile", Icons.Filled.Person, Icons.Outlined.Person),
    SETTINGS(NavRoutes.SETTINGS, "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
}

/**
 * App bottom navigation bar
 */
@Composable
fun BottomNavigationBar(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    // Only show bottom navigation on main screens, not detail screens
    val showBottomNav = when (currentRoute) {
        NavRoutes.HOME, 
        NavRoutes.MATCHES, 
        NavRoutes.CHAT, 
        NavRoutes.AI_CHAT,
        NavRoutes.OFFERS,
        NavRoutes.PROFILE,
        NavRoutes.SETTINGS -> true
        else -> false
    }
    
    if (showBottomNav) {
        NavigationBar(
            modifier = modifier
                .fillMaxWidth()
                .height(60.dp),
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            BottomNavItem.values().forEach { item ->
                NavigationBarItem(
                    icon = {
                        Icon(
                            imageVector = if (currentRoute == item.route) {
                                item.selectedIcon
                            } else {
                                item.unselectedIcon
                            },
                            contentDescription = item.title,
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    label = {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.bodySmall
                        )
                    },
                    selected = currentRoute == item.route,
                    onClick = {
                        // Avoid unnecessary navigation to the same route
                        if (currentRoute != item.route) {
                            navController.navigate(item.route) {
                                // Pop up to the start destination of the graph to
                                // avoid building up a large stack of destinations
                                popUpTo(NavRoutes.HOME) {
                                    saveState = true
                                }
                                // Avoid multiple copies of the same destination when
                                // reselecting the same item
                                launchSingleTop = true
                                // Restore state when reselecting a previously selected item
                                restoreState = true
                            }
                        }
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                )
            }
        }
    }
}