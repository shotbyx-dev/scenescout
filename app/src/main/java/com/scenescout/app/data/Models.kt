package com.scenescout.app.data

import com.scenescout.app.data.imagery.SpotImage

/** What kind of shoot a spot is good for. */
enum class ShootType(val label: String) {
    MUSIC_VIDEO("Music video"),
    RUN_AND_GUN("Run & gun"),
    SCENIC("Scenic / B-roll"),
    NARRATIVE("Narrative"),
    DRONE("Drone / aerial"),
}

/** A filming location. */
data class Spot(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val description: String,
    val tags: List<String> = emptyList(),
    val bestFor: List<ShootType> = emptyList(),
    /** 0..100 scenic score estimate — on-device heuristics; vision AI is on the roadmap. */
    val aiScore: Int = 0,
    /** 0..5 community star average. */
    val communityRating: Double = 0.0,
    val reviewCount: Int = 0,
    val permit: PermitInfo = PermitInfo.unknown(),
    val submittedBy: String = "SceneScout",
    /** Imagery for this spot; see ImagerySource for display vs AI rules. */
    val images: List<SpotImage> = emptyList(),
    /**
     * Google Places photo resource names, e.g. "places/abc/photos/xyz".
     * Parallel [photoCredits] holds the author credit per photo.
     */
    val photoRefs: List<String> = emptyList(),
    val photoCredits: List<String> = emptyList(),
)

/** A community review of a spot. */
data class SpotReview(
    val id: String,
    val spotId: String,
    val author: String,
    /** 1..5 stars. */
    val stars: Int,
    val text: String,
    /** e.g. "nobody bothered us", "golden hour amazing". */
    val shootNotes: String = "",
    /** Best time of day reported, e.g. "sunset", "early morning". */
    val bestTime: String = "",
    val timestampMs: Long = System.currentTimeMillis(),
)

/** Permit requirement info for a spot's jurisdiction. */
data class PermitInfo(
    /** NONE, SIMPLE (free/online), REQUIRED (paid application). */
    val level: PermitLevel,
    val summary: String,
    val authorityName: String = "",
    val authorityUrl: String = "",
) {
    companion object {
        fun unknown() = PermitInfo(
            PermitLevel.UNKNOWN,
            "Permit rules not yet verified for this spot — check with the local film office.",
        )
    }
}

enum class PermitLevel(val label: String) {
    NONE("No permit needed"),
    SIMPLE("Simple / free permit"),
    REQUIRED("Permit required"),
    UNKNOWN("Unknown"),
}

/** Result of scoring a spot for a shoot. */
data class ScenicScore(
    val overall: Int,          // 0..100
    val visualAppeal: Int,     // 0..100 heuristic estimate (type, rating, photos)
    val shootability: Int,     // 0..100 crowd/access/parking friendliness
    val community: Int,        // 0..100 from star ratings
    val label: String,         // "Must shoot", "Strong pick", ...
)
