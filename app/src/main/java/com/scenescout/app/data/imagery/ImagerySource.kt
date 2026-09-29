package com.scenescout.app.data.imagery

/**
 * Where a spot image comes from, and what we're legally allowed to do with it.
 *
 * The split matters because of Google's ToS: Google imagery may be DISPLAYED
 * (with attribution) but must NOT be fed to AI models. Mapillary imagery is
 * CC BY-SA and explicitly allows analysis/ML use, so it stays eligible for
 * future AI scoring.
 */
enum class ImagerySource(
    val analyzableByAi: Boolean,
    val attribution: String,
) {
    /** Google Place Photos — highest quality, display only. */
    GOOGLE_PLACE_PHOTO(
        analyzableByAi = false,
        attribution = "Photo © Google contributors",
    ),

    /** Mapillary street-level imagery — display + AI analysis allowed (CC BY-SA). */
    MAPILLARY(
        analyzableByAi = true,
        attribution = "Imagery © Mapillary contributors (CC BY-SA)",
    ),

    /** Photos the user (or community) uploaded — display + AI analysis allowed. */
    USER_UPLOAD(
        analyzableByAi = true,
        attribution = "Photo © uploader",
    ),
}

/** One image of a spot. */
data class SpotImage(
    val url: String,
    val source: ImagerySource,
    /** Width in px when known — used to prefer the sharpest image. */
    val widthPx: Int? = null,
    val capturedAtMs: Long? = null,
    /** Facing direction in degrees, when known (Mapillary compass_angle). */
    val compassDeg: Double? = null,
    /** Per-photo credit (e.g. the Wikimedia author); falls back to source.attribution. */
    val credit: String? = null,
    /** Commons file title, when known — used to score photo relevance. */
    val title: String? = null,
)
