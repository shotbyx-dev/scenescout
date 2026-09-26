package com.scenescout.app.data

/**
 * Spot data source. v1 ships with curated sample spots so the app works
 * offline on first run. This interface is where the real backends plug in:
 *  - Places API for nearby scenic places + Google reviews ("comments database")
 *  - Street View imagery -> AI vision scoring for [Spot.aiScore]
 *  - Firestore for community-submitted spots, stars, and shoot notes
 */
interface SpotRepository {
    fun nearbySpots(latitude: Double, longitude: Double, radiusKm: Double): List<Spot>
    fun spotById(id: String): Spot?
    fun reviewsFor(spotId: String): List<SpotReview>
}

class SampleSpotRepository : SpotRepository {

    private val spots = listOf(
        Spot(
            id = "wynwood-walls",
            name = "Wynwood Walls",
            latitude = 25.8010, longitude = -80.1995,
            description = "Iconic outdoor street-art district. Bold murals, textured walls, " +
                "great leading lines for music videos.",
            tags = listOf("murals", "urban", "colorful", "graffiti"),
            bestFor = listOf(ShootType.MUSIC_VIDEO, ShootType.RUN_AND_GUN, ShootType.SCENIC),
            aiScore = 92, communityRating = 4.7, reviewCount = 128,
            permit = PermitGuide.lookup("Miami, FL"),
        ),
        Spot(
            id = "south-pointe",
            name = "South Pointe Park",
            latitude = 25.7807, longitude = -80.1319,
            description = "Oceanfront park with skyline views, cruise ships passing, " +
                "and wide open lawns. Killer at golden hour.",
            tags = listOf("ocean", "skyline", "sunset", "park"),
            bestFor = listOf(ShootType.SCENIC, ShootType.DRONE, ShootType.MUSIC_VIDEO),
            aiScore = 95, communityRating = 4.9, reviewCount = 214,
            permit = PermitGuide.lookup("Miami, FL"),
        ),
        Spot(
            id = "vizcaya",
            name = "Vizcaya Museum & Gardens",
            latitude = 25.7441, longitude = -80.2108,
            description = "European-style villa and gardens on Biscayne Bay. Timeless " +
                "architecture, fountains, stone staircases.",
            tags = listOf("historic", "gardens", "architecture", "waterfront"),
            bestFor = listOf(ShootType.NARRATIVE, ShootType.MUSIC_VIDEO),
            aiScore = 97, communityRating = 4.8, reviewCount = 342,
            permit = PermitInfo(
                PermitLevel.REQUIRED,
                "Vizcaya requires advance booking and a location fee for commercial shoots.",
                "Vizcaya Museum", "https://vizcaya.org",
            ),
        ),
        Spot(
            id = "little-havana",
            name = "Calle Ocho, Little Havana",
            latitude = 25.7653, longitude = -80.2198,
            description = "Neon signs, domino tables, cigar shops. Authentic street energy " +
                "for run-and-gun storytelling.",
            tags = listOf("street", "neon", "culture", "night"),
            bestFor = listOf(ShootType.RUN_AND_GUN, ShootType.NARRATIVE, ShootType.MUSIC_VIDEO),
            aiScore = 84, communityRating = 4.4, reviewCount = 96,
            permit = PermitGuide.lookup("Miami, FL"),
        ),
        Spot(
            id = "key-biscayne",
            name = "Crandon Park Beach",
            latitude = 25.7156, longitude = -80.1558,
            description = "Wide empty beaches, mangroves, and the Bear Cut bridge. " +
                "Feels remote, 20 minutes from downtown.",
            tags = listOf("beach", "empty", "nature", "sunrise"),
            bestFor = listOf(ShootType.SCENIC, ShootType.DRONE, ShootType.MUSIC_VIDEO),
            aiScore = 90, communityRating = 4.6, reviewCount = 187,
            permit = PermitGuide.lookup("Miami, FL"),
        ),
    )

    private val reviews = listOf(
        SpotReview("r1", "south-pointe", "Marcus T.", 5,
            "Shot a music video here last Friday — nobody bothered us at all.",
            "Security waved, we had the lawn to ourselves until 7pm.", "golden hour"),
        SpotReview("r2", "south-pointe", "Dre", 5,
            "Sun is perfect right before sunset, skyline lights up behind you.",
            "", "sunset"),
        SpotReview("r3", "wynwood-walls", "Lena K.", 4,
            "Great backdrops but crowded on weekends. Go early morning.",
            "Parking was easy at 8am, crowds by noon.", "early morning"),
        SpotReview("r4", "vizcaya", "Sofia R.", 5,
            "Worth every penny of the location fee. Fountains at 4pm = magic.",
            "Booked 3 weeks out, staff was super helpful.", "afternoon"),
    )

    override fun nearbySpots(latitude: Double, longitude: Double, radiusKm: Double): List<Spot> {
        // v1: sample data is Miami-based; real impl queries Places API by lat/lng.
        return spots.sortedByDescending { ScenicScorer.scoreSpot(it).overall }
    }

    override fun spotById(id: String): Spot? = spots.firstOrNull { it.id == id }

    override fun reviewsFor(spotId: String): List<SpotReview> =
        reviews.filter { it.spotId == spotId }
}
