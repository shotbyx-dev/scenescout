package com.scenescout.app

import com.scenescout.app.data.PermitGuide
import com.scenescout.app.data.PermitLevel
import com.scenescout.app.data.SunTimes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class SunTimesTest {

    @Test
    fun `miami equinox sunrise near 7am sunset near 7pm`() {
        val zone = ZoneId.of("America/New_York")
        // Near equinox; Miami sunrise ~7:05am EDT, sunset ~7:15pm EDT.
        val w = SunTimes.forDate(25.76, -80.19, LocalDate.of(2026, 3, 20), zone)
        assertTrue("sunrise ${w.sunriseMinutes}", w.sunriseMinutes in 400..450)
        assertTrue("sunset ${w.sunsetMinutes}", w.sunsetMinutes in 1120..1180)
        assertEquals(w.sunriseMinutes, w.morningGoldenStart)
        assertEquals(w.sunriseMinutes + 60, w.morningGoldenEnd)
        assertEquals(w.sunsetMinutes - 60, w.eveningGoldenStart)
        assertEquals(w.sunsetMinutes, w.eveningGoldenEnd)
    }

    @Test
    fun `golden hour windows are one hour`() {
        val w = SunTimes.forDate(
            25.76, -80.19, LocalDate.of(2026, 9, 25), ZoneId.of("America/New_York"))
        assertEquals(60, w.morningGoldenEnd - w.morningGoldenStart)
        assertEquals(60, w.eveningGoldenEnd - w.eveningGoldenStart)
        assertTrue(w.sunriseMinutes < w.sunsetMinutes)
    }

    @Test
    fun `format renders 12h clock`() {
        assertEquals("7:05 AM", SunTimes.format(425))
        assertEquals("7:15 PM", SunTimes.format(1155))
        assertEquals("12:00 PM", SunTimes.format(720))
    }

    @Test
    fun `format shows a dash when the sun event does not happen`() {
        // Polar day/night: sunEventMinutes returns -1, never "12:00 AM".
        assertEquals("—", SunTimes.format(-1))
    }
}

class PermitGuideTest {

    @Test
    fun `known cities resolve`() {
        assertEquals(PermitLevel.REQUIRED, PermitGuide.lookup("Los Angeles, CA").level)
        assertEquals(PermitLevel.SIMPLE, PermitGuide.lookup("Miami, FL").level)
    }

    @Test
    fun `unknown city returns unknown`() {
        val info = PermitGuide.lookup("Springfield, ZZ")
        assertEquals(PermitLevel.UNKNOWN, info.level)
        assertTrue(info.summary.isNotBlank())
    }
}
