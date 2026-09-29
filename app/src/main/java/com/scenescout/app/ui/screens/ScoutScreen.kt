package com.scenescout.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.foundation.border
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.scenescout.app.data.MoodMatcher
import com.scenescout.app.data.ScenicScorer
import com.scenescout.app.data.Spot
import com.scenescout.app.data.VibeMatcher
import com.scenescout.app.ui.brand.BrandWatermark
import com.scenescout.app.ui.theme.GlassCard
import com.scenescout.app.ui.theme.GradientScoreBar
import com.scenescout.app.ui.theme.StaggeredItem

/**
 * AI Scout tab: two ways to find a location.
 *  - Vibe: describe the look you want ("moody neon alley, night shoot").
 *  - Song: paste lyrics or describe what the song feels like / is about —
 *    the matcher turns the song's mood into location matches and tells you
 *    which words drove each pick.
 */
@Composable
fun ScoutScreen(
    spots: List<Spot>,
    externalQuery: String,
    onSpotClick: (Spot) -> Unit,
) {
    var tab by remember { mutableIntStateOf(0) }
    var query by remember { mutableStateOf("") }
    var songText by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Pair<Spot, Int>>>(emptyList()) }
    var songResults by remember { mutableStateOf<List<MoodMatcher.SongMatch>>(emptyList()) }
    var searched by remember { mutableStateOf(false) }
    var songSearched by remember { mutableStateOf(false) }

    // Tag chips on Discover cards can send a query here.
    LaunchedEffect(externalQuery) {
        if (externalQuery.isNotBlank()) {
            tab = 0
            query = externalQuery
            results = VibeMatcher.rankForQuery(externalQuery, spots)
            searched = true
        }
    }

    Box(Modifier.fillMaxSize()) {
        BrandWatermark()
        Column(Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = tab) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Vibe") })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Song mood") })
        }
        Column(
            Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            if (tab == 0) {
                VibeSearch(
                    query = query,
                    onQueryChange = { query = it },
                    results = results,
                    searched = searched,
                    onSearch = {
                        results = VibeMatcher.rankForQuery(query, spots)
                        searched = true
                    },
                    onSpotClick = onSpotClick,
                )
            } else {
                SongSearch(
                    songText = songText,
                    onSongChange = { songText = it },
                    results = songResults,
                    searched = songSearched,
                    onSearch = {
                        songResults = MoodMatcher.rankForSong(songText, spots)
                        songSearched = true
                    },
                    onSpotClick = onSpotClick,
                )
            }
        }
    }
    }
}

@Composable
private fun VibeSearch(
    query: String,
    onQueryChange: (String) -> Unit,
    results: List<Pair<Spot, Int>>,
    searched: Boolean,
    onSearch: () -> Unit,
    onSpotClick: (Spot) -> Unit,
) {
    Text("AI Scout", style = MaterialTheme.typography.headlineSmall)
    Text(
        "Describe the vibe — SceneScout ranks matching spots by scenic score.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(12.dp))
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        label = { Text("e.g. moody neon alley, night shoot") },
        modifier = Modifier.fillMaxWidth(),
        minLines = 2,
    )
    Spacer(Modifier.height(12.dp))
    Button(onClick = onSearch, enabled = query.isNotBlank()) { Text("Find spots") }
    Spacer(Modifier.height(16.dp))
    if (searched) {
        if (results.isEmpty()) {
            ScoutEmptyState(
                "No matches yet",
                "Try words like neon, beach, gritty, truck, abandoned.",
            )
        } else {
            results.forEachIndexed { index, (spot, match) ->
                StaggeredItem(index = index) {
                    ScoutResultCard(spot, match, reasons = emptyList()) { onSpotClick(spot) }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    } else {
        Text(
            "Vision scoring is on the roadmap — your own uploads will " +
                "feed the cinematic scorer. " +
                "Today, Scout ranks spots with the on-device vibe and " +
                "song-mood matchers.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SongSearch(
    songText: String,
    onSongChange: (String) -> Unit,
    results: List<MoodMatcher.SongMatch>,
    searched: Boolean,
    onSearch: () -> Unit,
    onSpotClick: (Spot) -> Unit,
) {
    Text("Match the song", style = MaterialTheme.typography.headlineSmall)
    Text(
        "Paste lyrics or describe the song's feeling and what it's about — " +
            "lonely, rich, party, heartbreak — and get locations that feel the same.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(12.dp))
    OutlinedTextField(
        value = songText,
        onValueChange = onSongChange,
        label = { Text("Lyrics or feeling… e.g. lonely night drive, neon, heartbreak") },
        modifier = Modifier.fillMaxWidth(),
        minLines = 4,
    )
    Spacer(Modifier.height(12.dp))
    Button(onClick = onSearch, enabled = songText.isNotBlank()) {
        Text("Find locations for this song")
    }
    Spacer(Modifier.height(16.dp))
    if (searched) {
        if (results.isEmpty()) {
            ScoutEmptyState(
                "Nothing matched",
                "Try mood words like lonely, neon, party, rich, heartbreak, " +
                    "wild — or describe the story.",
            )
        } else {
            results.forEachIndexed { index, match ->
                StaggeredItem(index = index) {
                    ScoutResultCard(match.spot, match.score, match.reasons) {
                        onSpotClick(match.spot)
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun ScoutEmptyState(title: String, hint: String) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.Filled.SearchOff,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.size(40.dp),
        )
        Spacer(Modifier.height(8.dp))
        Text(title, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text(
            hint,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ScoutResultCard(
    spot: Spot,
    match: Int,
    reasons: List<String>,
    onClick: () -> Unit,
) {
    val score = ScenicScorer.scoreSpot(spot)
    GlassCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
            Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(spot.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    "match $match%", style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(Modifier.height(4.dp))
            GradientScoreBar(label = "Match", value = match)
            Spacer(Modifier.height(4.dp))
            Text(
                "${score.label} · ${score.overall}/100 scenic",
                style = MaterialTheme.typography.bodySmall,
            )
            if (reasons.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Why this fits:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    reasons.forEach { reason ->
                        // Informational pill — deliberately not clickable.
                        Text(
                            reason,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .border(
                                    1.dp, MaterialTheme.colorScheme.outlineVariant,
                                    MaterialTheme.shapes.small,
                                )
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                        )
                    }
                }
            }
        }
    }
}
