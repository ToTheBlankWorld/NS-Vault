package com.nsvault.app.data.playback

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import com.nsvault.app.data.crypto.VaultCipher
import com.nsvault.app.data.vault.VaultFileStore
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Media3 DataSource that streams plaintext audio straight out of an
 * encrypted vault file. No decrypted byte ever touches disk: seeking
 * maps to chunk-level random access, one decrypted chunk is held in
 * memory at a time, and buffers are zeroed on close.
 *
 * URIs: `nsvault:///<relative vault path>`.
 */
@UnstableApi
class VaultDataSource(
    private val cipher: VaultCipher,
    private val fileStore: VaultFileStore,
) : BaseDataSource(/* isNetwork = */ false) {

    private var reader: VaultCipher.VaultAudioReader? = null
    private var currentUri: Uri? = null
    private var position = 0L
    private var bytesRemaining = 0L
    private var opened = false

    override fun open(dataSpec: DataSpec): Long {
        // Defensive: Media3 pairs open/close, but never leak a reader if
        // open is somehow called twice.
        reader?.close()
        reader = null

        currentUri = dataSpec.uri
        transferInitializing(dataSpec)

        val relativePath = dataSpec.uri.path?.trimStart('/')
            ?.takeIf { it.isNotEmpty() && !it.contains("..") }
            ?: throw IOException("Invalid vault uri")

        val audioReader = cipher.openRandomAccess(fileStore.vaultFile(relativePath))
        reader = audioReader
        position = dataSpec.position
        if (position > audioReader.plaintextLength) {
            audioReader.close()
            reader = null
            throw IOException("Position out of range")
        }
        bytesRemaining = if (dataSpec.length != C.LENGTH_UNSET.toLong()) {
            dataSpec.length
        } else {
            audioReader.plaintextLength - position
        }

        opened = true
        transferStarted(dataSpec)
        return bytesRemaining
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (length == 0) return 0
        if (bytesRemaining <= 0) return C.RESULT_END_OF_INPUT
        val audioReader = reader ?: return C.RESULT_END_OF_INPUT

        val toRead = minOf(length.toLong(), bytesRemaining).toInt()
        val read = audioReader.readAt(position, buffer, offset, toRead)
        if (read < 0) return C.RESULT_END_OF_INPUT

        position += read
        bytesRemaining -= read
        bytesTransferred(read)
        return read
    }

    override fun getUri(): Uri? = currentUri

    override fun close() {
        reader?.close()
        reader = null
        currentUri = null
        if (opened) {
            opened = false
            transferEnded()
        }
    }
}

@UnstableApi
@Singleton
class VaultDataSourceFactory @Inject constructor(
    private val cipher: VaultCipher,
    private val fileStore: VaultFileStore,
) : DataSource.Factory {

    override fun createDataSource(): DataSource = VaultDataSource(cipher, fileStore)
}
