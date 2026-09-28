package com.scenescout.app.data.places

import com.scenescout.app.data.ShootType
import com.scenescout.app.data.Spot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Google Places API (New) — the reliable backend for discovery and photos.
 *
 * - Nearby Search around GPS for shoot-worthy place types.
 * - Text Search (location-biased) backing the Discover search box.
 * - Place Photos for card heroes and galleries (display-only, with credit).
 *
 * Needs an API key with "Places API (New)" enabled; the map itself needs
 * "Maps SDK for Android" on the same key. Key resolution lives in
 * [ApiKeyStore] (user-pasted) with BuildConfig.MAPS_API_KEY as fallback.
 */
object GooglePlaces {

    const val NOT_SET = "MAPS_API_KEY_NOT_SET"

    fun hasKey(key: String) = key.isNotBlank() && key != NOT_SET

    /**
     * Place types worth filming (Places API "New" Table A). Kept tight:
     * every type here is a plausible shoot location, so results stay
     * cinematic instead of a generic business directory.
     */
    val SHOOT_TYPES = listOf(
        "museum", "art_gallery", "tourist_attraction",
        "performing_arts_theater", "movie_theater", "night_club",
        "park", "national_park", "botanical_garden", "beach",
        "marina", "stadium", "amusement_park", "aquarium", "zoo",
        "church", "mosque", "hindu_temple", "synagogue",
    )

    private const val FIELD_MASK =
        "places.id,places.displayName,places.formattedAddress,places.location," +
            "places.photos,places.rating,places.userRatingCount,places.types," +
            "places.primaryTypeDisplayName,places.editorialSummary"

    // ------------------------------------------------------------------
    // Request builders — pure, unit-tested.
    // ------------------------------------------------------------------

    fun nearbyBody(lat: Double, lng: Double, radiusM: Int): String =
        JSONObject()
            .put("includedTypes", JSONArray(SHOOT_TYPES))
            .put("maxResultCount", 20)
            .put("rankPreference", "POPULARITY")
            .put(
                "locationRestriction",
                JSONObject().put(
                    "circle",
                    JSONObject()
                        .put(
                            "center",
                            JSONObject()
                                .put("latitude", lat)
                                .put("longitude", lng),
                        )
                        .put("radius", radiusM.toDouble()),
                ),
            )
            .toString()

    fun textBody(query: String, lat: Double, lng: Double): String =
        JSONObject()
            .put("textQuery", query)
            .put("maxResultCount", 20)
            .put(
                "locationBias",
                JSONObject().put(
                    "circle",
                    JSONObject()
                        .put(
                            "center",
                            JSONObject()
                                .put("latitude", lat)
                                .put("longitude", lng),
                        )
                        .put("radius", 20000.0),
                ),
            )
            .toString()

    /** Direct image URL for a Places photo (Coil follows the redirect). */
    fun photoUrl(photoName: String, apiKey: String, maxWidthPx: Int = 800): String =
        "https://places.googleapis.com/v1/$photoName/media" +
            "?maxWidthPx=$maxWidthPx&key=$apiKey"

    // ------------------------------------------------------------------
    // Parsing + mapping — pure, unit-tested.
    // ------------------------------------------------------------------

    /** Parses a Nearby/Text Search response into Spots. Never throws. */
    fun parsePlaces(json: String): List<Spot> {
        return try {
            val places = JSONObject(json).optJSONArray("places") ?: return emptyList()
            (0 until places.length()).mapNotNull { i ->
                parsePlace(places.optJSONObject(i))
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun parsePlace(p: JSONObject?): Spot? {
        if (p == null) return null
        val id = p.optString("id").ifBlank { return null }
        val name = p.optJSONObject("displayName")?.optString("text").orEmpty()
            .ifBlank { return null }
        val loc = p.optJSONObject("location") ?: return null
        val lat = loc.optDouble("latitude", Double.NaN)
        val lng = loc.optDouble("longitude", Double.NaN)
        if (lat.isNaN() || lng.isNaN()) return null

        val types = mutableListOf<String>()
        p.optJSONArray("types")?.let { arr ->
            for (i in 0 until arr.length()) types += arr.optString(i)
        }
        val summary = p.optJSONObject("editorialSummary")?.optString("text").orEmpty()
        val address = p.optString("formattedAddress")
        val photos = mutableListOf<String>()
        val credits = mutableListOf<String>()
        p.optJSONArray("photos")?.let { arr ->
            for (i in 0 until arr.length()) {
                val ph = arr.optJSONObject(i) ?: continue
                val ref = ph.optString("name")
                if (ref.isBlank()) continue
                photos += ref
                val credit = ph.optJSONArray("authorAttributions")
                    ?.optJSONObject(0)?.optString("displayName").orEmpty()
                credits += credit.ifBlank { "Google" }
            }
        }
        return Spot(
            id = "google:$id",
            name = name,
            latitude = lat,
            longitude = lng,
            description = summary.ifBlank { address },
            tags = tagsFor(types),
            bestFor = bestFor(types),
            communityRating = p.optDouble("rating", 0.0),
            reviewCount = p.optInt("userRatingCount", 0),
            photoRefs = photos,
            photoCredits = credits,
            submittedBy = "Google Places",
        )
    }

    /** Human-readable tags from Places types. */
    fun tagsFor(types: List<String>): List<String> {
        val pretty = mapOf(
            "tourist_attraction" to "attraction",
            "art_gallery" to "art gallery",
            "performing_arts_theater" to "theater",
            "movie_theater" to "cinema",
            "night_club" to "nightlife",
            "botanical_garden" to "garden",
            "national_park" to "national park",
            "hindu_temple" to "temple",
        )
        return types
            .map { pretty[it] ?: it.replace('_', ' ') }
            .distinct()
    }

    /** Shoot-type filters derived from Places types. */
    fun bestFor(types: List<String>): List<ShootType> {
        val out = mutableSetOf<ShootType>()
        for (t in types) {
            // Independent ifs (not when): one type can earn several
            // categories, e.g. stadium -> MUSIC_VIDEO + DRONE.
            if (t in setOf(
                    "night_club", "performing_arts_theater",
                    "movie_theater", "stadium",
                )
            ) {
                out += ShootType.MUSIC_VIDEO
            }
            if (t in setOf(
                    "beach", "park", "national_park", "botanical_garden",
                    "marina", "tourist_attraction", "zoo", "aquarium",
                )
            ) {
                out += ShootType.SCENIC
            }
            if (t in setOf(
                    "museum", "art_gallery", "church", "mosque",
                    "hindu_temple", "synagogue",
                )
            ) {
                out += ShootType.NARRATIVE
            }
            if (t in setOf("stadium", "amusement_park", "beach", "national_park")) {
                out += ShootType.DRONE
            }
        }
        if (out.isEmpty()) out += ShootType.RUN_AND_GUN
        return out.toList()
    }

    // ------------------------------------------------------------------
    // Network.
    // ------------------------------------------------------------------

    private suspend fun post(endpoint: String, body: String, apiKey: String): String =
        withContext(Dispatchers.IO) {
            val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("X-Goog-Api-Key", apiKey)
                setRequestProperty("X-Goog-FieldMask", FIELD_MASK)
                setRequestProperty(
                    "User-Agent", "SceneScout/1.0 (shotbyx-dev; videographer location app)")
                connectTimeout = 15000
                readTimeout = 30000
            }
            try {
                conn.outputStream.bufferedWriter().use { it.write(body) }
                if (conn.responseCode != 200) {
                    val err = conn.errorStream?.bufferedReader()?.readText()?.take(300)
                    throw Exception("Places HTTP ${conn.responseCode}: $err")
                }
                conn.inputStream.bufferedReader().readText()
            } finally {
                conn.disconnect()
            }
        }

    /**
     * Nearby shoot-worthy places. Cached per ~1km cell for 10 minutes so
     * tab switches and small map pans don't burn API quota.
     */
    suspend fun discover(
        lat: Double, lng: Double, radiusKm: Double, apiKey: String,
    ): List<Spot> = withContext(Dispatchers.IO) {
        val cell = "${(lat * 100).toInt()}:${(lng * 100).toInt()}"
        synchronized(cache) {
            val (ts, spots) = cache[cell] ?: (0L to emptyList())
            if (System.currentTimeMillis() - ts < CACHE_TTL_MS && spots.isNotEmpty()) {
                return@withContext spots
            }
        }
        val json = post(
            "https://places.googleapis.com/v1/places:searchNearby",
            nearbyBody(lat, lng, (radiusKm * 1000).toInt()),
            apiKey,
        )
        val spots = parsePlaces(json)
        synchronized(cache) { cache[cell] = System.currentTimeMillis() to spots }
        spots
    }

    /** Location-biased text search for the Discover search box. */
    suspend fun textSearch(
        query: String, lat: Double, lng: Double, apiKey: String,
    ): List<Spot> = parsePlaces(
        post(
            "https://places.googleapis.com/v1/places:searchText",
            textBody(query, lat, lng),
            apiKey,
        ),
    )

    fun clearCache() = synchronized(cache) { cache.clear() }

    private val cache = mutableMapOf<String, Pair<Long, List<Spot>>>()
    private const val CACHE_TTL_MS = 10 * 60 * 1000L

    /** Human-readable explanation for a Places failure. Never throws. */
    fun friendlyError(cause: Throwable?): String {
        val msg = cause?.message.orEmpty()
        return when {
            "HTTP 400" in msg ->
                "Google rejected the request — the API key looks invalid. " +
                    "Check the key in About > Google API key."
            "HTTP 403" in msg ->
                "Google refused the key — enable Places API (New) and billing " +
                    "on the key's Cloud project, and check its restrictions."
            "HTTP 429" in msg ->
                "Google quota exceeded — wait a bit and try again."
            "timeout" in msg.lowercase() || cause is java.net.SocketTimeoutException ->
                "Google didn't answer in time — check your connection and retry."
            cause is java.net.UnknownHostException ->
                "No internet connection — showing sample spots."
            msg.isNotBlank() -> "Live discovery failed: ${msg.take(160)}"
            else -> "Live discovery failed — showing sample spots."
        }
    }

    // ------------------------------------------------------------------
    // Merging.
    // ------------------------------------------------------------------

    /**
     * Static samples first, then live spots that aren't near-duplicates of a
     * static one (within [minSeparationM]).
     */
    fun mergeSpots(
        static: List<Spot>, live: List<Spot>, minSeparationM: Double = 150.0,
    ): List<Spot> {
        val fresh = live.filter { o ->
            static.none { s ->
                haversineM(s.latitude, s.longitude, o.latitude, o.longitude) < minSeparationM
            }
        }
        return static + fresh
    }

    internal fun haversineM(
        lat1: Double, lon1: Double, lat2: Double, lon2: Double,
    ): Double {
        val r = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        return 2 * r * asin(sqrt(a))
    }
}
