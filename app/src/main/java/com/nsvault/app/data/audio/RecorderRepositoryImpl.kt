package com.nsvault.app.data.audio

import com.nsvault.app.core.common.VaultLog
import com.nsvault.app.domain.model.AbandonedCapture
import com.nsvault.app.domain.model.RecorderState
import com.nsvault.app.domain.model.StorageWarning
import com.nsvault.app.domain.repository.RecorderRepository
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecorderRepositoryImpl @Inject constructor(
    private val engine: RecordingEngine,
) : RecorderRepository {

    override val state: StateFlow<RecorderState> = engine.state
    override val elapsedMillis: StateFlow<Long> = engine.elapsedMillis
    override val level: StateFlow<Float> = engine.level
    override val storageWarning: StateFlow<StorageWarning?> = engine.storageWarning
    override val saveProgress: StateFlow<Float?> = engine.saveProgress

    /** Start recording in-process. The foreground service was removed because Android 16+ enforces restrictions that cause same-process crashes. */
    override fun start() {
        engine.start()
    }

    override fun pause() = engine.pause()

    override fun resume() = engine.resume()

    override fun stop() = engine.stop()

    override fun finalizeRecording(title: String) = engine.finalizeWithTitle(title)

    override fun cancel() = engine.cancel()

    override fun acknowledgeResult() = engine.acknowledgeResult()

    override suspend fun findAbandonedCapture(): AbandonedCapture? =
        engine.findAbandonedCapture()

    override suspend fun recoverCapture(capture: AbandonedCapture): Long? =
        engine.recoverCapture(capture)

    override suspend fun discardCapture(capture: AbandonedCapture) =
        engine.discardCapture(capture)
}
