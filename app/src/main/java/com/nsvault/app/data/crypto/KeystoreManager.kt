package com.nsvault.app.data.crypto

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.nsvault.app.core.common.VaultLog
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns every key NS Vault uses. Keys are AES-256-GCM, generated inside
 * the Android Keystore (StrongBox when the device has it), and are
 * non-exportable by construction — no key material ever exists in app
 * memory, preferences, or the database.
 *
 * Phase 2 uses [seal]/[open] for small records (the PIN digest).
 * Phase 4 reuses the same key management for streaming file encryption.
 */
@Singleton
class KeystoreManager @Inject constructor() {

    /**
     * Encrypt [plaintext] under the key at [alias].
     * Blob layout: `[version:1][ivLength:1][iv][ciphertext+tag]`.
     */
    fun seal(alias: String, plaintext: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey(alias))
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(plaintext)
        return byteArrayOf(BLOB_VERSION, iv.size.toByte()) + iv + ciphertext
    }

    /**
     * Decrypt a blob produced by [seal]. GCM authentication makes any
     * tampering throw rather than return corrupt data.
     */
    fun open(alias: String, blob: ByteArray): ByteArray {
        require(blob.size > 2 && blob[0] == BLOB_VERSION) { "Unsupported blob format" }
        val ivLength = blob[1].toInt()
        val iv = blob.copyOfRange(2, 2 + ivLength)
        val ciphertext = blob.copyOfRange(2 + ivLength, blob.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(alias), GCMParameterSpec(GCM_TAG_BITS, iv))
        return cipher.doFinal(ciphertext)
    }

    fun getOrCreateKey(alias: String): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(alias, null) as? SecretKey)?.let { return it }
        return generateKey(alias)
    }

    private fun generateKey(alias: String): SecretKey {
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        return try {
            generator.init(keySpec(alias, strongBox = true))
            generator.generateKey().also {
                VaultLog.i(TAG, "Generated StrongBox-backed key")
            }
        } catch (e: Exception) {
            // StrongBox missing or at capacity — fall back to the TEE-backed keystore.
            generator.init(keySpec(alias, strongBox = false))
            generator.generateKey().also {
                VaultLog.i(TAG, "Generated TEE-backed key")
            }
        }
    }

    private fun keySpec(alias: String, strongBox: Boolean): KeyGenParameterSpec =
        KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setRandomizedEncryptionRequired(true)
            .apply { if (strongBox) setIsStrongBoxBacked(true) }
            .build()

    companion object {
        private const val TAG = "Keystore"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val BLOB_VERSION: Byte = 1
        const val GCM_TAG_BITS = 128

        /** Wraps small sensitive records (PIN digest). */
        const val ALIAS_PREFS = "nsvault.key.prefs.v1"

        /** Will encrypt vault audio files from Phase 4 on. */
        const val ALIAS_VAULT_FILES = "nsvault.key.files.v1"
    }
}
