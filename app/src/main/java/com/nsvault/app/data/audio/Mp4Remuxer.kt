package com.nsvault.app.data.audio

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import com.nsvault.app.core.common.VaultLog
import java.io.File
import java.nio.ByteBuffer
import javax.inject.Inject

/**
 * Losslessly repackages an AAC-ADTS capture into an MPEG-4 (.m4a)
 * container — a stream copy, no re-encode. Capturing in ADTS is what
 * makes crash recovery real: a truncated ADTS stream stays decodable,
 * while a crashed MPEG-4 capture would be corrupt.
 */
class Mp4Remuxer @Inject constructor() {

    /** Returns true on success; on failure [target] is cleaned up. */
    fun remux(source: File, target: File, onProgress: (Float) -> Unit = {}): Boolean {
        var extractor: MediaExtractor? = null
        var muxer: MediaMuxer? = null
        return try {
            extractor = MediaExtractor().apply { setDataSource(source.absolutePath) }
            val trackIndex = (0 until extractor.trackCount).firstOrNull { index ->
                extractor.getTrackFormat(index)
                    .getString(MediaFormat.KEY_MIME)
                    ?.startsWith("audio/") == true
            } ?: return false

            val format = extractor.getTrackFormat(trackIndex)
            val durationUs = if (format.containsKey(MediaFormat.KEY_DURATION)) {
                format.getLong(MediaFormat.KEY_DURATION).coerceAtLeast(1)
            } else {
                0L
            }
            extractor.selectTrack(trackIndex)

            muxer = MediaMuxer(target.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val outputTrack = muxer.addTrack(format)
            muxer.start()

            val buffer = ByteBuffer.allocate(BUFFER_SIZE)
            val info = MediaCodec.BufferInfo()
            while (true) {
                val sampleSize = extractor.readSampleData(buffer, 0)
                if (sampleSize < 0) break
                info.set(0, sampleSize, extractor.sampleTime, MediaCodec.BUFFER_FLAG_KEY_FRAME)
                muxer.writeSampleData(outputTrack, buffer, info)
                if (durationUs > 0) {
                    onProgress((extractor.sampleTime.toFloat() / durationUs).coerceIn(0f, 1f))
                }
                extractor.advance()
            }
            muxer.stop()
            true
        } catch (e: Exception) {
            VaultLog.e(TAG, "Remux failed", e)
            runCatching { muxer?.stop() }
            target.delete()
            false
        } finally {
            runCatching { muxer?.release() }
            runCatching { extractor?.release() }
        }
    }

    companion object {
        private const val TAG = "Remuxer"
        private const val BUFFER_SIZE = 256 * 1024
    }
}
