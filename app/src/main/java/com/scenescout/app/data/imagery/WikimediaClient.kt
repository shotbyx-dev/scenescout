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
            )
        }
        return out
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
}
