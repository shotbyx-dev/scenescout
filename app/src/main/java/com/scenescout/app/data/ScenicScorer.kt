package com.scenescout.app.data

import kotlin.math.roundToInt

/**
 * Scores a filming spot by blending three signals:
 *  - visualAppeal: AI analysis of Street View / photo imagery (0..100)
 *  - shootability: how practical the spot is (crowds, access, parking) (0..100)
 *  - community: normalized star ratings from videographers (0..100)
 *
 * Pure Kotlin — no Android dependencies — so it is fully unit-testable.
 */
object ScenicScorer {

    /** Weights used when all three signals are present. */
    private const val W_AI = 0.45
    private const val W_SHOOT = 0.30
    private const val W_COMMUNITY = 0.25

    fun score(
        visualAppeal: Int,
        shootability: Int,
        communityStars: Double, // 0..5, 0 = no ratings yet
    ): ScenicScore {
        val ai = visualAppeal.coerceIn(0, 100)
        val shoot = shootability.coerceIn(0, 100)
        val community = starsToHundred(communityStars)

        val overall = if (communityStars <= 0.0) {
            // No community data yet: blend AI + shootability only, re-normalized.
            ((ai * W_AI + shoot * W_SHOOT) / (W_AI + W_SHOOT)).roundToInt()
        } else {
            (ai * W_AI + shoot * W_SHOOT + community * W_COMMUNITY).roundToInt()
        }.coerceIn(0, 100)

        return ScenicScore(
            overall = overall,
            visualAppeal = ai,
            shootability = shoot,
            community = community,
            label = labelFor(overall),
        )
    }

    /** Convenience: score a Spot directly from its stored fields. */
    fun scoreSpot(spot: Spot, shootability: Int = 70): ScenicScore =
        score(spot.aiScore, shootability, spot.communityRating)

    /**
     * Estimate shootability from a review's free text. Looks for signals like
     * "nobody bothered us" (+), "crowded" (-), "parking" (+), "kicked out" (-).
     */
    fun shootabilityFromNotes(notes: String): Int {
        val t = notes.lowercase()
        var s = 60 // neutral baseline
        val positive = listOf(
            "nobody bothered", "no one bothered", "empty", "quiet",
            "parking", "easy access", "friendly", "welcoming",
        )
        val negative = listOf(
            "crowded", "busy", "kicked out", "security", "permit",
            "no parking", "dangerous", "sketchy",
        )
        for (p in positive) if (t.contains(p)) s += 8
        for (n in negative) if (t.contains(n)) s -= 10
        return s.coerceIn(0, 100)
    }

    private fun starsToHundred(stars: Double): Int =
        (stars.coerceIn(0.0, 5.0) / 5.0 * 100).roundToInt()

    private fun labelFor(overall: Int): String = when {
        overall >= 85 -> "Must shoot"
        overall >= 70 -> "Strong pick"
        overall >= 55 -> "Worth a look"
        overall >= 40 -> "Backup option"
        else -> "Skip"
    }
}
