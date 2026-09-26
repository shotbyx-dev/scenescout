package com.scenescout.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Shotbyx brand: chrome-on-black with the logo's red glow as the primary
// accent, golden-hour amber kept as the secondary for scores and sun.
private val ShotRed = Color(0xFFFF3D2E)
private val ShotRedDeep = Color(0xFF8F1D14)
private val Amber = Color(0xFFFFB300)
private val Chrome = Color(0xFFC9CCD6)
private val Ink = Color(0xFF08080A)
private val SurfaceDark = Color(0xFF101014)
private val TextSoft = Color(0xFFF2EFE8)

private val SceneScoutColors = darkColorScheme(
    primary = ShotRed,
    onPrimary = Color.White,
    primaryContainer = ShotRedDeep,
    onPrimaryContainer = Color.White,
    secondary = Amber,
    onSecondary = Color.Black,
    tertiary = Chrome,
    background = Ink,
    onBackground = TextSoft,
    surface = SurfaceDark,
    onSurface = TextSoft,
    surfaceVariant = Color(0xFF1B1B22),
    onSurfaceVariant = Color(0xFFB9B3A8),
    errorContainer = Color(0xFF3D1110),
    onErrorContainer = Color(0xFFFFB4AB),
)

@Composable
fun SceneScoutTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = SceneScoutColors, content = content)
}
