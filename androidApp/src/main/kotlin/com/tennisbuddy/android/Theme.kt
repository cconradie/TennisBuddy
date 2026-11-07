package com.tennisbuddy.android

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Palette taken from the logo: court green and tennis-ball yellow.
val TennisGreenDark = Color(0xFF0B3B2A)
val TennisGreen = Color(0xFF1B5E3A)
val TennisGreenLight = Color(0xFFDCEBDD)
val TennisYellow = Color(0xFFDFFD2B)
val TennisBackground = Color(0xFFF5FAEE)

private val TennisColors = lightColorScheme(
    primary = TennisGreen,
    onPrimary = Color.White,
    primaryContainer = TennisYellow,
    onPrimaryContainer = TennisGreenDark,
    secondary = TennisYellow,
    onSecondary = TennisGreenDark,
    secondaryContainer = TennisYellow,
    onSecondaryContainer = TennisGreenDark,
    tertiary = TennisGreenDark,
    onTertiary = TennisYellow,
    background = TennisBackground,
    onBackground = TennisGreenDark,
    surface = TennisBackground,
    onSurface = TennisGreenDark,
    surfaceVariant = TennisGreenLight,
    onSurfaceVariant = TennisGreenDark,
    outline = TennisGreen,
)

/** Always the green and yellow scheme, regardless of the phone's light/dark setting. */
@Composable
fun TennisBuddyTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = TennisColors, content = content)
}
