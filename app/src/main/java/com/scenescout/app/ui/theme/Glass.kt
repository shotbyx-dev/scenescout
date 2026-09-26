package com.scenescout.app.ui.theme

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Shotbyx glass system: translucent cards with hairline chrome borders,
 * red-glow brand background, staggered entrances and shimmer loading.
 * Dark, cinematic, unmistakably Shotbyx — never stock Material.
 */

private val GlassShape = RoundedCornerShape(20.dp)

/** Translucent glass card; pass onClick to make it tappable. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = CardDefaults.cardColors(
        containerColor = Color.White.copy(alpha = 0.06f),
    )
    val border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f))
    if (onClick != null) {
        Card(
            onClick = onClick, modifier = modifier,
            shape = GlassShape, border = border, colors = colors,
        ) { Column { content() } }
    } else {
        Card(
            modifier = modifier,
            shape = GlassShape, border = border, colors = colors,
        ) { Column { content() } }
    }
}

/**
 * Brand backdrop: near-black with a red glow bleeding from the top edge
 * (the Shotbyx logo glow) and a faint violet depth at the bottom.
 * Place behind screen content, above the theme background.
 */
@Composable
fun BrandGlowBackground() {
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .size(420.dp)
                .offset(x = (-90).dp, y = (-160).dp)
                .background(
                    Brush.radialGradient(
                        listOf(
                            Color(0xFFFF2E2E).copy(alpha = 0.22f),
                            Color.Transparent,
                        ),
                    ),
                ),
        )
        Box(
            Modifier
                .size(520.dp)
                .offset(x = 160.dp, y = 480.dp)
                .background(
                    Brush.radialGradient(
                        listOf(
                            Color(0xFF6A3DF0).copy(alpha = 0.12f),
                            Color.Transparent,
                        ),
                    ),
                ),
        )
    }
}

/** Shimmering placeholder shown while a photo loads. */
@Composable
fun ShimmerBox(modifier: Modifier) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val x by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1300), repeatMode = RepeatMode.Restart,
        ),
        label = "shimmerX",
    )
    Box(
        modifier.background(
            Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.03f),
                    Color.White.copy(alpha = 0.10f),
                    Color.White.copy(alpha = 0.03f),
                ),
                start = Offset(x * 900f - 450f, 0f),
                end = Offset(x * 900f + 150f, 200f),
            ),
        ),
    )
}

/**
 * One-shot staggered entrance for list items: fades and rises into place
 * with a per-index delay. Plays once per item identity.
 */
@Composable
fun StaggeredItem(
    index: Int,
    content: @Composable () -> Unit,
) {
    val state = remember {
        MutableTransitionState(false).apply { targetState = true }
    }
    AnimatedVisibility(
        visibleState = state,
        enter = fadeIn(tween(450, delayMillis = (index * 70).coerceAtMost(560))) +
            slideInVertically(
                tween(450, delayMillis = (index * 70).coerceAtMost(560)),
            ) { it / 4 },
    ) { content() }
}
