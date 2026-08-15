package com.nsvault.app.data.vault

import com.nsvault.app.core.common.VaultLog
import com.nsvault.app.core.di.IoDispatcher
import com.nsvault.app.data.crypto.VaultCipher
import com.nsvault.app.data.database.RecordingDao
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * One-time hardening pass: any recording saved before the encryption
 * engine existed (development builds of Phase 3) is encrypted in place
 * on startup, so the vault invariant — only ciphertext at rest — holds
 * for every file regardless of when it was created.
 */
@Singleton
class VaultMigrator @Inject constructor(
    private val dao: RecordingDao,
    private val fileStore: VaultFileStore,
    private val cipher: VaultCipher,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {

    suspend fun encryptLegacyPlaintext() = withContext(ioDispatcher) {
        val legacy = dao.legacyPlaintext()
        if (legacy.isEmpty()) return@withContext
        VaultLog.i(TAG, "Encrypting ${legacy.size} legacy recording(s)")

        legacy.forEach { row ->
            val source = fileStore.vaultFile(row.relativePath)
            if (!source.exists()) return@forEach
            val target = fileStore.vaultFileFor(
                row.createdAtEpochMs,
                row.uuid,
                VaultFileStore.VAULT_EXT,
            )
            try {
                val sha = cipher.encrypt(
                    source = source,
                    target = target,
                    uuid = row.uuid,
                    createdAtEpochMs = row.createdAtEpochMs,
                    audioFormat = row.container,
                )
                if (!cipher.verify(target, sha)) {
                    target.delete()
                    VaultLog.e(TAG, "Legacy migration verification failed; keeping original")
                    return@forEach
                }
                fileStore.secureDelete(source)
                dao.updateFile(
                    uuid = row.uuid,
                    relativePath = fileStore.relativePathOf(target),
                    sizeBytes = target.length(),
                )
            } catch (e: Exception) {
                target.delete()
                VaultLog.e(TAG, "Legacy migration failed for a recording", e)
            }
        }
    }

    companion object {
        private const val TAG = "Migrator"
    }
}
