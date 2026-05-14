package com.example.nr.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = CivicTeal80,
    secondary = CivicBlue80,
    tertiary = CivicAmber80,
    primaryContainer = Color(0xFF0F3A3A),
    secondaryContainer = Color(0xFF172554),
    tertiaryContainer = Color(0xFF3B2F12),
    background = Color(0xFF07111F),
    surface = Color(0xFF0D1828),
    surfaceVariant = Color(0xFF172437),
    onPrimary = Color(0xFF062521),
    onSecondary = Color(0xFF071A3A),
    onTertiary = Color(0xFF3A2100),
    onPrimaryContainer = Color(0xFFBFF7EF),
    onSecondaryContainer = Color(0xFFDBEAFE),
    onTertiaryContainer = Color(0xFFFDE68A),
    onBackground = Color(0xFFEAF2F7),
    onSurface = Color(0xFFEAF2F7),
    onSurfaceVariant = Color(0xFFB8C7D9),
    outline = Color(0xFF53657A),
    error = Color(0xFFFCA5A5),
    errorContainer = Color(0xFF450A0A),
    onErrorContainer = Color(0xFFFEE2E2)
)

private val LightColorScheme = lightColorScheme(
    primary = CivicTeal40,
    secondary = CivicBlue40,
    tertiary = CivicAmber40,
    background = CivicBackground,
    surface = CivicSurface,
    surfaceVariant = CivicSurfaceVariant,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = CivicText,
    onSurface = CivicText,
    onSurfaceVariant = CivicMutedText,
    outline = CivicOutline
)

@Composable
fun NRTheme(
    darkTheme: Boolean = true,
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

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
