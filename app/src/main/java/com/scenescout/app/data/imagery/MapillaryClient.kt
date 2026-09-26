package com.scenescout.app.data.imagery

import kotlin.math.cos
import kotlin.math.PI

/**
 * Mapillary API v4 client (URL builders + response parsing helpers).
 *
 * Mapillary street-level imagery is CC BY-SA: it may be displayed AND fed to
 * AI models — it's the legal source for "AI looks at street imagery and finds
 * beautiful spots". Needs a (free) access token from mapillary.com/dashboard.
 *
 * Docs: https://www.mapillary.com/developer/api-documentation
 */
object MapillaryClient {

    const val GRAPH_BASE = "https://graph.mapillary.com"

    /**
     * Fields we request: the sharpest thumbnails available plus geometry,
     * capture time, compass, and the experimental quality score.
     */
    const val IMAGE_FIELDS =
        "id,thumb_256_url,thumb_1024_url,thumb_2048_url,thumb_original_url," +
            "computed_geometry,captured_at,compass_angle,quality_score"

    /**
     * Search for images near a point. The API supports a radius search
     * (max 50 m); we build a small bounding box around the point instead so
     * one request covers the immediate area around a spot.
     */
    fun searchUrl(
        latitude: Double,
        longitude: Double,
        accessToken: String,
        radiusMeters: Double = 50.0,
    ): String {
        val (west, south, east, north) = bbox(latitude, longitude, radiusMeters)
        return "$GRAPH_BASE/images" +
            "?access_token=$accessToken" +
            "&fields=$IMAGE_FIELDS" +
            "&bbox=$west,$south,$east,$north" +
            "&limit=20"
    }

    /** Single image by id (when we already know the key). */
    fun imageUrl(imageId: String, accessToken: String): String =
        "$GRAPH_BASE/$imageId?access_token=$accessToken&fields=$IMAGE_FIELDS"

    /**
     * Pick the sharpest thumbnail URL from a Mapillary image JSON object's
     * thumbnail fields. Prefers original > 2048 > 1024 > 256.
     */
    fun bestThumbnail(thumbs: Map<String, String?>): Pair<String, Int>? {
        val ranked = listOf(
            "thumb_original_url" to 4000,
            "thumb_2048_url" to 2048,
            "thumb_1024_url" to 1024,
            "thumb_256_url" to 256,
        )
        for ((field, width) in ranked) {
            val url = thumbs[field]
            if (!url.isNullOrBlank()) return url to width
        }
        return null
    }

    /** Bounding box (west,south,east,north) around a point. */
    fun bbox(lat: Double, lng: Double, radiusMeters: Double): DoubleArray {
        val latDelta = radiusMeters / 111_320.0
        val lngDelta = radiusMeters / (111_320.0 * cos(lat * PI / 180.0))
        return doubleArrayOf(
            lng - lngDelta, lat - latDelta,
            lng + lngDelta, lat + latDelta,
        )
    }
}
