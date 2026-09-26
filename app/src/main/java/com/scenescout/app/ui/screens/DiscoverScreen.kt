package com.scenescout.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import com.scenescout.app.data.ShootType
import com.scenescout.app.data.Spot
import com.scenescout.app.data.imagery.SpotImage

/** Discover tab: ranked list of spots, filterable by shoot type. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DiscoverScreen(
    spots: List<Spot>,
    heroImageFor: (Spot) -> SpotImage?,
    onSpotClick: (Spot) -> Unit,
) {
    var filter by remember { mutableStateOf<ShootType?>(null) }
    val visible = remember(spots, filter) {
        if (filter == null) spots else spots.filter { it.bestFor.contains(filter) }
    }
    Column(Modifier.fillMaxSize()) {
        Text(
            "Top spots near you",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp),
        )
        Text(
            "Ranked by AI scenic score + community ratings",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Spacer(Modifier.height(8.dp))
        FlowRow(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ShootType.entries.forEach { type ->
                FilterChip(
                    selected = filter == type,
                    onClick = { filter = if (filter == type) null else type },
                    label = { Text(type.label) },
                )
            }
        }
        SpotList(visible, heroImageFor, onSpotClick)
    }
}
