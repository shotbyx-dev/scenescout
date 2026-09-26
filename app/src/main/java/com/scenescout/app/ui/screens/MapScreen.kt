package com.scenescout.app.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import coil.compose.AsyncImage
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.scenescout.app.BuildConfig
import com.scenescout.app.data.Spot
import com.scenescout.app.data.imagery.SpotImage
import kotlinx.coroutines.launch

private const val MAPS_CONSOLE_URL = "https://console.cloud.google.com/google/maps-apis/overview"

/**
 * Map tab: centers on the user's GPS location with nearby shoot spots pinned.
 * Renders a real Google Map once MAPS_API_KEY is set in local.properties;
 * until then it shows setup steps with a working button.
 */
@Composable
fun MapScreen(
    spots: List<Spot>,
    userLocation: LatLng?,
    heroImageFor: (Spot) -> SpotImage?,
    onSpotClick: (Spot) -> Unit,
) {
    val hasKey = BuildConfig.MAPS_API_KEY != "MAPS_API_KEY_NOT_SET"
    if (!hasKey) {
        MapSetupGuide()
        return
    }
    val fallback = LatLng(25.7826, -80.1867) // Miami sample-data home
    val cameraState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(userLocation ?: fallback, 12f)
    }
    val scope = rememberCoroutineScope()
    var selected by remember { mutableStateOf<Spot?>(null) }

    // Recenter when the GPS fix arrives after first composition.
    LaunchedEffect(userLocation) {
        userLocation?.let {
            cameraState.animate(CameraUpdateFactory.newLatLngZoom(it, 12f))
        }
    }

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
        if (userLocation != null) {
            FloatingActionButton(
                onClick = {
                    scope.launch {
                        cameraState.animate(
                            CameraUpdateFactory.newLatLngZoom(userLocation, 12f),
                        )
                    }
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
            ) {
                Icon(Icons.Filled.MyLocation, contentDescription = "Center on my location")
            }
        }
        selected?.let { spot ->
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                onClick = { onSpotClick(spot) },
            ) {
                Column(Modifier.padding(12.dp)) {
                    heroImageFor(spot)?.let { hero ->
                        AsyncImage(
                            model = hero.url,
                            contentDescription = "Preview of ${spot.name}",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                                .clip(MaterialTheme.shapes.medium),
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            hero.source.attribution,
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

/** Shown until a Maps API key is configured — with a button that actually works. */
@Composable
private fun MapSetupGuide() {
    val context = LocalContext.current
    val openConsole = {
        val intent = Intent(Intent.ACTION_VIEW, MAPS_CONSOLE_URL.toUri())
        context.startActivity(intent)
    }
    Column(
        Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(32.dp))
        Text("Unlock the live map", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))
        Text(
            "The map needs a free Google Maps API key. It takes about 2 minutes:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(
                Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                SetupStep(1, "Tap the button below to open Google Cloud Console")
                SetupStep(2, "Create a project and enable \"Maps SDK for Android\"")
                SetupStep(3, "Create an API key under Credentials")
                SetupStep(4, "Add it to local.properties as MAPS_API_KEY=... and rebuild")
            }
        }
        Spacer(Modifier.height(20.dp))
        Button(onClick = openConsole, modifier = Modifier.fillMaxWidth()) {
            Text("Open Google Cloud Console")
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = openConsole, modifier = Modifier.fillMaxWidth()) {
            Text("Maps Platform documentation")
        }
        Spacer(Modifier.height(16.dp))
        Text(
            "Meanwhile, the Discover, AI Scout, and Community tabs work right now.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SetupStep(number: Int, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primary),
        ) {
            Text(
                "$number",
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.labelLarge,
            )
        }
        Spacer(Modifier.padding(4.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}
