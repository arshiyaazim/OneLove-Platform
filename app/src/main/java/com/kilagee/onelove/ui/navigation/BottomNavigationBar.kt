package com.kilagee.onelove.ui.navigation

import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource

/**
 * Bottom navigation bar for the app's main navigation
 * 
 * @param currentDestination The current active destination
 * @param onNavigate Callback when a navigation item is clicked
 */
@Composable
fun BottomNavigationBar(
    currentDestination: NavDestinations,
    onNavigate: (NavDestinations) -> Unit
) {
    NavigationBar(
        modifier = Modifier.navigationBarsPadding(),
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 8.0f
    ) {
        NavDestinations.bottomNavItems.forEach { destination ->
            val isSelected = currentDestination.route == destination.route
            NavigationBarItem(
                icon = {
                    destination.iconResId?.let { iconResId ->
                        Icon(
                            painter = painterResource(id = iconResId),
                            contentDescription = destination.titleResId?.let { stringResource(it) },
                            tint = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                },
                label = {
                    destination.titleResId?.let { titleResId ->
                        Text(
                            text = stringResource(titleResId),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                },
                selected = isSelected,
                onClick = { onNavigate(destination) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    }
}