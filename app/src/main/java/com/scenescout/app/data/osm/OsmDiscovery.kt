package com.scenescout.app.data.osm

import com.scenescout.app.data.PermitInfo
import com.scenescout.app.data.ShootType
import com.scenescout.app.data.Spot
import com.scenescout.app.data.imagery.ImagerySource
import com.scenescout.app.data.imagery.SpotImage
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.cos
import kotlin.math.asin
import kotlin.math.sqrt

/**
 * Live nearby-spot discovery via OpenStreetMap's Overpass API — free, no key.
 * Finds cinematic categories (viewpoints, murals/public art, piers, beaches,
 * historic sites, museums, parks, churches, theatres, marinas) around the
 * user's GPS, and resolves each place's curated photo from its
 * `wikimedia_commons` / `image` tags, so photos actually depict the place.
 *
 * Pure parsing/mapping functions are unit-testable; network lives in the
 * suspend functions with an in-memory cell cache and endpoint fallback.
 */
object OsmDiscovery {

    private val ENDPOINTS = listOf(
        "https://overpass.kumi.systems/api/interpreter",
        "https://overpass-api.de/api/interpreter",
    )
    private const val UA = "SceneScout/1.0 (shotbyx-dev; videographer location app)"
    private const val CACHE_TTL_MS = 10 * 60 * 1000L
    private const val MAX_RESULTS = 60
    private const val MAX_PARKS = 12

    data class OsmPlace(
        val osmType: String, // "node" | "way"
        val osmId: Long,
        val name: String?,
        val lat: Double,
        val lng: Double,
        val tags: Map<String, String>,
    )

    internal data class Kind(
        val label: String,
        val tags: List<String>,
        val bestFor: List<ShootType>,
        /** Estimated scenic score — refined when real imagery/AI exists. */
        val aiScore: Int,
        val blurb: String,
        /** Lower = more cinematic. Parks rank last so murals win. */
        val priority: Int,
    )

    /** First match wins — order is intentional (most specific first). */
    internal fun kindFor(tags: Map<String, String>): Kind? {
        val tourism = tags["tourism"]
        if (tourism == "viewpoint") return Kind(
            "Scenic viewpoint", listOf("viewpoint", "scenic", "vista"),
            listOf(ShootType.SCENIC, ShootType.DRONE, ShootType.NARRATIVE), 78,
            "Elevated view — worth scouting for wides and drone passes.", 0)
        if (tourism == "artwork" && tags["artwork_type"] == "mural") return Kind(
            "Mural", listOf("mural", "urban", "colorful", "street art"),
            listOf(ShootType.MUSIC_VIDEO, ShootType.RUN_AND_GUN, ShootType.SCENIC), 76,
            "Public mural — bold color and texture for music videos.", 1)
        if (tourism == "artwork") return Kind(
            "Public art", listOf("artwork", "urban", "sculpture"),
            listOf(ShootType.MUSIC_VIDEO, ShootType.RUN_AND_GUN, ShootType.SCENIC), 72,
            "Public artwork mapped by the community — go see it in person.", 4)
        if (tourism == "attraction") return Kind(
            "Attraction", listOf("attraction", "landmark"),
            listOf(ShootType.SCENIC, ShootType.RUN_AND_GUN, ShootType.MUSIC_VIDEO), 70,
            "Local attraction — scout angles and crowds before the shoot.", 6)
        if (tourism == "museum" || tourism == "gallery") return Kind(
            "Museum / gallery", listOf("museum", "historic", "interior"),
            listOf(ShootType.NARRATIVE, ShootType.SCENIC), 68,
            "Museum or gallery — check filming policy before you roll.", 7)
        if (tags["historic"] != null) return Kind(
            "Historic site", listOf("historic", "texture", "architecture"),
            listOf(ShootType.NARRATIVE, ShootType.SCENIC, ShootType.MUSIC_VIDEO), 72,
            "Historic site — texture and architecture for period looks.", 5)
        if (tags["man_made"] == "pier") return Kind(
            "Pier", listOf("pier", "water", "sunset"),
            listOf(ShootType.SCENIC, ShootType.MUSIC_VIDEO, ShootType.DRONE), 78,
            "Pier over the water — classic golden-hour leading lines.", 2)
        if (tags["leisure"] == "beach_resort" || tags["natural"] == "beach") return Kind(
            "Beach", listOf("beach", "ocean", "sunset"),
            listOf(ShootType.SCENIC, ShootType.MUSIC_VIDEO, ShootType.DRONE), 80,
            "Beach access — check drone and permit rules for the shoreline.", 3)
        if (tags["leisure"] == "marina") return Kind(
            "Marina", listOf("marina", "boats", "water"),
            listOf(ShootType.SCENIC, ShootType.MUSIC_VIDEO), 70,
            "Marina — masts, hulls and water reflections at blue hour.", 10)
        if (tags["leisure"] == "park") return Kind(
            "Park", listOf("park", "green", "open"),
            listOf(ShootType.SCENIC, ShootType.RUN_AND_GUN, ShootType.DRONE), 65,
            "Public park — open space, easy company moves.", 20)
        if (tags["natural"] == "peak") return Kind(
            "Peak", listOf("peak", "vista", "hike"),
            listOf(ShootType.SCENIC, ShootType.DRONE), 75,
            "High point — big-sky wides if you can get gear up there.", 11)
        val building = tags["building"]
        if (building == "cathedral" || building == "church" || building == "chapel") return Kind(
            "Church", listOf("church", "historic", "architecture"),
            listOf(ShootType.NARRATIVE, ShootType.MUSIC_VIDEO, ShootType.SCENIC), 70,
            "Church architecture — ask before shooting on the grounds.", 9)
        if (tags["amenity"] == "theatre") return Kind(
            "Theatre", listOf("theatre", "neon", "night"),
            listOf(ShootType.MUSIC_VIDEO, ShootType.NARRATIVE), 68,
            "Theatre marquee — neon and night looks.", 8)
        return null
    }

    /**
     * Builds the Overpass QL query. Parks get their own smaller query so 60
     * neighborhood parks can't crowd out the murals and viewpoints.
     */
    fun buildQuery(lat: Double, lng: Double, radiusM: Int, parksOnly: Boolean): String {
        val around = "(around:$radiusM,$lat,$lng)"
        val selectors = if (parksOnly) {
            listOf(
                """node["leisure"="park"]$around""",
                """way["leisure"="park"]$around""",
            )
        } else {
            listOf(
                """node["tourism"~"^(attraction|viewpoint|museum|gallery|artwork)$"]$around""",
                """node["historic"]$around""",
                """node["leisure"~"^(beach_resort|marina)$"]$around""",
                """node["natural"~"^(beach|peak)$"]$around""",
                """node["man_made"="pier"]$around""",
                """node["building"~"^(church|cathedral|chapel)$"]$around""",
                """node["amenity"="theatre"]$around""",
                """way["tourism"~"^(attraction|viewpoint|museum|gallery|artwork)$"]$around""",
                """way["historic"]$around""",
                """way["leisure"~"^(beach_resort|marina)$"]$around""",
                """way["man_made"="pier"]$around""",
                """way["building"~"^(church|cathedral|chapel)$"]$around""",
            )
        }
        val limit = if (parksOnly) MAX_PARKS else MAX_RESULTS
        return "[out:json][timeout:25];(${selectors.joinToString(";")};);out center tags $limit;"
    }

    /** Parses an Overpass JSON response. Pure — unit-testable. */
    fun parseElements(json: String): List<OsmPlace> {
        val out = mutableListOf<OsmPlace>()
        val elements = try {
            JSONObject(json).optJSONArray("elements")
        } catch (e: Exception) {
            null
        } ?: return out
        for (i in 0 until elements.length()) {
            val el = elements.optJSONObject(i) ?: continue
            val type = el.optString("type")
            val id = el.optLong("id", -1)
            if ((type != "node" && type != "way") || id < 0) continue
            val lat: Double
            val lng: Double
            if (type == "node") {
                lat = el.optDouble("lat", Double.NaN)
                lng = el.optDouble("lon", Double.NaN)
            } else {
                val center = el.optJSONObject("center") ?: continue
                lat = center.optDouble("lat", Double.NaN)
                lng = center.optDouble("lon", Double.NaN)
            }
            if (lat.isNaN() || lng.isNaN()) continue
            val tagsJson = el.optJSONObject("tags")
            val tags = mutableMapOf<String, String>()
            tagsJson?.keys()?.forEach { k -> tags[k] = tagsJson.optString(k) }
            out += OsmPlace(type, id, tags["name"], lat, lng, tags)
        }
        return out
    }

    /**
     * The place's curated photo reference: `wikimedia_commons=File:X.jpg`
     * (preferred — depicts the place) or a direct `image=` URL.
     */
    internal fun photoRefFor(tags: Map<String, String>): String? {
        val wc = tags["wikimedia_commons"]?.trim().orEmpty()
        if (wc.startsWith("File:", ignoreCase = true)) return wc
        val image = tags["image"]?.trim().orEmpty()
        if (image.startsWith("http")) return image
        return null
    }

    /**
     * Drops noise: unknown kinds, and unnamed parks (there are thousands;
     * an unnamed patch of grass is not a shoot location).
     */
    internal fun isNoise(place: OsmPlace): Boolean {
        val kind = kindFor(place.tags) ?: return true
        return place.name.isNullOrBlank() && kind.label == "Park"
    }

    /**
     * Drops duplicate live places: same named place mapped twice (node +
     * way), or two unnamed features of the same kind on top of each other.
     */
    internal fun dedupePlaces(places: List<OsmPlace>): List<OsmPlace> {
        val kept = mutableListOf<OsmPlace>()
        for (p in places) {
            val kind = kindFor(p.tags)?.label
            val dup = kept.any { k ->
                haversineM(p.lat, p.lng, k.lat, k.lng) < 150 &&
                    (p.name?.isNotBlank() == true && p.name.equals(k.name, ignoreCase = true) ||
                        (p.name.isNullOrBlank() && k.name.isNullOrBlank() &&
                            kind == kindFor(k.tags)?.label))
            }
            if (!dup) kept.add(p)
        }
        return kept
    }

    private fun resolveUrl(files: List<String>): String =
        "https://commons.wikimedia.org/w/api.php?action=query&format=json" +
            "&prop=imageinfo&iiprop=url&iiurlwidth=800" +
            "&titles=" + files.joinToString("|") { URLEncoder.encode(it, "UTF-8") }

    /**
     * Batch-resolves `File:X` titles to thumbnail URLs via the Commons API.
     * Returns title -> url. Pure-network, no Android.
     */
    suspend fun resolveFileUrls(files: List<String>): Map<String, String> =
        withContext(Dispatchers.IO) {
            if (files.isEmpty()) return@withContext emptyMap()
            try {
                val conn = URL(resolveUrl(files)).openConnection() as HttpURLConnection
                conn.setRequestProperty("User-Agent", UA)
                conn.connectTimeout = 10000
                conn.readTimeout = 15000
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val pages = JSONObject(body).optJSONObject("query")
                    ?.optJSONObject("pages") ?: return@withContext emptyMap()
                val out = mutableMapOf<String, String>()
                val keys = pages.keys()
                while (keys.hasNext()) {
                    val page = pages.optJSONObject(keys.next()) ?: continue
                    if (page.has("missing")) continue
                    val title = page.optString("title")
                    val info = page.optJSONArray("imageinfo")?.optJSONObject(0)
                    val url = info?.optString("thumburl")
                        ?.ifBlank { info.optString("url") }.orEmpty()
                    if (title.isNotBlank() && url.isNotBlank()) out[title] = url
                }
                out
            } catch (e: Exception) {
                emptyMap()
            }
        }

    fun toSpot(place: OsmPlace, photo: SpotImage?): Spot? {
        val kind = kindFor(place.tags) ?: return null
        val name = place.name?.takeIf { it.isNotBlank() }
            ?: "Unnamed ${kind.label.lowercase()}"
        return Spot(
            id = "osm-${place.osmType}-${place.osmId}",
            name = name,
            latitude = place.lat,
            longitude = place.lng,
            description = "${kind.label} mapped by the OpenStreetMap community. ${kind.blurb}",
            tags = (kind.tags + "osm").distinct(),
            bestFor = kind.bestFor,
            aiScore = kind.aiScore,
            permit = PermitInfo.unknown(),
            submittedBy = "OpenStreetMap",
            images = listOfNotNull(photo),
        )
    }

    private val cache = mutableMapOf<String, Pair<Long, List<Spot>>>()

    /**
     * Full live pipeline: discover places, resolve their curated photos,
     * return Spots ranked most-cinematic-first. Cached per ~1km cell for
     * 10 minutes. Throws on network failure so callers can fall back to
     * static data.
     */
    suspend fun discoverSpots(
        lat: Double, lng: Double, radiusKm: Double,
    ): List<Spot> = withContext(Dispatchers.IO) {
        val key = "%.2f,%.2f".format(lat, lng)
        val now = System.currentTimeMillis()
        cache[key]?.let { (ts, spots) ->
            if (now - ts < CACHE_TTL_MS) return@withContext spots
        }
        val radiusM = (radiusKm * 1000).toInt()
        val raw = queryOverpass(buildQuery(lat, lng, radiusM, parksOnly = false)) +
            queryOverpass(buildQuery(lat, lng, radiusM, parksOnly = true))
        val places = dedupePlaces(raw.filter { !isNoise(it) })
            .sortedWith(compareBy(
                { kindFor(it.tags)!!.priority },
                { haversineM(lat, lng, it.lat, it.lng) },
            ))
        val files = places.mapNotNull { p ->
            photoRefFor(p.tags)?.takeIf { it.startsWith("File:", ignoreCase = true) }
        }.distinct()
        val urls = resolveFileUrls(files)
        val spots = places.mapNotNull { place ->
            val ref = photoRefFor(place.tags)
            val photo = when {
                ref == null -> null
                ref.startsWith("http") -> SpotImage(
                    ref, ImagerySource.WIKIMEDIA_COMMONS, widthPx = 800,
                    credit = "Photo via OpenStreetMap")
                else -> urls[ref]?.let {
                    SpotImage(it, ImagerySource.WIKIMEDIA_COMMONS, widthPx = 800,
                        credit = "© Wikimedia Commons contributors")
                }
            }
            toSpot(place, photo)
        }
        cache[key] = now to spots
        spots
    }

    /**
     * One Overpass query with endpoint fallback. Throws if every endpoint
     * fails, so the caller can fall back to static spots.
     */
    private fun queryOverpass(query: String): List<OsmPlace> {
        var lastError: Exception? = null
        for (endpoint in ENDPOINTS) {
            try {
                return postQuery(endpoint, query)
            } catch (e: Exception) {
                lastError = e
            }
        }
        throw lastError ?: Exception("Overpass query failed")
    }

    private fun postQuery(endpoint: String, query: String): List<OsmPlace> {
        val conn = URL(endpoint).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.setRequestProperty("User-Agent", UA)
            conn.setRequestProperty(
                "Content-Type", "application/x-www-form-urlencoded")
            conn.connectTimeout = 10000
            conn.readTimeout = 30000
            val body = "data=" + URLEncoder.encode(query, "UTF-8")
            conn.outputStream.bufferedWriter().use { it.write(body) }
            if (conn.responseCode != 200) throw Exception("HTTP ${conn.responseCode}")
            val json = conn.inputStream.bufferedReader().use { it.readText() }
            return parseElements(json)
        } finally {
            conn.disconnect()
        }
    }

    /**
     * Merges static + live spots, dropping live ones within [minSeparationM]
     * of a static spot (same place, two sources).
     */
    fun merge(static: List<Spot>, live: List<Spot>, minSeparationM: Double = 150.0): List<Spot> {
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
