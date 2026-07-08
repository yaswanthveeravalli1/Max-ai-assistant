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
    primary = AetherPrimary,
    onPrimary = AetherOnPrimary,
    primaryContainer = AetherPrimaryContainer,
    onPrimaryContainer = AetherOnPrimaryContainer,
    inversePrimary = AetherInversePrimary,
    secondary = AetherSecondary,
    onSecondary = AetherOnSecondary,
    secondaryContainer = AetherSecondaryContainer,
    onSecondaryContainer = AetherOnSecondaryContainer,
    tertiary = AetherTertiary,
    onTertiary = AetherOnTertiary,
    tertiaryContainer = AetherTertiaryContainer,
    onTertiaryContainer = AetherOnTertiaryContainer,
    background = AetherBackground,
    onBackground = AetherOnBackground,
    surface = AetherSurface,
    onSurface = AetherOnSurface,
    surfaceVariant = AetherSurfaceVariant,
    onSurfaceVariant = AetherOnSurfaceVariant,
    surfaceTint = AetherPrimary,
    inverseSurface = AetherOnSurface,
    inverseOnSurface = Color(0xFF313030),
    error = AetherError,
    onError = AetherOnError,
    errorContainer = AetherErrorContainer,
    onErrorContainer = AetherOnErrorContainer,
    outline = AetherOutline,
    outlineVariant = AetherOutlineVariant
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Force DarkTheme by default for Aether HUD aesthetic
  dynamicColor: Boolean = false, // Disable dynamic color to enforce exact color hex specifications
  content: @Composable () -> Unit,
) {
  val colorScheme = DarkColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
