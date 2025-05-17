package com.kilagee.onelove.ui.navigation

/**
 * Interface for defining app navigation destinations
 */
interface NavigationDestination {
    /**
     * Unique route for the destination
     */
    val route: String
    
    /**
     * Title to be displayed in the app bar
     */
    val titleRes: Int
    
    /**
     * Icon resource for the destination (for bottom navigation)
     */
    val iconRes: Int?
        get() = null
}