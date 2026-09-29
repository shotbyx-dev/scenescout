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
import androidx.compose.material.icons.filled.PhotoCamera
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
import com.scenescout.app.ui.theme.GlassCard
import com.scenescout.app.ui.theme.GradientScoreBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.scenescout.app.data.imagery.GoogleImageryRepository
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
    imagery: GoogleImageryRepository,
    onBack: () -> Unit,
    /** Bumped when the API key changes — gallery refetches with the new key. */
    keyTick: Int = 0,
) {
    val score = remember(spot) { ScenicScorer.scoreSpot(spot) }
    // Google Place Photos for this place, loaded asynchronously.
    var images by remember(spot.id, keyTick) { mutableStateOf(imagery.imagesFor(spot)) }
    var photosLoading by remember(spot.id, keyTick) { mutableStateOf(true) }
    // URLs that failed to load (offline, revoked) — dropped from the gallery.
    var failedUrls by remember(spot.id, keyTick) { mutableStateOf(emptySet<String>()) }
    LaunchedEffect(spot.id, keyTick) {
        images = runCatching { imagery.imagesForAsync(spot) }.getOrNull()
            ?: imagery.imagesFor(spot)
        photosLoading = false
    }
    // Golden hour uses the device timezone — correct wherever the scout is.
    val sun = remember(spot) {
        SunTimes.forDate(
            spot.latitude, spot.longitude,
            LocalDate.now(), ZoneId.systemDefault(),
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
            appendLine("https://www.google.com/maps/search/?api=1&query=" +
                "${spot.latitude},${spot.longitude}")
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
        try {
            context.startActivity(Intent.createChooser(intent, "Share this spot"))
        } catch (_: Exception) {
            // No app on the device handles plain-text sharing (minimal ROMs,
            // restricted work profiles): never crash on a Share tap.
            android.widget.Toast.makeText(
                context, "No app available to share with", android.widget.Toast.LENGTH_SHORT,
            ).show()
        }
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
            // Imagery strip — real nearby photos, sharpest first, with credit.
            Column {
                Text("Photos near this spot", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                if (photosLoading && images.isEmpty()) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Finding real photos near this spot…",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (!photosLoading && images.isEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(vertical = 8.dp),
                    ) {
                        Icon(
                            Icons.Filled.PhotoCamera,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                .copy(alpha = 0.6f),
                            modifier = Modifier.width(40.dp).height(40.dp),
                        )
                        Text(
                            "No public photos found near this spot yet — " +
                                "scout it yourself and be the first to shoot it.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                // Drop failed URLs before layout: zero-size items would still
                // collect arrangement gaps, and the empty state must appear
                // when every image failed.
                val shown = images.filter { it.url !in failedUrls }
                if (shown.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(shown, key = { it.url }) { image ->
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
                                            onError = {
                                                failedUrls = failedUrls + image.url
                                            },
                                        )
                                        Column(Modifier.padding(8.dp)) {
                                            Text(image.credit ?: image.source.attribution,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            if (image.source.analyzableByAi) {
                                                // Informational badge, not a button.
                                                Text(
                                                    "✓ AI-eligible image",
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
                            "Place photos © Google contributors, " +
                                "shown with credit.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            // Score breakdown
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Row(Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        Text("Scenic score", style = MaterialTheme.typography.titleMedium)
                        ScoreBadge(score.overall, score.label)
                    }
                    Spacer(Modifier.height(12.dp))
                    GradientScoreBar("Visual appeal", score.visualAppeal)
                    GradientScoreBar("Shootability", score.shootability)
                    GradientScoreBar("Community", score.community)
                    Spacer(Modifier.height(8.dp))
                    Text(spot.description, style = MaterialTheme.typography.bodyMedium)
                }
            }
            // Golden hour
            GlassCard(modifier = Modifier.fillMaxWidth()) {
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
            GlassCard(modifier = Modifier.fillMaxWidth()) {
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
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            repeat(review.stars.coerceIn(0, 5)) {
                                Icon(Icons.Filled.Star, contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary)
                            }
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
