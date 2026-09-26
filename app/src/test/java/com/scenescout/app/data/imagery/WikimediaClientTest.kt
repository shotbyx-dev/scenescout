package com.scenescout.app.data.imagery

import org.junit.Assert.*
import org.junit.Test

class WikimediaClientTest {

    private val sampleJson = """
        {
          "query": {
            "pages": {
              "123": {
                "title": "File:Beach.jpg",
                "imageinfo": [{
                  "thumburl": "https://upload.wikimedia.org/thumb.jpg",
                  "url": "https://upload.wikimedia.org/full.jpg",
                  "extmetadata": {
                    "Artist": {"value": "<a href=\"x\">Jane Doe</a>"},
                    "LicenseShortName": {"value": "CC BY-SA 4.0"}
                  }
                }]
              },
              "124": { "title": "File:Empty.jpg" }
            }
          }
        }
    """.trimIndent()

    @Test
    fun `parse extracts photos with cleaned credit`() {
        val images = WikimediaClient.parse(sampleJson)
        assertEquals(1, images.size)
        val img = images.single()
        assertEquals("https://upload.wikimedia.org/thumb.jpg", img.url)
        assertEquals(ImagerySource.WIKIMEDIA_COMMONS, img.source)
        assertFalse(img.source.analyzableByAi) // display only
        assertNotNull(img.credit)
        assertTrue(img.credit!!.contains("Jane Doe"))
        assertTrue(img.credit.contains("Wikimedia Commons"))
        assertFalse(img.credit.contains("<a")) // HTML stripped
    }

    @Test
    fun `parse handles empty or malformed responses`() {
        assertTrue(WikimediaClient.parse("{}").isEmpty())
        assertTrue(WikimediaClient.parse("not json").isEmpty())
    }

    @Test
    fun `searchUrl hits the commons geosearch api`() {
        val url = WikimediaClient.searchUrl(26.1, -80.1)
        assertTrue(url.startsWith("https://commons.wikimedia.org/w/api.php"))
        assertTrue(url.contains("generator=geosearch"))
        assertTrue(url.contains("26.1|-80.1"))
    }

    @Test
    fun `wikimedia ranks ahead of uploads for display, never for AI`() {
        val images = listOf(
            SpotImage("u", ImagerySource.USER_UPLOAD, widthPx = 100),
            SpotImage("w", ImagerySource.WIKIMEDIA_COMMONS, widthPx = 100),
        )
        val gallery = BestImagery.gallery(images)
        assertEquals(ImagerySource.WIKIMEDIA_COMMONS, gallery.first().source)
        assertNull(BestImagery.forAnalysis(images.filter {
            it.source == ImagerySource.WIKIMEDIA_COMMONS
        }))
    }
}
