package com.nsvault.app.data.vault

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import com.nsvault.app.core.common.VaultLog
import com.nsvault.app.core.di.IoDispatcher
import com.nsvault.app.data.audio.Mp4Remuxer
import com.nsvault.app.data.crypto.VaultCipher
import com.nsvault.app.data.database.RecordingDao
import com.nsvault.app.data.database.RecordingEntity
import com.nsvault.app.domain.model.Recording
import com.nsvault.app.domain.model.RecordingStatus
import com.nsvault.app.domain.model.SaveStage
import com.nsvault.app.domain.model.VaultStats
import com.nsvault.app.domain.repository.VaultRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VaultRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: RecordingDao,
    private val fileStore: VaultFileStore,
    private val remuxer: Mp4Remuxer,
    private val cipher: VaultCipher,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : VaultRepository {

    override val stats: Flow<VaultStats> = dao.observeStats().map { row ->
        VaultStats(
            recordingCount = row.recordingCount,
            totalDurationMs = row.totalDurationMs ?: 0,
            totalSizeBytes = row.totalSizeBytes ?: 0,
        )
    }

    override val latestRecording: Flow<Recording?> =
        dao.observeLatest().map { entity -> entity?.toDomain() }

    override suspend fun beginRecording(uuid: String, createdAtEpochMs: Long) {
        withContext(ioDispatcher) {
            dao.insert(
                RecordingEntity(
                    uuid = uuid,
                    title = defaultTitle(createdAtEpochMs),
                    createdAtEpochMs = createdAtEpochMs,
                    durationMs = 0,
                    sizeBytes = 0,
                    relativePath = "",
                    container = VaultFileStore.CONTAINER_M4A,
                    status = RecordingStatus.RECORDING,
                ),
            )
        }
    }

    override suspend fun finalizeCapture(
        uuid: String,
        capture: File,
        createdAtEpochMs: Long,
        durationMs: Long,
        recovered: Boolean,
        waveformPreview: ByteArray?,
        onEvent: (SaveStage, Float) -> Unit,
    ): Long = withContext(ioDispatcher) {
        // 1. Remux the crash-safe ADTS stream into a clean .m4a. If the
        //    capture is too damaged to remux, encrypt the raw stream —
        //    audio is never discarded.
        onEvent(SaveStage.Preparing, 0f)
        val m4a = fileStore.tempFileFor(uuid, VaultFileStore.CONTAINER_M4A)
        val remuxed = remuxer.remux(capture, m4a) { progress ->
            onEvent(SaveStage.Preparing, progress)
        }
        val plaintext = if (remuxed) m4a else capture.also { m4a.delete() }
        val container = if (remuxed) VaultFileStore.CONTAINER_M4A else VaultFileStore.CAPTURE_EXT

        // 2. Encrypt into the vault, hashing the plaintext as we go.
        val target = fileStore.vaultFileFor(createdAtEpochMs, uuid, VaultFileStore.VAULT_EXT)
        try {
            onEvent(SaveStage.Encrypting, 0f)
            val plaintextSha = cipher.encrypt(
                source = plaintext,
                target = target,
                uuid = uuid,
                createdAtEpochMs = createdAtEpochMs,
                audioFormat = container,
            ) { progress ->
                onEvent(SaveStage.Encrypting, progress)
            }

            // 3. Round-trip verification: decrypt everything we just
            //    wrote and compare digests before touching the originals.
            onEvent(SaveStage.Verifying, 0f)
            val verified = cipher.verify(target, plaintextSha) { progress ->
                onEvent(SaveStage.Verifying, progress)
            }
            if (!verified) throw IOException("Round-trip verification failed")

            // 4. Only now is the plaintext destroyed.
            onEvent(SaveStage.Finishing, 0f)
            fileStore.secureDelete(capture)
            if (plaintext != capture) fileStore.secureDelete(plaintext)

            val status = if (recovered) RecordingStatus.RECOVERED else RecordingStatus.COMPLETED
            val existing = dao.findByUuid(uuid)
            val id = if (existing != null) {
                dao.finalize(
                    uuid = uuid,
                    durationMs = durationMs,
                    sizeBytes = target.length(),
                    relativePath = fileStore.relativePathOf(target),
                    container = container,
                    status = status,
                    waveform = waveformPreview,
                )
                existing.id
            } else {
                dao.insert(
                    RecordingEntity(
                        uuid = uuid,
                        title = defaultTitle(createdAtEpochMs),
                        createdAtEpochMs = createdAtEpochMs,
                        durationMs = durationMs,
                        sizeBytes = target.length(),
                        relativePath = fileStore.relativePathOf(target),
                        container = container,
                        status = status,
                        waveform = waveformPreview,
                    ),
                )
            }
            onEvent(SaveStage.Finishing, 1f)
            id
        } catch (e: Exception) {
            VaultLog.e(TAG, "Save pipeline failed", e)
            target.delete()
            throw e
        }
    }

    override suspend fun markFailed(uuid: String) {
        withContext(ioDispatcher) {
            dao.updateStatus(uuid, RecordingStatus.FAILED)
        }
    }

    override suspend fun inProgressRecordings(): List<Recording> = withContext(ioDispatcher) {
        dao.inProgress().map { it.toDomain() }
    }

    override suspend fun availableStorageBytes(): Long = withContext(ioDispatcher) {
        fileStore.availableBytes()
    }

    override val recordings: Flow<List<Recording>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun rename(uuid: String, title: String) = withContext(ioDispatcher) {
        dao.rename(uuid, title.trim())
    }

    override suspend fun setFavorite(uuid: String, favorite: Boolean) = withContext(ioDispatcher) {
        dao.setFavorite(uuid, favorite)
    }

    override suspend fun delete(uuid: String) = withContext(ioDispatcher) {
        dao.findByUuid(uuid)?.let { row ->
            if (row.relativePath.isNotEmpty()) {
                fileStore.secureDelete(fileStore.vaultFile(row.relativePath))
            }
        }
        dao.deleteByUuid(uuid)
    }

    override suspend fun importRecording(
        uri: Uri,
        onProgress: (Float) -> Unit,
    ): Long? = withContext(ioDispatcher) {
        val uuid = UUID.randomUUID().toString()
        val displayName = queryDisplayName(uri)
        val extension = displayName?.substringAfterLast('.', "")?.lowercase()
            .takeUnless { it.isNullOrEmpty() } ?: VaultFileStore.CONTAINER_M4A
        val temp = fileStore.tempFileFor(uuid, extension)

        try {
            // 1. Copy out of SAF — the original document is never touched.
            context.contentResolver.openInputStream(uri)?.use { input ->
                temp.outputStream().use { output -> input.copyTo(output) }
            } ?: return@withContext null
            if (temp.length() == 0L) {
                temp.delete()
                return@withContext null
            }
            onProgress(0.1f)

            val durationMs = probeDurationMs(temp)
                ?: run {
                    // Not decodable audio — refuse rather than store junk.
                    temp.delete()
                    return@withContext null
                }
            val createdAt = System.currentTimeMillis()

            // 2. Same encrypt → verify pipeline as recordings.
            val target = fileStore.vaultFileFor(createdAt, uuid, VaultFileStore.VAULT_EXT)
            try {
                val sha = cipher.encrypt(
                    source = temp,
                    target = target,
                    uuid = uuid,
                    createdAtEpochMs = createdAt,
                    audioFormat = extension,
                ) { progress -> onProgress(0.1f + progress * 0.6f) }

                val verified = cipher.verify(target, sha) { progress ->
                    onProgress(0.7f + progress * 0.3f)
                }
                if (!verified) throw IOException("Import verification failed")
            } catch (e: Exception) {
                target.delete()
                throw e
            }

            fileStore.secureDelete(temp)
            dao.insert(
                RecordingEntity(
                    uuid = uuid,
                    title = displayName?.substringBeforeLast('.')?.takeIf { it.isNotBlank() }
                        ?: defaultTitle(createdAt),
                    createdAtEpochMs = createdAt,
                    durationMs = durationMs,
                    sizeBytes = target.length(),
                    relativePath = fileStore.relativePathOf(target),
                    container = extension,
                    status = RecordingStatus.COMPLETED,
                ),
            )
        } catch (e: Exception) {
            VaultLog.e(TAG, "Import failed", e)
            temp.delete()
            null
        }
    }

    private fun queryDisplayName(uri: Uri): String? = runCatching {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
        }
    }.getOrNull()

    private fun probeDurationMs(file: File): Long? = runCatching {
        MediaMetadataRetriever().use { retriever ->
            retriever.setDataSource(file.absolutePath)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong()
        }
    }.getOrNull()

    private fun defaultTitle(createdAtEpochMs: Long): String =
        TITLE_STAMP.format(Instant.ofEpochMilli(createdAtEpochMs))

    override suspend fun getRecording(id: Long): Recording? = withContext(ioDispatcher) {
        dao.findById(id)?.toDomain()
    }

    override suspend fun exportForSharing(uuid: String): Uri? = withContext(ioDispatcher) {
        val row = dao.findByUuid(uuid) ?: return@withContext null
        if (row.relativePath.isEmpty()) return@withContext null
        val source = fileStore.vaultFile(row.relativePath)
        if (!source.exists()) return@withContext null

        try {
            val shareDir = File(context.cacheDir, SHARE_DIR)
            shareDir.deleteRecursively()
            shareDir.mkdirs()
            val safeName = row.title.replace(Regex("[^\\w \\-·•]"), "_").take(60).ifBlank { "recording" }
            val out = File(shareDir, "$safeName.${row.container}")
            cipher.decryptingStream(source).use { input ->
                out.outputStream().use { output -> input.copyTo(output) }
            }
            androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                out,
            )
        } catch (e: Exception) {
            VaultLog.e(TAG, "Share export failed", e)
            null
        }
    }

    override suspend fun clearShareCache(): Unit = withContext(ioDispatcher) {
        File(context.cacheDir, SHARE_DIR).deleteRecursively()
    }

    private fun RecordingEntity.toDomain() = Recording(
        id = id,
        uuid = uuid,
        title = title,
        createdAtEpochMs = createdAtEpochMs,
        durationMs = durationMs,
        sizeBytes = sizeBytes,
        relativePath = relativePath,
        isFavorite = isFavorite,
        waveformPreview = waveform,
        resumePositionMs = resumePositionMs,
    )

    override suspend fun saveResumePosition(uuid: String, positionMs: Long) = withContext(ioDispatcher) {
        dao.updateResumePosition(uuid, positionMs)
    }

    companion object {
        private const val TAG = "Vault"
        private const val SHARE_DIR = "share"

        private val TITLE_STAMP = DateTimeFormatter
            .ofPattern("d MMM • hh:mm a")
            .withZone(ZoneId.systemDefault())
    }
}
