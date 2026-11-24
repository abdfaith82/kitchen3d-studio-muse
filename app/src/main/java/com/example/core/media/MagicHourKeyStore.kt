package com.example.core.media

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
 * v1.9 — the free Magic Hour API key, stored AES-256/GCM in AndroidKeyStore
 * (same pattern as [com.example.core.ai.AiApiKeyStore], its own slot).
 * The key comes from magichour.ai — free account, no card — and is only ever
 * sent to api.magichour.ai. Every failure degrades to "no key".
 */
object MagicHourKeyStore {
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val PREFS_NAME = "kitchen_magichour_key_prefs"
    private const val ENCRYPTED_KEY = "magichour_api_key_enc"
    private const val IV_KEY = "magichour_api_key_iv"
    private const val KEYSTORE_ALIAS = "kitchen_magichour_api_key"

    fun hasApiKey(context: Context): Boolean = loadApiKey(context) != null

    fun loadApiKey(context: Context): String? {
        return try {
            val prefs = prefs(context)
            val enc = prefs.getString(ENCRYPTED_KEY, null) ?: return null
            val iv = prefs.getString(IV_KEY, null) ?: return null
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE,
                loadOrCreateKey(),
                GCMParameterSpec(128, Base64.decode(iv, Base64.DEFAULT))
            )
            String(cipher.doFinal(Base64.decode(enc, Base64.DEFAULT)), Charsets.UTF_8)
        } catch (_: Exception) {
            null
        }
    }

    fun saveApiKey(context: Context, key: String) {
        try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, loadOrCreateKey())
            val enc = Base64.encodeToString(cipher.doFinal(key.toByteArray(Charsets.UTF_8)), Base64.DEFAULT)
            val iv = Base64.encodeToString(cipher.iv, Base64.DEFAULT)
            prefs(context).edit().putString(ENCRYPTED_KEY, enc).putString(IV_KEY, iv).apply()
        } catch (_: Exception) {
        }
    }

    fun clearApiKey(context: Context) {
        prefs(context).edit().remove(ENCRYPTED_KEY).remove(IV_KEY).apply()
    }

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun loadOrCreateKey(): SecretKey {
        val ks = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (ks.getKey(KEYSTORE_ALIAS, null) as? SecretKey)?.let { return it }
        val kg = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        kg.init(
            KeyGenParameterSpec.Builder(
                KEYSTORE_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build()
        )
        return kg.generateKey()
    }
}
