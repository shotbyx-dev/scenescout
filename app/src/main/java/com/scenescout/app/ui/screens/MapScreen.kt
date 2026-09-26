package com.scenescout.app.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.scenescout.app.BuildConfig
import com.scenescout.app.data.Spot

/**
 * Map tab: shows the user's area with nearby shoot spots pinned.
 * Renders a real Google Map once MAPS_API_KEY is set in local.properties;
 * until then it shows setup guidance instead of a blank map.
 */
@Composable
fun MapScreen(spots: List<Spot>, onSpotClick: (Spot) -> Unit) {
    val hasKey = BuildConfig.MAPS_API_KEY != "MAPS_API_KEY_NOT_SET"
    if (!hasKey) {
        MapPlaceholder()
        return
    }
    // Default to Miami (sample data home); real app centers on device location.
    val defaultCenter = LatLng(25.7826, -80.1867)
    val cameraState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultCenter, 11f)
    }
    var selected by remember { mutableStateOf<Spot?>(null) }

    Box(Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraState,
        ) {
            spots.forEach { spot ->
                Marker(
                    state = MarkerState(LatLng(spot.latitude, spot.longitude)),
                    title = spot.name,
                    snippet = "Tap for details",
                    onClick = {
                        selected = spot
                        false
                    },
                )
            }
        }
        selected?.let { spot ->
            androidx.compose.material3.Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                onClick = { onSpotClick(spot) },
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(spot.name, style = MaterialTheme.typography.titleMedium)
                    Text("Tap to open spot details",
                        style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun MapPlaceholder() {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(64.dp))
        Text("Map preview", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))
        Text(
            "Add your Google Maps API key to local.properties " +
                "(MAPS_API_KEY=...) and rebuild to see live pins " +
                "for nearby shoot spots.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = {}) { Text("How to get a key") }
    }
}
