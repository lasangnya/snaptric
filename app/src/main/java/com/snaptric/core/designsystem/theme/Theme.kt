package com.snaptric.core.designsystem.theme

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

// ==========================
// Light Color Scheme
// ==========================
private val LightColorScheme = lightColorScheme(
    primary = Amber70,
    onPrimary = Color.White,
    primaryContainer = Amber90,
    onPrimaryContainer = Amber10,

    secondary = Charcoal50,
    onSecondary = Color.White,
    secondaryContainer = Charcoal95,
    onSecondaryContainer = Charcoal10,

    tertiary = NeutralVariant50,
    onTertiary = Color.White,
    tertiaryContainer = NeutralVariant90,
    onTertiaryContainer = NeutralVariant30,

    background = Color(0xFFF7F5F2), // Warm off-white
    onBackground = Charcoal10,

    surface = Color(0xFFFFFFFF),
    onSurface = Charcoal10,
    surfaceVariant = NeutralVariant90,
    onSurfaceVariant = NeutralVariant50,

    surfaceTint = Amber70,

    inverseSurface = Charcoal20,
    inverseOnSurface = Charcoal95,
    inversePrimary = Amber80,

    error = Error40,
    onError = Color.White,
    errorContainer = Error90,
    onErrorContainer = Error10,

    outline = NeutralVariant80,
    outlineVariant = NeutralVariant90,

    scrim = Charcoal10.copy(alpha = 0.6f)
)

// ==========================
// Dark Color Scheme
// ==========================
private val DarkColorScheme = darkColorScheme(
    primary = Amber70,
    onPrimary = Amber10,
    primaryContainer = Amber30,
    onPrimaryContainer = Amber90,

    secondary = Charcoal70,
    onSecondary = Charcoal10,
    secondaryContainer = Charcoal30,
    onSecondaryContainer = Charcoal95,

    tertiary = NeutralVariant60,
    onTertiary = Charcoal10,
    tertiaryContainer = NeutralVariant30,
    onTertiaryContainer = NeutralVariant90,

    background = Charcoal10,
    onBackground = Charcoal95,

    surface = Charcoal20,
    onSurface = Charcoal90,
    surfaceVariant = Charcoal30,
    onSurfaceVariant = Charcoal70,

    surfaceTint = Amber70,

    inverseSurface = Color(0xFFF7F5F2),
    inverseOnSurface = Charcoal10,
    inversePrimary = Amber40,

    error = Error80,
    onError = Error10,
    errorContainer = Error30,
    onErrorContainer = Error90,

    outline = Charcoal60,
    outlineVariant = Charcoal30,

    scrim = Charcoal10.copy(alpha = 0.8f)
)

@Composable
fun SnaptricTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Disabled by default to preserve brand identity
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = SnaptricTypography,
        shapes = SnaptricShapes,
        content = content
    )
}
