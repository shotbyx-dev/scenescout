package com.scenescout.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.scenescout.app.data.SampleSpotRepository
import com.scenescout.app.data.Spot
import com.scenescout.app.data.imagery.BestImagery
import com.scenescout.app.data.imagery.SampleImageryRepository
import com.scenescout.app.data.imagery.SpotImage
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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SceneScoutApp() }
    }
}

@Composable
fun SceneScoutApp() {
    SceneScoutTheme {
        val repo = remember { SampleSpotRepository() }
        val imageryRepo = remember {
            SampleImageryRepository(
                mapsApiKey = BuildConfig.MAPS_API_KEY,
                mapillaryToken = BuildConfig.MAPILLARY_TOKEN,
            )
        }
        // Sample data is Miami-based; the real app passes the device location.
        val spots = remember { repo.nearbySpots(25.7826, -80.1867, 50.0) }
        val imagesBySpot = remember(spots) {
            spots.associate { it.id to imageryRepo.imagesFor(it) }
        }
        val allReviews = remember { spots.flatMap { repo.reviewsFor(it.id) } }
        var tab by remember { mutableStateOf(Tab.MAP) }
        var openSpot by remember { mutableStateOf<Spot?>(null) }

        val goToSpot: (Spot) -> Unit = { openSpot = it }

        if (openSpot != null) {
            SpotDetailScreen(
                spot = openSpot!!,
                reviews = repo.reviewsFor(openSpot!!.id),
                images = imagesBySpot[openSpot!!.id].orEmpty(),
                onBack = { openSpot = null },
            )
        } else {
            Scaffold(
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
                androidx.compose.foundation.layout.Box(
                    Modifier.padding(inner),
                ) {
                    when (tab) {
                        Tab.MAP -> MapScreen(
                        spots = spots,
                        heroImageFor = { spot ->
                            BestImagery.forDisplay(imagesBySpot[spot.id].orEmpty())
                        },
                        onSpotClick = goToSpot,
                    )
                        Tab.DISCOVER -> DiscoverScreen(spots, goToSpot)
                        Tab.SCOUT -> ScoutScreen(spots, goToSpot)
                        Tab.COMMUNITY -> CommunityScreen(spots, allReviews, goToSpot)
                    }
                }
            }
        }
    }
}
