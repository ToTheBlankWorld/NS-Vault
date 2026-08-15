package com.nsvault.app.domain.model

data class Recording(
    val id: Long,
    val uuid: String,
    val title: String,
    val createdAtEpochMs: Long,
    val durationMs: Long,
    val sizeBytes: Long,
    val relativePath: String,
    val isFavorite: Boolean,
    /** Quantized level preview (one byte per point); null for imports. */
    val waveformPreview: ByteArray? = null,
    val resumePositionMs: Long = 0,
)

data class VaultStats(
    val recordingCount: Int,
    val totalDurationMs: Long,
    val totalSizeBytes: Long,
) {
    companion object {
        val Empty = VaultStats(recordingCount = 0, totalDurationMs = 0, totalSizeBytes = 0)
    }
}
