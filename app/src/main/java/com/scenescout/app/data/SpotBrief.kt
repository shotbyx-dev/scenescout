package com.scenescout.app.data

import com.scenescout.app.data.SunTimes.Windows

/**
 * The content model for the client one-pager: everything a client or artist
 * needs to understand the scene at a glance. Pure Kotlin — the PDF renderer
 * and the share-text builder both consume this.
 */
data class BriefReview(
    val author: String,
    val stars: Int,
    val text: String,
    val bestTime: String,
)

data class SpotBrief(
    val title: String,
    val scoreLine: String,
    val coordinates: String,
    val mapsLink: String,
    val description: String,
    val tags: List<String>,
    val bestFor: List<String>,
    val goldenHour: String,
    val sunriseSunset: String,
    val permitLevel: String,
    val permitSummary: String,
    val reviews: List<BriefReview>,
    val footer: String = "Shared from SceneScout — Created by Shotbyx",
)

object SpotBriefBuilder {
    fun build(
        spot: Spot,
        score: ScenicScore,
        sun: Windows,
        reviews: List<SpotReview>,
    ): SpotBrief {
        val coord = "%.4f, %.4f".format(spot.latitude, spot.longitude)
        return SpotBrief(
            title = spot.name,
            scoreLine = "${score.overall}/100 · ${score.label}",
            coordinates = coord,
            mapsLink = "https://www.google.com/maps/search/?api=1&query=" +
                "${spot.latitude},${spot.longitude}",
            description = spot.description,
            tags = spot.tags,
            bestFor = spot.bestFor.map { it.label },
            goldenHour = "${SunTimes.format(sun.morningGoldenStart)}–" +
                "${SunTimes.format(sun.morningGoldenEnd)} / " +
                "${SunTimes.format(sun.eveningGoldenStart)}–" +
                SunTimes.format(sun.eveningGoldenEnd),
            sunriseSunset = "Sunrise ${SunTimes.format(sun.sunriseMinutes)} · " +
                "Sunset ${SunTimes.format(sun.sunsetMinutes)}",
            permitLevel = spot.permit.level.label,
            permitSummary = spot.permit.summary,
            reviews = reviews.take(3).map {
                BriefReview(it.author, it.stars, it.text, it.bestTime)
            },
        )
    }
}
