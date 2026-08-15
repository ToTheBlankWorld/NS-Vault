package com.nsvault.app.data.crypto

import kotlinx.serialization.Serializable

/**
 * Self-describing metadata sealed inside every vault file, so the
 * vault can be understood, migrated, and verified without the Room
 * database. Encrypted with the file's own data key and authenticated
 * together with the audio content.
 *
 * [reserved] plus JSON's ignore-unknown-keys parsing is the forward
 * compatibility escape hatch: future versions may add fields without
 * breaking older readers.
 */
@Serializable
data class VaultFileMetadata(
    val formatVersion: Int,
    val uuid: String,
    val createdAtEpochMs: Long,
    val audioFormat: String,
    val chunkSizeBytes: Int,
    val cipher: String,
    val reserved: Map<String, String> = emptyMap(),
)
