package com.nsvault.app.core.common

/**
 * Compact waveform previews: levels in 0..1 quantized to one byte per
 * point. Stored with each recording so the player can show the real
 * shape of the audio without decoding it.
 */
object WaveformPreview {

    const val POINTS = 240

    /** Downsample by bucket-max (peaks matter more than averages). */
    fun encode(levels: List<Float>, points: Int = POINTS): ByteArray {
        if (levels.isEmpty()) return ByteArray(0)
        val out = ByteArray(minOf(points, levels.size))
        val bucketSize = levels.size.toFloat() / out.size
        for (i in out.indices) {
            val start = (i * bucketSize).toInt()
            val end = (((i + 1) * bucketSize).toInt()).coerceAtMost(levels.size)
            var peak = 0f
            for (j in start until maxOf(end, start + 1)) {
                if (levels[j] > peak) peak = levels[j]
            }
            out[i] = (peak.coerceIn(0f, 1f) * 255f).toInt().toByte()
        }
        return out
    }

    fun decode(bytes: ByteArray?): FloatArray? {
        if (bytes == null || bytes.isEmpty()) return null
        return FloatArray(bytes.size) { i -> (bytes[i].toInt() and 0xFF) / 255f }
    }
}
