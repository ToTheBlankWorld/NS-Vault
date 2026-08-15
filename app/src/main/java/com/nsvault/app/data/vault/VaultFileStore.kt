package com.nsvault.app.data.vault

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns the vault's on-disk layout inside app-private storage —
 * invisible to the gallery, music players, and file browsers.
 *
 * ```
 * filesDir/
 *   capture/                    in-flight recordings (crash-recoverable)
 *   vault/<year>/<month>/       committed recordings
 * ```
 */
@Singleton
class VaultFileStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    val captureDir: File
        get() = File(context.filesDir, CAPTURE_DIR).apply { mkdirs() }

    private val vaultDir: File
        get() = File(context.filesDir, VAULT_DIR).apply { mkdirs() }

    /** Files are named by recording UUID — identity survives recovery. */
    fun captureFileFor(uuid: String): File = File(captureDir, "$uuid.$CAPTURE_EXT")

    fun tempFileFor(uuid: String, extension: String): File =
        File(captureDir, "$uuid.$extension")

    fun vaultFileFor(createdAtEpochMs: Long, uuid: String, extension: String): File {
        val dir = File(vaultDir, DIR_STAMP.format(instantOf(createdAtEpochMs))).apply { mkdirs() }
        return File(dir, "$uuid.$extension")
    }

    fun listCaptures(): List<File> =
        captureDir.listFiles { file ->
            file.isFile && file.length() > 0 && file.extension == CAPTURE_EXT
        }?.toList().orEmpty()

    /**
     * Overwrite with zeros, then delete — plaintext should not linger
     * in freed blocks. (Best effort on flash storage, still worthwhile.)
     */
    fun secureDelete(file: File) {
        if (!file.exists()) return
        runCatching {
            java.io.RandomAccessFile(file, "rws").use { raf ->
                val zeros = ByteArray(64 * 1024)
                var remaining = raf.length()
                raf.seek(0)
                while (remaining > 0) {
                    val count = minOf(remaining, zeros.size.toLong()).toInt()
                    raf.write(zeros, 0, count)
                    remaining -= count
                }
            }
        }
        file.delete()
    }

    fun vaultFile(relativePath: String): File = File(vaultDir, relativePath)

    fun relativePathOf(file: File): String =
        file.absolutePath.removePrefix(vaultDir.absolutePath).trimStart(File.separatorChar)

    fun availableBytes(): Long = context.filesDir.usableSpace

    private fun instantOf(epochMs: Long) = Instant.ofEpochMilli(epochMs)

    companion object {
        private const val CAPTURE_DIR = "capture"
        private const val VAULT_DIR = "vault"

        /** Crash-safe streaming capture format; remuxed to .m4a on save. */
        const val CAPTURE_EXT = "aac"
        const val CONTAINER_M4A = "m4a"

        /** Committed vault files are always encrypted. */
        const val VAULT_EXT = "enc"

        private val DIR_STAMP = DateTimeFormatter
            .ofPattern("yyyy/MM")
            .withZone(ZoneId.systemDefault())
    }
}
