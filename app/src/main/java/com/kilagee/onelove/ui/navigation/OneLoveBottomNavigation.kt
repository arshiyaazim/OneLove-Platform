package com.kilagee.onelove.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Message
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState

/**
 * OneLove bottom navigation bar
 * 
 * @param navController Navigation controller
 */
@Composable
fun OneLoveBottomNavigation(
    navController: NavController
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        BottomNavItems.forEach { item ->
            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = if (isRouteSelected(currentRoute, item.route)) {
                            item.selectedIcon
                        } else {
                            item.unselectedIcon
                        },
                        contentDescription = item.title
                    )
                },
                label = { Text(text = item.title) },
                selected = isRouteSelected(currentRoute, item.route),
                onClick = {
                    if (!isRouteSelected(currentRoute, item.route)) {
                        navController.navigate(item.route) {
                            // Pop up to the start destination of the graph to
                            // avoid building up a large stack of destinations
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            // Avoid multiple copies of the same destination
                            launchSingleTop = true
                            // Restore state when reselecting a previously selected item
                            restoreState = true
                        }
                    }
                }
            )
        }
    }
}

/**
 * Check if the current route matches one of the main sections
 */
private fun isRouteSelected(currentRoute: String?, itemRoute: String): Boolean {
    if (currentRoute == null) return false
    
    return when (itemRoute) {
        Routes.DISCOVER -> currentRoute.startsWith(Routes.DISCOVER)
        Routes.MATCHES -> currentRoute.startsWith(Routes.MATCHES)
        Routes.CHAT -> currentRoute.startsWith(Routes.CHAT)
        Routes.PROFILE -> currentRoute.startsWith(Routes.PROFILE) && 
                !currentRoute.contains("/", ignoreCase = true)
        else -> currentRoute == itemRoute
    }
}

/**
 * Bottom navigation item
 */
private data class BottomNavItem(
    val title: String,
    val route: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

/**
 * List of bottom navigation items
 */
private val BottomNavItems = listOf(
    BottomNavItem(
        title = "Discover",
        route = Routes.DISCOVER,
        selectedIcon = Icons.Filled.Search,
        unselectedIcon = Icons.Outlined.Search
    ),
    BottomNavItem(
        title = "Matches",
        route = Routes.MATCHES,
        selectedIcon = Icons.Filled.Favorite,
        unselectedIcon = Icons.Outlined.FavoriteBorder
    ),
    BottomNavItem(
        title = "Chat",
        route = Routes.CHAT,
        selectedIcon = Icons.Filled.Message,
        unselectedIcon = Icons.Outlined.Message
    ),
    BottomNavItem(
        title = "Profile",
        route = Routes.PROFILE,
        selectedIcon = Icons.Filled.Person,
        unselectedIcon = Icons.Outlined.Person
    )
)