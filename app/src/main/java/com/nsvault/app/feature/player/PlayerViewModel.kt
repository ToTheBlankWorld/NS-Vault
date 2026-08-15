package com.nsvault.app.feature.player

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.navigation.toRoute
import com.nsvault.app.core.common.VaultLog
import com.nsvault.app.core.common.WaveformPreview
import com.nsvault.app.data.auth.SessionManager
import com.nsvault.app.data.playback.VaultDataSourceFactory
import com.nsvault.app.domain.model.Recording
import com.nsvault.app.domain.repository.VaultRepository
import com.nsvault.app.feature.auth.BiometricAuthenticator
import com.nsvault.app.navigation.Route
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@UnstableApi
@HiltViewModel
class PlayerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle,
    private val vaultRepository: VaultRepository,
    private val dataSourceFactory: VaultDataSourceFactory,
    private val sessionManager: SessionManager,
    private val biometricAuthenticator: BiometricAuthenticator,
) : ViewModel() {

    data class UiState(
        val recording: Recording? = null,
        val waveform: FloatArray? = null,
        val isPlaying: Boolean = false,
        val positionMs: Long = 0,
        val durationMs: Long = 0,
        val speed: Float = 1f,
        val isScrubbing: Boolean = false,
        val error: Boolean = false,
    ) {
        val progress: Float
            get() = if (durationMs > 0) {
                (positionMs.toFloat() / durationMs).coerceIn(0f, 1f)
            } else {
                0f
            }
    }

    private val route: Route.Player = savedStateHandle.toRoute()

    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()

    private var player: ExoPlayer? = null

    init {
        viewModelScope.launch {
            val recording = vaultRepository.getRecording(route.recordingId)
            if (recording == null || recording.relativePath.isEmpty()) {
                _uiState.update { it.copy(error = true) }
                return@launch
            }
            _uiState.update {
                it.copy(
                    recording = recording,
                    waveform = WaveformPreview.decode(recording.waveformPreview),
                    durationMs = recording.durationMs,
                )
            }
            setUpPlayer(recording, recording.resumePositionMs)
            startPositionTicker()
        }

        // The vault locking mid-playback silences it immediately.
        viewModelScope.launch {
            sessionManager.isUnlocked.collect { unlocked ->
                if (!unlocked) player?.pause()
            }
        }
    }

    private fun setUpPlayer(recording: Recording, resumePositionMs: Long = 0) {
        val extractors = DefaultExtractorsFactory().setConstantBitrateSeekingEnabled(true)
        val exoPlayer = ExoPlayer.Builder(context)
            .setMediaSourceFactory(ProgressiveMediaSource.Factory(dataSourceFactory, extractors))
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                /* handleAudioFocus = */ true,
            )
            .build()

        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _uiState.update { it.copy(isPlaying = isPlaying) }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    val duration = exoPlayer.duration
                    if (duration != C.TIME_UNSET && duration > 0) {
                        _uiState.update { it.copy(durationMs = duration) }
                    }
                }
                if (playbackState == Player.STATE_ENDED) {
                    exoPlayer.pause()
                    exoPlayer.seekTo(0)
                    val rec = _uiState.value.recording ?: return
                    viewModelScope.launch {
                        vaultRepository.saveResumePosition(rec.uuid, 0)
                    }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                VaultLog.e(TAG, "Playback failed")
                _uiState.update { it.copy(error = true) }
            }
        })

        exoPlayer.setMediaItem(
            MediaItem.fromUri(Uri.parse("nsvault:///${recording.relativePath.replace('\\', '/')}")),
        )
        exoPlayer.prepare()
        if (resumePositionMs > 0) {
            exoPlayer.seekTo(resumePositionMs)
        }
        player = exoPlayer
    }

    private fun startPositionTicker() {
        viewModelScope.launch {
            while (isActive) {
                val current = player
                if (current != null && !_uiState.value.isScrubbing) {
                    _uiState.update { it.copy(positionMs = current.currentPosition.coerceAtLeast(0)) }
                }
                delay(TICK_MS)
            }
        }
    }

    fun togglePlayPause() {
        val current = player ?: return
        if (current.isPlaying) {
            current.pause()
        } else {
            current.play()
        }
    }

    /** Pause when the screen leaves the foreground — saves battery and
     *  keeps audio from playing to an unattended, locked vault. */
    fun pausePlayback() {
        player?.pause()
        saveResumePosition()
    }

    private fun saveResumePosition() {
        val recording = _uiState.value.recording ?: return
        val position = player?.currentPosition?.coerceAtLeast(0) ?: return
        viewModelScope.launch {
            vaultRepository.saveResumePosition(recording.uuid, position)
        }
    }

    val canUseBiometric: Boolean
        get() = biometricAuthenticator.isAvailable

    fun authenticateForShare(
        activity: androidx.fragment.app.FragmentActivity,
        onAuthenticated: () -> Unit,
    ) {
        biometricAuthenticator.authenticate(
            activity = activity,
            title = "Confirm to share",
            negativeText = "Cancel",
            onSuccess = onAuthenticated,
        )
    }

    fun skip(deltaMs: Long) {
        val current = player ?: return
        val duration = _uiState.value.durationMs
        current.seekTo((current.currentPosition + deltaMs).coerceIn(0, duration))
    }

    fun onScrub(fraction: Float) {
        val duration = _uiState.value.durationMs
        _uiState.update {
            it.copy(isScrubbing = true, positionMs = (duration * fraction).toLong())
        }
    }

    fun onScrubEnd(fraction: Float) {
        val duration = _uiState.value.durationMs
        player?.seekTo((duration * fraction).toLong().coerceIn(0, duration))
        _uiState.update { it.copy(isScrubbing = false) }
    }

    fun cycleSpeed() {
        val current = player ?: return
        val next = SPEEDS[(SPEEDS.indexOf(_uiState.value.speed) + 1) % SPEEDS.size]
        current.setPlaybackSpeed(next)
        _uiState.update { it.copy(speed = next) }
    }

    /** Decrypt into the share cache; caller launches the share sheet. */
    suspend fun exportForSharing(): Uri? {
        val recording = _uiState.value.recording ?: return null
        player?.pause()
        return vaultRepository.exportForSharing(recording.uuid)
    }

    override fun onCleared() {
        saveResumePosition()
        player?.release()
        player = null
    }

    companion object {
        private const val TAG = "Player"
        private const val TICK_MS = 200L
        private val SPEEDS = listOf(1f, 1.25f, 1.5f, 2f, 0.75f)
    }
}
