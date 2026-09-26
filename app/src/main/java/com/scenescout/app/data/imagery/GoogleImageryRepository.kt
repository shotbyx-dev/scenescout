package com.scenescout.app.data.imagery

import com.scenescout.app.data.Spot
import com.scenescout.app.data.places.GooglePlaces
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Google Places imagery: real Place Photos for heroes and galleries.
 * Display-only with per-photo author credit (Google's attribution terms).
 * Results are cached per spot id so scrolling never re-fires photo URLs.
 */
class GoogleImageryRepository(
    private val apiKey: () -> String,
) : ImageryRepository {

    override fun imagesFor(spot: Spot): List<SpotImage> {
        val key = apiKey()
        if (!GooglePlaces.hasKey(key) || spot.photoRefs.isEmpty()) return emptyList()
        return spot.photoRefs.mapIndexed { i, ref ->
            SpotImage(
                url = GooglePlaces.photoUrl(ref, key, 1600),
                title = spot.name,
                credit = spot.photoCredits.getOrNull(i)?.ifBlank { "Google" }
                    ?: "Google",
                source = ImagerySource.GOOGLE_PLACE_PHOTO,
            )
        }
    }

    suspend fun imagesForAsync(spot: Spot): List<SpotImage> =
        withContext(Dispatchers.Default) { imagesFor(spot) }

    /**
     * Single best display image for cards/list rows. Null when the place has
     * no Google photos — the card then shows its branded gradient.
     */
    suspend fun heroForAsync(spot: Spot): SpotImage? {
        heroCache[spot.id]?.let { return it }
        if (heroCache.containsKey(spot.id)) return null
        val hero = withContext(Dispatchers.Default) {
            val key = apiKey()
            if (!GooglePlaces.hasKey(key) || spot.photoRefs.isEmpty()) {
                null
            } else {
                SpotImage(
                    url = GooglePlaces.photoUrl(spot.photoRefs.first(), key, 800),
                    title = spot.name,
                    credit = spot.photoCredits.firstOrNull()?.ifBlank { "Google" }
                        ?: "Google",
                    source = ImagerySource.GOOGLE_PLACE_PHOTO,
                )
            }
        }
        heroCache[spot.id] = hero
        return hero
    }

    fun clearCache() = heroCache.clear()

    private val heroCache = mutableMapOf<String, SpotImage?>()
}
