package com.example.kerasondevice.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = KerasRed,
    onPrimary = OnSurfaceDark,
    primaryContainer = KerasRedContainer,
    onPrimaryContainer = OnSurfaceDark,
    secondary = SecondaryTextDark,
    onSecondary = OnSurfaceDark,
    secondaryContainer = SurfaceVariantDark,
    onSecondaryContainer = OnSurfaceDark,
    tertiary = Success,
    onTertiary = OnSurfaceDark,
    tertiaryContainer = SurfaceVariantDark,
    onTertiaryContainer = OnSurfaceDark,
    background = BackgroundDark,
    onBackground = OnSurfaceDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    surfaceTint = KerasRed,
    inverseSurface = SurfaceLight,
    inverseOnSurface = OnSurfaceLight,
    error = Error,
    onError = OnSurfaceDark,
    errorContainer = KerasRedContainer,
    onErrorContainer = OnSurfaceDark,
    outline = OutlineDark,
    outlineVariant = SurfaceVariantDark,
    scrim = BackgroundDark
)

private val LightColorScheme = lightColorScheme(
    primary = KerasRed,
    onPrimary = OnSurfaceLight,
    primaryContainer = Color(0xFFFFDAD6),
    onPrimaryContainer = Color(0xFF410002),
    secondary = OnSurfaceVariantLight,
    onSecondary = OnSurfaceLight,
    secondaryContainer = SurfaceVariantLight,
    onSecondaryContainer = OnSurfaceLight,
    tertiary = Success,
    onTertiary = OnSurfaceLight,
    tertiaryContainer = SurfaceVariantLight,
    onTertiaryContainer = OnSurfaceLight,
    background = BackgroundLight,
    onBackground = OnSurfaceLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    surfaceTint = KerasRed,
    inverseSurface = SurfaceDark,
    inverseOnSurface = OnSurfaceDark,
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
    outline = OutlineLight,
    outlineVariant = SurfaceVariantLight,
    scrim = BackgroundLight
)

@Composable
fun KerasOnDeviceTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            // Dynamic color disabled by default to enforce Keras brand identity
            if (darkTheme) DarkColorScheme else LightColorScheme
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = KerasTypography,
        content = content
    )
}
