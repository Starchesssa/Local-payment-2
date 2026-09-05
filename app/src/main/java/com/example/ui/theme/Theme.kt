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
    primary = GeometricBluePrimaryDark,
    onPrimary = GeometricBlueOnPrimaryDark,
    primaryContainer = GeometricBlueContainerDark,
    onPrimaryContainer = GeometricBlueOnContainerDark,
    secondary = GeometricNavySecondaryDark,
    onSecondary = GeometricNavyOnSecondaryDark,
    secondaryContainer = GeometricNavyContainerDark,
    onSecondaryContainer = GeometricNavyOnContainerDark,
    background = GeometricBackgroundDark,
    surface = GeometricSurfaceDark,
    surfaceVariant = GeometricSurfaceVariantDark,
    onBackground = GeometricTextPrimaryDark,
    onSurface = GeometricTextPrimaryDark,
    onSurfaceVariant = GeometricTextSecondaryDark,
    outline = GeometricTextSecondaryDark,
    outlineVariant = GeometricBorderDark,
    error = StatusFailed
)

private val LightColorScheme = lightColorScheme(
    primary = GeometricBluePrimary,
    onPrimary = GeometricBlueOnPrimary,
    primaryContainer = GeometricBlueContainer,
    onPrimaryContainer = GeometricBlueOnContainer,
    secondary = GeometricNavySecondary,
    onSecondary = GeometricNavyOnSecondary,
    secondaryContainer = GeometricNavyContainer,
    onSecondaryContainer = GeometricNavyOnContainer,
    background = GeometricBackgroundLight,
    surface = GeometricSurfaceLight,
    surfaceVariant = GeometricSurfaceVariantLight,
    onBackground = GeometricTextPrimaryLight,
    onSurface = GeometricTextPrimaryLight,
    onSurfaceVariant = GeometricTextSecondaryLight,
    outline = GeometricTextSecondaryLight,
    outlineVariant = GeometricBorderLight,
    error = StatusFailed
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep Geometric Balance palette by default
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
