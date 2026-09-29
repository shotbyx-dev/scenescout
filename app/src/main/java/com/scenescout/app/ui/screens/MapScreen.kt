package com.scenescout.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.scenescout.app.data.Spot
import com.scenescout.app.data.imagery.GoogleImageryRepository
import com.scenescout.app.data.imagery.SpotImage
import com.scenescout.app.ui.theme.GlassCard
import com.scenescout.app.ui.theme.ShimmerBox

/** Dark basemap style so the map matches the cinematic theme. */
private const val DARK_MAP_STYLE = """[
  {"elementType":"geometry","stylers":[{"color":"#1a1a1e"}]},
  {"elementType":"labels.icon","stylers":[{"visibility":"off"}]},
  {"elementType":"labels.text.fill","stylers":[{"color":"#8a8a93"}]},
  {"elementType":"labels.text.stroke","stylers":[{"color":"#1a1a1e"}]},
  {"featureType":"administrative","elementType":"geometry","stylers":[{"color":"#2a2a30"}]},
  {"featureType":"poi","stylers":[{"visibility":"off"}]},
  {"featureType":"road","elementType":"geometry.fill","stylers":[{"color":"#2c2c33"}]},
  {"featureType":"road","elementType":"geometry.stroke","stylers":[{"color":"#1a1a1e"}]},
  {"featureType":"road","elementType":"labels.text.fill","stylers":[{"color":"#8a8a93"}]},
  {"featureType":"transit","stylers":[{"visibility":"off"}]},
  {"featureType":"water","elementType":"geometry","stylers":[{"color":"#0e1626"}]}
]"""

/**
 * Map tab: Google Maps centered on GPS with nearby shoot spots pinned.
 * Powered by the Maps SDK for Android (needs the API key); discovery and
 * photos come from the Places API.
 */
@Composable
fun MapScreen(
    spots: List<Spot>,
    userLocation: LatLng?,
    hasApiKey: Boolean,
    imagery: GoogleImageryRepository?,
    onSpotClick: (Spot) -> Unit,
    onOpenKeySettings: () -> Unit,
) {
    var selected by remember { mutableStateOf<Spot?>(null) }
    var recenterTick by remember { mutableStateOf(0) }
    val fallback = remember { LatLng(25.7826, -80.1867) } // Miami sample-data home
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(userLocation ?: fallback, 12f)
    }

    // Recenter when the GPS fix arrives after first composition,
    // or when the user taps the recenter button.
    LaunchedEffect(userLocation, recenterTick) {
        userLocation?.let {
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(it, 12f), 800,
            )
        }
    }

    Box(Modifier.fillMaxSize()) {
        if (!hasApiKey) {
            // Honest setup state instead of a broken map.
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                GlassCard(modifier = Modifier.padding(24.dp)) {
                    Column(Modifier.padding(20.dp)) {
                        Text(
                            "Google key needed",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Add your Google API key to unlock live discovery " +
                                "and real photos. The map tiles themselves need " +
                                "the key bundled into the app build — if the map " +
                                "is blank even with a key saved, the build is " +
                                "missing it.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = onOpenKeySettings) {
                            Text("Add API key")
                        }
                    }
                }
            }
        } else {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(
                    mapStyleOptions = MapStyleOptions(DARK_MAP_STYLE),
                ),
                uiSettings = MapUiSettings(
                    compassEnabled = false,
                    myLocationButtonEnabled = false,
                ),
                onMapClick = { selected = null },
            ) {
                spots.forEach { spot ->
                    Marker(
                        state = MarkerState(
                            position = LatLng(spot.latitude, spot.longitude),
                        ),
                        title = spot.name,
                        onClick = {
                            selected = spot
                            true
                        },
                    )
                }
            }
        }
        if (userLocation == null) {
            Card(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                        .copy(alpha = 0.92f),
                ),
            ) {
                Text(
                    "GPS unavailable — showing the Miami sample area. " +
                        "Enable location for spots near you.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(12.dp),
                )
            }
        }
        if (userLocation != null && hasApiKey) {
            FloatingActionButton(
                onClick = { recenterTick++ },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
            ) {
                Icon(Icons.Filled.MyLocation, contentDescription = "Center on my location")
            }
        }
        selected?.let { spot ->
            var previewVisible by remember(spot.id) { mutableStateOf(false) }
            LaunchedEffect(spot.id) { previewVisible = true }
            AnimatedVisibility(
                visible = previewVisible,
                enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { it / 2 },
                exit = fadeOut(tween(200)),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
            ) {
                GlassCard(onClick = { onSpotClick(spot) }) {
                    Column(Modifier.padding(12.dp)) {
                        var hero by remember(spot.id) { mutableStateOf<SpotImage?>(null) }
                        var photoLoaded by remember(spot.id) { mutableStateOf(false) }
                        LaunchedEffect(spot.id) {
                            hero = imagery?.heroForAsync(spot)
                        }
                        hero?.let { heroImage ->
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                                    .clip(MaterialTheme.shapes.medium),
                            ) {
                                if (!photoLoaded) ShimmerBox(Modifier.fillMaxSize())
                                AsyncImage(
                                    model = heroImage.url,
                                    contentDescription = "Preview of ${spot.name}",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                    onSuccess = { photoLoaded = true },
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                heroImage.credit ?: heroImage.source.attribution,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(4.dp))
                        }
                        Text(spot.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Tap to open spot details",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }
}
