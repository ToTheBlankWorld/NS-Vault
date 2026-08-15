package com.nsvault.app.domain.model

/**
 * Lifecycle of a recording's metadata. A row is written the moment
 * recording begins, so no capture can ever become orphaned history.
 */
enum class RecordingStatus {
    /** In progress — or interrupted and awaiting a recovery decision. */
    RECORDING,

    /** Saved through the normal stop pipeline. */
    COMPLETED,

    /** Salvaged by crash recovery. */
    RECOVERED,

    /** Yielded no audio, or was explicitly discarded by the user. */
    FAILED,
}

/** Stages reported by the save pipeline, each with 0..1 progress. */
enum class SaveStage {
    /** Remuxing the capture into its final container. */
    Preparing,

    Encrypting,

    /** Round-trip decryption check of the written vault file. */
    Verifying,

    /** Metadata commit and secure cleanup. */
    Finishing,
}
