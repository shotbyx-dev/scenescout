package com.scenescout.app.data.imagery

import com.scenescout.app.data.Spot

/**
 * Provides imagery for a spot.
 *
 * Quality strategy ("best thing we can use"):
 *  - DISPLAY: Google Place Photos (sharp, current, per-photo credit).
 *  - AI ANALYSIS: only Mapillary + user uploads (Google ToS forbids
 *    AI/ML use of their imagery).
 */
interface ImageryRepository {
    fun imagesFor(spot: Spot): List<SpotImage>
}
