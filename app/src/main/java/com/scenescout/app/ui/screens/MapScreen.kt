package com.scenescout.app.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import com.scenescout.app.data.Spot
import com.scenescout.app.data.imagery.SpotImage
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView

/** Free dark basemap — no API key, no billing, works out of the box. */
private const val DARK_STYLE_URL =
    "https://basemaps.cartocdn.com/gl/dark-matter-gl-style/style.json"

/**
 * Map tab: centers on the user's GPS location with nearby shoot spots pinned.
 * Powered by MapLibre + OpenStreetMap/CARTO tiles — no API key needed.
 */
@Composable
fun MapScreen(
    spots: List<Spot>,
    userLocation: LatLng?,
    heroImageFor: (Spot) -> SpotImage?,
    onSpotClick: (Spot) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var selected by remember { mutableStateOf<Spot?>(null) }
    var mapLibreMap by remember { mutableStateOf<MapLibreMap?>(null) }
    val markerToSpot = remember { mutableMapOf<Long, Spot>() }

    val mapView = remember { MapView(context).apply { onCreate(null) } }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDestroy()
        }
    }

    val fallback = remember { LatLng(25.7826, -80.1867) } // Miami sample-data home

    fun addMarkers(map: MapLibreMap) {
        map.clear()
        markerToSpot.clear()
        spots.forEach { spot ->
            val marker = map.addMarker(
                MarkerOptions()
                    .position(LatLng(spot.latitude, spot.longitude))
                    .title(spot.name),
            )
            markerToSpot[marker.id] = spot
        }
    }

    LaunchedEffect(spots) {
        mapLibreMap?.let(::addMarkers)
    }
    // Recenter when the GPS fix arrives after first composition.
    LaunchedEffect(userLocation) {
        val map = mapLibreMap ?: return@LaunchedEffect
        userLocation?.let {
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(it, 12.0))
        }
    }

    Box(Modifier.fillMaxSize()) {
        AndroidView(
            factory = {
                mapView.apply {
                    getMapAsync { map ->
                        map.uiSettings.isCompassEnabled = false
                        map.cameraPosition = CameraPosition.Builder()
                            .target(userLocation ?: fallback)
                            .zoom(12.0)
                            .build()
                        map.setStyle(DARK_STYLE_URL) { addMarkers(map) }
                        map.setOnMarkerClickListener { marker ->
                            selected = markerToSpot[marker.id]
                            true
                        }
                        mapLibreMap = map
                    }
                }
            },
            modifier = Modifier.fillMaxSize(),
        )
        if (userLocation != null) {
            FloatingActionButton(
                onClick = {
                    mapLibreMap?.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(userLocation, 12.0),
                    )
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
