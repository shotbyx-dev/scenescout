package com.scenescout.app

import com.scenescout.app.data.MoodMatcher
import com.scenescout.app.data.PermitInfo
import com.scenescout.app.data.PermitLevel
import com.scenescout.app.data.ShootType
import com.scenescout.app.data.Spot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MoodMatcherTest {

    private fun spot(id: String, tags: List<String>, desc: String = "") = Spot(
        id = id, name = id, latitude = 0.0, longitude = 0.0,
        description = desc, tags = tags,
        bestFor = listOf(ShootType.MUSIC_VIDEO),
        aiScore = 80, communityRating = 4.0, reviewCount = 10,
        permit = PermitInfo(PermitLevel.SIMPLE, "ok"),
    )

    private val stadium = spot("stadium", listOf("abandoned", "graffiti", "concrete"),
        "raw industrial ruin")
    private val beach = spot("beach", listOf("beach", "ocean", "sunset"),
        "calm shoreline at golden hour")

    @Test
    fun `lonely heartbreak finds abandoned spot`() {
        val results = MoodMatcher.rankForSong(
            "lonely heartbreak driving at night", listOf(stadium, beach),
        )
        assertTrue(results.isNotEmpty())
        assertEquals("stadium", results[0].spot.id)
        assertTrue(results[0].reasons.contains("lonely"))
    }

    @Test
    fun `romantic sunset finds beach`() {
        val results = MoodMatcher.rankForSong(
            "a romantic love song at sunset", listOf(stadium, beach),
        )
        assertTrue(results.isNotEmpty())
        assertEquals("beach", results[0].spot.id)
    }

    @Test
    fun `party neon finds urban spot`() {
        val club = spot("club", listOf("neon", "night", "downtown"), "city lights")
        val results = MoodMatcher.rankForSong(
            "party all night in the city", listOf(stadium, beach, club),
        )
        assertTrue(results.isNotEmpty())
        assertEquals("club", results[0].spot.id)
    }

    @Test
    fun `reasons explain the match`() {
        val results = MoodMatcher.rankForSong("dark and dangerous",
            listOf(stadium, beach))
        assertTrue(results.isNotEmpty())
        assertTrue(results[0].reasons.isNotEmpty())
    }

    @Test
    fun `empty text returns empty`() {
        assertTrue(MoodMatcher.rankForSong("the and a", listOf(stadium)).isEmpty())
        assertTrue(MoodMatcher.rankForSong("", listOf(stadium)).isEmpty())
    }

    @Test
    fun `unknown words still match as plain keywords`() {
        val results = MoodMatcher.rankForSong("graffiti", listOf(stadium, beach))
        assertEquals(1, results.size)
        assertEquals("stadium", results[0].spot.id)
    }
}
