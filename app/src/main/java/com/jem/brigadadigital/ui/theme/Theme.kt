package com.jem.brigadadigital.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = AccentOrange,
    onPrimary = TextOnAccent,
    primaryContainer = AccentOrange.copy(alpha = 0.2f),
    onPrimaryContainer = AccentOrange,
    
    secondary = AccentBlue,
    onSecondary = TextOnAccent,
    secondaryContainer = AccentBlue.copy(alpha = 0.2f),
    onSecondaryContainer = AccentBlue,
    
    tertiary = AccentTeal,
    onTertiary = TextOnAccent,
    tertiaryContainer = AccentTeal.copy(alpha = 0.2f),
    onTertiaryContainer = AccentTeal,
    
    background = DeepBackground,
    onBackground = TextPrimary,
    surface = SurfaceCard,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = TextSecondary,
    
    error = VibrantRed,
    onError = TextOnAccent,
    outline = BorderLight
)

@Composable
fun BrigadaDigitalTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme // Forced reference theme
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        androidx.compose.runtime.SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.surface.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}