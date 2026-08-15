package com.nsvault.app.domain.repository

import com.nsvault.app.domain.model.AbandonedCapture
import com.nsvault.app.domain.model.RecorderState
import com.nsvault.app.domain.model.StorageWarning
import kotlinx.coroutines.flow.StateFlow

/**
 * The UI's only doorway to the recording engine.
 */
interface RecorderRepository {

    val state: StateFlow<RecorderState>

    /** Recorded time in milliseconds, pause-aware, ticking while recording. */
    val elapsedMillis: StateFlow<Long>

    /** Smoothed microphone level in 0..1 for the live waveform. */
    val level: StateFlow<Float>

    val storageWarning: StateFlow<StorageWarning?>

    /** 0..1 progress of the current save-pipeline stage; null when idle. */
    val saveProgress: StateFlow<Float?>

    fun start()

    fun pause()

    fun resume()

    /** Stop and run the save pipeline (stop → encrypt → save → completed). */
    fun stop()

    /** Continue the save pipeline with a user-given title. Only valid after stop(). */
    fun finalizeRecording(title: String)

    /** Discard the in-progress capture. Only valid while capturing. */
    fun cancel()

    /** Return a terminal state (Completed/Failed) to Idle. */
    fun acknowledgeResult()

    /** A capture orphaned by a crash, if any (never while capturing). */
    suspend fun findAbandonedCapture(): AbandonedCapture?

    /** Salvage an orphaned capture into the vault. Null if unsalvageable. */
    suspend fun recoverCapture(capture: AbandonedCapture): Long?

    /** Delete an orphaned capture after explicit user confirmation. */
    suspend fun discardCapture(capture: AbandonedCapture)
}
