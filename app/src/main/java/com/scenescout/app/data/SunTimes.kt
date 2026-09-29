package com.scenescout.app.data

import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.*

/**
 * Approximate sunrise/sunset + golden-hour windows for a lat/lng and date.
 * Pure Kotlin, no Android APIs — unit-testable. Accuracy is within ~10 minutes,
 * good enough for shoot planning ("the sun is perfect at this time").
 */
object SunTimes {

    data class Windows(
        val sunriseMinutes: Int,   // minutes after local midnight
        val sunsetMinutes: Int,
        val morningGoldenStart: Int,
        val morningGoldenEnd: Int,
        val eveningGoldenStart: Int,
        val eveningGoldenEnd: Int,
    )

    fun forDate(latitude: Double, longitude: Double, date: LocalDate, zone: ZoneId): Windows {
        val sunrise = sunEventMinutes(latitude, longitude, date, zone, rising = true)
        val sunset = sunEventMinutes(latitude, longitude, date, zone, rising = false)
        // -1 means the event doesn't happen that day (polar day/night):
        // propagate the sentinel instead of doing arithmetic on it.
        return Windows(
            sunriseMinutes = sunrise,
            sunsetMinutes = sunset,
            morningGoldenStart = sunrise,
            morningGoldenEnd = if (sunrise < 0) -1 else sunrise + 60,
            eveningGoldenStart = if (sunset < 0) -1 else sunset - 60,
            eveningGoldenEnd = sunset,
        )
    }

    /**
     * Formats minutes-after-midnight as "6:24 AM". Negative input means the
     * event doesn't happen that day (polar day/night) — shown as "—".
     */
    fun format(minutes: Int): String {
        if (minutes < 0) return "—"
        val h = (minutes / 60).coerceIn(0, 23)
        val m = (minutes % 60).coerceIn(0, 59)
        val ampm = if (h < 12) "AM" else "PM"
        val h12 = if (h % 12 == 0) 12 else h % 12
        return "%d:%02d %s".format(h12, m, ampm)
    }

    /**
     * NOAA-style solar calculation. Returns minutes after local midnight,
     * or -1 when the sun never rises/sets that day at that latitude.
     */
    private fun sunEventMinutes(
        lat: Double, lng: Double, date: LocalDate, zone: ZoneId, rising: Boolean,
    ): Int {
        val n = date.dayOfYear
        val lngHour = lng / 15.0
        val t = n + ((if (rising) 6.0 else 18.0) - lngHour) / 24.0
        val m = 0.9856 * t - 3.289
        var l = m + 1.916 * sin(Math.toRadians(m)) + 0.020 * sin(Math.toRadians(2 * m)) + 282.634
        l = ((l % 360) + 360) % 360
        var ra = Math.toDegrees(atan(0.91764 * tan(Math.toRadians(l))))
        ra = ((ra % 360) + 360) % 360
        val lQuadrant = floor(l / 90) * 90
        val raQuadrant = floor(ra / 90) * 90
        ra += lQuadrant - raQuadrant
        ra /= 15.0
        val sinDec = 0.39782 * sin(Math.toRadians(l))
        val cosDec = cos(asin(sinDec))
        val zenith = Math.toRadians(90.833)
        val cosH = (cos(zenith) - sinDec * sin(Math.toRadians(lat))) /
            (cosDec * cos(Math.toRadians(lat)))
        if (cosH > 1 || cosH < -1) return -1
        var h = if (rising) 360 - Math.toDegrees(acos(cosH)) else Math.toDegrees(acos(cosH))
        h /= 15.0
        val tLocal = h + ra - 0.06571 * t - 6.622
        var ut = tLocal - lngHour
        ut = ((ut % 24) + 24) % 24
        // Convert UTC to local zone at solar noon-ish of that date.
        val offsetHours = zone.rules
            .getOffset(date.atTime(12, 0).atZone(zone).toInstant())
            .totalSeconds / 3600.0
        val local = ((ut + offsetHours) % 24 + 24) % 24
        return (local * 60).roundToInt()
    }
}
