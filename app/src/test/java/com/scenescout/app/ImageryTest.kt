package com.scenescout.app

import com.scenescout.app.data.imagery.BestImagery
import com.scenescout.app.data.imagery.ImagerySource
import com.scenescout.app.data.imagery.MapillaryClient
import com.scenescout.app.data.imagery.SpotImage
import com.scenescout.app.data.imagery.StreetView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ImageryTest {

    private fun img(url: String, source: ImagerySource, width: Int? = null) =
        SpotImage(url = url, source = source, widthPx = width)

    // --- StreetView URL builders ---

    @Test
    fun `street view still url has required params`() {
        val url = StreetView.stillUrl(25.76, -80.19, "KEY123")
        assertTrue(url.startsWith("https://maps.googleapis.com/maps/api/streetview?"))
        assertTrue(url.contains("location=25.76,-80.19"))
        assertTrue(url.contains("key=KEY123"))
        assertTrue(url.contains("scale=2")) // hi-dpi for sharpest stills
    }

    @Test
    fun `street view metadata url is well formed`() {
        val url = StreetView.metadataUrl(25.76, -80.19, "KEY123")
        assertTrue(url.contains("/streetview/metadata?"))
        assertTrue(url.contains("location=25.76,-80.19"))
    }

    @Test
    fun `panorama strip covers four headings`() {
        val strip = StreetView.panoramaStrip(25.76, -80.19, "KEY123")
        assertEquals(4, strip.size)
        assertTrue(strip.all { it.source == ImagerySource.GOOGLE_STREET_VIEW })
        assertTrue(strip.map { it.url }.toSet().size == 4)
        assertTrue(strip[0].url.contains("heading=0"))
        assertTrue(strip[1].url.contains("heading=90"))
    }

    // --- MapillaryClient ---

    @Test
    fun `mapillary search url targets graph api with bbox`() {
        val url = MapillaryClient.searchUrl(25.76, -80.19, "MLY|TOKEN")
        assertTrue(url.startsWith("https://graph.mapillary.com/images?"))
        assertTrue(url.contains("access_token=MLY|TOKEN"))
        assertTrue(url.contains("bbox="))
        assertTrue(url.contains("thumb_2048_url"))
    }

    @Test
    fun `mapillary bbox surrounds the point`() {
        val (w, s, e, n) = MapillaryClient.bbox(25.76, -80.19, 50.0)
        assertTrue(w < -80.19 && e > -80.19)
        assertTrue(s < 25.76 && n > 25.76)
        // ~50m each side: box is small (well under 0.01 degrees)
        assertTrue((e - w) < 0.01 && (n - s) < 0.01)
    }

    @Test
    fun `mapillary best thumbnail prefers sharpest available`() {
        val thumbs = mapOf(
            "thumb_256_url" to "https://x/256.jpg",
            "thumb_2048_url" to "https://x/2048.jpg",
            "thumb_1024_url" to "https://x/1024.jpg",
        )
        val (url, width) = MapillaryClient.bestThumbnail(thumbs)!!
        assertEquals("https://x/2048.jpg", url)
        assertEquals(2048, width)
    }

    @Test
    fun `mapillary best thumbnail falls back gracefully`() {
        val (url, width) = MapillaryClient.bestThumbnail(
            mapOf("thumb_256_url" to "https://x/256.jpg"))!!
        assertEquals(256, width)
        assertNull(MapillaryClient.bestThumbnail(emptyMap()))
    }

    // --- BestImagery picker ---

    @Test
    fun `display prefers google photo over mapillary at equal resolution`() {
        val images = listOf(
            img("mly", ImagerySource.MAPILLARY, 1024),
            img("ggl", ImagerySource.GOOGLE_PLACE_PHOTO, 1024),
        )
        assertEquals("ggl", BestImagery.forDisplay(images)!!.url)
    }

    @Test
    fun `display prefers sharper image regardless of source`() {
        val images = listOf(
            img("ggl", ImagerySource.GOOGLE_PLACE_PHOTO, 640),
            img("mly", ImagerySource.MAPILLARY, 2048),
        )
        assertEquals("mly", BestImagery.forDisplay(images)!!.url)
    }

    @Test
    fun `analysis never returns google imagery`() {
        val images = listOf(
            img("ggl", ImagerySource.GOOGLE_STREET_VIEW, 4000),
            img("mly", ImagerySource.MAPILLARY, 1024),
        )
        val pick = BestImagery.forAnalysis(images)!!
        assertEquals("mly", pick.url)
        assertTrue(pick.source.analyzableByAi)
    }

    @Test
    fun `analysis returns null when only google imagery exists`() {
        val images = listOf(img("ggl", ImagerySource.GOOGLE_STREET_VIEW, 4000))
        assertNull(BestImagery.forAnalysis(images))
        assertFalse(BestImagery.hasAnalyzableImage(images))
    }

    @Test
    fun `user uploads are analyzable`() {
        val images = listOf(img("up", ImagerySource.USER_UPLOAD, 800))
        assertTrue(BestImagery.hasAnalyzableImage(images))
        assertEquals("up", BestImagery.forAnalysis(images)!!.url)
    }

    @Test
    fun `gallery is sharpest first`() {
        val images = listOf(
            img("a", ImagerySource.USER_UPLOAD, 300),
            img("b", ImagerySource.MAPILLARY, 2048),
            img("c", ImagerySource.GOOGLE_STREET_VIEW, 1280),
        )
        assertEquals(listOf("b", "c", "a"),
            BestImagery.gallery(images).map { it.url })
    }

    @Test
    fun `empty list handled`() {
        assertNull(BestImagery.forDisplay(emptyList()))
        assertNull(BestImagery.forAnalysis(emptyList()))
        assertTrue(BestImagery.gallery(emptyList()).isEmpty())
    }
}
