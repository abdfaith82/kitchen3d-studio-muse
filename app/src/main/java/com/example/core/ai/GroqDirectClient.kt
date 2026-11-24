package com.example.core.ai

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

/**
 * Direct Groq REST client v1.8 — second provider next to Gemini.
 *
 * Groq's API is OpenAI-compatible: POST
 * `https://api.groq.com/openai/v1/chat/completions` with a Bearer key,
 * `response_format: {"type": "json_object"}` and the same kitchen prompt the
 * Gemini path uses. Free plan, no card (verified Nov 2026 from Groq's own
 * rate-limits page): 30 requests/min, 1,000 requests/day, 8,000 tokens/min,
 * 200,000 tokens/day per chat model. One generation costs ~3–4K tokens, so
 * the token cap binds first at roughly 50 designs/day — still far more than
 * personal use, and a second quota to fall back to when Gemini's runs out.
 *
 * Error mapping differs from Gemini in one place: Groq returns 401 for a bad
 * key (400 means a malformed request, e.g. a model that rejects JSON mode),
 * so 400 maps to SERVER_ERROR here, not INVALID_KEY.
 */
object GroqDirectClient {
    private const val ENDPOINT = "https://api.groq.com/openai/v1/chat/completions"

    /** Tried in order; a 404 on one model falls through to the next. */
    val MODEL_FALLBACKS = listOf(
        "openai/gpt-oss-120b",
        "openai/gpt-oss-20b",
        "qwen/qwen3.8-27b"
    )

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    suspend fun generateJson(prompt: String, apiKey: String): AiDirectResult =
        withContext(Dispatchers.IO) {
            val requestBody = JSONObject().apply {
                put("model", MODEL_FALLBACKS.first())
                put(
                    "messages",
                    JSONArray().put(
                        JSONObject().apply {
                            put("role", "system")
                            put(
                                "content",
                                "You are a CAD Kitchen Architect. Reply with one valid JSON object only, no markdown, no extra text."
                            )
                        }
                    ).put(
                        JSONObject().apply {
                            put("role", "user")
                            put("content", prompt)
                        }
                    )
                )
                put("temperature", 0.7)
                put(
                    "response_format",
                    JSONObject().put("type", "json_object")
                )
            }.toString()

            for (model in MODEL_FALLBACKS) {
                val bodyWithModel = JSONObject(requestBody).put("model", model).toString()
                val request = Request.Builder()
                    .url(ENDPOINT)
                    .header("Authorization", "Bearer $apiKey")
                    .post(bodyWithModel.toRequestBody("application/json".toMediaType()))
                    .build()
                try {
                    httpClient.newCall(request).execute().use { response ->
                        val bodyText = response.body?.string().orEmpty()
                        when {
                            response.isSuccessful -> {
                                val content = extractResponseText(bodyText)
                                    ?.let(GeminiDirectClient::extractJsonObject)
                                return@withContext if (content.isNullOrBlank()) {
                                    AiDirectResult.Failure(AiDirectFailure.EMPTY_RESPONSE)
                                } else {
                                    AiDirectResult.Success(content)
                                }
                            }
                            response.code == 404 -> {
                                // Model retired/renamed — try the next one.
                            }
                            response.code == 401 || response.code == 403 ->
                                return@withContext AiDirectResult.Failure(
                                    AiDirectFailure.INVALID_KEY,
                                    response.code.toString()
                                )
                            response.code == 429 ->
                                return@withContext AiDirectResult.Failure(
                                    AiDirectFailure.QUOTA_EXCEEDED
                                )
                            else ->
                                return@withContext AiDirectResult.Failure(
                                    AiDirectFailure.SERVER_ERROR,
                                    response.code.toString()
                                )
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: SocketTimeoutException) {
                    return@withContext AiDirectResult.Failure(AiDirectFailure.TIMEOUT)
                } catch (_: IOException) {
                    return@withContext AiDirectResult.Failure(AiDirectFailure.NETWORK)
                }
            }
            AiDirectResult.Failure(AiDirectFailure.MODEL_UNAVAILABLE)
        }

    /** OpenAI shape: `choices[0].message.content` — null when unexpected. */
    fun extractResponseText(responseBody: String): String? {
        return try {
            val root = JSONObject(responseBody)
            val choices = root.optJSONArray("choices") ?: return null
            val first = choices.optJSONObject(0) ?: return null
            val message = first.optJSONObject("message") ?: return null
            message.optString("content")?.takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        }
    }
}
