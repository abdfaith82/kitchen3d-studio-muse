package com.example.core.media

import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * v1.9 — keyless photorealistic image generation via Pollinations.
 *
 * Verified live in the sibling app (2026-11-24): the legacy endpoint
 * `image.pollinations.ai/prompt/{prompt}?model=flux` answers anonymously
 * (no key, no account, no card); if it ever returns 401 and a key is
 * supplied, the keyed `gen.pollinations.ai/image/...` endpoint is tried
 * instead. One image = one GET; the file is a plain JPEG in [cacheDir].
 * Honest limit: one anonymous model (flux) and a community service — it can
 * be slow or briefly unavailable; the dialog reports that in Arabic.
 */
object PollinationsImageClient {
    const val LEGACY_BASE = "https://image.pollinations.ai"
    const val BASE = "https://gen.pollinations.ai"
    const val DEFAULT_MODEL = "flux"

    /** Pure URL builder (unit-tested): the exact GET the client performs. */
    fun buildImageUrl(
        prompt: String,
        width: Int = 1024,
        height: Int = 576,
        seed: Int = 42,
        model: String = DEFAULT_MODEL,
        keyed: Boolean = false
    ): String {
        val p = URLEncoder.encode(prompt, "UTF-8").replace("+", "%20")
        val base = if (keyed) BASE else LEGACY_BASE
        val path = if (keyed) "image" else "prompt"
        return "$base/$path/$p?model=$model&width=$width&height=$height&seed=$seed"
    }

    /** Downloads one image for [prompt] into [cacheDir]; [key] may be blank. */
    suspend fun generateImage(
        prompt: String,
        cacheDir: File,
        width: Int = 1024,
        height: Int = 576,
        key: String = ""
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val seed = (0..999999).random()
            val file = File(cacheDir, "kitchen-img-${UUID.randomUUID()}.jpg")
            fun download(url: String, auth: String?): Int {
                val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 20_000
                    readTimeout = 120_000
                    auth?.let { setRequestProperty("Authorization", "Bearer $it") }
                }
                val code = conn.responseCode
                if (code in 200..299) {
                    conn.inputStream?.use { input ->
                        file.outputStream().use { out -> input.copyTo(out) }
                    }
                }
                return code
            }
            val code = download(buildImageUrl(prompt, width, height, seed), null)
            if (code == 401 && key.isNotBlank()) {
                val code2 = download(buildImageUrl(prompt, width, height, seed, keyed = true), key)
                if (code2 !in 200..299) error("$code2")
            } else if (code !in 200..299) {
                error("$code")
            }
            if (!file.exists() || file.length() < 1024) error("صورة فارغة")
            file
        }
    }
}
