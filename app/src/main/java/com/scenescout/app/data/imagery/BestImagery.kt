package com.scenescout.app.data.imagery

/**
 * Picks the best image for each job, honoring the legal split:
 *  - forDisplay: sharpest image from ANY source (Google's are the best
 *    looking — display is allowed with attribution).
 *  - forAnalysis: sharpest image from AI-eligible sources ONLY
 *    (Mapillary, user uploads). Google imagery is never sent to AI.
 *
 * Pure Kotlin — unit-testable.
 */
object BestImagery {

    /** Display priority when resolutions tie: Google photos look best. */
    private val displayOrder = listOf(
        ImagerySource.GOOGLE_PLACE_PHOTO,
        ImagerySource.GOOGLE_STREET_VIEW,
        ImagerySource.MAPILLARY,
        ImagerySource.USER_UPLOAD,
    )

    /** Best single image to SHOW the user for a spot. */
    fun forDisplay(images: List<SpotImage>): SpotImage? =
        images.maxWithOrNull(
            compareBy<SpotImage> { it.widthPx ?: 0 }
                .thenBy { -(displayOrder.indexOf(it.source)) },
        )

    /** All images SHOWABLE in a gallery strip, sharpest first. */
    fun gallery(images: List<SpotImage>): List<SpotImage> =
        images.sortedWith(
            compareByDescending<SpotImage> { it.widthPx ?: 0 }
                .thenBy { displayOrder.indexOf(it.source) },
        )

    /** Best image the AI is ALLOWED to analyze for a spot. */
    fun forAnalysis(images: List<SpotImage>): SpotImage? =
        images.filter { it.source.analyzableByAi }
            .maxByOrNull { it.widthPx ?: 0 }

    /** True when a spot has at least one AI-eligible image to score. */
    fun hasAnalyzableImage(images: List<SpotImage>): Boolean =
        images.any { it.source.analyzableByAi }
}
