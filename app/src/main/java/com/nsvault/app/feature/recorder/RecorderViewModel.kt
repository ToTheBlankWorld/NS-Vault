package com.nsvault.app.feature.recorder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nsvault.app.core.common.VaultLog
import com.nsvault.app.domain.model.RecorderState
import com.nsvault.app.domain.model.StorageWarning
import com.nsvault.app.domain.repository.RecorderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RecorderViewModel @Inject constructor(
    private val recorder: RecorderRepository,
) : ViewModel() {

    val state: StateFlow<RecorderState> = recorder.state
    val elapsedMillis: StateFlow<Long> = recorder.elapsedMillis
    val level: StateFlow<Float> = recorder.level
    val storageWarning: StateFlow<StorageWarning?> = recorder.storageWarning
    val saveProgress: StateFlow<Float?> = recorder.saveProgress

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun start() {
        _error.value = null
        val currentState = state.value
        if (currentState != RecorderState.Idle) {
            if (currentState is RecorderState.Failed || currentState is RecorderState.Completed) {
                recorder.acknowledgeResult()
            } else {
                return
            }
        }
        viewModelScope.launch {
            try {
                recorder.start()
            } catch (e: Exception) {
                VaultLog.e("RecorderVM", "Failed to start recording", e)
                _error.value = "Couldn't start recording: ${e.localizedMessage ?: "Unknown error"}"
            }
        }
    }

    fun pause() = try { recorder.pause() } catch (e: Exception) { VaultLog.e("RecorderVM", "Pause failed", e) }

    fun resume() = try { recorder.resume() } catch (e: Exception) { VaultLog.e("RecorderVM", "Resume failed", e) }

    fun stop() = try { recorder.stop() } catch (e: Exception) { VaultLog.e("RecorderVM", "Stop failed", e) }

    fun finalizeRecording(title: String) = try {
        recorder.finalizeRecording(title)
    } catch (e: Exception) {
        VaultLog.e("RecorderVM", "Finalize failed", e)
        _error.value = "Couldn't save recording: ${e.localizedMessage ?: "Unknown error"}"
    }

    fun cancel() = try { recorder.cancel() } catch (e: Exception) { VaultLog.e("RecorderVM", "Cancel failed", e) }

    fun acknowledgeResult() = recorder.acknowledgeResult()

    fun clearError() { _error.value = null }
}
