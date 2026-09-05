package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val StarkDarkColorScheme = darkColorScheme(
  primary = StarkCyanPrimary,
  onPrimary = StarkCyanOnPrimary,
  primaryContainer = StarkCyanContainer,
  onPrimaryContainer = StarkOnCyanContainer,
  secondary = StarkTealSecondary,
  onSecondary = Color(0xFF00382E),
  secondaryContainer = Color(0xFF005144),
  onSecondaryContainer = Color(0xFF73F8D4),
  tertiary = StarkGoldTertiary,
  onTertiary = Color(0xFF432C00),
  tertiaryContainer = StarkGoldContainer,
  onTertiaryContainer = Color(0xFFFFDF9E),
  background = StarkDarkBackground,
  onBackground = StarkTextPrimary,
  surface = StarkDarkSurface,
  onSurface = StarkTextPrimary,
  surfaceVariant = StarkSurfaceVariant,
  onSurfaceVariant = StarkTextSecondary,
  outline = StarkSurfaceBorder,
  error = StarkWarningRed,
  onError = Color.White
)

private val StarkLightColorScheme = lightColorScheme(
  primary = Color(0xFF006874),
  onPrimary = Color.White,
  primaryContainer = Color(0xFF97F0FF),
  onPrimaryContainer = Color(0xFF001F24),
  secondary = Color(0xFF006B5D),
  onSecondary = Color.White,
  tertiary = Color(0xFF825500),
  onTertiary = Color.White,
  background = Color(0xFFF8FAFC),
  onBackground = Color(0xFF0F172A),
  surface = Color(0xFFFFFFFF),
  onSurface = Color(0xFF0F172A),
  surfaceVariant = Color(0xFFE2E8F0),
  onSurfaceVariant = Color(0xFF475569),
  outline = Color(0xFFCBD5E1),
  error = Color(0xFFBA1A1A),
  onError = Color.White
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true,
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) StarkDarkColorScheme else StarkLightColorScheme
  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}

