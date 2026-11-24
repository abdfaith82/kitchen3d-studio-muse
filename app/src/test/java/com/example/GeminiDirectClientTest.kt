package com.example

import com.example.core.ai.AiDesignUiState
import com.example.core.ai.GeminiDirectClient
import com.example.core.ai.GeminiKitchenDesignService
import com.example.domain.model.RoomLayoutShape
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * v1.7 — direct Gemini REST client (no Firebase). The service used to fail
 * every generation with "Default FirebaseApp is not initialized" because the
 * repo has no google-services.json. These tests pin the replacement:
 * response-text extraction, fence-tolerant JSON extraction, and the
 * no-key Arabic guidance that replaces the old raw English error.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class GeminiDirectClientTest {

    private val sampleRestBody = """
        {"candidates":[{"content":{"parts":[{"text":"{\"proposalTitleAr\":\"مطبخ\",\"units\":[]}"}],"role":"model"},"finishReason":"STOP"}]}
    """.trimIndent()

    @Test
    fun `extractResponseText reads candidates 0 content parts 0 text`() {
        assertEquals(
            "{\"proposalTitleAr\":\"مطبخ\",\"units\":[]}",
            GeminiDirectClient.extractResponseText(sampleRestBody)
        )
    }

    @Test
    fun `extractResponseText returns null for error body`() {
        assertNull(GeminiDirectClient.extractResponseText("""{"error":{"code":400,"message":"API key not valid"}}"""))
    }

    @Test
    fun `extractJsonObject strips markdown fences`() {
        val fenced = "```json\n{\"proposalTitleAr\":\"مطبخ\"}\n```"
        assertEquals("{\"proposalTitleAr\":\"مطبخ\"}", GeminiDirectClient.extractJsonObject(fenced))
    }

    @Test
    fun `extractJsonObject strips preamble text around the object`() {
        val withPreamble = "إليك التصميم:\n{\"proposalTitleAr\":\"مطبخ\"}\nتم بحمد الله."
        assertEquals("{\"proposalTitleAr\":\"مطبخ\"}", GeminiDirectClient.extractJsonObject(withPreamble))
    }

    @Test
    fun `extractJsonObject returns null when there is no object`() {
        assertNull(GeminiDirectClient.extractJsonObject("لا يوجد JSON هنا"))
    }

    @Test
    fun `model fallback chain has at least two models and no retired gemini-2-0-flash`() {
        val chain = GeminiDirectClient.MODEL_FALLBACKS
        assertTrue(chain.size >= 2)
        assertTrue(chain.none { it == "gemini-2.0-flash" })
    }

    @Test
    fun `blank api key returns Arabic guidance without touching the network`() = runBlocking {
        val service = GeminiKitchenDesignService()
        val result = service.generateKitchenDesign(
            roomWidthMm = 3800f,
            roomDepthMm = 3400f,
            roomHeightMm = 2600f,
            layoutShape = RoomLayoutShape.RECTANGLE,
            stylePreference = com.example.core.ai.KitchenAiStylePreference.ROYAL_OAK_AND_CHARCOAL,
            includeIsland = false,
            customNotes = "",
            apiKey = ""
        )
        assertTrue(result is AiDesignUiState.Error)
        val message = (result as AiDesignUiState.Error).message
        assertTrue(message.contains("aistudio.google.com"))
        assertTrue(message.contains("Gemini"))
        // The old raw English Firebase error must never reach the user again.
        assertTrue(!message.contains("FirebaseApp"))
    }
}
