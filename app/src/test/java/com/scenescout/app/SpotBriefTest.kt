package com.scenescout.app

import com.scenescout.app.data.PermitInfo
import com.scenescout.app.data.PermitLevel
import com.scenescout.app.data.ScenicScorer
import com.scenescout.app.data.ShootType
import com.scenescout.app.data.Spot
import com.scenescout.app.data.SpotBriefBuilder
import com.scenescout.app.data.SpotReview
import com.scenescout.app.data.SunTimes
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpotBriefTest {

    private val spot = Spot(
        id = "stadium", name = "Miami Marine Stadium",
        latitude = 25.7447, longitude = -80.1698,
        description = "Abandoned waterfront stadium swallowed by graffiti.",
        tags = listOf("abandoned", "graffiti", "concrete"),
        bestFor = listOf(ShootType.MUSIC_VIDEO, ShootType.RUN_AND_GUN),
        aiScore = 93, communityRating = 4.8, reviewCount = 156,
        permit = PermitInfo(PermitLevel.SIMPLE, "Small crews usually fine."),
    )

    @Test
    fun `brief carries the essentials`() {
        val score = ScenicScorer.scoreSpot(spot)
        val sun = SunTimes.forDate(
            spot.latitude, spot.longitude,
            LocalDate.of(2026, 9, 26), ZoneId.of("America/New_York"),
        )
        val reviews = listOf(
            SpotReview("r5", "stadium", "Marcus T.", 5,
                "Had the place to ourselves for hours.", "Bring boots.", "golden hour"),
        )
        val brief = SpotBriefBuilder.build(spot, score, sun, reviews)

        assertEquals("Miami Marine Stadium", brief.title)
        assertTrue(brief.scoreLine.contains("/100"))
        assertTrue(brief.scoreLine.contains(score.label))
        assertTrue(brief.coordinates.contains("25.7447"))
        assertTrue(brief.mapsLink.startsWith("https://www.google.com/maps/search/"))
        assertTrue(brief.goldenHour.contains("/"))
        assertTrue(brief.sunriseSunset.contains("Sunrise"))
        assertEquals("Small crews usually fine.", brief.permitSummary)
        assertEquals(1, brief.reviews.size)
        assertEquals("Marcus T.", brief.reviews[0].author)
        assertTrue(brief.footer.contains("Shotbyx"))
    }

    @Test
    fun `brief handles spots with no reviews`() {
        val score = ScenicScorer.scoreSpot(spot)
        val sun = SunTimes.forDate(
            spot.latitude, spot.longitude,
            LocalDate.of(2026, 9, 26), ZoneId.of("America/New_York"),
        )
        val brief = SpotBriefBuilder.build(spot, score, sun, emptyList())
        assertTrue(brief.reviews.isEmpty())
        assertTrue(brief.title.isNotBlank())
    }
}
