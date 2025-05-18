package com.kilagee.onelove.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Shapes for OneLove app following Material 3 guidelines
 */
val OneLoveShapes = Shapes(
    // Small components like chips, buttons
    small = RoundedCornerShape(4.dp),
    
    // Medium components like cards, dialogs
    medium = RoundedCornerShape(12.dp),
    
    // Large components like bottom sheets, navigation drawers
    large = RoundedCornerShape(16.dp),
    
    // Extra large components (custom)
    extraLarge = RoundedCornerShape(28.dp)
)