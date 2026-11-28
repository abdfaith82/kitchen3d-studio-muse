package com.example.core.filament

import android.graphics.Bitmap
import com.example.core.math.Vector3
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * v1.17 — Chinese features, batch 3 (panorama + lighting + growth).
 *
 * Three pure, unit-testable pieces live here:
 *  1. [PanoramaEngine] — equirectangular ray directions for a 360° panorama
 *     render (same sun convention as [DirectionalSunLight.directionToLight]).
 *  2. [DaylightSimulation] — time-of-day → sun position/colour/intensity, so the
 *     render studio can offer a clock slider and named day scenarios instead
 *     of raw azimuth/elevation numbers.
 *  3. [FalseColorLuxMap] — a false-colour (pseudo-colour) lux map: the rendered
 *     bitmap's luminance is re-coloured blue → cyan → green → yellow → red,
 *     the way Kujiale's 伪色图 shows light distribution at a glance.
 */

/** Equirectangular (2:1) panorama directions. */
object PanoramaEngine {

    /** Ray direction for the pixel at normalized coordinates (u, v) in [0, 1]. */
    fun equirectDirection(u: Float, v: Float): Vector3 {
        val azimuthRad = (u.coerceIn(0f, 1f) * 2f * PI).toFloat()
        val polarRad = (v.coerceIn(0f, 1f) * PI).toFloat()
        val sinPolar = sin(polarRad)
        return Vector3(
            sinPolar * sin(azimuthRad),
            cos(polarRad),
            sinPolar * cos(azimuthRad)
        ).normalized()
    }

    /** Panorama render size: 2048 x 1024 (2:1 equirectangular). */
    const val PANORAMA_WIDTH = 2048
    const val PANORAMA_HEIGHT = 1024

    /** Eye-level camera position at the room centre, for the panorama render. */
    fun panoramaEye(roomWidthMm: Float, roomDepthMm: Float): Vector3 =
        Vector3(roomWidthMm * 0.5f, 1600f, roomDepthMm * 0.5f)
}

/** Time-of-day sun model: an hour of the day maps to a full sun setup. */
object DaylightSimulation {

    data class DaylightSettings(
        val azimuthDegrees: Float,
        val elevationDegrees: Float,
        val colorTemperatureKelvin: Float,
        val illuminanceLux: Float,
        val ledOn: Boolean,
        val labelAr: String
    )

    /** Named day scenarios shown as chips (hour in 24h clock). */
    val SCENARIOS: List<Pair<String, Float>> = listOf(
        "فجر" to 6.0f,
        "ظهر" to 12.0f,
        "غروب" to 17.5f,
        "ليل" to 21.0f
    )

    private const val DAWN_HOUR = 5.5f
    private const val DUSK_HOUR = 19.5f

    /** Sun setup for [hour] (0..24). Night hours return a dim, warm, LED-on setup. */
    fun sunForHour(hour: Float): DaylightSettings {
        val h = hour.coerceIn(0f, 24f)
        if (h < DAWN_HOUR || h > DUSK_HOUR) {
            return DaylightSettings(
                azimuthDegrees = 180f,
                elevationDegrees = 4f,
                colorTemperatureKelvin = 4200f,
                illuminanceLux = 900f,
                ledOn = true,
                labelAr = "ليل"
            )
        }
        // Day arc: t = 0 at dawn, 0.5 at noon, 1 at dusk.
        val t = (h - DAWN_HOUR) / (DUSK_HOUR - DAWN_HOUR)
        val arc = sin(t * PI).toFloat() // 0 at the ends, 1 at noon
        val azimuth = 70f + t * 220f // east (70°) → south (180°) → west (290°)
        val elevation = 2f + arc * 62f
        val kelvin = 2800f + arc * 3700f // golden at the ends, 6500K at noon
        val lux = 12_000f + arc * 85_000f
        val label = when {
            h < 8f -> "فجر"
            h < 11f -> "صباح"
            h < 14f -> "ظهر"
            h < 16.5f -> "عصر"
            else -> "غروب"
        }
        return DaylightSettings(
            azimuthDegrees = azimuth,
            elevationDegrees = elevation,
            colorTemperatureKelvin = kelvin,
            illuminanceLux = lux,
            ledOn = h < 7.5f || h > 17.5f,
            labelAr = label
        )
    }

    /** "06:30"-style label for the slider. */
    fun hourLabel(hour: Float): String {
        val totalMinutes = (hour.coerceIn(0f, 24f) * 60f).toInt()
        val hh = (totalMinutes / 60) % 24
        val mm = totalMinutes % 60
        return "%02d:%02d".format(hh, mm)
    }
}

/** False-colour lux map: luminance → blue/cyan/green/yellow/red. */
object FalseColorLuxMap {

    // Ramp stops: (t, r, g, b) — t in 0..1.
    private val STOPS = listOf(
        Quad(0.00f, 0, 0, 139), // dark blue — dimmest
        Quad(0.25f, 0, 191, 255), // cyan
        Quad(0.50f, 0, 200, 0), // green
        Quad(0.75f, 255, 215, 0), // yellow
        Quad(1.00f, 255, 0, 0) // red — brightest
    )

    private data class Quad(val t: Float, val r: Int, val g: Int, val b: Int)

    /** ARGB colour for a normalized luminance [t] in [0, 1]. Pure — unit-testable. */
    fun colorForLuminance(t: Float): Int {
        val x = t.coerceIn(0f, 1f)
        val hi = STOPS.indexOfFirst { it.t >= x }.coerceAtLeast(1)
        val lo = STOPS[hi - 1]
        val up = STOPS[hi]
        val f = if (up.t == lo.t) 0f else (x - lo.t) / (up.t - lo.t)
        val r = (lo.r + (up.r - lo.r) * f).toInt().coerceIn(0, 255)
        val g = (lo.g + (up.g - lo.g) * f).toInt().coerceIn(0, 255)
        val b = (lo.b + (up.b - lo.b) * f).toInt().coerceIn(0, 255)
        return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    }

    /**
     * Re-colours [source] by luminance. The source is first scaled down to
     * [maxWidth] — the map is about light distribution, not detail, and a
     * 4K bitmap would take seconds to recolour on a phone.
     */
    fun applyToBitmap(source: Bitmap, maxWidth: Int = 960): Bitmap {
        val scale = if (source.width > maxWidth) maxWidth.toFloat() / source.width else 1f
        val w = (source.width * scale).toInt().coerceAtLeast(1)
        val h = (source.height * scale).toInt().coerceAtLeast(1)
        val small = if (scale < 1f) Bitmap.createScaledBitmap(source, w, h, true) else source
        val pixels = IntArray(w * h)
        small.getPixels(pixels, 0, w, 0, 0, w, h)
        for (i in pixels.indices) {
            val p = pixels[i]
            val r = (p shr 16) and 0xFF
            val g = (p shr 8) and 0xFF
            val b = p and 0xFF
            val luminance = (0.2126f * r + 0.7152f * g + 0.0722f * b) / 255f
            pixels[i] = colorForLuminance(luminance)
        }
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        out.setPixels(pixels, 0, w, 0, 0, w, h)
        if (small !== source) small.recycle()
        return out
    }
}
