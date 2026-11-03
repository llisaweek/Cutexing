package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme =
  lightColorScheme(
    primary = CutestarPrimary,
    secondary = CutestarSecondary,
    tertiary = CutestarTertiary,
    background = BackgroundLight,
    surface = SurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onPrimary = SurfaceLight,
    onBackground = OnSurfaceLight,
    onSurface = OnSurfaceLight,
    onSurfaceVariant = OnSurfaceVariantLight,
  )

@Composable
fun CutestarTheme(
  darkTheme: Boolean = false, // Strictly light theme as requested
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  // Always enforce LightColorScheme
  MaterialTheme(
    colorScheme = LightColorScheme,
    typography = Typography,
    content = content,
  )
}

// Backward-compatible alias
@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = false,
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  CutestarTheme(darkTheme = false, dynamicColor = false, content = content)
}
