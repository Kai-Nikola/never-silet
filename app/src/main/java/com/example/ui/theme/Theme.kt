package com.example.ui.theme

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
    primary = GuardianCyan,
    onPrimary = Color(0xFF00363F),
    primaryContainer = GuardianCyanDark,
    onPrimaryContainer = Color.White,
    secondary = GuardianTeal,
    onSecondary = Color(0xFF003830),
    error = GuardianCoral,
    onError = Color.White,
    background = SlateBackgroundDark,
    onBackground = TextPrimaryDark,
    surface = SlateSurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = SlateCardDark,
    onSurfaceVariant = TextSecondaryDark,
    outline = SlateBorderDark
)

private val LightColorScheme = lightColorScheme(
    primary = GuardianCyanLight,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6F2FE),
    onPrimaryContainer = Color(0xFF001F29),
    secondary = GuardianTealLight,
    onSecondary = Color.White,
    error = GuardianCoral,
    onError = Color.White,
    background = SlateBackgroundLight,
    onBackground = TextPrimaryLight,
    surface = SlateSurfaceLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = SlateCardLight,
    onSurfaceVariant = TextSecondaryLight,
    outline = SlateBorderLight
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Use our crafted sleek guardian theme
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
