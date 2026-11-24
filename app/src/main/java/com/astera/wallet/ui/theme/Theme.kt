package com.astera.wallet.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val AsteraColorScheme =
  darkColorScheme(
    primary = AsteraPrimary,
    onPrimary = Color.White,
    primaryContainer = AsteraCard,
    onPrimaryContainer = Color.White,
    secondary = AsteraAmber,
    onSecondary = Color(0xFF101E40),
    secondaryContainer = AsteraAmberSoft,
    onSecondaryContainer = AsteraAmber,
    tertiary = AsteraSkyBlue,
    background = AsteraBgBottom,
    onBackground = AsteraTextPrimary,
    surface = AsteraCard,
    onSurface = AsteraTextPrimary,
    surfaceVariant = AsteraFieldBg,
    onSurfaceVariant = AsteraTextSecondary,
    outline = AsteraCardBorder,
    error = AsteraRed,
    onError = Color.White,
  )

private val AsteraShapes =
  Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
  )

@Composable
fun AsteraTheme(content: @Composable () -> Unit) {
  MaterialTheme(
    colorScheme = AsteraColorScheme,
    typography = AsteraTypography,
    shapes = AsteraShapes,
    content = content,
  )
}
