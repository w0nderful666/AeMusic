package com.aemusic.provider.account

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.core.content.edit
import com.aemusic.core.model.MusicSourceId
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Stores provider session cookies encrypted by a non-exportable Android Keystore key. */
class ProviderCredentialStore(context: Context) {
    private val preferences = context.getSharedPreferences("provider_credentials", Context.MODE_PRIVATE)

    fun cookie(sourceId: MusicSourceId): String? {
        return secret(sourceId.value)
    }

    fun secret(key: String): String? {
        val payload = preferences.getString(key, null) ?: return null
        return runCatching {
            val pieces = payload.split(':', limit = 2)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, Base64.decode(pieces[0], Base64.NO_WRAP)))
            String(cipher.doFinal(Base64.decode(pieces[1], Base64.NO_WRAP)), Charsets.UTF_8)
        }.getOrNull()
    }

    fun save(sourceId: MusicSourceId, cookie: String) {
        saveSecret(sourceId.value, cookie)
    }

    fun saveSecret(key: String, value: String) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val encrypted = Base64.encodeToString(cipher.iv, Base64.NO_WRAP) + ":" +
            Base64.encodeToString(cipher.doFinal(value.toByteArray()), Base64.NO_WRAP)
        preferences.edit { putString(key, encrypted) }
    }

    fun clear(sourceId: MusicSourceId) = preferences.edit { remove(sourceId.value) }
    fun clearSecret(key: String) = preferences.edit { remove(key) }
    fun isConnected(sourceId: MusicSourceId) = cookie(sourceId) != null

    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").run {
            init(
                KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build(),
            )
            generateKey()
        }
    }

    companion object {
        private const val KEY_ALIAS = "aemusic-provider-session-v1"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val WEBDAV_PASSWORD = "cloud:webdav:password"
        const val NAVIDROME_PASSWORD = "cloud:navidrome:token"
    }
}
