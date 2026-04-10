package com.workout.android.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = BrownPrimary,
    onPrimary = Background,
    primaryContainer = BrownContainer,
    onPrimaryContainer = OnBrownContainer,
    secondary = RestBlue,
    background = Background,
    surface = Surface,
    surfaceVariant = SurfaceVariant,
    onBackground = OnSurface,
    onSurface = OnSurface,
    onSurfaceVariant = OnSurfaceMuted,
    error = DangerRed
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF6B3F22),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFEDD5C0),
    onPrimaryContainer = Color(0xFF3E1A05),
    secondary = RestBlue,
    background = Color(0xFFFFFFFF),
    surface = Color(0xFFF5F5F5),
    surfaceVariant = Color(0xFFEBEBEB),
    onBackground = Color(0xFF111214),
    onSurface = Color(0xFF111214),
    onSurfaceVariant = Color(0xFF555558),
    error = DangerRed
)

@Composable
fun WorkoutAppTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = AppTypography,
        content = content
    )
}
