package com.scenescout.app.data.planner

import com.scenescout.app.data.Spot
import com.scenescout.app.data.SunTimes
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

/**
 * Pure shoot-day planning logic: project scaffolding, gear presets, and the
 * auto day-schedule builder (call time, golden-hour blocks per location,
 * lunch, wrap). Unit-testable — no Android APIs.
 */
object PlannerLogic {

    val GEAR_PRESETS = listOf(
        "Camera body", "24-70mm f/2.8", "70-200mm", "35mm prime",
        "Drone + 3 batteries", "Gimbal", "Tripod", "ND filters",
        "Field monitor", "Lav mics + recorder", "LED light kit",
        "Memory cards", "SSD / hard drive", "Location releases",
        "Gaffer tape & clamps",
    )

    fun newProject(title: String, client: String, shootDate: String): ShootProject =
        ShootProject(
            title = title.trim(),
            client = client.trim(),
            shootDate = shootDate.trim(),
            gear = GEAR_PRESETS.map { GearItem(name = it) },
        )

    /**
     * Builds a shoot-day schedule for [project] on [date]:
     * call time, morning + evening golden-hour blocks for each shot location
     * (via [SunTimes]), lunch, and wrap. Blocks sorted by time.
     */
    fun buildDaySchedule(
        project: ShootProject,
        spotsByName: Map<String, Spot>,
        date: LocalDate,
        zone: ZoneId,
    ): List<ScheduleBlock> {
        val blocks = mutableListOf<ScheduleBlock>()
        val locations = project.shots
            .mapNotNull { spotsByName[it.locationName] }
            .distinctBy { it.id }
        val firstLocation = locations.firstOrNull()?.name
            ?: project.shots.firstOrNull()?.locationName?.takeIf { it.isNotBlank() }
            ?: "base"

        blocks += ScheduleBlock(
            time = "06:00", title = "Call time",
            note = "Crew call @ $firstLocation — load in, block the shots",
        )
        locations.forEach { spot ->
            val sun = SunTimes.forDate(spot.latitude, spot.longitude, date, zone)
            blocks += ScheduleBlock(
                time = fmt(sun.morningGoldenStart),
                title = "Morning golden hour — ${spot.name}",
                note = "${fmt(sun.morningGoldenStart)}–${fmt(sun.morningGoldenEnd)} · " +
                    "Sunrise ${fmt(sun.sunriseMinutes)}",
            )
        }
        blocks += ScheduleBlock(
            time = "12:30", title = "Lunch / company move",
            note = "Charge batteries, dump cards",
        )
        locations.forEach { spot ->
            val sun = SunTimes.forDate(spot.latitude, spot.longitude, date, zone)
            blocks += ScheduleBlock(
                time = fmt(sun.eveningGoldenStart),
                title = "Evening golden hour — ${spot.name}",
                note = "${fmt(sun.eveningGoldenStart)}–${fmt(sun.eveningGoldenEnd)} · " +
                    "Sunset ${fmt(sun.sunsetMinutes)}",
            )
        }
        val wrapMinutes = locations.maxOfOrNull {
            SunTimes.forDate(it.latitude, it.longitude, date, zone).eveningGoldenEnd
        }?.let { if (it < 0) -1 else it + 30 } ?: (20 * 60)
        blocks += ScheduleBlock(
            time = fmt(wrapMinutes), title = "Wrap",
            note = "Gear check against the packing list",
        )
        return blocks.sortedBy { it.time }
    }

    private fun fmt(minutes: Int): String =
        if (minutes < 0) "—" else "%02d:%02d".format(minutes / 60, minutes % 60)
}
