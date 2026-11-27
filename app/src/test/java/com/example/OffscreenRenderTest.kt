package com.example

import com.example.core.filament.OffscreenRenderer4K
import com.example.core.filament.RenderResolutionPreset
import com.example.core.math.Vector3
import com.example.domain.model.RoomLayoutShape
import com.example.domain.model.RoomStructure
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * v1.11 — the ray tracer went parallel (tile rows) with 2x2 jittered AA and
 * 2-tap soft sun shadows. Pins the new path end to end at FHD: a render
 * completes, returns a bitmap at the requested size, and reports progress.
 * Kept at FHD (the smallest preset) so the test stays fast on CI.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class OffscreenRenderTest {

    @Test
    fun `fhd render with AA on completes at 1920x1080 and reports progress`() = runBlocking {
        val room = RoomStructure(
            widthMm = 4200f,
            depthMm = 3600f,
            heightMm = 2700f,
            layoutShape = RoomLayoutShape.RECTANGULAR
        )
        var sawProgress = false
        var maxPercent = 0
        val result = OffscreenRenderer4K.renderScene(
            context = null,
            room = room,
            elements = emptyList(),
            cameraEye = Vector3(2100f, 1600f, 5200f),
            cameraTarget = Vector3(2100f, 900f, 1800f),
            fovDegrees = 45f,
            resolutionPreset = RenderResolutionPreset.FHD_1080P,
            enableAntiAliasing = true,
            onProgress = { prog ->
                sawProgress = true
                if (prog.percent > maxPercent) maxPercent = prog.percent
            }
        )
        assertNotNull(result.bitmap)
        assertEquals(1920, result.width)
        assertEquals(1080, result.height)
        assertEquals(1920, result.bitmap.width)
        assertEquals(1080, result.bitmap.height)
        assertTrue("expected progress callbacks", sawProgress)
        assertTrue("expected progress to reach the render phase", maxPercent >= 50)
        // The frame is not a single flat colour: floor, walls and sky differ.
        val pixels = IntArray(1920 * 1080)
        result.bitmap.getPixels(pixels, 0, 1920, 0, 0, 1920, 1080)
        assertTrue("rendered frame looks uniform", pixels.toSet().size > 100)
        // v1.12 (P0-3): the old photometric exposure multiplier crushed every
        // render to near-black (mean luminance ~0.01). A sunlit empty room
        // must land in a normal photographic range after ACES.
        var luminanceSum = 0.0
        var sampled = 0
        var i = 0
        while (i < pixels.size) {
            val px = pixels[i]
            val r = (px shr 16) and 0xFF
            val g = (px shr 8) and 0xFF
            val b = px and 0xFF
            luminanceSum += (0.2126 * r + 0.7152 * g + 0.0722 * b) / 255.0
            sampled++
            i += 16
        }
        val meanLuminance = luminanceSum / sampled
        assertTrue("rendered frame is near-black (mean luminance $meanLuminance)", meanLuminance > 0.15)
        assertTrue("rendered frame is blown out (mean luminance $meanLuminance)", meanLuminance < 0.95)
    }

    @Test
    fun `fhd render with AA off completes at the same size`() = runBlocking {
        val room = RoomStructure(
            widthMm = 4200f,
            depthMm = 3600f,
            heightMm = 2700f,
            layoutShape = RoomLayoutShape.RECTANGULAR
        )
        val result = OffscreenRenderer4K.renderScene(
            context = null,
            room = room,
            elements = emptyList(),
            cameraEye = Vector3(2100f, 1600f, 5200f),
            cameraTarget = Vector3(2100f, 900f, 1800f),
            fovDegrees = 45f,
            resolutionPreset = RenderResolutionPreset.FHD_1080P,
            enableAntiAliasing = false
        )
        assertEquals(1920, result.bitmap.width)
        assertEquals(1080, result.bitmap.height)
    }
}
