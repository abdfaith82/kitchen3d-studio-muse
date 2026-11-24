package com.example.core.ai

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

/**
 * Direct Gemini REST client v1.7 — replaces Firebase AI Logic.
 *
 * The old path (`Firebase.ai.generativeModel("gemini-3.8-flash")`) could never
 * work in this app: the repo has no google-services.json by design (see
 * .env.example), so no FirebaseApp ever initializes and every generation
 * failed with "Default FirebaseApp is not initialized". This client calls
 * `generativelanguage.googleapis.com` directly with the user's free API key
 * from Google AI Studio (aistudio.google.com → Get API key), no Firebase
 * project, no account, no card.
 *
 * Model ids churn fast (gemini-2.0-flash already returns 404 as of mid-2026),
 * so generation tries a fallback chain and only treats 404 as "try the next
 * model". Snapshot of the chain, Nov 2026: 2.5-flash, 3.5-flash, 3.8-flash.
 */
enum class AiDirectFailure {
    INVALID_KEY,
    QUOTA_EXCEEDED,
    MODEL_UNAVAILABLE,
    NETWORK,
    TIMEOUT,
    EMPTY_RESPONSE,
    SERVER_ERROR
}

sealed interface AiDirectResult {
    data class Success(val jsonText: String) : AiDirectResult
    data class Failure(val kind: AiDirectFailure, val detail: String? = null) : AiDirectResult
}

object GeminiDirectClient {
    private const val ENDPOINT_TEMPLATE =
        "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent"

    /** Tried in order; a 404 on one model falls through to the next. */
    val MODEL_FALLBACKS = listOf("gemini-2.5-flash", "gemini-3.5-flash", "gemini-3.8-flash")

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
                put(
                    "contents",
                    org.json.JSONArray().put(
                        JSONObject().put(
                            "parts",
                            org.json.JSONArray().put(JSONObject().put("text", prompt))
                        )
                    )
                )
                put(
                    "generationConfig",
                    JSONObject().apply {
                        put("responseMimeType", "application/json")
                        put("temperature", 0.7)
                    }
                )
            }.toString()

            for (model in MODEL_FALLBACKS) {
                val request = Request.Builder()
                    .url(ENDPOINT_TEMPLATE.format(model))
                    .header("x-goog-api-key", apiKey)
                    .post(requestBody.toRequestBody("application/json".toMediaType()))
                    .build()
                try {
                    httpClient.newCall(request).execute().use { response ->
                        val bodyText = response.body?.string().orEmpty()
                        when {
                            response.isSuccessful -> {
                                val content = extractResponseText(bodyText)
                                    ?.let(::extractJsonObject)
                                return@withContext if (content.isNullOrBlank()) {
                                    AiDirectResult.Failure(AiDirectFailure.EMPTY_RESPONSE)
                                } else {
                                    AiDirectResult.Success(content)
                                }
                            }
                            response.code == 404 -> {
                                // Model retired/renamed — try the next one.
                            }
                            response.code == 400 || response.code == 401 || response.code == 403 ->
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

    /** `candidates[0].content.parts[0].text` — null when the shape is unexpected. */
    fun extractResponseText(responseBody: String): String? {
        return try {
            val root = JSONObject(responseBody)
            val candidates = root.optJSONArray("candidates") ?: return null
            val first = candidates.optJSONObject(0) ?: return null
            val content = first.optJSONObject("content") ?: return null
            val parts = content.optJSONArray("parts") ?: return null
            parts.optJSONObject(0)?.optString("text")?.takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * The API promises raw JSON (responseMimeType application/json), but models
     * sometimes wrap it in ```json fences or preamble text. Take the outermost
     * `{ ... }` block; null when there is none.
     */
    fun extractJsonObject(raw: String): String? {
        val start = raw.indexOf('{')
        val end = raw.lastIndexOf('}')
        if (start < 0 || end <= start) return null
        return raw.substring(start, end + 1)
    }
}
