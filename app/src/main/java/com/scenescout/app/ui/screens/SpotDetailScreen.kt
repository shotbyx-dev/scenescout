package com.scenescout.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.scenescout.app.data.PermitLevel
import com.scenescout.app.data.ScenicScorer
import com.scenescout.app.data.Spot
import com.scenescout.app.data.SpotReview
import com.scenescout.app.data.SunTimes
import com.scenescout.app.data.imagery.BestImagery
import com.scenescout.app.data.imagery.SpotImage
import com.scenescout.app.ui.brief.BriefPdfButton
import java.time.LocalDate
import java.time.ZoneId

/** Full detail page for one spot: score breakdown, golden hour, permits, reviews. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpotDetailScreen(
    spot: Spot,
    reviews: List<SpotReview>,
    images: List<SpotImage>,
    onBack: () -> Unit,
) {
    val score = remember(spot) { ScenicScorer.scoreSpot(spot) }
    // Sample coordinates are Miami; real app uses the spot's city timezone.
    val sun = remember(spot) {
        SunTimes.forDate(
            spot.latitude, spot.longitude,
            LocalDate.now(), ZoneId.of("America/New_York"),
        )
    }
    val context = androidx.compose.ui.platform.LocalContext.current
    val shareSpot = {
        val bestFor = spot.bestFor.joinToString(", ") { it.label }
        val reviewLine = reviews.firstOrNull()?.let {
            "\uD83D\uDCAC \"${it.text}\" — ${it.author}\n"
        }.orEmpty()
        val text = buildString {
            appendLine("\uD83C\uDFAC ${spot.name} — ${score.overall}/100 (${score.label})")
            appendLine("\uD83D\uDCCD ${spot.latitude}, ${spot.longitude}")
            appendLine("\uD83C\uDF05 Golden hour: " +
                "${SunTimes.format(sun.morningGoldenStart)}–${SunTimes.format(sun.morningGoldenEnd)} / " +
                "${SunTimes.format(sun.eveningGoldenStart)}–${SunTimes.format(sun.eveningGoldenEnd)}")
            appendLine("\uD83C\uDFA5 Best for: $bestFor")
            appendLine("\uD83D\uDCCB Permit: ${spot.permit.level.label} — ${spot.permit.summary}")
            append(reviewLine)
            append("Shared from SceneScout — Created by Shotbyx")
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Shoot location: ${spot.name}")
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, "Share this spot"))
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(spot.name) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = shareSpot) {
                        Icon(Icons.Filled.Share, contentDescription = "Share this spot")
                    }
                },
            )
        },
    ) { inner ->
        Column(
            Modifier.fillMaxSize().padding(inner).padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Client one-pager: generate a PDF brief and share it.
            BriefPdfButton(
                spot = spot,
                reviews = reviews,
                images = images,
            )
            // Imagery strip — sharpest first, with attribution + AI badge.
            if (images.isNotEmpty()) {
                Column {
                    Text("Street views", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(images, key = { it.url }) { image ->
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface),
                            ) {
                                Column(Modifier.width(280.dp)) {
                                    AsyncImage(
                                        model = image.url,
                                        contentDescription = "View of ${spot.name}",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(160.dp)
                                            .clip(MaterialTheme.shapes.medium),
                                    )
                                    Column(Modifier.padding(8.dp)) {
                                        Text(image.source.attribution,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        if (image.source.analyzableByAi) {
                                            // Informational badge, not a button.
                                            Text(
                                                "✓ AI can score this",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (!BestImagery.hasAnalyzableImage(images)) {
                        Text(
                            "These previews are display-only (Google ToS). " +
                                "AI scoring uses Mapillary or uploaded photos.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            // Score breakdown
            Card(colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(16.dp)) {
                    Row(Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        Text("Scenic score", style = MaterialTheme.typography.titleMedium)
                        ScoreBadge(score.overall, score.label)
                    }
                    Spacer(Modifier.height(12.dp))
                    ScoreBar("AI visual appeal", score.visualAppeal)
                    ScoreBar("Shootability", score.shootability)
                    ScoreBar("Community", score.community)
                    Spacer(Modifier.height(8.dp))
                    Text(spot.description, style = MaterialTheme.typography.bodyMedium)
                }
            }
            // Golden hour
            Card(colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(16.dp)) {
                    Text("Best light today", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text("Morning golden hour: " +
                        "${SunTimes.format(sun.morningGoldenStart)} – " +
                        SunTimes.format(sun.morningGoldenEnd))
                    Text("Evening golden hour: " +
                        "${SunTimes.format(sun.eveningGoldenStart)} – " +
                        SunTimes.format(sun.eveningGoldenEnd))
                    Text("Sunrise ${SunTimes.format(sun.sunriseMinutes)} · " +
                        "Sunset ${SunTimes.format(sun.sunsetMinutes)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            // Permit
            Card(colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(16.dp)) {
                    Text("Permits", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(spot.permit.level.label,
                        color = when (spot.permit.level) {
                            PermitLevel.NONE -> MaterialTheme.colorScheme.primary
                            PermitLevel.UNKNOWN -> MaterialTheme.colorScheme.onSurfaceVariant
                            else -> MaterialTheme.colorScheme.error
                        },
                        style = MaterialTheme.typography.labelLarge)
                    Text(spot.permit.summary, style = MaterialTheme.typography.bodyMedium)
                    if (spot.permit.authorityName.isNotBlank()) {
                        Text(spot.permit.authorityName,
                            style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
            // Reviews
            Text("Shoot reports (${reviews.size})",
                style = MaterialTheme.typography.titleMedium)
            reviews.forEach { review ->
                Card(colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            repeat(review.stars) {
                                Icon(Icons.Filled.Star, contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary)
                            }
                            Spacer(Modifier.height(0.dp))
                            Text("  ${review.author}",
                                style = MaterialTheme.typography.labelMedium)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(review.text, style = MaterialTheme.typography.bodyMedium)
                        if (review.shootNotes.isNotBlank()) {
                            Text(review.shootNotes,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScoreBar(label: String, value: Int) {
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodySmall)
            Text("$value", style = MaterialTheme.typography.bodySmall)
        }
        LinearProgressIndicator(progress = { value / 100f },
            modifier = Modifier.fillMaxWidth())
    }
}
