package com.scenescout.app.data

import kotlin.math.pow

/**
 * Spot data source. v1 ships with curated sample spots so the app works
 * offline on first run. This interface is where the real backends plug in:
 *  - Places API for nearby scenic places + Google reviews ("comments database")
 *  - Future: vision-AI scoring of imagery for [Spot.aiScore] (heuristics for now)
 *  - Firestore for community-submitted spots, stars, and shoot notes
 */
interface SpotRepository {
    fun nearbySpots(latitude: Double, longitude: Double, radiusKm: Double): List<Spot>
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
        // ---- Urban / gritty / roadside: real texture for music videos ----
        Spot(
            id = "marine-stadium",
            name = "Miami Marine Stadium",
            latitude = 25.7447, longitude = -80.1698,
            description = "Abandoned waterfront stadium swallowed by graffiti. " +
                "Raw concrete, insane textures, total post-apocalyptic energy.",
            tags = listOf("abandoned", "graffiti", "concrete", "decay", "urban",
                "stadium", "gritty"),
            bestFor = listOf(ShootType.MUSIC_VIDEO, ShootType.RUN_AND_GUN,
                ShootType.NARRATIVE),
            aiScore = 93, communityRating = 4.8, reviewCount = 156,
            permit = PermitGuide.lookup("Miami, FL"),
        ),
        Spot(
            id = "hialeah-railyard",
            name = "Hialeah Rail Yard",
            latitude = 25.8570, longitude = -80.2780,
            description = "Freight yard with rusted boxcars, floodlights, and endless " +
                "industrial lines. Best after dark from the perimeter.",
            tags = listOf("industrial", "freight", "trains", "rust", "night",
                "gritty", "urban"),
            bestFor = listOf(ShootType.MUSIC_VIDEO, ShootType.RUN_AND_GUN),
            aiScore = 85, communityRating = 4.3, reviewCount = 74,
            permit = PermitGuide.lookup("Miami, FL"),
        ),
        Spot(
            id = "us1-roadside",
            name = "US-1 Roadside Relics",
            latitude = 25.4475, longitude = -80.4783,
            description = "Highway strip with junkyards, a broken-down truck or two, " +
                "faded billboards, and dusty lots. Props everywhere you look.",
            tags = listOf("roadside", "truck", "highway", "props", "junkyard",
                "billboard", "dusty", "wreck"),
            bestFor = listOf(ShootType.MUSIC_VIDEO, ShootType.RUN_AND_GUN,
                ShootType.NARRATIVE),
            aiScore = 82, communityRating = 4.5, reviewCount = 63,
            permit = PermitInfo(
                PermitLevel.SIMPLE,
                "Roadside shoulders are usually fine for small crews; " +
                    "private lots need the owner's okay.",
            ),
        ),
        Spot(
            id = "downtown-rooftop",
            name = "Downtown Rooftop Garage",
            latitude = 25.7745, longitude = -80.1930,
            description = "Open-air parking rooftop: bare concrete, painted lines, " +
                "and the skyline glowing behind you. Empty after 9pm.",
            tags = listOf("rooftop", "concrete", "skyline", "night", "parking",
                "urban"),
            bestFor = listOf(ShootType.MUSIC_VIDEO, ShootType.NARRATIVE),
            aiScore = 88, communityRating = 4.6, reviewCount = 112,
            permit = PermitInfo(
                PermitLevel.SIMPLE,
                "Garage is private property — ask building management; " +
                    "many allow small shoots off-hours.",
            ),
        ),
        Spot(
            id = "little-haiti",
            name = "Little Haiti Murals",
            latitude = 25.8250, longitude = -80.1950,
            description = "Caribbean color everywhere: hand-painted murals, botanicas, " +
                "record shops. Loud, joyful street energy.",
            tags = listOf("murals", "colorful", "culture", "street", "paint",
                "urban"),
            bestFor = listOf(ShootType.MUSIC_VIDEO, ShootType.RUN_AND_GUN,
                ShootType.SCENIC),
            aiScore = 87, communityRating = 4.5, reviewCount = 89,
            permit = PermitGuide.lookup("Miami, FL"),
        ),
        // ---- Fort Lauderdale & beyond: near the user's home turf ----
        Spot(
            id = "ftl-beach",
            name = "Fort Lauderdale Beach",
            latitude = 26.1189, longitude = -80.1046,
            description = "Palm-lined A1A strip, white sand, sunrise over the Atlantic. " +
                "Clean wide shots by day, neon glow by night.",
            tags = listOf("beach", "sunrise", "palm trees", "neon"),
            bestFor = listOf(ShootType.SCENIC, ShootType.MUSIC_VIDEO, ShootType.DRONE),
            aiScore = 89, communityRating = 4.6, reviewCount = 143,
            permit = PermitGuide.lookup("Miami, FL"),
        ),
        Spot(
            id = "riverwalk",
            name = "Riverwalk & Las Olas",
            latitude = 26.1194, longitude = -80.1373,
            description = "Downtown riverfront with bridges, yachts, and a glowing skyline. " +
                "Cinematic night reflections on the New River.",
            tags = listOf("skyline", "river", "night", "bridges", "urban"),
            bestFor = listOf(ShootType.NARRATIVE, ShootType.MUSIC_VIDEO, ShootType.SCENIC),
            aiScore = 91, communityRating = 4.7, reviewCount = 118,
            permit = PermitGuide.lookup("Miami, FL"),
        ),
        Spot(
            id = "fatvillage",
            name = "FATVillage Arts District",
            latitude = 26.1218, longitude = -80.1470,
            description = "Warehouse blocks covered in murals, rusted roll-up doors, " +
                "string lights. Raw industrial texture minutes from downtown.",
            tags = listOf("murals", "warehouses", "gritty", "industrial", "graffiti"),
            bestFor = listOf(ShootType.MUSIC_VIDEO, ShootType.RUN_AND_GUN, ShootType.NARRATIVE),
            aiScore = 87, communityRating = 4.5, reviewCount = 89,
            permit = PermitGuide.lookup("Miami, FL"),
        ),
        Spot(
            id = "hollywood-broadwalk",
            name = "Hollywood Beach Broadwalk",
            latitude = 26.0113, longitude = -80.1170,
            description = "Retro beach motels with vintage neon signs along a 2.5-mile " +
                "boardwalk. Pure Americana time capsule.",
            tags = listOf("neon", "retro", "boardwalk", "motels", "americana"),
            bestFor = listOf(ShootType.MUSIC_VIDEO, ShootType.NARRATIVE, ShootType.SCENIC),
            aiScore = 88, communityRating = 4.6, reviewCount = 102,
            permit = PermitGuide.lookup("Miami, FL"),
        ),
        Spot(
            id = "port-everglades",
            name = "Port Everglades",
            latitude = 26.0867, longitude = -80.1167,
            description = "Towering container cranes and stacked shipping containers. " +
                "Massive industrial scale — shoot from public roads at the perimeter.",
            tags = listOf("industrial", "cranes", "containers", "gritty", "scale"),
            bestFor = listOf(ShootType.MUSIC_VIDEO, ShootType.DRONE, ShootType.RUN_AND_GUN),
            aiScore = 85, communityRating = 4.3, reviewCount = 64,
            permit = PermitInfo(
                PermitLevel.SIMPLE,
                "Stay on public roads outside the port gates; the cranes read " +
                    "huge on camera from a distance.",
                "Port Everglades", "https://www.porteverglades.net",
            ),
        ),
        Spot(
            id = "dania-pier",
            name = "Dania Beach Pier",
            latitude = 26.0523, longitude = -80.1110,
            description = "Old-school fishing pier, weathered wood, pelicans, and empty " +
                "sunrise beaches. Moody and quiet at dawn.",
            tags = listOf("pier", "sunrise", "weathered", "ocean", "moody"),
            bestFor = listOf(ShootType.SCENIC, ShootType.MUSIC_VIDEO, ShootType.NARRATIVE),
            aiScore = 86, communityRating = 4.5, reviewCount = 77,
            permit = PermitGuide.lookup("Miami, FL"),
        ),
        Spot(
            id = "bayfront-park",
            name = "Bayfront Park & Bayside",
            latitude = 25.7792, longitude = -80.1839,
            description = "Marina, palm promenade, and the Skyviews ferris wheel against " +
                "the downtown skyline. Built-in production design.",
            tags = listOf("skyline", "marina", "ferris wheel", "night", "urban"),
            bestFor = listOf(ShootType.MUSIC_VIDEO, ShootType.SCENIC, ShootType.NARRATIVE),
            aiScore = 90, communityRating = 4.6, reviewCount = 156,
            permit = PermitGuide.lookup("Miami, FL"),
        ),
        Spot(
            id = "matheson-hammock",
            name = "Matheson Hammock Park",
            latitude = 25.6828, longitude = -80.2797,
            description = "Man-made atoll pool ringed by mangroves, plus dense hammock " +
                "trails. Otherworldly and empty on weekday mornings.",
            tags = listOf("mangroves", "atoll", "nature", "moody", "empty"),
            bestFor = listOf(ShootType.SCENIC, ShootType.DRONE, ShootType.MUSIC_VIDEO),
            aiScore = 88, communityRating = 4.7, reviewCount = 93,
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
        SpotReview("r5", "marine-stadium", "Marcus T.", 5,
            "Shot a drill video here — had the place to ourselves for hours.",
            "Graffiti walls are unreal on camera. Bring boots, rough ground.",
            "golden hour"),
        SpotReview("r6", "hialeah-railyard", "Dre", 4,
            "Stay on the perimeter after dark. Floodlights are free production design.",
            "Security patrols occasionally, keep it quick.", "night"),
        SpotReview("r7", "us1-roadside", "Lena K.", 5,
            "Found a broken-down truck that made the whole video.",
            "Owner let us shoot for $20. Props everywhere on this strip.", "morning"),
    )

    override fun nearbySpots(latitude: Double, longitude: Double, radiusKm: Double): List<Spot> {
        // Real GPS filtering: haversine distance, sorted nearest-first, then best score.
        return spots
            .map { it to haversineKm(latitude, longitude, it.latitude, it.longitude) }
            .filter { (_, km) -> km <= radiusKm }
            .sortedWith(
                compareBy<Pair<Spot, Double>> { (_, km) -> km }
                    .thenByDescending { (spot, _) -> ScenicScorer.scoreSpot(spot).overall },
            )
            .map { (spot, _) -> spot }
    }

    private fun haversineKm(
        lat1: Double, lon1: Double, lat2: Double, lon2: Double,
    ): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2).pow(2.0) +
            Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
            Math.sin(dLon / 2).pow(2.0)
        return 2 * r * Math.asin(Math.sqrt(a))
    }

    override fun reviewsFor(spotId: String): List<SpotReview> =
        reviews.filter { it.spotId == spotId }
}
