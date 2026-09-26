package com.scenescout.app.data.imagery

/**
 * Google imagery URL builders — DISPLAY ONLY.
 *
 * Google Maps Platform ToS prohibits using this imagery to train, test, or run
 * AI/ML models, and prohibits extracting data from it. These URLs are for
 * showing the user what a spot looks like, with the required attribution.
 * AI scoring must use [ImagerySource.MAPILLARY] or [ImagerySource.USER_UPLOAD].
 */
object StreetView {

    private const val STATIC_BASE = "https://maps.googleapis.com/maps/api/streetview"
    private const val METADATA_BASE = "https://maps.googleapis.com/maps/api/streetview/metadata"

    /**
     * Highest-quality still for a location. size caps at 640x640 per request
     * (640x640 with scale=2 returns a 1280px image on capable plans).
     */
    fun stillUrl(
        latitude: Double,
        longitude: Double,
        apiKey: String,
        headingDeg: Int = 0,
        pitchDeg: Int = 0,
        width: Int = 640,
        height: Int = 640,
        hiDpi: Boolean = true,
    ): String =
        "$STATIC_BASE?size=${width}x$height" +
            "&location=$latitude,$longitude" +
            "&heading=$headingDeg&pitch=$pitchDeg" +
            (if (hiDpi) "&scale=2" else "") +
            "&key=$apiKey"

    /** Free, unlimited check for whether Street View exists at a location. */
    fun metadataUrl(latitude: Double, longitude: Double, apiKey: String): String =
        "$METADATA_BASE?location=$latitude,$longitude&key=$apiKey"

    /** 4 headings around a point, for a mini panorama strip. */
    fun panoramaStrip(
        latitude: Double,
        longitude: Double,
        apiKey: String,
    ): List<SpotImage> = listOf(0, 90, 180, 270).map { heading ->
        SpotImage(
            url = stillUrl(latitude, longitude, apiKey, headingDeg = heading),
            source = ImagerySource.GOOGLE_STREET_VIEW,
            widthPx = 1280,
        )
    }
}
