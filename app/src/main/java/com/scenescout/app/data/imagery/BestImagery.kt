package com.scenescout.app.data.imagery

/**
 * Picks the best image to show, honoring the legal split: Google imagery
 * may be DISPLAYED (with attribution) but must never be fed to AI models.
 * Only Mapillary (CC BY-SA) and user uploads are eligible for future AI
 * analysis — no AI scorer ships in this version.
 *
 * Pure Kotlin — unit-testable.
 */
object BestImagery {

    /** Display priority when resolutions tie: Google photos look best. */
    private val displayOrder = listOf(
        ImagerySource.GOOGLE_PLACE_PHOTO,
        ImagerySource.MAPILLARY,
        ImagerySource.USER_UPLOAD,
    )

    /** Best single image to SHOW the user for a spot. */
    fun forDisplay(images: List<SpotImage>): SpotImage? =
        images.maxWithOrNull(
            compareBy<SpotImage> { it.widthPx ?: 0 }
                .thenBy { -(displayOrder.indexOf(it.source)) },
        )

    /** True when a spot has at least one AI-eligible image to score. */
    fun hasAnalyzableImage(images: List<SpotImage>): Boolean =
        images.any { it.source.analyzableByAi }
}
