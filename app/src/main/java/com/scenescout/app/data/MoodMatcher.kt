package com.scenescout.app.data

/**
 * Matches a song to shoot locations. The user pastes lyrics or describes the
 * song's feeling ("lonely night drive, neon, heartbreak"); the matcher maps
 * emotional/theme words onto location vibes, then ranks spots and explains
 * each pick with the words that matched.
 *
 * Pure Kotlin — unit-testable.
 */
object MoodMatcher {

    data class SongMatch(
        val spot: Spot,
        val score: Int,
        val reasons: List<String>,
    )

    /** Emotion/theme word -> location vibes it suggests. */
    private val moodMap: Map<String, List<String>> = mapOf(
        // Feelings
        "lonely" to listOf("abandoned", "empty", "night", "desert"),
        "loneliness" to listOf("abandoned", "empty", "night"),
        "heartbreak" to listOf("rain", "moody", "night", "empty"),
        "heartbroken" to listOf("rain", "moody", "night", "empty"),
        "sad" to listOf("moody", "rain", "empty", "grey"),
        "melancholy" to listOf("moody", "empty", "vintage"),
        "love" to listOf("sunset", "beach", "romantic", "golden"),
        "romantic" to listOf("sunset", "historic", "water", "golden"),
        "romance" to listOf("sunset", "historic", "water"),
        "happy" to listOf("colorful", "bright", "beach", "party"),
        "party" to listOf("neon", "night", "colorful"),
        "celebration" to listOf("neon", "colorful", "bright"),
        "angry" to listOf("industrial", "concrete", "gritty"),
        "rage" to listOf("industrial", "gritty", "dark"),
        "dark" to listOf("night", "moody", "abandoned"),
        "darkness" to listOf("night", "moody", "abandoned"),
        "dreamy" to listOf("water", "sunset", "soft", "hazy"),
        "dream" to listOf("water", "sunset", "soft"),
        "nostalgia" to listOf("vintage", "historic", "roadside", "retro"),
        "nostalgic" to listOf("vintage", "historic", "roadside"),
        "wild" to listOf("desert", "empty", "road", "free"),
        "free" to listOf("road", "desert", "ocean", "open"),
        "freedom" to listOf("road", "desert", "ocean"),
        "dangerous" to listOf("gritty", "industrial", "night"),
        "danger" to listOf("gritty", "industrial", "night"),
        "rich" to listOf("luxury", "historic", "architecture", "gold"),
        "luxury" to listOf("historic", "architecture", "marble"),
        "luxurious" to listOf("historic", "architecture", "gold"),
        "fame" to listOf("neon", "city", "night", "billboard"),
        "famous" to listOf("neon", "city", "billboard"),
        "hustle" to listOf("urban", "street", "gritty", "city"),
        "grind" to listOf("urban", "industrial", "gritty"),
        "street" to listOf("urban", "gritty"),
        "streets" to listOf("urban", "gritty", "neon"),
        "city" to listOf("urban", "skyline", "night"),
        "summer" to listOf("beach", "ocean", "bright", "palm"),
        "night" to listOf("neon", "dark", "lights"),
        "drive" to listOf("road", "highway", "neon"),
        "driving" to listOf("road", "highway", "neon"),
        "road" to listOf("highway", "roadside", "desert"),
        "highway" to listOf("road", "roadside", "neon"),
        "ocean" to listOf("beach", "water", "waves"),
        "beach" to listOf("ocean", "sand", "palm"),
        "rain" to listOf("moody", "wet", "neon"),
        "money" to listOf("luxury", "city", "neon"),
        "power" to listOf("skyline", "architecture", "city"),
        "rebel" to listOf("gritty", "abandoned", "graffiti"),
        "rebellion" to listOf("gritty", "abandoned", "graffiti"),
        "ghost" to listOf("abandoned", "empty", "fog"),
        "haunted" to listOf("abandoned", "dark", "fog"),
        "paradise" to listOf("beach", "palm", "bright"),
        "tropical" to listOf("beach", "palm", "colorful"),
        "ghetto" to listOf("urban", "gritty", "street"),
        "trap" to listOf("urban", "night", "neon"),
    )

    private val stopwords = setOf(
        "the", "and", "for", "with", "that", "this", "from", "have", "has",
        "had", "was", "were", "are", "is", "it", "its", "you", "your", "yours",
        "she", "her", "hers", "him", "his", "they", "them", "their", "theirs",
        "but", "not", "all", "can", "just", "like", "into", "out", "about",
        "when", "what", "where", "who", "how", "why", "will", "would", "could",
        "should", "been", "being", "over", "under", "again", "once", "here",
        "there", "then", "than", "too", "very", "got", "get", "gotta",
        "wanna", "gonna", "yeah", "oh", "uh", "hey", "chorus", "verse",
        "bridge", "hook", "intro", "outro", "refrain",
    )

    /**
     * Pull (theme word, vibe words) signals out of free text. A word becomes
     * a signal when it is in the mood map or when it is a usable vibe word
     * itself.
     */
    fun signalsFor(text: String): List<Pair<String, List<String>>> {
        return text.lowercase()
            .split(Regex("\\W+"))
            .filter { it.length > 2 && it !in stopwords }
            .distinct()
            .mapNotNull { word ->
                val vibes = moodMap[word]
                when {
                    vibes != null -> word to vibes
                    // Unknown words still work as plain vibe keywords.
                    else -> word to listOf(word)
                }
            }
    }

    /**
     * Rank spots for the song text. A signal matches a spot when any of its
     * vibe words (or their VibeMatcher synonyms) appear in the spot's text.
     */
    fun rankForSong(text: String, spots: List<Spot>): List<SongMatch> {
        val signals = signalsFor(text)
        if (signals.isEmpty()) return emptyList()

        return spots.mapNotNull { spot ->
            val haystack = (spot.name + " " + spot.description + " " +
                spot.tags.joinToString(" ") + " " +
                spot.bestFor.joinToString(" ") { it.label }).lowercase()
            val matched = signals.filter { (_, vibes) ->
                vibes.any { vibe ->
                    VibeMatcher.variantsFor(vibe).any { haystack.contains(it) }
                }
            }.map { it.first }.distinct()
            if (matched.isEmpty()) {
                null
            } else {
                val raw = (matched.size.toDouble() / signals.size * 100)
                    .toInt().coerceIn(1, 100)
                val blended =
                    (raw * 0.6 + ScenicScorer.scoreSpot(spot).overall * 0.4).toInt()
                SongMatch(spot, blended, matched.take(4))
            }
        }.sortedByDescending { it.score }
    }
}
