package com.scenescout.app.data

import org.junit.Assert.*
import org.junit.Test

class NearbyRadiusTest {

    private val repo = SampleSpotRepository()

    @Test
    fun `nearbySpots filters by real distance from Wynwood`() {
        // Wynwood Walls approx location.
        val near = repo.nearbySpots(25.8012, -80.1996, 5.0)
        assertTrue(near.isNotEmpty())
        assertTrue(near.any { it.id == "wynwood-walls" })
        // Far-away South Pointe Park should be outside a 5 km Wynwood radius.
        assertTrue(near.none { it.id == "south-pointe" })
    }

    @Test
    fun `nearbySpots returns empty far from the dataset`() {
        // Mid-Atlantic ocean — nothing within 50 km.
        assertTrue(repo.nearbySpots(30.0, -60.0, 50.0).isEmpty())
    }

    @Test
    fun `nearbySpots sorts nearest first`() {
        val all = repo.nearbySpots(25.8012, -80.1996, 500.0)
        assertTrue(all.isNotEmpty())
        assertEquals("wynwood-walls", all.first().id)
    }
}
