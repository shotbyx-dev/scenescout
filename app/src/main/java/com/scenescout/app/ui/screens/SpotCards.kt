package com.scenescout.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.scenescout.app.data.ScenicScorer
import com.scenescout.app.data.Spot
import com.scenescout.app.data.imagery.GoogleImageryRepository
import com.scenescout.app.data.imagery.SpotImage
import com.scenescout.app.ui.theme.GlassCard
import com.scenescout.app.ui.theme.ShimmerBox
import com.scenescout.app.ui.theme.StaggeredItem

/** Shared spot card used by Discover + Community tabs. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpotCard(
    spot: Spot,
    heroImage: SpotImage?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onTagClick: (String) -> Unit = {},
    /** Bumped when the API key changes — resets photo state so images refetch. */
    keyTick: Int = 0,
) {
    val score = ScenicScorer.scoreSpot(spot)
    var photoLoaded by remember(spot.id, heroImage?.url, keyTick) { mutableStateOf(false) }
    var photoFailed by remember(spot.id, heroImage?.url, keyTick) { mutableStateOf(false) }
    GlassCard(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
    ) {
            // Cinematic image header — real imagery when available, otherwise a
            // vibe-tinted gradient so the list never looks empty.
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(150.dp),
            ) {
                if (heroImage != null && !photoFailed) {
                    if (!photoLoaded) ShimmerBox(Modifier.fillMaxSize())
                    AsyncImage(
                        model = heroImage.url,
                        contentDescription = "Preview of ${spot.name}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        onSuccess = { photoLoaded = true },
                        // Offline or broken URL: fall back to the gradient
                        // instead of shimmering forever.
                        onError = { photoFailed = true },
                    )
                } else {
                    val (top, bottom) = gradientFor(spot)
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(listOf(top, bottom)),
                            ),
                    )
                    Icon(
                        Icons.Filled.Place,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.75f),
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
                // Score floats over the imagery.
                ScoreBadge(
                    score.overall, score.label,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp),
                )
            }
            Column(Modifier.padding(16.dp)) {
                Text(spot.name, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    spot.description, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Filled.Star, contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        "%.1f (%d)".format(spot.communityRating, spot.reviewCount),
                        style = MaterialTheme.typography.labelMedium,
                    )
                    Icon(
                        Icons.Filled.Place, contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        spot.permit.level.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    spot.tags.take(3).forEach { tag ->
                        AssistChip(
                            onClick = { onTagClick(tag) },
                            label = { Text(tag) },
                        )
                    }
                }
                if (heroImage != null) {
                    Text(
                        heroImage.credit ?: heroImage.source.attribution,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
    }
}

@Composable
fun ScoreBadge(overall: Int, label: String, modifier: Modifier = Modifier) {
    val container = when {
        overall >= 85 -> Color(0xFF2E7D4F) // must shoot
        overall >= 70 -> MaterialTheme.colorScheme.primary // strong pick
        overall >= 55 -> Color(0xFFB26A00) // worth a look
        else -> Color(0xFF8C4A3C) // backup / skip
    }
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = container),
    ) {
        Column(
            Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "$overall", style = MaterialTheme.typography.titleMedium,
                color = Color.White,
            )
            Text(
                label, style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.9f),
            )
        }
    }
}

/** Picks a cinematic gradient from the spot's tags when no photo is loaded. */
private fun gradientFor(spot: Spot): Pair<Color, Color> {
    val tags = spot.tags.joinToString(" ").lowercase()
    return when {
        "neon" in tags || "night" in tags ->
            Color(0xFF2B1B4E) to Color(0xFF0D0B1E)
        "beach" in tags || "ocean" in tags || "water" in tags ->
            Color(0xFF0E6E6E) to Color(0xFF083B4C)
        "abandoned" in tags || "gritty" in tags || "industrial" in tags ->
            Color(0xFF6B3B1F) to Color(0xFF1F1712)
        "mural" in tags || "colorful" in tags || "graffiti" in tags ->
            Color(0xFF8A2B5C) to Color(0xFF2E1530)
        "historic" in tags || "garden" in tags ->
            Color(0xFF3F5D2A) to Color(0xFF16210F)
        else -> Color(0xFF3A3A4A) to Color(0xFF14141C)
    }
}

@Composable
fun SpotList(
    spots: List<Spot>,
    imagery: GoogleImageryRepository?,
    onSpotClick: (Spot) -> Unit,
    modifier: Modifier = Modifier,
    onTagClick: (String) -> Unit = {},
    /** Bumped when the API key changes — heroes refetch with the new key. */
    keyTick: Int = 0,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        itemsIndexed(spots, key = { _, s -> s.id }) { index, spot ->
            // Hero photos load async per card and are cached in the repo,
            // so scrolling stays smooth and each spot fetches only once.
            var hero by remember(spot.id, keyTick) { mutableStateOf<SpotImage?>(null) }
            LaunchedEffect(spot.id, keyTick) {
                hero = runCatching { imagery?.heroForAsync(spot) }.getOrNull()
            }
            StaggeredItem(index = index) {
                SpotCard(
                    spot, hero,
                    onClick = { onSpotClick(spot) },
                    onTagClick = onTagClick,
                    keyTick = keyTick,
                )
            }
        }
        item {
            Text(
                "Created by Shotbyx",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 4.dp),
            )
        }
    }
}
