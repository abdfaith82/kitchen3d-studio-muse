package com.example.core.ai

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Gemini API key storage v1.7 — the key lives encrypted on the device only.
 *
 * Why this exists: the repo deliberately has NO google-services.json (see
 * .env.example), so Firebase never initializes and the old Firebase AI Logic
 * path (`Firebase.ai.generativeModel`) threw "Default FirebaseApp is not
 * initialized" for every user. The Gemini feature now calls the Gemini REST
 * API directly with a free user key from Google AI Studio, and that key is
 * stored here: AES-256/GCM in the AndroidKeyStore, ciphertext + IV in
 * private SharedPreferences. No new dependency (no EncryptedSharedPreferences
 * in the catalog); every failure degrades to "no key" instead of crashing.
 */
object GeminiApiKeyStore {
    private const val PREFS_NAME = "kitchen_gemini_key_prefs"
    private const val KEY_ENCRYPTED = "gemini_api_key_enc"
    private const val KEY_IV = "gemini_api_key_iv"
    private const val KEYSTORE_ALIAS = "kitchen_gemini_api_key"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"

    fun hasApiKey(context: Context): Boolean = loadApiKey(context) != null

    fun loadApiKey(context: Context): String? {
        return try {
            val prefs = prefs(context)
            val enc = prefs.getString(KEY_ENCRYPTED, null) ?: return null
            val iv = prefs.getString(KEY_IV, null) ?: return null
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE,
                getOrCreateKey(),
                GCMParameterSpec(128, Base64.decode(iv, Base64.DEFAULT))
            )
            String(cipher.doFinal(Base64.decode(enc, Base64.DEFAULT)), Charsets.UTF_8)
                .ifBlank { null }
        } catch (_: Exception) {
            null
        }
    }

    fun saveApiKey(context: Context, apiKey: String): Boolean {
        val trimmed = apiKey.trim()
        if (trimmed.isEmpty()) return false
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
            val encrypted = cipher.doFinal(trimmed.toByteArray(Charsets.UTF_8))
            prefs(context).edit()
                .putString(KEY_ENCRYPTED, Base64.encodeToString(encrypted, Base64.NO_WRAP))
                .putString(KEY_IV, Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
                .apply()
            true
        } catch (_: Exception) {
            false
        }
    }

    fun clearApiKey(context: Context) {
        prefs(context).edit()
            .remove(KEY_ENCRYPTED)
            .remove(KEY_IV)
            .apply()
    }

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEYSTORE_ALIAS, null) as? SecretKey)?.let { return it }
        val spec = KeyGenParameterSpec.Builder(
            KEYSTORE_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .build()
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
            .apply { init(spec) }
            .generateKey()
    }
}
