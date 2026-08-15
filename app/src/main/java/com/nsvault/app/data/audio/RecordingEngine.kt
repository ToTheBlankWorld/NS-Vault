package com.nsvault.app.data.audio

import android.content.Context
import android.media.MediaMetadataRetriever
import android.media.MediaRecorder
import android.os.Build
import android.os.PowerManager
import android.os.SystemClock
import com.nsvault.app.core.common.VaultLog
import com.nsvault.app.core.common.WaveformPreview
import com.nsvault.app.core.di.DefaultDispatcher
import com.nsvault.app.core.di.IoDispatcher
import com.nsvault.app.data.vault.VaultFileStore
import com.nsvault.app.domain.model.AbandonedCapture
import com.nsvault.app.domain.model.AudioQuality
import com.nsvault.app.domain.model.RecorderError
import com.nsvault.app.domain.model.RecorderState
import com.nsvault.app.domain.model.RecorderState.Companion.isCapturing
import com.nsvault.app.domain.model.SaveStage
import com.nsvault.app.domain.model.StorageWarning
import com.nsvault.app.domain.repository.SettingsRepository
import com.nsvault.app.domain.repository.VaultRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

/**
 * The single owner of MediaRecorder and the recording state machine.
 *
 * Lives in the application scope — Activity recreation, rotation, and
 * backgrounding cannot touch it. Every recording is identified by a
 * UUID minted the moment it starts; its metadata row exists from that
 * same moment, so no capture can ever be orphaned.
 *
 * Every command and background loop runs on a single-thread dispatcher,
 * so MediaRecorder calls are never concurrent and command handlers
 * (start/pause/resume/stop/cancel) can never interleave — e.g. a
 * user-triggered [cancel] and a storage-triggered [stop] are serialized
 * rather than both passing their guards. Heavy work (encryption) is
 * delegated to the IO dispatcher and never blocks this thread.
 */
@Singleton
class RecordingEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val fileStore: VaultFileStore,
    private val vaultRepository: VaultRepository,
    private val settingsRepository: SettingsRepository,
    @DefaultDispatcher defaultDispatcher: CoroutineDispatcher,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {

    private val scope = CoroutineScope(SupervisorJob() + defaultDispatcher.limitedParallelism(1))

    private val _state = MutableStateFlow<RecorderState>(RecorderState.Idle)
    val state: StateFlow<RecorderState> = _state.asStateFlow()

    private val _elapsedMillis = MutableStateFlow(0L)
    val elapsedMillis: StateFlow<Long> = _elapsedMillis.asStateFlow()

    private val _level = MutableStateFlow(0f)
    val level: StateFlow<Float> = _level.asStateFlow()

    private val _storageWarning = MutableStateFlow<StorageWarning?>(null)
    val storageWarning: StateFlow<StorageWarning?> = _storageWarning.asStateFlow()

    /** 0..1 during the save pipeline, null otherwise. */
    private val _saveProgress = MutableStateFlow<Float?>(null)
    val saveProgress: StateFlow<Float?> = _saveProgress.asStateFlow()

    private var recorder: MediaRecorder? = null
    private var currentUuid: String? = null
    private var captureFile: File? = null
    private var captureStartedAtEpochMs = 0L

    /** Recorded time in completed segments (excludes the live segment). */
    private var accumulatedMs = 0L
    private var segmentStartElapsed = 0L

    private var tickerJob: Job? = null
    private var meterJob: Job? = null
    private var storageJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null

    /** Live levels kept for the stored waveform preview; capped by halving. */
    private val waveformSamples = ArrayList<Float>()

    fun start() {
        scope.launch {
            if (_state.value != RecorderState.Idle) return@launch
            _state.value = RecorderState.Preparing
            if (fileStore.availableBytes() < MIN_START_BYTES) {
                _state.value = RecorderState.Failed(RecorderError.StorageFull)
                return@launch
            }
            val uuid = UUID.randomUUID().toString()
            val startedAt = System.currentTimeMillis()
            val file = fileStore.captureFileFor(uuid)
            val quality = runCatching { settingsRepository.audioQuality.first() }
                .getOrDefault(AudioQuality.HIGH)
            try {
                val mediaRecorder = createRecorder().apply {
                    setAudioSource(MediaRecorder.AudioSource.MIC)
                    setOutputFormat(MediaRecorder.OutputFormat.AAC_ADTS)
                    setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                    setAudioSamplingRate(SAMPLE_RATE_HZ)
                    setAudioEncodingBitRate(quality.bitRate)
                    setAudioChannels(1)
                    setOutputFile(file.absolutePath)
                    prepare()
                    start()
                }
                recorder = mediaRecorder
                currentUuid = uuid
                captureFile = file
                captureStartedAtEpochMs = startedAt
                accumulatedMs = 0
                segmentStartElapsed = SystemClock.elapsedRealtime()
                _elapsedMillis.value = 0
                _storageWarning.value = null
                waveformSamples.clear()
                acquireWakeLock()

                // Metadata exists from second zero — status RECORDING.
                runCatching { vaultRepository.beginRecording(uuid, startedAt) }
                    .onFailure { VaultLog.e(TAG, "Failed to write initial metadata", it) }

                _state.value = RecorderState.Recording
                startTicker()
                startMeter()
                startStorageMonitor()
            } catch (e: Exception) {
                VaultLog.e(TAG, "Failed to start capture", e)
                releaseRecorder()
                file.delete()
                runCatching { vaultRepository.markFailed(uuid) }
                _state.value = RecorderState.Failed(RecorderError.MicrophoneUnavailable)
            }
        }
    }

    fun pause() {
        scope.launch {
            if (_state.value != RecorderState.Recording) return@launch
            try {
                recorder?.pause()
                accumulatedMs += SystemClock.elapsedRealtime() - segmentStartElapsed
                _elapsedMillis.value = accumulatedMs
                _level.value = 0f
                _state.value = RecorderState.Paused
            } catch (e: Exception) {
                VaultLog.e(TAG, "Pause failed", e)
            }
        }
    }

    fun resume() {
        scope.launch {
            if (_state.value != RecorderState.Paused) return@launch
            try {
                recorder?.resume()
                segmentStartElapsed = SystemClock.elapsedRealtime()
                _state.value = RecorderState.Recording
            } catch (e: Exception) {
                VaultLog.e(TAG, "Resume failed", e)
            }
        }
    }

    fun stop() {
        scope.launch {
            if (!_state.value.isCapturing) return@launch
            _state.value = RecorderState.Stopping
            haltCapture()
        }
    }

    fun finalizeWithTitle(title: String) {
        scope.launch {
            if (_state.value != RecorderState.AwaitingTitle) return@launch
            val uuid = currentUuid
            if (uuid != null) {
                runCatching { vaultRepository.rename(uuid, title) }
            }
            finishCapture()
        }
    }

    fun cancel() {
        scope.launch {
            val canCancel = _state.value.isCapturing || _state.value is RecorderState.AwaitingTitle
            if (!canCancel) return@launch
            stopJobs()
            releaseRecorder()
            captureFile?.delete()
            captureFile = null
            val uuid = currentUuid
            currentUuid = null
            if (uuid != null) runCatching { vaultRepository.markFailed(uuid) }
            releaseWakeLock()
            _elapsedMillis.value = 0
            _level.value = 0f
            _state.value = RecorderState.Idle
        }
    }

    fun acknowledgeResult() {
        scope.launch {
            val current = _state.value
            if (current is RecorderState.Completed || current is RecorderState.Failed) {
                _state.value = RecorderState.Idle
                _elapsedMillis.value = 0
                _storageWarning.value = null
                _saveProgress.value = null
            }
        }
    }

    /**
     * DB-driven scan: RECORDING-status rows whose capture file still
     * exists. Rows whose file vanished are closed as FAILED; capture
     * files with no row (edge case) still surface via their filename
     * UUID so nothing is ever silently lost.
     */
    suspend fun findAbandonedCapture(): AbandonedCapture? = withContext(ioDispatcher) {
        if (_state.value != RecorderState.Idle) return@withContext null
        val activeUuid = currentUuid

        val fromDb = vaultRepositoryInProgress()
            .filter { it.uuid != activeUuid }
            .mapNotNull { row ->
                val file = fileStore.captureFileFor(row.uuid)
                if (file.exists() && file.length() > 0) {
                    AbandonedCapture(
                        uuid = row.uuid,
                        createdAtEpochMs = row.createdAtEpochMs,
                        sizeBytes = file.length(),
                    )
                } else {
                    runCatching { vaultRepository.markFailed(row.uuid) }
                    null
                }
            }

        val knownUuids = fromDb.map { it.uuid }.toSet()
        val orphans = fileStore.listCaptures()
            .filter { file ->
                val uuid = file.nameWithoutExtension
                uuid != activeUuid && uuid !in knownUuids
            }
            .map { file ->
                AbandonedCapture(
                    uuid = file.nameWithoutExtension,
                    createdAtEpochMs = file.lastModified(),
                    sizeBytes = file.length(),
                )
            }

        (fromDb + orphans).maxByOrNull { it.createdAtEpochMs }
    }

    suspend fun recoverCapture(capture: AbandonedCapture): Long? = withContext(ioDispatcher) {
        val file = fileStore.captureFileFor(capture.uuid)
        if (!file.exists()) return@withContext null
        val duration = probeDurationMs(file) ?: 0L
        try {
            vaultRepository.finalizeCapture(
                uuid = capture.uuid,
                capture = file,
                createdAtEpochMs = capture.createdAtEpochMs,
                durationMs = duration,
                recovered = true,
                waveformPreview = null,
                onEvent = { _, _ -> },
            )
        } catch (e: Exception) {
            VaultLog.e(TAG, "Capture recovery failed", e)
            null
        }
    }

    suspend fun discardCapture(capture: AbandonedCapture): Unit = withContext(ioDispatcher) {
        fileStore.captureFileFor(capture.uuid).delete()
        runCatching { vaultRepository.markFailed(capture.uuid) }
    }

    private suspend fun haltCapture() {
        stopJobs()
        _level.value = 0f
        if (_state.value == RecorderState.Recording) {
            accumulatedMs += SystemClock.elapsedRealtime() - segmentStartElapsed
        }
        _elapsedMillis.value = accumulatedMs

        val stoppedCleanly = try {
            recorder?.stop()
            true
        } catch (e: Exception) {
            VaultLog.e(TAG, "Recorder stop failed", e)
            false
        }
        releaseRecorder()
        releaseWakeLock()

        val file = captureFile
        val uuid = currentUuid
        if (file == null || uuid == null || !stoppedCleanly || file.length() == 0L) {
            file?.delete()
            captureFile = null
            currentUuid = null
            if (uuid != null) runCatching { vaultRepository.markFailed(uuid) }
            _state.value = RecorderState.Failed(RecorderError.CaptureFailed)
            return
        }

        _state.value = RecorderState.AwaitingTitle
    }

    private suspend fun finishCapture() {
        val file = captureFile ?: return
        val uuid = currentUuid ?: return
        val durationMs = _elapsedMillis.value
        captureFile = null
        currentUuid = null

        try {
            val id = vaultRepository.finalizeCapture(
                uuid = uuid,
                capture = file,
                createdAtEpochMs = captureStartedAtEpochMs,
                durationMs = durationMs,
                recovered = false,
                waveformPreview = WaveformPreview.encode(waveformSamples),
                onEvent = ::onSaveEvent,
            )
            waveformSamples.clear()
            _saveProgress.value = null
            _state.value = RecorderState.Completed(id)
        } catch (e: Exception) {
            VaultLog.e(TAG, "Save pipeline failed", e)
            _saveProgress.value = null
            _state.value = RecorderState.Failed(RecorderError.SaveFailed)
        }
    }

    private fun onSaveEvent(stage: SaveStage, progress: Float) {
        _state.value = when (stage) {
            SaveStage.Preparing -> RecorderState.Stopping
            SaveStage.Encrypting -> RecorderState.Encrypting
            SaveStage.Verifying, SaveStage.Finishing -> RecorderState.Saving
        }
        _saveProgress.value = progress
    }

    private suspend fun vaultRepositoryInProgress() =
        runCatching { vaultRepository.inProgressRecordings() }.getOrDefault(emptyList())

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = scope.launch {
            while (isActive) {
                if (_state.value == RecorderState.Recording) {
                    _elapsedMillis.value =
                        accumulatedMs + (SystemClock.elapsedRealtime() - segmentStartElapsed)
                }
                delay(TICK_MS)
            }
        }
    }

    private fun startMeter() {
        meterJob?.cancel()
        meterJob = scope.launch {
            var smoothed = 0f
            while (isActive) {
                if (_state.value == RecorderState.Recording) {
                    val raw = runCatching { recorder?.maxAmplitude ?: 0 }.getOrDefault(0)
                    // Perceptual curve, then asymmetric smoothing: fast attack,
                    // gentle release — this is what keeps the waveform fluid.
                    val target = sqrt(raw / MAX_AMPLITUDE)
                    val alpha = if (target > smoothed) ATTACK else RELEASE
                    smoothed += alpha * (target - smoothed)
                    _level.value = smoothed.coerceIn(0f, 1f)

                    // Feed the stored preview; halve when large so memory
                    // stays bounded on multi-hour sessions.
                    waveformSamples.add(_level.value)
                    if (waveformSamples.size >= WAVEFORM_SAMPLE_CAP) {
                        for (i in 0 until waveformSamples.size / 2) {
                            waveformSamples[i] = maxOf(
                                waveformSamples[2 * i],
                                waveformSamples[2 * i + 1],
                            )
                        }
                        waveformSamples.subList(waveformSamples.size / 2, waveformSamples.size).clear()
                    }
                }
                delay(METER_MS)
            }
        }
    }

    private fun startStorageMonitor() {
        storageJob?.cancel()
        storageJob = scope.launch {
            while (isActive) {
                val available = fileStore.availableBytes()
                when {
                    available < CRITICAL_BYTES && _state.value.isCapturing -> {
                        // Save what exists rather than let the capture die.
                        _storageWarning.value = StorageWarning.Critical
                        stop()
                        return@launch
                    }

                    available < LOW_BYTES -> _storageWarning.value = StorageWarning.Low

                    else -> _storageWarning.value = null
                }
                delay(STORAGE_CHECK_MS)
            }
        }
    }

    private fun stopJobs() {
        tickerJob?.cancel()
        meterJob?.cancel()
        storageJob?.cancel()
        tickerJob = null
        meterJob = null
        storageJob = null
    }

    private fun createRecorder(): MediaRecorder =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }

    private fun releaseRecorder() {
        runCatching { recorder?.reset() }
        runCatching { recorder?.release() }
        recorder = null
    }

    private fun acquireWakeLock() {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKELOCK_TAG)
            .apply { acquire(WAKELOCK_TIMEOUT_MS) }
    }

    private fun releaseWakeLock() {
        runCatching { if (wakeLock?.isHeld == true) wakeLock?.release() }
        wakeLock = null
    }

    private fun probeDurationMs(file: File): Long? = runCatching {
        MediaMetadataRetriever().use { retriever ->
            retriever.setDataSource(file.absolutePath)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong()
        }
    }.getOrNull()

    companion object {
        private const val TAG = "Engine"

        private const val SAMPLE_RATE_HZ = 44_100
        private const val MAX_AMPLITUDE = 32_767f

        private const val TICK_MS = 100L
        private const val METER_MS = 50L
        private const val ATTACK = 0.55f
        private const val RELEASE = 0.12f
        private const val WAVEFORM_SAMPLE_CAP = 120_000

        private const val STORAGE_CHECK_MS = 15_000L
        private const val MIN_START_BYTES = 100L * 1024 * 1024
        private const val LOW_BYTES = 200L * 1024 * 1024
        private const val CRITICAL_BYTES = 50L * 1024 * 1024

        private const val WAKELOCK_TAG = "nsvault:recording"

        /** Safety ceiling only; refreshed lifecycle keeps real sessions alive. */
        private const val WAKELOCK_TIMEOUT_MS = 6L * 60 * 60 * 1000
    }
}
