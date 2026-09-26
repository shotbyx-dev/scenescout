package com.scenescout.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.scenescout.app.data.SampleSpotRepository
import com.scenescout.app.data.Spot
import com.scenescout.app.data.imagery.BestImagery
import com.scenescout.app.data.imagery.SampleImageryRepository
import com.scenescout.app.data.imagery.SpotImage
import com.scenescout.app.ui.screens.AboutScreen
import com.scenescout.app.ui.screens.CommunityScreen
import com.scenescout.app.ui.screens.DiscoverScreen
import com.scenescout.app.ui.screens.MapScreen
import com.scenescout.app.ui.screens.ScoutScreen
import com.scenescout.app.ui.screens.SpotDetailScreen
import com.scenescout.app.ui.theme.SceneScoutTheme

private enum class Tab(val label: String, val icon: ImageVector) {
    MAP("Map", Icons.Filled.Map),
    DISCOVER("Discover", Icons.Filled.Explore),
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
        var userLocation by remember { mutableStateOf<LatLng?>(null) }
        var locationAsked by remember { mutableStateOf(false) }

        // Ask for GPS once; the map centers on the user when available.
        if (!locationAsked) {
            LaunchedEffect(Unit) {
                locationAsked = true
                requestLocation { userLocation = it }
            }
        }

        // Sample data is Miami-based; the real app queries Places API by GPS.
        val center = userLocation ?: LatLng(25.7826, -80.1867)
        val spots = remember(center) {
            repo.nearbySpots(center.latitude, center.longitude, 50.0)
        }
        val imagesBySpot = remember(spots) {
            spots.associate { it.id to imageryRepo.imagesFor(it) }
        }
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
        val heroImageFor: (Spot) -> SpotImage? = { spot ->
            BestImagery.forDisplay(imagesBySpot[spot.id].orEmpty())
        }

        when {
            showAbout -> AboutScreen(onBack = { showAbout = false })
            openSpot != null -> SpotDetailScreen(
                spot = openSpot!!,
                reviews = repo.reviewsFor(openSpot!!.id),
                images = imagesBySpot[openSpot!!.id].orEmpty(),
                onBack = { openSpot = null },
            )
            else -> Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("SceneScout") },
                        actions = {
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
                    when (tab) {
                        Tab.MAP -> MapScreen(
                            spots = spots,
                            userLocation = userLocation,
                            heroImageFor = heroImageFor,
                            onSpotClick = goToSpot,
                        )
                        Tab.DISCOVER -> DiscoverScreen(spots, heroImageFor, goToSpot, searchTag)
                        Tab.SCOUT -> ScoutScreen(spots, scoutQuery, goToSpot)
                        Tab.COMMUNITY -> CommunityScreen(spots, allReviews, goToSpot)
                    }
                }
            }
        }
    }
}
