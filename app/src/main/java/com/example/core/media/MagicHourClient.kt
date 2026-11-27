package com.example.core.media

import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Magic Hour — video from a generated kitchen image, free key, no card.
 * Ported from the sibling app, whose spec was verified live against the
 * official openapi.json on 2026-11-24:
 * POST https://api.magichour.ai/v1/files/upload-urls (items:[{type:"image",
 * extension}]) → PUT the bytes on the signed upload_url (no Authorization)
 * → POST /v1/image-to-video {end_seconds, model, resolution:"480p",
 * style:{prompt}, assets:{image_file_path}} → GET /v1/video-projects/{id}
 * until complete (or error/canceled) → downloads[0].url (signed link,
 * downloaded WITHOUT Authorization, expires in ~24h).
 * Free credit is finite (400 on signup + 100/day up to 7 times — about 15
 * short 480p clips in total); 402 means the free credit ran out.
 * Free models: wan-2.2, ltx-2.5, minimax-h3 (480p).
 */
object MagicHourClient {
    const val BASE = "https://api.magichour.ai"
    val FREE_MODELS = listOf("wan-2.2", "ltx-2.5", "minimax-h3")

    /**
     * v1.12 (P1-10) — API failures used to reach the user as raw English JSON
     * ("401: {\"error\": ...}"). The raw body now goes to Logcat only; the
     * user sees an Arabic message they can act on.
     */
    private fun arabicHttpError(code: Int, raw: String, tag: String): Nothing {
        android.util.Log.e("MagicHour", "$tag HTTP $code: ${raw.take(500)}")
        val message = when (code) {
            401, 403 -> "المفتاح غير صحيح أو منتهٍ — تحقق من مفتاح Magic Hour في الأسفل"
            402 -> "انتهى الرصيد المجاني (402)"
            429 -> "تجاوزت الحد المسموح — انتظر قليلاً وحاول مجدداً"
            400 -> "طلب غير صالح — جرّب صورة أخرى"
            500, 502, 503 -> "خدمة الفيديو غير متاحة الآن — حاول لاحقاً"
            else -> "تعذّر الاتصال بخدمة الفيديو (رمز $code)"
        }
        error(message)
    }

    /** Pure parser (unit-tested): (upload_url, file_path) from an upload-urls body. */
    fun parseUploadUrlsResponse(raw: String): Pair<String, String> {
        val item = JSONObject(raw).optJSONArray("items")?.optJSONObject(0)
            ?: error("رد بلا رابط رفع")
        val uploadUrl = item.optString("upload_url")
        val filePath = item.optString("file_path")
        if (uploadUrl.isBlank() || filePath.isBlank()) error("رد رفع ناقص")
        return uploadUrl to filePath
    }

    /** Pure parser (unit-tested): (status, download url or null) from a project body. */
    fun parseProjectStatus(raw: String): Pair<String, String?> {
        val j = JSONObject(raw)
        val status = j.optString("status")
        val url = j.optJSONArray("downloads")?.let { arr ->
            if (arr.length() > 0) arr.getJSONObject(0).optString("url") else null
        }
        return status to url?.ifBlank { null }
    }

    private suspend fun postProject(path: String, body: JSONObject, key: String): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val conn = (URL("$BASE$path").openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 20_000
                    readTimeout = 60_000
                    setRequestProperty("Authorization", "Bearer $key")
                    setRequestProperty("Content-Type", "application/json")
                    doOutput = true
                }
                conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
                val code = conn.responseCode
                val raw = (if (code in 200..299) conn.inputStream else conn.errorStream)
                    ?.bufferedReader()?.use { it.readText() } ?: ""
                if (code !in 200..299) arabicHttpError(code, raw, "submit")
                val id = JSONObject(raw).optString("id")
                if (id.isBlank()) error("رد بلا معرّف مشروع") else id
            }
        }

    /** Uploads a local image and returns its file_path for image-to-video. */
    suspend fun uploadImage(file: File, key: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val ext = file.extension.ifBlank { "jpg" }.lowercase()
            val body = JSONObject().put(
                "items",
                JSONArray().put(JSONObject().put("type", "image").put("extension", ext))
            )
            val conn = (URL("$BASE/v1/files/upload-urls").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 20_000
                readTimeout = 60_000
                setRequestProperty("Authorization", "Bearer $key")
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
            }
            conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            val raw = (if (code in 200..299) conn.inputStream else conn.errorStream)
                ?.bufferedReader()?.use { it.readText() } ?: ""
            if (code !in 200..299) arabicHttpError(code, raw, "upload-urls")
            val (uploadUrl, filePath) = parseUploadUrlsResponse(raw)
            val put = (URL(uploadUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "PUT"
                connectTimeout = 20_000
                readTimeout = 120_000
                setRequestProperty("Content-Type", "image/jpeg")
                doOutput = true
            }
            put.outputStream.use { out -> file.inputStream().use { input -> input.copyTo(out) } }
            val putCode = put.responseCode
            if (putCode !in 200..299) arabicHttpError(putCode, "", "upload")
            filePath
        }
    }

    /** Image → video: returns the project id. */
    suspend fun submitImageToVideo(
        prompt: String,
        model: String,
        durationSec: Int,
        imageFilePath: String,
        key: String
    ): Result<String> {
        val body = JSONObject()
            .put("name", "Kitchen image-to-video")
            .put("end_seconds", durationSec)
            .put("model", model)
            .put("resolution", "480p")
            .put("style", JSONObject().put("prompt", prompt))
            .put("assets", JSONObject().put("image_file_path", imageFilePath))
        return postProject("/v1/image-to-video", body, key)
    }

    /** Project status: (status, download url when complete or null). */
    suspend fun projectStatus(projectId: String, key: String): Result<Pair<String, String?>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val conn = (URL("$BASE/v1/video-projects/$projectId").openConnection() as HttpURLConnection).apply {
                    connectTimeout = 20_000
                    readTimeout = 30_000
                    setRequestProperty("Authorization", "Bearer $key")
                }
                val code = conn.responseCode
                val raw = (if (code in 200..299) conn.inputStream else conn.errorStream)
                    ?.bufferedReader()?.use { it.readText() } ?: ""
                if (code !in 200..299) arabicHttpError(code, raw, "status")
                parseProjectStatus(raw)
            }
        }

    /** Downloads a signed link (no Authorization) into [target]. */
    suspend fun downloadUrl(url: String, target: File): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 20_000
                readTimeout = 180_000
            }
            val code = conn.responseCode
            if (code !in 200..299) arabicHttpError(code, "", "download")
            conn.inputStream?.use { input -> target.outputStream().use { out -> input.copyTo(out) } }
            target
        }
    }
}
