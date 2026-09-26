package com.scenescout.app.data.imagery

import com.scenescout.app.data.Spot

/**
 * Provides imagery for a spot from every legal source, in one place.
 *
 * Quality strategy ("best thing we can use"):
 *  - DISPLAY: Google Place Photos / Street View stills when a Maps API key is
 *    set (sharpest, most current). Falls back to Mapillary, then user uploads.
 *  - AI ANALYSIS: only Mapillary + user uploads (Google ToS forbids AI use).
 *
 * v0.2 wires the URL builders; network fetching + JSON parsing lands with the
 * Firestore/Mapillary integration. Sample spots get Street View strips when a
 * key is configured, so the map shows real imagery on first run.
 */
interface ImageryRepository {
    fun imagesFor(spot: Spot): List<SpotImage>
}

class SampleImageryRepository(
    private val mapsApiKey: String = "",
    private val mapillaryToken: String = "",
) : ImageryRepository {

    private val hasGoogleKey = mapsApiKey.isNotBlank() &&
        mapsApiKey != "MAPS_API_KEY_NOT_SET"
    private val hasMapillary = mapillaryToken.isNotBlank()

    override fun imagesFor(spot: Spot): List<SpotImage> {
        val out = mutableListOf<SpotImage>()
        // Sharpest display imagery first: Google Street View strip.
        if (hasGoogleKey) {
            out += StreetView.panoramaStrip(spot.latitude, spot.longitude, mapsApiKey)
        }
        // Community uploads always allowed (display + AI).
        out += spot.images.filter { it.source == ImagerySource.USER_UPLOAD }
        return BestImagery.gallery(out)
    }

    /**
     * Full gallery: sync sources plus real nearby photos from Wikimedia
     * Commons (free, no key). Call from a coroutine and update the UI.
     */
    suspend fun imagesForAsync(spot: Spot): List<SpotImage> {
        val base = imagesFor(spot).toMutableList()
        // Wikimedia photos are real and location-relevant — put them first
        // when there's nothing sharper from Google.
        val wiki = WikimediaClient.searchPhotos(spot.latitude, spot.longitude)
        if (wiki.isNotEmpty() && base.none {
                it.source == ImagerySource.GOOGLE_STREET_VIEW ||
                    it.source == ImagerySource.GOOGLE_PLACE_PHOTO
            }
        ) {
            base.addAll(0, wiki)
        } else {
            base += wiki
        }
        return BestImagery.gallery(base)
    }

    /**
     * Live Mapillary search for a spot. Returns null when no token is set.
     * The next iteration fetches this URL and turns the JSON into SpotImages
     * via [MapillaryClient.bestThumbnail].
     */
    fun mapillarySearchUrlFor(spot: Spot): String? =
        if (hasMapillary) {
            MapillaryClient.searchUrl(spot.latitude, spot.longitude, mapillaryToken)
        } else {
            null
        }
}
