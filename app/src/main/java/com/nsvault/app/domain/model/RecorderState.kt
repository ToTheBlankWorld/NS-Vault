package com.nsvault.app.domain.model

/**
 * The recording engine's observable state machine. The UI renders
 * these states and issues commands through the repository — it never
 * touches the engine or MediaRecorder directly.
 */
sealed interface RecorderState {
    data object Idle : RecorderState
    data object Preparing : RecorderState
    data object Recording : RecorderState
    data object Paused : RecorderState
    data object Stopping : RecorderState
    data object AwaitingTitle : RecorderState
    data object Encrypting : RecorderState
    data object Saving : RecorderState
    data class Completed(val recordingId: Long) : RecorderState
    data class Failed(val error: RecorderError) : RecorderState

    companion object {
        val RecorderState.isCapturing: Boolean
            get() = this is Recording || this is Paused

        val RecorderState.isFinalizing: Boolean
            get() = this is Stopping || this is Encrypting || this is Saving
    }
}

enum class RecorderError {
    MicrophoneUnavailable,
    StorageFull,
    CaptureFailed,
    SaveFailed,
}

enum class StorageWarning {
    /** Roomy enough to continue, but the user should know. */
    Low,

    /** Recording was stopped and saved automatically to avoid data loss. */
    Critical,
}

/**
 * A capture left behind by a crash or forced kill, found on launch.
 * Always surfaced to the user — never silently deleted. Identified by
 * the UUID assigned when its recording began, so recovering preserves
 * the original metadata row.
 */
data class AbandonedCapture(
    val uuid: String,
    val createdAtEpochMs: Long,
    val sizeBytes: Long,
)
