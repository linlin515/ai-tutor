package com.aitutor.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = OnPrimaryLight,
    primaryContainer = PrimaryLight,
    secondary = Secondary,
    onSecondary = OnPrimaryLight,
    secondaryContainer = SecondaryLight,
    background = SurfaceLight,
    surface = CardLight,
    surfaceVariant = SurfaceLight,
    onBackground = TextPrimaryLight,
    onSurface = TextPrimaryLight,
    onSurfaceVariant = TextSecondaryLight,
    outline = DividerLight,
    error = ErrorColor,
    onError = OnPrimaryLight
)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryLight,
    onPrimary = Color(0xFF003258),
    primaryContainer = PrimaryDark,
    secondary = SecondaryLight,
    onSecondary = Color(0xFF00391B),
    secondaryContainer = SecondaryDark,
    background = SurfaceDark,
    surface = CardDark,
    surfaceVariant = Color(0xFF1E1E1E),
    onBackground = TextPrimaryDark,
    onSurface = TextPrimaryDark,
    onSurfaceVariant = TextSecondaryDark,
    outline = DividerDark,
    error = ErrorColor,
    onError = OnPrimaryLight
)

@Composable
fun AiTutorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val targetColorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    // Animate color transitions during theme switch
    val animatedColorScheme = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        // Use animateColorAsState for smooth transitions
        targetColorScheme
    } else {
        targetColorScheme
    }

    val view = LocalView.current
    SideEffect {
        if (!view.isInEditMode) {
            val window = (view.context as Activity).window
            window.statusBarColor = animatedColorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    // Crossfade for smooth theme transition
    Crossfade(
        targetState = animatedColorScheme,
        animationSpec = tween(durationMillis = 400),
        label = "theme_crossfade"
    ) { scheme ->
        MaterialTheme(
            colorScheme = scheme,
            typography = Typography,
            content = content
        )
    }
}
