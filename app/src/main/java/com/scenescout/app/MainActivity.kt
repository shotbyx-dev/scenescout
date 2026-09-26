package com.scenescout.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.launch
import org.maplibre.android.geometry.LatLng
import com.scenescout.app.data.SampleSpotRepository
import com.scenescout.app.data.Spot
import com.scenescout.app.data.imagery.SampleImageryRepository
import com.scenescout.app.data.osm.OsmDiscovery
import com.scenescout.app.data.planner.PlannerStore
import com.scenescout.app.ui.screens.AboutScreen
import com.scenescout.app.ui.screens.CommunityScreen
import com.scenescout.app.ui.screens.DiscoverScreen
import com.scenescout.app.ui.screens.MapScreen
import com.scenescout.app.ui.screens.PlannerScreen
import com.scenescout.app.ui.screens.ScoutScreen
import com.scenescout.app.ui.screens.SpotDetailScreen
import com.scenescout.app.ui.theme.BrandGlowBackground
import com.scenescout.app.ui.theme.SceneScoutTheme

private enum class Tab(val label: String, val icon: ImageVector) {
    MAP("Map", Icons.Filled.Map),
    DISCOVER("Discover", Icons.Filled.Explore),
    PLANNER("Planner", Icons.Filled.Assignment),
    SCOUT("AI Scout", Icons.Filled.AutoAwesome),
    COMMUNITY("Community", Icons.Filled.People),
}

class MainActivity : ComponentActivity() {

    private var onLocationResult: ((LatLng?) -> Unit)? = null

    private val locationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) fetchLocation() else onLocationResult?.invoke(null)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SceneScoutApp(::requestLocation) }
    }

    private fun requestLocation(onResult: (LatLng?) -> Unit) {
        onLocationResult = onResult
        val granted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) fetchLocation()
        else locationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    private fun fetchLocation() {
        try {
            @Suppress("MissingPermission")
            LocationServices.getFusedLocationProviderClient(this)
                .lastLocation
                .addOnSuccessListener { loc ->
                    onLocationResult?.invoke(
                        loc?.let { LatLng(it.latitude, it.longitude) },
                    )
                }
                .addOnFailureListener { onLocationResult?.invoke(null) }
        } catch (e: SecurityException) {
            onLocationResult?.invoke(null)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SceneScoutApp(requestLocation: ((LatLng?) -> Unit) -> Unit) {
    SceneScoutTheme {
        val repo = remember { SampleSpotRepository() }
        val imageryRepo = remember {
            SampleImageryRepository(
                mapsApiKey = BuildConfig.MAPS_API_KEY,
                mapillaryToken = BuildConfig.MAPILLARY_TOKEN,
            )
        }
        val context = androidx.compose.ui.platform.LocalContext.current
        val plannerStore = remember { PlannerStore(context) }
        var userLocation by remember { mutableStateOf<LatLng?>(null) }
        var locationAsked by remember { mutableStateOf(false) }

        // Ask for GPS once; the map centers on the user when available.
        if (!locationAsked) {
            LaunchedEffect(Unit) {
                locationAsked = true
                requestLocation { userLocation = it }
            }
        }

        // Live discovery: static spots + OpenStreetMap places around the GPS.
        // Re-runs whenever the center moves, plus a manual refresh button —
        // the app stays connected and keeps pulling fresh spots.
        // Stable across recompositions so the refresh effect only re-fires
        // when the location actually changes.
        val center = remember(userLocation) {
            userLocation ?: LatLng(25.7826, -80.1867)
        }
        var liveSpots by remember {
            mutableStateOf(repo.nearbySpots(center.latitude, center.longitude, 50.0))
        }
        var isLive by remember { mutableStateOf(false) }
        var refreshing by remember { mutableStateOf(false) }
        val scope = rememberCoroutineScope()
        val refreshSpots: (LatLng) -> Unit = { c ->
            if (!refreshing) {
                refreshing = true
                scope.launch {
                    val static = repo.nearbySpots(c.latitude, c.longitude, 50.0)
                    val live = try {
                        OsmDiscovery.discoverSpots(c.latitude, c.longitude, 10.0)
                    } catch (e: Exception) {
                        null // offline — keep static spots
                    }
                    liveSpots = if (live != null) OsmDiscovery.merge(static, live) else static
                    isLive = live != null
                    refreshing = false
                }
            }
        }
        LaunchedEffect(center) { refreshSpots(center) }
        val spots = liveSpots
        val allReviews = remember(spots) { spots.flatMap { repo.reviewsFor(it.id) } }
        var tab by remember { mutableStateOf(Tab.MAP) }
        var openSpot by remember { mutableStateOf<Spot?>(null) }
        var showAbout by remember { mutableStateOf(false) }
        // Tag chips on Discover cards send their tag to AI Scout.
        var scoutQuery by remember { mutableStateOf("") }

        val goToSpot: (Spot) -> Unit = { openSpot = it }
        val searchTag: (String) -> Unit = { tag ->
            scoutQuery = tag
            tab = Tab.SCOUT
        }

        when {
            showAbout -> AboutScreen(onBack = { showAbout = false })
            openSpot != null -> SpotDetailScreen(
                spot = openSpot!!,
                reviews = repo.reviewsFor(openSpot!!.id),
                imagery = imageryRepo,
                onBack = { openSpot = null },
            )
            else -> Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("SceneScout")
                                Spacer(Modifier.width(8.dp))
                                // Honest connectivity state: live discovery or static fallback.
                                val (label, color) = if (refreshing) {
                                    "Updating…" to MaterialTheme.colorScheme.onSurfaceVariant
                                } else if (isLive) {
                                    "● Live" to Color(0xFF4CAF50)
                                } else {
                                    "● Offline" to MaterialTheme.colorScheme.onSurfaceVariant
                                }
                                Text(
                                    label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = color,
                                )
                            }
                        },
                        actions = {
                            IconButton(onClick = { refreshSpots(center) }) {
                                Icon(Icons.Filled.Refresh, contentDescription = "Refresh spots")
                            }
                            IconButton(onClick = { showAbout = true }) {
                                Icon(Icons.Filled.Info, contentDescription = "About")
                            }
                        },
                    )
                },
                bottomBar = {
                    NavigationBar {
                        Tab.entries.forEach { t ->
                            NavigationBarItem(
                                selected = tab == t,
                                onClick = { tab = t },
                                icon = { Icon(t.icon, contentDescription = t.label) },
                                label = { Text(t.label) },
                            )
                        }
                    }
                },
            ) { inner ->
                Box(Modifier.padding(inner)) {
                    BrandGlowBackground()
                    AnimatedContent(
                        targetState = tab,
                        transitionSpec = {
                            (fadeIn(tween(280)) +
                                slideInHorizontally(tween(280)) { it / 8 }) togetherWith
                                fadeOut(tween(180))
                        },
                        label = "tabTransition",
                    ) { current ->
                        when (current) {
                            Tab.MAP -> MapScreen(
                                spots = spots,
                                userLocation = userLocation,
                                imagery = imageryRepo,
                                onSpotClick = goToSpot,
                            )
                            Tab.DISCOVER -> DiscoverScreen(spots, imageryRepo, goToSpot, searchTag)
                            Tab.PLANNER -> PlannerScreen(spots, plannerStore)
                            Tab.SCOUT -> ScoutScreen(spots, scoutQuery, goToSpot)
                            Tab.COMMUNITY -> CommunityScreen(spots, allReviews, goToSpot)
                        }
                    }
                }
            }
        }
    }
}
