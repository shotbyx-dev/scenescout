package com.scenescout.app

import com.scenescout.app.data.ScenicScorer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScenicScorerTest {

    @Test
    fun `perfect spot scores 100`() {
        val s = ScenicScorer.score(100, 100, 5.0)
        assertEquals(100, s.overall)
        assertEquals("Must shoot", s.label)
    }

    @Test
    fun `weights blend three signals`() {
        // 80*0.45 + 60*0.30 + 80*0.25 = 36 + 18 + 20 = 74
        val s = ScenicScorer.score(80, 60, 4.0)
        assertEquals(74, s.overall)
        assertEquals("Strong pick", s.label)
    }

    @Test
    fun `no community ratings renormalizes weights`() {
        // (90*0.45 + 70*0.30) / 0.75 = 82
        val s = ScenicScorer.score(90, 70, 0.0)
        assertEquals(82, s.overall)
    }

    @Test
    fun `labels cover the full range`() {
        assertEquals("Must shoot", ScenicScorer.score(95, 95, 5.0).label)
        assertEquals("Strong pick", ScenicScorer.score(75, 75, 4.0).label)
        assertEquals("Worth a look", ScenicScorer.score(60, 60, 3.0).label)
        assertEquals("Backup option", ScenicScorer.score(45, 45, 2.0).label)
        assertEquals("Skip", ScenicScorer.score(20, 20, 1.0).label)
    }

    @Test
    fun `inputs are clamped`() {
        val s = ScenicScorer.score(999, -5, 99.0)
        assertTrue(s.overall in 0..100)
        assertTrue(s.visualAppeal in 0..100)
    }

    @Test
    fun `positive shoot notes raise shootability`() {
        val good = ScenicScorer.shootabilityFromNotes(
            "Nobody bothered us, easy parking, quiet at 7am")
        val bad = ScenicScorer.shootabilityFromNotes(
            "Crowded, security kicked us out, no parking")
        assertTrue(good > 60)
        assertTrue(bad < 60)
    }
}
