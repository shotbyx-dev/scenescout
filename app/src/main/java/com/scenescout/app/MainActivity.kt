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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.google.android.gms.location.Priority
import kotlinx.coroutines.launch
import com.google.android.gms.maps.model.LatLng
import com.scenescout.app.data.SampleSpotRepository
import com.scenescout.app.data.Spot
import com.scenescout.app.data.imagery.GoogleImageryRepository
import com.scenescout.app.data.places.ApiKeyStore
import com.scenescout.app.data.places.GooglePlaces
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

    /**
     * Active GPS fix first; falls back to the last known location when a
     * fresh fix can't be obtained (indoors, GPS warming up). Null only when
     * there is genuinely no location to use.
     */
    private fun fetchLocation() {
        try {
            val client = LocationServices.getFusedLocationProviderClient(this)
            @Suppress("MissingPermission")
            val deliver: (android.location.Location?) -> Unit = { loc ->
                onLocationResult?.invoke(
                    loc?.let { LatLng(it.latitude, it.longitude) },
                )
            }
            @Suppress("MissingPermission")
            fun useLastLocation() {
                client.lastLocation
                    .addOnSuccessListener { deliver(it) }
                    .addOnFailureListener { onLocationResult?.invoke(null) }
            }
            @Suppress("MissingPermission")
            client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener { loc ->
                    if (loc != null) deliver(loc) else useLastLocation()
                }
                .addOnFailureListener { useLastLocation() }
        } catch (e: SecurityException) {
            onLocationResult?.invoke(null)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SceneScoutApp(requestLocation: ((LatLng?) -> Unit) -> Unit) {
    SceneScoutTheme {
        val context = androidx.compose.ui.platform.LocalContext.current
        val repo = remember { SampleSpotRepository() }
        val apiKeyStore = remember { ApiKeyStore(context) }
        // Bumped whenever the key is saved/cleared so discovery re-runs.
        var keyTick by remember { mutableStateOf(0) }
        // User-pasted key wins; the build-time key (CI secret) is the fallback.
        val apiKey = remember(keyTick) {
            apiKeyStore.getKey()?.ifBlank { null } ?: BuildConfig.MAPS_API_KEY
        }
        val hasApiKey = GooglePlaces.hasKey(apiKey)
        val imageryRepo = remember(keyTick) {
            GoogleImageryRepository(apiKey = { apiKey })
        }
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
        // Crash reporter: if the previous run died, show the stack trace so
        // it can be diagnosed (and pasted to the developer) without logcat.
        var crashLog by remember { mutableStateOf<String?>(null) }
        LaunchedEffect(Unit) {
            crashLog = SceneScoutApplication.consumeCrashLog(
                context.applicationContext as android.app.Application,
            )
        }
        crashLog?.let { log ->
            val keyState = remember(apiKey) {
                val buildKey = BuildConfig.MAPS_API_KEY != GooglePlaces.NOT_SET &&
                    BuildConfig.MAPS_API_KEY.isNotBlank()
                val pasted = apiKeyStore.getKey()?.isNotBlank() == true
                "maps build key present=$buildKey, pasted key present=$pasted"
            }
            AlertDialog(
                onDismissRequest = { crashLog = null },
                title = { Text("The app crashed last time") },
                text = {
                    Column(Modifier.verticalScroll(rememberScrollState())) {
                        Text(
                            "Copy this report and send it over — it says " +
                                "exactly what went wrong.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            keyState,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            log,
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        val cm = ContextCompat.getSystemService(
                            context, android.content.ClipboardManager::class.java)
                        cm?.setPrimaryClip(
                            android.content.ClipData.newPlainText("SceneScout crash", log))
                        crashLog = null
                    }) { Text("Copy report") }
                },
                dismissButton = {
                    TextButton(onClick = { crashLog = null }) { Text("Dismiss") }
                },
            )
        }

        // Live discovery: static spots + Google Places around the GPS.
        // Re-runs whenever the center moves, the key changes, or the user
        // hits refresh. Cached per ~1km cell for 10 minutes (quota-friendly).
        val center = remember(userLocation) {
            userLocation ?: LatLng(25.7826, -80.1867)
        }
        var liveSpots by remember {
            mutableStateOf(repo.nearbySpots(center.latitude, center.longitude, 50.0))
        }
        var isLive by remember { mutableStateOf(false) }
        var refreshing by remember { mutableStateOf(false) }
        // Last Google Places failure, shown as a dismissible banner —
        // the app never silently pretends to be live.
        var discoveryError by remember { mutableStateOf<String?>(null) }
        val scope = rememberCoroutineScope()
        val refreshSpots: (LatLng) -> Unit = { c ->
            if (!refreshing) {
                refreshing = true
                scope.launch {
                    val static = repo.nearbySpots(c.latitude, c.longitude, 50.0)
                    val live = if (hasApiKey) {
                        try {
                            val spots = GooglePlaces.discover(
                                c.latitude, c.longitude, 10.0, apiKey)
                            discoveryError = null
                            spots
                        } catch (e: Exception) {
                            discoveryError = GooglePlaces.friendlyError(e)
                            null // keep static spots
                        }
                    } else {
                        null // no key yet — static spots + setup prompts
                    }
                    liveSpots = if (live != null) {
                        GooglePlaces.mergeSpots(static, live)
                    } else {
                        static
                    }
                    isLive = live != null
                    refreshing = false
                }
            }
        }
        LaunchedEffect(center, keyTick) { refreshSpots(center) }
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
            showAbout -> AboutScreen(
                onBack = { showAbout = false },
                apiKeyStore = apiKeyStore,
                hasBuildKey = BuildConfig.MAPS_API_KEY != GooglePlaces.NOT_SET &&
                    BuildConfig.MAPS_API_KEY.isNotBlank(),
                onKeyChanged = { keyTick++ },
            )
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
                            IconButton(
                                onClick = {
                                    // Manual refresh bypasses the 10-min cache.
                                    GooglePlaces.clearCache()
                                    discoveryError = null
                                    refreshSpots(center)
                                },
                            ) {
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
                    Column(Modifier.fillMaxSize()) {
                        discoveryError?.let { err ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                                        .copy(alpha = 0.95f),
                                ),
                            ) {
                                Row(
                                    modifier = Modifier.padding(
                                        start = 12.dp, top = 8.dp,
                                        end = 4.dp, bottom = 8.dp,
                                    ),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        err,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.weight(1f),
                                    )
                                    IconButton(
                                        onClick = { discoveryError = null },
                                    ) {
                                        Icon(
                                            Icons.Filled.Close,
                                            contentDescription = "Dismiss",
                                        )
                                    }
                                }
                            }
                        }
                        Box(Modifier.weight(1f)) {
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
                                hasApiKey = hasApiKey,
                                imagery = imageryRepo,
                                onSpotClick = goToSpot,
                                onOpenKeySettings = { showAbout = true },
                            )
                            Tab.DISCOVER -> DiscoverScreen(
                                spots, imageryRepo, goToSpot, searchTag,
                                hasApiKey = hasApiKey,
                                onServerSearch = { query ->
                                    if (!refreshing && hasApiKey) {
                                        refreshing = true
                                        scope.launch {
                                            val found = try {
                                                GooglePlaces.textSearch(
                                                    query,
                                                    center.latitude, center.longitude,
                                                    apiKey,
                                                )
                                            } catch (e: Exception) {
                                                discoveryError = GooglePlaces.friendlyError(e)
                                                emptyList()
                                            }
                                            if (found.isNotEmpty()) {
                                                val static = repo.nearbySpots(
                                                    center.latitude, center.longitude, 50.0)
                                                liveSpots = GooglePlaces.mergeSpots(static, found)
                                                isLive = true
                                            }
                                            refreshing = false
                                        }
                                    }
                                },
                            )
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
    }
}
