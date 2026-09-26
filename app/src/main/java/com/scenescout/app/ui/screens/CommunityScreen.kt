package com.scenescout.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.scenescout.app.data.Spot
import com.scenescout.app.data.SpotReview
import com.scenescout.app.ui.brand.BrandWatermark
import com.scenescout.app.ui.theme.GlassCard
import com.scenescout.app.ui.theme.StaggeredItem

/** Community tab: latest reviews with shoot notes ("nobody bothered us", best light). */
@Composable
fun CommunityScreen(
    spots: List<Spot>,
    reviews: List<SpotReview>,
    onSpotClick: (Spot) -> Unit,
) {
    val byId = spots.associateBy { it.id }
    Box(Modifier.fillMaxSize()) {
        BrandWatermark()
        Column(Modifier.fillMaxSize()) {
        Text(
            "Community intel",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp),
        )
        Text(
            "Real shoot reports from videographers — stars, best light, hassle level.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Spacer(Modifier.height(8.dp))
        LazyColumn(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            itemsIndexed(reviews) { index, review ->
                val spot = byId[review.spotId]
                StaggeredItem(index = index) {
                    GlassCard(
                        onClick = { spot?.let(onSpotClick) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(spot?.name ?: "Unknown spot",
                                style = MaterialTheme.typography.titleSmall)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                repeat(review.stars) {
                                    Icon(Icons.Filled.Star, contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text("\"${review.text}\"",
                            style = MaterialTheme.typography.bodyMedium)
                        if (review.shootNotes.isNotBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text("Shoot notes: ${review.shootNotes}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (review.bestTime.isNotBlank()) {
                            Text("Best light: ${review.bestTime}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary)
                        }
                        Text("— ${review.author}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    }
                }
            }
        }
    }
}
}
