package com.scenescout.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.scenescout.app.data.ScenicScorer
import com.scenescout.app.data.Spot
import com.scenescout.app.data.VibeMatcher

/**
 * AI Scout tab: describe the vibe you want ("moody neon alley for a night
 * music video") and get ranked matches. v1 matches on tags/description;
 * the production version sends Street View imagery to a vision model.
 */
@Composable
fun ScoutScreen(spots: List<Spot>, onSpotClick: (Spot) -> Unit) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Pair<Spot, Int>>>(emptyList()) }
    var searched by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("AI Scout", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Describe the vibe. AI ranks matching spots by scenic score.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("e.g. moody neon alley, night shoot") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = false,
            minLines = 2,
        )
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = {
                results = VibeMatcher.rankForQuery(query, spots)
                searched = true
            },
            enabled = query.isNotBlank(),
        ) { Text("Find spots") }
        Spacer(Modifier.height(16.dp))
        if (searched) {
            if (results.isEmpty()) {
                Text("No matches yet — try words like neon, beach, gritty, truck, abandoned.")
            } else {
                results.forEach { (spot, match) ->
                    ScoutResultCard(spot, match) { onSpotClick(spot) }
                    Spacer(Modifier.height(8.dp))
                }
            }
        } else {
            Text(
                "Street View AI analysis plugs in here: the app pulls imagery " +
                    "around you, scores each frame for cinematic quality, " +
                    "and pins the winners on your map.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ScoutResultCard(spot: Spot, match: Int, onClick: () -> Unit) {
    val score = ScenicScorer.scoreSpot(spot)
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(spot.name, style = MaterialTheme.typography.titleMedium)
                Text("vibe $match%", style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { score.overall / 100f },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(4.dp))
            Text("${score.label} · AI ${score.overall}/100",
                style = MaterialTheme.typography.bodySmall)
        }
    }
}

/** Simple keyword match; production version uses embeddings + vision scores. */
