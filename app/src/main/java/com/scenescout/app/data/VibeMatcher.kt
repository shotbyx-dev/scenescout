package com.scenescout.app.data

/**
 * Matches a free-text vibe ("gritty abandoned warehouse", "broken down truck")
 * against spots. Expands query words through a synonym map so the scout
 * understands videographer language, not just exact tags.
 *
 * Pure Kotlin — unit-testable.
 */
object VibeMatcher {

    private val synonyms: Map<String, List<String>> = mapOf(
        // Urban / gritty world
        "gritty" to listOf("gritty", "urban", "decay", "abandoned", "industrial",
            "rust", "concrete", "alley", "street", "raw"),
        "abandoned" to listOf("abandoned", "decay", "derelict", "empty",
            "forgotten", "ruins"),
        "urban" to listOf("urban", "city", "street", "downtown", "industrial",
            "concrete", "alley"),
        "industrial" to listOf("industrial", "warehouse", "factory", "freight",
            "trains", "yard"),
        // Roadside / props world
        "truck" to listOf("truck", "vehicle", "car", "roadside", "highway",
            "junkyard", "wreck"),
        "roadside" to listOf("roadside", "highway", "road", "truck", "diner",
            "motel", "billboard"),
        "props" to listOf("props", "objects", "textures", "details", "junk",
            "antique"),
        "desert" to listOf("desert", "dusty", "empty", "barren"),
        // Look / mood words
        "neon" to listOf("neon", "night", "signs", "lights", "glow"),
        "moody" to listOf("moody", "dark", "night", "fog", "shadows", "gritty"),
        "colorful" to listOf("colorful", "murals", "graffiti", "bright", "paint"),
        "scenic" to listOf("scenic", "ocean", "beach", "sunset", "nature",
            "park", "view"),
        "historic" to listOf("historic", "old", "vintage", "architecture",
            "antique"),
        "water" to listOf("water", "ocean", "bay", "river", "fountain",
            "waterfront", "beach"),
        "night" to listOf("night", "neon", "dark", "lights", "skyline"),
    )

    /**
     * All word variants that count as a hit for [word]: the word itself plus
     * its synonyms.
     */
    fun variantsFor(word: String): List<String> =
        listOf(word) + (synonyms[word].orEmpty())

    /**
     * Rank spots for a query. A query word scores a hit when the word itself
     * or any of its synonyms appears in the spot's searchable text.
     */
    fun rankForQuery(query: String, spots: List<Spot>): List<Pair<Spot, Int>> {
        val words = query.lowercase()
            .split(Regex("\\W+"))
            .filter { it.length > 2 }
            .toSet()
        if (words.isEmpty()) return emptyList()

        return spots.mapNotNull { spot ->
            val haystack = (spot.name + " " + spot.description + " " +
                spot.tags.joinToString(" ") + " " +
                spot.bestFor.joinToString(" ") { it.label }).lowercase()
            val hits = words.count { word ->
                VibeMatcher.variantsFor(word).any { haystack.contains(it) }
            }
            if (hits == 0) null
            else {
                val match = (hits.toDouble() / words.size * 100).toInt()
                    .coerceIn(1, 100)
                // Blend vibe match with the spot's overall scenic score.
                val blended =
                    (match * 0.6 + ScenicScorer.scoreSpot(spot).overall * 0.4).toInt()
                spot to blended
            }
        }.sortedByDescending { it.second }
    }
}
