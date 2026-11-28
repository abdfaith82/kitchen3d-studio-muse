package com.example

import com.example.core.filament.DaylightSimulation
import com.example.core.filament.FalseColorLuxMap
import com.example.core.filament.PanoramaEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class PanoramaEngineTest {

    private fun assertDir(u: Float, v: Float, x: Float, y: Float, z: Float) {
        val d = PanoramaEngine.equirectDirection(u, v)
        assertEquals(x, d.x, 0.01f)
        assertEquals(y, d.y, 0.01f)
        assertEquals(z, d.z, 0.01f)
        val len = kotlin.math.sqrt(d.x * d.x + d.y * d.y + d.z * d.z)
        assertEquals(1f, len, 0.01f)
    }

    @Test
    fun `equirect horizon points around the compass`() {
        assertDir(0.00f, 0.5f, 0f, 0f, 1f) // azimuth 0 -> +Z
        assertDir(0.25f, 0.5f, 1f, 0f, 0f) // azimuth 90 -> +X
        assertDir(0.50f, 0.5f, 0f, 0f, -1f) // azimuth 180 -> -Z
        assertDir(0.75f, 0.5f, -1f, 0f, 0f) // azimuth 270 -> -X
    }

    @Test
    fun `equirect poles are straight up and down`() {
        assertDir(0.3f, 0.0f, 0f, 1f, 0f)
        assertDir(0.7f, 1.0f, 0f, -1f, 0f)
    }

    @Test
    fun `noon sun is high white and bright with LED off`() {
        val noon = DaylightSimulation.sunForHour(12f)
        assertTrue(noon.elevationDegrees > 55f)
        assertTrue(noon.colorTemperatureKelvin > 6000f)
        assertTrue(noon.illuminanceLux > 80_000f)
        assertTrue(!noon.ledOn)
        assertEquals("ظهر", noon.labelAr)
        assertTrue(abs(noon.azimuthDegrees - 180f) < 12f)
    }

    @Test
    fun `dawn sun is low and golden with LED on`() {
        val dawn = DaylightSimulation.sunForHour(6f)
        assertTrue(dawn.elevationDegrees < 15f)
        assertTrue(dawn.colorTemperatureKelvin < 3500f)
        assertTrue(dawn.ledOn)
        assertEquals("فجر", dawn.labelAr)
    }

    @Test
    fun `night is dim warm and LED on`() {
        val night = DaylightSimulation.sunForHour(21f)
        assertEquals(900f, night.illuminanceLux, 0.01f)
        assertTrue(night.ledOn)
        assertEquals("ليل", night.labelAr)
    }

    @Test
    fun `sunset is in the west`() {
        val sunset = DaylightSimulation.sunForHour(17.5f)
        assertTrue(sunset.azimuthDegrees > 240f)
        assertEquals("غروب", sunset.labelAr)
    }

    @Test
    fun `hour label formats HH MM`() {
        assertEquals("06:30", DaylightSimulation.hourLabel(6.5f))
        assertEquals("12:00", DaylightSimulation.hourLabel(12f))
        assertEquals("21:00", DaylightSimulation.hourLabel(21f))
    }

    @Test
    fun `false colour ramp endpoints`() {
        assertEquals(0xFF00008B.toInt(), FalseColorLuxMap.colorForLuminance(0f))
        assertEquals(0xFFFF0000.toInt(), FalseColorLuxMap.colorForLuminance(1f))
        assertEquals(0xFF00C800.toInt(), FalseColorLuxMap.colorForLuminance(0.5f))
    }

    @Test
    fun `false colour ramp is opaque and monotonic in red at the bright end`() {
        val mid = FalseColorLuxMap.colorForLuminance(0.75f)
        assertEquals(0xFF, (mid shr 24) and 0xFF)
        assertEquals(255, (mid shr 16) and 0xFF) // yellow: full red
        val bright = FalseColorLuxMap.colorForLuminance(0.9f)
        assertTrue(((bright shr 8) and 0xFF) < 215) // red takes over, green falls
    }
}
