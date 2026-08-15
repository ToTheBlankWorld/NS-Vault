package com.nsvault.app.domain.repository

import com.nsvault.app.domain.model.Recording
import com.nsvault.app.domain.model.SaveStage
import com.nsvault.app.domain.model.VaultStats
import kotlinx.coroutines.flow.Flow
import java.io.File

interface VaultRepository {

    val stats: Flow<VaultStats>

    val latestRecording: Flow<Recording?>

    /** All committed recordings, newest first. */
    val recordings: Flow<List<Recording>>

    suspend fun rename(uuid: String, title: String)

    suspend fun setFavorite(uuid: String, favorite: Boolean)

    /** Securely delete the vault file, then remove the metadata row. */
    suspend fun delete(uuid: String)

    /**
     * Copy an external audio file into the vault: encrypt, verify,
     * and register it. The source document is never modified.
     * Returns the recording id, or null if the file was unreadable.
     */
    suspend fun importRecording(
        uri: android.net.Uri,
        onProgress: (Float) -> Unit,
    ): Long?

    /**
     * Write the metadata row the moment recording starts (status
     * RECORDING) so an interruption can never orphan a capture.
     */
    suspend fun beginRecording(uuid: String, createdAtEpochMs: Long)

    /**
     * Run the full save pipeline on a finished capture: remux →
     * encrypt → round-trip verify → secure-delete plaintext → commit
     * metadata. Emits [onEvent] with stage + 0..1 progress throughout.
     * Returns the recording id.
     */
    suspend fun finalizeCapture(
        uuid: String,
        capture: File,
        createdAtEpochMs: Long,
        durationMs: Long,
        recovered: Boolean,
        waveformPreview: ByteArray?,
        onEvent: (SaveStage, Float) -> Unit,
    ): Long

    /** Close the metadata row for a capture that yielded nothing. */
    suspend fun markFailed(uuid: String)

    /** Rows still in RECORDING status — the recovery scan's source of truth. */
    suspend fun inProgressRecordings(): List<Recording>

    /** Free bytes on the vault's storage volume. */
    suspend fun availableStorageBytes(): Long

    suspend fun getRecording(id: Long): Recording?

    /**
     * Decrypt a recording into the app-private share cache and return
     * a content URI for it. Only called after explicit user
     * confirmation and re-authentication; the export is cleared on the
     * next app start.
     */
    suspend fun exportForSharing(uuid: String): android.net.Uri?

    /** Remove any decrypted share exports from the cache. */
    suspend fun clearShareCache()

    suspend fun saveResumePosition(uuid: String, positionMs: Long)
}
