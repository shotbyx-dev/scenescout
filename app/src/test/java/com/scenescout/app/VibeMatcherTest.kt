package com.scenescout.app

import com.scenescout.app.data.PermitInfo
import com.scenescout.app.data.PermitLevel
import com.scenescout.app.data.ShootType
import com.scenescout.app.data.Spot
import com.scenescout.app.data.VibeMatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VibeMatcherTest {

    private fun spot(id: String, tags: List<String>, desc: String = "") = Spot(
        id = id, name = id, latitude = 0.0, longitude = 0.0,
        description = desc, tags = tags,
        bestFor = listOf(ShootType.MUSIC_VIDEO),
        aiScore = 80, communityRating = 4.0, reviewCount = 10,
        permit = PermitInfo(PermitLevel.SIMPLE, "ok"),
    )

    private val grittySpot = spot("stadium", listOf("abandoned", "graffiti", "concrete"),
        "raw industrial ruin")
    private val beachSpot = spot("beach", listOf("beach", "ocean"),
        "calm shoreline")

    @Test
    fun `exact tag matches`() {
        val results = VibeMatcher.rankForQuery("graffiti", listOf(grittySpot, beachSpot))
        assertEquals(1, results.size)
        assertEquals("stadium", results[0].first.id)
    }

    @Test
    fun `synonym expands query to tags`() {
        // "gritty" should find the abandoned/concrete spot via synonyms.
        val results = VibeMatcher.rankForQuery("gritty", listOf(grittySpot, beachSpot))
        assertEquals(1, results.size)
        assertEquals("stadium", results[0].first.id)
    }

    @Test
    fun `truck finds roadside vehicles`() {
        val truckSpot = spot("roadside", listOf("roadside", "junkyard"),
            "broken down truck on the lot")
        val results = VibeMatcher.rankForQuery("truck", listOf(truckSpot, beachSpot))
        assertEquals(1, results.size)
        assertEquals("roadside", results[0].first.id)
    }

    @Test
    fun `no match returns empty`() {
        val results = VibeMatcher.rankForQuery("snowy mountains", listOf(grittySpot))
        assertTrue(results.isEmpty())
    }

    @Test
    fun `empty query returns empty`() {
        assertTrue(VibeMatcher.rankForQuery("  ", listOf(grittySpot)).isEmpty())
    }

    @Test
    fun `sorted by blended score descending`() {
        val results = VibeMatcher.rankForQuery(
            "abandoned", listOf(grittySpot, beachSpot, grittySpot.copy(id = "stadium2")),
        )
        val ids = results.map { it.first.id }
        assertTrue(ids.contains("stadium"))
        assertTrue(ids.contains("stadium2"))
    }
}
