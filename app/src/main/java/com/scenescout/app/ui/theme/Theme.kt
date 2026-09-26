package com.scenescout.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Cinematic dark theme: deep blacks, warm amber accent (golden hour).
private val Amber = Color(0xFFFFB300)
private val AmberDim = Color(0xFF8A6100)
private val Ink = Color(0xFF0B0B0E)
private val SurfaceDark = Color(0xFF141419)
private val TextSoft = Color(0xFFE8E4DA)

private val SceneScoutColors = darkColorScheme(
    primary = Amber,
    onPrimary = Color.Black,
    secondary = AmberDim,
    background = Ink,
    onBackground = TextSoft,
    surface = SurfaceDark,
    onSurface = TextSoft,
    surfaceVariant = Color(0xFF1E1E26),
    onSurfaceVariant = Color(0xFFB9B3A4),
)

@Composable
fun SceneScoutTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = SceneScoutColors, content = content)
}
