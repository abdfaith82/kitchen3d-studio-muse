package com.example

import com.example.core.ai.KitchenAiStylePreference
import com.example.core.media.KitchenVisualPrompt
import com.example.core.media.MagicHourClient
import com.example.core.media.PollinationsImageClient
import com.example.domain.model.RoomLayoutShape
import com.example.domain.model.RoomStructure
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * v1.9 — Kitchen Media chain (text → image → video). Pins the pure parts:
 * the English prompt built from the room + style, the keyless Pollinations
 * image URL, and the Magic Hour response parsers (upload-urls + status).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class KitchenMediaTest {

    private fun room() = RoomStructure(
        widthMm = 4200f,
        depthMm = 3600f,
        heightMm = 2700f,
        layoutShape = RoomLayoutShape.RECTANGULAR
    )

    @Test
    fun `prompt carries room dimensions in meters and the style guidance`() {
        val prompt = KitchenVisualPrompt.buildKitchenImagePrompt(
            room = room(),
            style = KitchenAiStylePreference.ROYAL_OAK_AND_CHARCOAL,
            includeIsland = false
        )
        assertTrue(prompt.contains("4.2m wide x 3.6m deep"))
        assertTrue(prompt.contains("2.7m ceiling"))
        assertTrue(prompt.contains("rectangular layout"))
        assertTrue(prompt.contains("Natural Oak"))
        assertTrue(prompt.contains("No island"))
        assertTrue(prompt.contains("Photorealistic"))
    }

    @Test
    fun `prompt mentions the island and appends custom notes when given`() {
        val prompt = KitchenVisualPrompt.buildKitchenImagePrompt(
            room = room(),
            style = KitchenAiStylePreference.MODERN_MINIMALIST_WHITE,
            includeIsland = true,
            customNotes = "Add a window above the sink."
        )
        assertTrue(prompt.contains("island with breakfast bar"))
        assertTrue(prompt.contains("Add a window above the sink."))
        assertTrue(!prompt.contains("No island"))
    }

    @Test
    fun `pollinations image url is keyless legacy with flux and 16x9 size`() {
        val url = PollinationsImageClient.buildImageUrl(
            prompt = "luxury kitchen",
            width = 1024,
            height = 576,
            seed = 42
        )
        assertTrue(url.startsWith("https://image.pollinations.ai/prompt/"))
        assertTrue(url.contains("model=flux"))
        assertTrue(url.contains("width=1024"))
        assertTrue(url.contains("height=576"))
        assertTrue(url.contains("seed=42"))
        assertTrue(url.contains("luxury%20kitchen"))
    }

    @Test
    fun `magic hour upload urls parser returns upload url and file path`() {
        val raw = """{"items":[{"upload_url":"https://signed.example/upload","file_path":"uploads/abc.jpg"}]}"""
        val (uploadUrl, filePath) = MagicHourClient.parseUploadUrlsResponse(raw)
        assertEquals("https://signed.example/upload", uploadUrl)
        assertEquals("uploads/abc.jpg", filePath)
    }

    @Test
    fun `magic hour status parser returns complete with the download url`() {
        val raw = """{"status":"complete","downloads":[{"url":"https://cdn.example/clip.mp4"}]}"""
        val (status, url) = MagicHourClient.parseProjectStatus(raw)
        assertEquals("complete", status)
        assertEquals("https://cdn.example/clip.mp4", url)
    }

    @Test
    fun `magic hour status parser returns null url while rendering`() {
        val raw = """{"status":"rendering","downloads":[]}"""
        val (status, url) = MagicHourClient.parseProjectStatus(raw)
        assertEquals("rendering", status)
        assertEquals(null, url)
    }

    @Test
    fun `magic hour free models are the verified 480p trio`() {
        assertEquals(listOf("wan-2.2", "ltx-2.5", "minimax-h3"), MagicHourClient.FREE_MODELS)
    }
}
