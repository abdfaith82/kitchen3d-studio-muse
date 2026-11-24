package com.example

import com.example.core.ai.AiDesignUiState
import com.example.core.ai.GeminiDirectClient
import com.example.core.ai.AiProvider
import com.example.core.ai.GeminiKitchenDesignService
import com.example.core.ai.GroqDirectClient
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
 * v1.7/v1.8 — direct REST clients (Gemini + Groq, no Firebase). The service
 * used to fail every generation with "Default FirebaseApp is not
 * initialized" because the repo has no google-services.json. These tests pin
 * the replacement: response-text extraction (both API shapes), fence-tolerant
 * JSON extraction, the model fallback chains, and the no-key Arabic guidance
 * that replaces the old raw English error.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AiDirectClientTest {

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
    fun `groq extractResponseText reads choices 0 message content`() {
        val groqBody = """
            {"choices":[{"message":{"role":"assistant","content":"{\"proposalTitleAr\":\"مطبخ\"}"},"finish_reason":"stop"}],"usage":{"total_tokens":1234}}
        """.trimIndent()
        assertEquals(
            "{\"proposalTitleAr\":\"مطبخ\"}",
            GroqDirectClient.extractResponseText(groqBody)
        )
    }

    @Test
    fun `groq extractResponseText returns null for error body`() {
        assertNull(
            GroqDirectClient.extractResponseText(
                """{"error":{"message":"Incorrect API key provided","type":"invalid_request_error","code":401}}"""
            )
        )
    }

    @Test
    fun `groq model fallback chain starts with gpt-oss-120b and has no enterprise llama`() {
        val chain = GroqDirectClient.MODEL_FALLBACKS
        assertEquals("openai/gpt-oss-120b", chain.first())
        assertTrue(chain.size >= 2)
        assertTrue(chain.none { it.contains("llama-3.3-70b") })
    }

    @Test
    fun `blank groq key returns Arabic guidance naming groq console without network`() = runBlocking {
        val service = GeminiKitchenDesignService()
        val result = service.generateKitchenDesign(
            roomWidthMm = 3800f,
            roomDepthMm = 3400f,
            roomHeightMm = 2600f,
            layoutShape = RoomLayoutShape.RECTANGLE,
            stylePreference = com.example.core.ai.KitchenAiStylePreference.ROYAL_OAK_AND_CHARCOAL,
            includeIsland = false,
            customNotes = "",
            apiKey = "",
            provider = AiProvider.GROQ
        )
        assertTrue(result is AiDesignUiState.Error)
        val message = (result as AiDesignUiState.Error).message
        assertTrue(message.contains("console.groq.com"))
        assertTrue(message.contains("Groq"))
        assertTrue(!message.contains("FirebaseApp"))
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
