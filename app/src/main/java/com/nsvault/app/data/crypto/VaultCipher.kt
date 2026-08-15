package com.nsvault.app.data.crypto

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.EOFException
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.PushbackInputStream
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Streaming, seekable AES-256-GCM for vault audio files.
 *
 * Envelope design: every file gets a fresh random 256-bit data key
 * (DEK) sealed by the hardware Keystore key and carried in the file
 * header — the fast software cipher does the bulk work, the hardware
 * key never leaves the Keystore, and chunk nonces can be derived
 * deterministically without weakening anything.
 *
 * File format (`.enc`, version 2):
 * ```
 * [4B magic "NSV1"][1B version][4B chunkSize][2B wrappedKeyLen]
 * [wrappedKey][8B noncePrefix]
 * [4B metaLen][sealed VaultFileMetadata]      (v2+)
 * repeated chunk: [4B ciphertextLen][ciphertext+tag]
 * ```
 * The metadata frame is sealed with the same DEK (reserved nonce
 * counter, distinct AAD domain), so header and content authenticate
 * as one unit: a header cannot be transplanted between files, and any
 * tamper fails a GCM tag. Chunk i uses IV = noncePrefix ‖ BE32(i) and
 * AAD = [version, isFinalChunk] — reordering fails the IV counter,
 * truncation fails the missing final flag. Chunked framing keeps
 * memory constant for multi-hour files and lets playback seek by
 * chunk index. Version 1 files (no metadata frame) remain readable.
 */
@Singleton
class VaultCipher @Inject constructor(
    private val keystore: KeystoreManager,
) {

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Encrypt [source] into [target] with a sealed metadata header.
     * Reports [onProgress] in 0..1 and returns the SHA-256 of the
     * plaintext for round-trip verification.
     */
    @Throws(IOException::class)
    fun encrypt(
        source: File,
        target: File,
        uuid: String,
        createdAtEpochMs: Long,
        audioFormat: String,
        onProgress: (Float) -> Unit = {},
    ): ByteArray {
        val dek = ByteArray(DEK_BYTES).also { SecureRandom().nextBytes(it) }
        try {
            val key: SecretKey = SecretKeySpec(dek, "AES")
            val wrappedKey = keystore.seal(KeystoreManager.ALIAS_VAULT_FILES, dek)
            val noncePrefix = ByteArray(NONCE_PREFIX_BYTES).also { SecureRandom().nextBytes(it) }
            val digest = MessageDigest.getInstance("SHA-256")
            val totalBytes = source.length().coerceAtLeast(1)
            var processedBytes = 0L

            DataOutputStream(target.outputStream().buffered()).use { out ->
                out.write(MAGIC)
                out.writeByte(VERSION.toInt())
                out.writeInt(CHUNK_SIZE)
                out.writeShort(wrappedKey.size)
                out.write(wrappedKey)
                out.write(noncePrefix)

                val metadata = VaultFileMetadata(
                    formatVersion = VERSION.toInt(),
                    uuid = uuid,
                    createdAtEpochMs = createdAtEpochMs,
                    audioFormat = audioFormat,
                    chunkSizeBytes = CHUNK_SIZE,
                    cipher = CIPHER_NAME,
                )
                val metaPlain = json.encodeToString(metadata).encodeToByteArray()
                val metaSealed = metaCipher(Cipher.ENCRYPT_MODE, key, noncePrefix, VERSION)
                    .doFinal(metaPlain)
                out.writeInt(metaSealed.size)
                out.write(metaSealed)

                PushbackInputStream(source.inputStream().buffered(), 1).use { input ->
                    val chunk = ByteArray(CHUNK_SIZE)
                    var counter = 0
                    while (true) {
                        val read = readFully(input, chunk)
                        val peek = input.read()
                        val isFinal = peek == -1
                        if (!isFinal) input.unread(peek)

                        digest.update(chunk, 0, read)
                        val cipher =
                            chunkCipher(Cipher.ENCRYPT_MODE, key, noncePrefix, counter, isFinal, VERSION)
                        val sealed = cipher.doFinal(chunk, 0, read)
                        out.writeInt(sealed.size)
                        out.write(sealed)

                        processedBytes += read
                        onProgress((processedBytes.toFloat() / totalBytes).coerceIn(0f, 1f))
                        counter++
                        if (isFinal) break
                    }
                }
            }
            return digest.digest()
        } finally {
            dek.fill(0)
        }
    }

    /**
     * Decrypt [source] end-to-end, comparing against [expectedSha256].
     * Exercises the metadata frame, every GCM tag, and the
     * anti-truncation final flag; any corruption returns false.
     */
    fun verify(source: File, expectedSha256: ByteArray, onProgress: (Float) -> Unit = {}): Boolean {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val totalPlain = source.length().coerceAtLeast(1)
            var processedBytes = 0L
            decryptingStream(source).use { input ->
                val buffer = ByteArray(64 * 1024)
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    digest.update(buffer, 0, read)
                    processedBytes += read
                    onProgress((processedBytes.toFloat() / totalPlain).coerceIn(0f, 1f))
                }
            }
            MessageDigest.isEqual(expectedSha256, digest.digest())
        } catch (e: Exception) {
            false
        }
    }

    /** The sealed metadata of a vault file (v1 files return null). */
    @Throws(IOException::class)
    fun readMetadata(source: File): VaultFileMetadata? {
        val pushback = PushbackInputStream(source.inputStream().buffered(), 1)
        val data = DataInputStream(pushback)
        data.use {
            val header = readHeader(data)
            return header.metadata
        }
    }

    /**
     * A streaming plaintext view of an encrypted vault file. Used by
     * verification now and the playback DataSource in Phase 6.
     */
    @Throws(IOException::class)
    fun decryptingStream(source: File): InputStream {
        val pushback = PushbackInputStream(source.inputStream().buffered(), 1)
        val data = DataInputStream(pushback)
        try {
            val header = readHeader(data)
            return ChunkedDecryptingStream(
                pushback = pushback,
                data = data,
                key = header.key,
                noncePrefix = header.noncePrefix,
                chunkSize = header.chunkSize,
                version = header.version,
            )
        } catch (e: Exception) {
            data.close()
            throw if (e is IOException) e else IOException("Failed to open vault file", e)
        }
    }

    private class Header(
        val version: Byte,
        val chunkSize: Int,
        val key: SecretKey,
        val noncePrefix: ByteArray,
        val metadata: VaultFileMetadata?,
    )

    /** Reads and validates everything before the first content chunk. */
    private fun readHeader(data: DataInputStream): Header {
        val magic = ByteArray(MAGIC.size)
        data.readFully(magic)
        if (!magic.contentEquals(MAGIC)) throw IOException("Not a vault file")
        val version = data.readByte()
        if (version < 1 || version > VERSION) throw IOException("Unsupported vault format")
        val chunkSize = data.readInt()
        if (chunkSize <= 0 || chunkSize > MAX_CHUNK_SIZE) throw IOException("Corrupt header")
        val wrappedKey = ByteArray(data.readUnsignedShort())
        data.readFully(wrappedKey)
        val noncePrefix = ByteArray(NONCE_PREFIX_BYTES)
        data.readFully(noncePrefix)

        val dek = keystore.open(KeystoreManager.ALIAS_VAULT_FILES, wrappedKey)
        val key: SecretKey = SecretKeySpec(dek, "AES")
        dek.fill(0)

        val metadata = if (version >= 2) {
            val metaLength = data.readInt()
            if (metaLength <= 0 || metaLength > MAX_META_BYTES) throw IOException("Corrupt metadata frame")
            val sealed = ByteArray(metaLength)
            data.readFully(sealed)
            val plain = try {
                metaCipher(Cipher.DECRYPT_MODE, key, noncePrefix, version).doFinal(sealed)
            } catch (e: Exception) {
                throw IOException("Vault metadata failed authentication", e)
            }
            json.decodeFromString<VaultFileMetadata>(plain.decodeToString())
        } else {
            null
        }
        return Header(version, chunkSize, key, noncePrefix, metadata)
    }

    /**
     * Random-access plaintext view for playback. Constant-size chunk
     * frames make chunk offsets computable, so seeking decrypts only
     * the chunk that covers the requested position. One decrypted
     * chunk is held at a time and zeroed on [VaultAudioReader.close].
     */
    @Throws(IOException::class)
    fun openRandomAccess(source: File): VaultAudioReader {
        // Parse the header with the sequential reader logic, tracking
        // how many bytes it consumed.
        val raf = java.io.RandomAccessFile(source, "r")
        try {
            val magic = ByteArray(MAGIC.size)
            raf.readFully(magic)
            if (!magic.contentEquals(MAGIC)) throw IOException("Not a vault file")
            val version = raf.readByte()
            if (version < 1 || version > VERSION) throw IOException("Unsupported vault format")
            val chunkSize = raf.readInt()
            if (chunkSize <= 0 || chunkSize > MAX_CHUNK_SIZE) throw IOException("Corrupt header")
            val wrappedKey = ByteArray(raf.readUnsignedShort())
            raf.readFully(wrappedKey)
            val noncePrefix = ByteArray(NONCE_PREFIX_BYTES)
            raf.readFully(noncePrefix)

            val dek = keystore.open(KeystoreManager.ALIAS_VAULT_FILES, wrappedKey)
            val key: SecretKey = SecretKeySpec(dek, "AES")
            dek.fill(0)

            if (version >= 2) {
                val metaLength = raf.readInt()
                if (metaLength <= 0 || metaLength > MAX_META_BYTES) {
                    throw IOException("Corrupt metadata frame")
                }
                val sealedMeta = ByteArray(metaLength)
                raf.readFully(sealedMeta)
                try {
                    metaCipher(Cipher.DECRYPT_MODE, key, noncePrefix, version).doFinal(sealedMeta)
                } catch (e: Exception) {
                    throw IOException("Vault metadata failed authentication", e)
                }
            }

            val dataStart = raf.filePointer
            val frameFull = 4L + chunkSize + GCM_TAG_BITS / 8
            val totalChunkBytes = raf.length() - dataStart
            if (totalChunkBytes <= 0) throw IOException("Vault file has no audio")
            val chunkCount = ((totalChunkBytes + frameFull - 1) / frameFull).toInt()
            val finalFrameBytes = totalChunkBytes - (chunkCount - 1) * frameFull
            val finalPlainBytes = finalFrameBytes - 4 - GCM_TAG_BITS / 8
            if (finalPlainBytes < 0) throw IOException("Corrupt chunk framing")
            val plaintextLength = (chunkCount - 1).toLong() * chunkSize + finalPlainBytes

            return VaultAudioReader(
                raf = raf,
                key = key,
                noncePrefix = noncePrefix,
                chunkSize = chunkSize,
                version = version,
                dataStart = dataStart,
                frameFull = frameFull,
                chunkCount = chunkCount,
                plaintextLength = plaintextLength,
            )
        } catch (e: Exception) {
            raf.close()
            throw if (e is IOException) e else IOException("Failed to open vault file", e)
        }
    }

    /** See [openRandomAccess]. Not thread-safe; one reader per player. */
    inner class VaultAudioReader internal constructor(
        private val raf: java.io.RandomAccessFile,
        private val key: SecretKey,
        private val noncePrefix: ByteArray,
        private val chunkSize: Int,
        private val version: Byte,
        private val dataStart: Long,
        private val frameFull: Long,
        private val chunkCount: Int,
        val plaintextLength: Long,
    ) : java.io.Closeable {

        private var loadedIndex = -1
        private var loadedPlain: ByteArray = EMPTY

        fun readAt(position: Long, buffer: ByteArray, offset: Int, length: Int): Int {
            if (position >= plaintextLength) return -1
            val index = (position / chunkSize).toInt()
            if (index != loadedIndex) loadChunk(index)
            val within = (position - index.toLong() * chunkSize).toInt()
            val available = loadedPlain.size - within
            if (available <= 0) return -1
            val count = minOf(length, available)
            System.arraycopy(loadedPlain, within, buffer, offset, count)
            return count
        }

        private fun loadChunk(index: Int) {
            if (index < 0 || index >= chunkCount) throw IOException("Chunk out of range")
            raf.seek(dataStart + index * frameFull)
            val sealedLength = raf.readInt()
            if (sealedLength < GCM_TAG_BITS / 8 || sealedLength > chunkSize + GCM_TAG_BITS / 8) {
                throw IOException("Corrupt chunk frame")
            }
            val sealed = ByteArray(sealedLength)
            raf.readFully(sealed)
            val isFinal = index == chunkCount - 1
            val plain = try {
                chunkCipher(Cipher.DECRYPT_MODE, key, noncePrefix, index, isFinal, version)
                    .doFinal(sealed)
            } catch (e: Exception) {
                throw IOException("Vault file failed authentication", e)
            }
            loadedPlain.fill(0)
            loadedPlain = plain
            loadedIndex = index
        }

        override fun close() {
            loadedPlain.fill(0)
            loadedPlain = EMPTY
            loadedIndex = -1
            raf.close()
        }
    }

    private fun metaCipher(
        mode: Int,
        key: SecretKey,
        noncePrefix: ByteArray,
        version: Byte,
    ): Cipher = gcm(mode, key, noncePrefix, META_COUNTER, byteArrayOf(version, AAD_METADATA))

    private fun chunkCipher(
        mode: Int,
        key: SecretKey,
        noncePrefix: ByteArray,
        counter: Int,
        isFinal: Boolean,
        version: Byte,
    ): Cipher = gcm(mode, key, noncePrefix, counter, byteArrayOf(version, if (isFinal) 1 else 0))

    private fun gcm(
        mode: Int,
        key: SecretKey,
        noncePrefix: ByteArray,
        counter: Int,
        aad: ByteArray,
    ): Cipher {
        val iv = ByteBuffer.allocate(GCM_IV_BYTES)
            .put(noncePrefix)
            .putInt(counter)
            .array()
        return Cipher.getInstance(TRANSFORMATION).apply {
            init(mode, key, GCMParameterSpec(GCM_TAG_BITS, iv))
            updateAAD(aad)
        }
    }

    private fun readFully(input: InputStream, buffer: ByteArray): Int {
        var offset = 0
        while (offset < buffer.size) {
            val read = input.read(buffer, offset, buffer.size - offset)
            if (read < 0) break
            offset += read
        }
        return offset
    }

    /**
     * Sequential chunk reader. [pushback] and [data] wrap the same
     * underlying stream; [pushback] provides the one-byte lookahead
     * that detects the final chunk.
     */
    private inner class ChunkedDecryptingStream(
        private val pushback: PushbackInputStream,
        private val data: DataInputStream,
        private val key: SecretKey,
        private val noncePrefix: ByteArray,
        private val chunkSize: Int,
        private val version: Byte,
    ) : InputStream() {

        private var plain: ByteArray = EMPTY
        private var position = 0
        private var counter = 0
        private var finalSeen = false

        override fun read(): Int {
            val single = ByteArray(1)
            val read = read(single, 0, 1)
            return if (read < 0) -1 else single[0].toInt() and 0xFF
        }

        override fun read(b: ByteArray, off: Int, len: Int): Int {
            if (len == 0) return 0
            while (position >= plain.size) {
                if (!loadNextChunk()) return -1
            }
            val count = minOf(len, plain.size - position)
            System.arraycopy(plain, position, b, off, count)
            position += count
            return count
        }

        private fun loadNextChunk(): Boolean {
            if (finalSeen) return false
            val sealedLength = try {
                data.readInt()
            } catch (e: EOFException) {
                // The final-chunk flag was never reached — truncated file.
                throw IOException("Vault file truncated")
            }
            if (sealedLength < GCM_TAG_BITS / 8 || sealedLength > chunkSize + GCM_TAG_BITS / 8) {
                throw IOException("Corrupt chunk frame")
            }
            val sealed = ByteArray(sealedLength)
            data.readFully(sealed)

            // Final iff nothing follows this frame.
            val peek = pushback.read()
            val isFinal = peek == -1
            if (!isFinal) pushback.unread(peek)

            plain = try {
                chunkCipher(Cipher.DECRYPT_MODE, key, noncePrefix, counter, isFinal, version)
                    .doFinal(sealed)
            } catch (e: Exception) {
                throw IOException("Vault file failed authentication", e)
            }
            position = 0
            counter++
            finalSeen = isFinal
            return true
        }

        override fun close() {
            data.close()
        }
    }

    companion object {
        private val MAGIC = byteArrayOf(
            'N'.code.toByte(),
            'S'.code.toByte(),
            'V'.code.toByte(),
            '1'.code.toByte(),
        )
        private const val VERSION: Byte = 2
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val CIPHER_NAME = "AES-256-GCM"
        private const val DEK_BYTES = 32
        private const val NONCE_PREFIX_BYTES = 8
        private const val GCM_IV_BYTES = 12
        private const val GCM_TAG_BITS = 128
        private const val CHUNK_SIZE = 512 * 1024
        private const val MAX_CHUNK_SIZE = 4 * 1024 * 1024
        private const val MAX_META_BYTES = 64 * 1024

        /** Reserved nonce counter for the metadata frame. */
        private const val META_COUNTER = -1

        /** AAD domain marker separating metadata from content chunks. */
        private const val AAD_METADATA: Byte = 2

        private val EMPTY = ByteArray(0)
    }
}
