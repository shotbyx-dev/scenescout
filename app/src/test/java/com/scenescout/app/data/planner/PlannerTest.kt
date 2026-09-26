package com.scenescout.app.data.planner

import com.scenescout.app.data.Spot
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.*
import org.junit.Test

class PlannerTest {

    private fun spot(name: String, lat: Double = 26.1, lng: Double = -80.1) =
        Spot(id = name, name = name, latitude = lat, longitude = lng, description = "d")

    @Test
    fun `project json round trip preserves everything`() {
        val p = PlannerLogic.newProject("Neon Nights", "Client X", "2026-10-01").copy(
            treatment = "Moody neon story",
            shots = listOf(ShotItem(description = "Wide alley dolly",
                locationName = "FATVillage", lens = "35mm")),
            schedule = listOf(ScheduleBlock(time = "06:00", title = "Call time")),
            gear = listOf(GearItem(name = "Camera", packed = true)),
        )
        val restored = ShootProject.listFromJson(ShootProject.listToJson(listOf(p))).single()
        assertEquals(p.title, restored.title)
        assertEquals(p.client, restored.client)
        assertEquals(p.shootDate, restored.shootDate)
        assertEquals(p.treatment, restored.treatment)
        assertEquals(p.shots.single().description, restored.shots.single().description)
        assertEquals(p.schedule.single().time, restored.schedule.single().time)
        assertEquals(p.gear.single().name, restored.gear.single().name)
        assertTrue(restored.gear.single().packed)
    }

    @Test
    fun `new project ships with the gear presets unpacked`() {
        val p = PlannerLogic.newProject("T", "", "2026-10-01")
        assertEquals(PlannerLogic.GEAR_PRESETS.size, p.gear.size)
        assertTrue(p.gear.none { it.packed })
        assertTrue(p.gear.any { it.name == "Drone + 3 batteries" })
    }

    @Test
    fun `day schedule is sorted and covers call, golden hours, lunch, wrap`() {
        val a = spot("Beach Spot")
        val b = spot("Alley Spot", lat = 26.2, lng = -80.2)
        val project = PlannerLogic.newProject("T", "", "2026-10-01").copy(
            shots = listOf(
                ShotItem(description = "Sunrise wide", locationName = "Beach Spot"),
                ShotItem(description = "Night neon", locationName = "Alley Spot"),
            ),
        )
        val blocks = PlannerLogic.buildDaySchedule(
            project, mapOf("Beach Spot" to a, "Alley Spot" to b),
            LocalDate.of(2026, 10, 1), ZoneId.of("America/New_York"),
        )
        assertTrue(blocks.size >= 6)
        assertEquals(blocks.sortedBy { it.time }, blocks) // sorted
        assertEquals("06:00", blocks.first().time)
        assertTrue(blocks.first().title.contains("Call time"))
        assertTrue(blocks.any { it.title.contains("Beach Spot") })
        assertTrue(blocks.any { it.title.contains("Alley Spot") })
        assertTrue(blocks.any { it.title.contains("Lunch") })
        assertEquals("Wrap", blocks.last().title)
    }

    @Test
    fun `day schedule works with no matched locations`() {
        val project = PlannerLogic.newProject("T", "", "2026-10-01")
        val blocks = PlannerLogic.buildDaySchedule(
            project, emptyMap(), LocalDate.of(2026, 10, 1), ZoneId.of("America/New_York"),
        )
        assertEquals(listOf("06:00", "12:30", "20:00"), blocks.map { it.time })
    }

    @Test
    fun `corrupt planner json loads as empty rather than crashing`() {
        assertTrue(ShootProject.listFromJson("").isEmpty())
    }
}
