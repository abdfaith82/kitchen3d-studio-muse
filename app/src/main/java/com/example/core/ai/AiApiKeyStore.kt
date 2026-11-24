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
 * Per-provider API key storage — one encrypted key per [AiProvider].
 *
 * v1.7 introduced this for Gemini; v1.8 generalizes it for Groq. Each
 * provider keeps its own SharedPreferences file and its own AndroidKeyStore
 * alias (AES-256/GCM, no new dependency). The Gemini slot keeps the exact
 * prefs/alias names from v1.7, so a key saved in v1.7 still loads in v1.8.
 * Every failure degrades to "no key" instead of crashing.
 */
object AiApiKeyStore {
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"

    private data class ProviderSlot(
        val prefsName: String,
        val encryptedKey: String,
        val ivKey: String,
        val keystoreAlias: String
    )

    private fun slot(provider: AiProvider): ProviderSlot = when (provider) {
        AiProvider.GEMINI -> ProviderSlot(
            prefsName = "kitchen_gemini_key_prefs",
            encryptedKey = "gemini_api_key_enc",
            ivKey = "gemini_api_key_iv",
            keystoreAlias = "kitchen_gemini_api_key"
        )
        AiProvider.GROQ -> ProviderSlot(
            prefsName = "kitchen_groq_key_prefs",
            encryptedKey = "groq_api_key_enc",
            ivKey = "groq_api_key_iv",
            keystoreAlias = "kitchen_groq_api_key"
        )
    }

    fun hasApiKey(context: Context, provider: AiProvider): Boolean =
        loadApiKey(context, provider) != null

    fun loadApiKey(context: Context, provider: AiProvider): String? {
        return try {
            val s = slot(provider)
            val prefs = prefs(context, s.prefsName)
            val enc = prefs.getString(s.encryptedKey, null) ?: return null
            val iv = prefs.getString(s.ivKey, null) ?: return null
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE,
                getOrCreateKey(s.keystoreAlias),
                GCMParameterSpec(128, Base64.decode(iv, Base64.DEFAULT))
            )
            String(cipher.doFinal(Base64.decode(enc, Base64.DEFAULT)), Charsets.UTF_8)
                .ifBlank { null }
        } catch (_: Exception) {
            null
        }
    }

    fun saveApiKey(context: Context, provider: AiProvider, apiKey: String): Boolean {
        val trimmed = apiKey.trim()
        if (trimmed.isEmpty()) return false
        return try {
            val s = slot(provider)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey(s.keystoreAlias))
            val encrypted = cipher.doFinal(trimmed.toByteArray(Charsets.UTF_8))
            prefs(context, s.prefsName).edit()
                .putString(s.encryptedKey, Base64.encodeToString(encrypted, Base64.NO_WRAP))
                .putString(s.ivKey, Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
                .apply()
            true
        } catch (_: Exception) {
            false
        }
    }

    fun clearApiKey(context: Context, provider: AiProvider) {
        val s = slot(provider)
        prefs(context, s.prefsName).edit()
            .remove(s.encryptedKey)
            .remove(s.ivKey)
            .apply()
    }

    private fun prefs(context: Context, name: String): SharedPreferences =
        context.getSharedPreferences(name, Context.MODE_PRIVATE)

    private fun getOrCreateKey(alias: String): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(alias, null) as? SecretKey)?.let { return it }
        val spec = KeyGenParameterSpec.Builder(
            alias,
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
