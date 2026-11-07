package com.tennisbuddy.android

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Palette from the dashboard design: deep court green, darker cards, tennis-ball yellow.
val TennisBackground = Color(0xFF123F31)
val TennisCard = Color(0xFF0C2B22)
val TennisTile = Color(0xFF1B4A38)
val TennisYellow = Color(0xFFDDFB2B)
val TennisDarkText = Color(0xFF0B2A1F)
val TennisLoss = Color(0xFFE5604F)
val TennisWin = Color(0xFFB5E04B)

private val TennisColors = darkColorScheme(
    primary = TennisYellow,
    onPrimary = TennisDarkText,
    primaryContainer = TennisYellow,
    onPrimaryContainer = TennisDarkText,
    secondary = TennisYellow,
    onSecondary = TennisDarkText,
    tertiary = TennisWin,
    onTertiary = TennisDarkText,
    background = TennisBackground,
    onBackground = Color.White,
    surface = TennisBackground,
    onSurface = Color.White,
    surfaceVariant = TennisTile,
    onSurfaceVariant = Color.White,
    outline = TennisYellow,
    error = TennisLoss,
)

/** Always the dark green and yellow scheme, regardless of the phone's light/dark setting. */
@Composable
fun TennisBuddyTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = TennisColors, content = content)
}
