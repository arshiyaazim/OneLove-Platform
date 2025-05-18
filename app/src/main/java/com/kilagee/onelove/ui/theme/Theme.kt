package com.kilagee.onelove.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Light Theme Colors
private val LightColorScheme = lightColorScheme(
    primary = LovePrimary,
    onPrimary = LoveOnPrimary,
    primaryContainer = LovePrimaryContainer,
    onPrimaryContainer = LoveOnPrimaryContainer,
    secondary = LoveSecondary,
    onSecondary = LoveOnSecondary,
    secondaryContainer = LoveSecondaryContainer,
    onSecondaryContainer = LoveOnSecondaryContainer,
    tertiary = LoveTertiary,
    onTertiary = LoveOnTertiary,
    tertiaryContainer = LoveTertiaryContainer,
    onTertiaryContainer = LoveOnTertiaryContainer,
    error = LoveError,
    onError = LoveOnError,
    errorContainer = LoveErrorContainer,
    onErrorContainer = LoveOnErrorContainer,
    background = LoveBackground,
    onBackground = LoveOnBackground,
    surface = LoveSurface,
    onSurface = LoveOnSurface,
    surfaceVariant = LoveSurfaceVariant,
    onSurfaceVariant = LoveOnSurfaceVariant,
    outline = LoveOutline,
    outlineVariant = LoveOutlineVariant,
    scrim = LoveScrim
)

// Dark Theme Colors
private val DarkColorScheme = darkColorScheme(
    primary = LovePrimaryDark,
    onPrimary = LoveOnPrimaryDark,
    primaryContainer = LovePrimaryContainerDark,
    onPrimaryContainer = LoveOnPrimaryContainerDark,
    secondary = LoveSecondaryDark,
    onSecondary = LoveOnSecondaryDark,
    secondaryContainer = LoveSecondaryContainerDark,
    onSecondaryContainer = LoveOnSecondaryContainerDark,
    tertiary = LoveTertiaryDark,
    onTertiary = LoveOnTertiaryDark,
    tertiaryContainer = LoveTertiaryContainerDark,
    onTertiaryContainer = LoveOnTertiaryContainerDark,
    error = LoveErrorDark,
    onError = LoveOnErrorDark,
    errorContainer = LoveErrorContainerDark,
    onErrorContainer = LoveOnErrorContainerDark,
    background = LoveBackgroundDark,
    onBackground = LoveOnBackgroundDark,
    surface = LoveSurfaceDark,
    onSurface = LoveOnSurfaceDark,
    surfaceVariant = LoveSurfaceVariantDark,
    onSurfaceVariant = LoveOnSurfaceVariantDark,
    outline = LoveOutlineDark,
    outlineVariant = LoveOutlineVariantDark,
    scrim = LoveScrimDark
)

/**
 * Main theme composable for OneLove app
 */
@Composable
fun OneLoveTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    // Determine color scheme based on device version and preferences
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    
    // Set the status bar and navigation bar colors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    // Apply the complete Material 3 theme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = OneLoveTypography,
        shapes = OneLoveShapes,
        content = content
    )
}