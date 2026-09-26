package com.scenescout.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import com.scenescout.app.data.ShootType
import com.scenescout.app.data.Spot
import com.scenescout.app.data.imagery.GoogleImageryRepository
import com.scenescout.app.ui.brand.BrandWatermark

/** Discover tab: ranked list of spots, filterable by shoot type. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DiscoverScreen(
    spots: List<Spot>,
    imagery: GoogleImageryRepository?,
    onSpotClick: (Spot) -> Unit,
    onTagClick: (String) -> Unit,
    hasApiKey: Boolean = true,
    /** Fired on keyboard search: runs a Google Text Search and merges results. */
    onServerSearch: (String) -> Unit = {},
) {
    var filter by remember { mutableStateOf<ShootType?>(null) }
    var query by remember { mutableStateOf("") }
    val visible = remember(spots, filter, query) {
        spots.filter { s ->
            (filter == null || s.bestFor.contains(filter)) &&
                (query.isBlank() ||
                    (s.name + " " + s.description + " " + s.tags.joinToString(" "))
                        .contains(query, ignoreCase = true))
        }
    }
    Column(Modifier.fillMaxSize()) {
        // Cinematic header with the Shotbyx watermark behind it.
        Box(
            Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                            Color.Transparent,
                        ),
                    ),
                ),
        ) {
            BrandWatermark(alpha = 0.10f)
            Column(Modifier.padding(start = 16.dp, top = 20.dp, end = 16.dp, bottom = 12.dp)) {
                Text("Find your next scene", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "Ranked by AI scenic score + community ratings",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        if (!hasApiKey) {
            Text(
                "Add your Google API key in About to unlock live Google " +
                    "discovery — showing sample spots for now.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Spacer(Modifier.height(8.dp))
        }
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            placeholder = { Text("Search spots by name, tag, vibe…") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = { if (query.isNotBlank()) onServerSearch(query) },
            ),
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
        SpotList(visible, imagery, onSpotClick, onTagClick = onTagClick)
    }
}
