package com.scenescout.app.data.imagery

import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Real, location-relevant photos with no API key: Wikimedia Commons has a
 * free geosearch API that returns geotagged photos near a coordinate.
 * Display-only (mixed CC licenses), with per-photo author credit.
 */
object WikimediaClient {

    fun searchUrl(
        latitude: Double,
        longitude: Double,
        radiusM: Int = 5000,
        limit: Int = 8,
    ): String =
        "https://commons.wikimedia.org/w/api.php?action=query&format=json" +
            "&generator=geosearch&ggscoord=$latitude|$longitude" +
            "&ggsradius=$radiusM&ggslimit=$limit&ggsnamespace=6" +
            "&prop=imageinfo&iiprop=url%7Cextmetadata&iiurlwidth=800"

    /**
     * Parses a geosearch+imageinfo response into [SpotImage]s.
     * Pure function — unit-testable with the org.json test dependency.
     */
    fun parse(json: String): List<SpotImage> {
        val out = mutableListOf<SpotImage>()
        val pages = try {
            JSONObject(json).optJSONObject("query")?.optJSONObject("pages")
        } catch (e: Exception) {
            null // Malformed API response — show no photos, never crash.
        } ?: return out
        val keys = pages.keys()
        while (keys.hasNext()) {
            val page = pages.optJSONObject(keys.next()) ?: continue
            // Skip specimen scans, labels, emblems and SVG diagrams —
            // the API returns them as "nearby photos" but they aren't.
            if (isJunkTitle(page.optString("title"))) continue
            val info = page.optJSONArray("imageinfo")?.optJSONObject(0) ?: continue
            val url = info.optString("thumburl").ifBlank { info.optString("url") }
            if (url.isBlank()) continue
            val meta = info.optJSONObject("extmetadata")
            val artist = meta?.optJSONObject("Artist")
                ?.optString("value").orEmpty()
                .replace(Regex("<[^>]*>"), "").trim()
                .take(60)
            val license = meta?.optJSONObject("LicenseShortName")
                ?.optString("value").orEmpty()
                .replace(Regex("<[^>]*>"), "").trim()
            val credit = listOfNotNull(
                artist.takeIf { it.isNotBlank() }?.let { "© $it" },
                "via Wikimedia Commons".takeIf { artist.isNotBlank() },
                license.takeIf { it.isNotBlank() },
            ).joinToString(" ").ifBlank { null }
            out += SpotImage(
                url = url,
                source = ImagerySource.WIKIMEDIA_COMMONS,
                widthPx = 800,
                credit = credit,
                title = page.optString("title").takeIf { it.isNotBlank() },
            )
        }
        return out
    }

    private val NAME_STOPWORDS = setOf(
        "the", "of", "and", "a", "an", "in", "on", "at", "de", "la", "el",
        "unnamed", "city",
    )

    /**
     * How likely a Commons file title actually depicts this spot.
     * Significant words from the place name weigh most; kind tags
     * (mural, pier, beach, …) add a little. A CVS-pharmacy photo near a
     * mural scores 0 and is never used as the card hero.
     */
    internal fun relevance(title: String, spotName: String, tags: List<String>): Int {
        val t = title.lowercase()
        var score = 0
        spotName.lowercase().split(Regex("\\W+"))
            .filter { it.length > 3 && it !in NAME_STOPWORDS }
            .forEach { w -> if (w in t) score += 3 }
        tags.map { it.lowercase() }
            .filter { it.length > 2 }
            .forEach { w -> if (w in t) score += 1 }
        return score
    }

    /**
     * Best Commons photo that plausibly depicts the spot, or null.
     * Heroes must depict the place — an irrelevant nearby photo is worse
     * than the branded gradient, so zero-relevance results are dropped.
     */
    suspend fun searchHeroPhoto(
        latitude: Double,
        longitude: Double,
        spotName: String,
        tags: List<String>,
    ): SpotImage? = withContext(Dispatchers.IO) {
        val photos = searchPhotos(latitude, longitude)
        photos.filter { relevance(it.title.orEmpty(), spotName, tags) > 0 }
            .maxByOrNull { relevance(it.title.orEmpty(), spotName, tags) }
    }

    suspend fun searchPhotos(latitude: Double, longitude: Double): List<SpotImage> =
        withContext(Dispatchers.IO) {
            try {
                val conn = URL(searchUrl(latitude, longitude))
                    .openConnection() as HttpURLConnection
                conn.setRequestProperty(
                    "User-Agent", "SceneScout/1.0 (shotbyx-dev; videographer location app)",
                )
                conn.connectTimeout = 8000
                conn.readTimeout = 8000
                conn.inputStream.bufferedReader().use { parse(it.readText()) }
            } catch (e: Exception) {
                emptyList()
            } finally {
                // Connection closed by reader use-block.
            }
        }

    /** Specimen scans, labels and SVG diagrams read as "photos" but aren't. */
    internal fun isJunkTitle(title: String): Boolean {
        val t = title.lowercase()
        return JUNK_MARKERS.any { t.contains(it) }
    }

    private val JUNK_MARKERS = listOf(
        "specimen", "casent", "dorsal", "head 1", "profile 1", "label 1",
        ".svg", "emblem", "logo",
    )
}
