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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Custom OneLove Light colors
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFFE94057),        // OneLove Red
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDADB),
    onPrimaryContainer = Color(0xFF410009),
    secondary = Color(0xFF8A4EFF),      // Purple
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEADDFF),
    onSecondaryContainer = Color(0xFF25005A),
    tertiary = Color(0xFF24A19C),       // Teal
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFAEF1ED),
    onTertiaryContainer = Color(0xFF002A28),
    background = Color(0xFFFFFBFF),
    onBackground = Color(0xFF201A1A),
    surface = Color(0xFFFFFBFF),
    onSurface = Color(0xFF201A1A),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002)
)

// Custom OneLove Dark colors
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFFFB2B8),        // OneLove Red (dark variant)
    onPrimary = Color(0xFF68000E),
    primaryContainer = Color(0xFFC00020),
    onPrimaryContainer = Color(0xFFFFDADB),
    secondary = Color(0xFFD3BBFF),      // Purple (dark variant)
    onSecondary = Color(0xFF400A8F),
    secondaryContainer = Color(0xFF5B35B5),
    onSecondaryContainer = Color(0xFFEADDFF),
    tertiary = Color(0xFF8EDED9),       // Teal (dark variant)
    onTertiary = Color(0xFF004F4A),
    tertiaryContainer = Color(0xFF007571),
    onTertiaryContainer = Color(0xFFAEF1ED),
    background = Color(0xFF121212),
    onBackground = Color(0xFFECE0DF),
    surface = Color(0xFF121212),
    onSurface = Color(0xFFECE0DF),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6)
)

/**
 * OneLove theme that applies custom colors
 */
@Composable
fun OneLoveTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}