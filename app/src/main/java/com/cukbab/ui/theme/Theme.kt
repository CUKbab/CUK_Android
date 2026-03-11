package com.cukbab.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

enum class ThemePreference {
    Light, Dark, System
}

val AccentColors = listOf(
    null, // System Default (Dynamic)
    Color(0xFF6750A4), // Purple
    Color(0xFF006A60), // Teal
    Color(0xFF984061), // Rose
    Color(0xFF3E5AA9), // Blue
    Color(0xFF6B5E00), // Yellow
    Color(0xFF006D3B), // Green
    Color(0xFFBF360C)  // Orange
)

@Composable
fun CUKbabTheme(
    themePreference: ThemePreference = ThemePreference.System,
    baseFontSize: Float = DEFAULT_FONT_SIZE,
    customAccentColor: Color? = null,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themePreference) {
        ThemePreference.Light -> false
        ThemePreference.Dark -> true
        ThemePreference.System -> isSystemInDarkTheme()
    }

    val context = LocalContext.current
    
    val colorScheme = when {
        customAccentColor != null -> {
            if (darkTheme) {
                darkColorScheme(
                    primary = customAccentColor,
                    secondary = customAccentColor,
                    primaryContainer = customAccentColor.copy(alpha = 0.3f),
                    secondaryContainer = customAccentColor.copy(alpha = 0.2f),
                )
            } else {
                lightColorScheme(
                    primary = customAccentColor,
                    secondary = customAccentColor,
                    primaryContainer = customAccentColor.copy(alpha = 0.1f),
                    secondaryContainer = customAccentColor.copy(alpha = 0.15f),
                )
            }
        }
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val insetsController = WindowCompat.getInsetsController(window, view)
            
            WindowCompat.setDecorFitsSystemWindows(window, false)
            
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
            
            @Suppress("DEPRECATION")
            window.statusBarColor = Color.Transparent.toArgb()
            @Suppress("DEPRECATION")
            window.navigationBarColor = Color.Transparent.toArgb()
            
            @Suppress("DEPRECATION")
            window.isStatusBarContrastEnforced = false
            @Suppress("DEPRECATION")
            window.isNavigationBarContrastEnforced = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = getTypography(baseFontSize),
        content = content
    )
}
