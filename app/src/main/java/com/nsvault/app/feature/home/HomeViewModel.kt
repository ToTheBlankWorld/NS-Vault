package com.nsvault.app.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nsvault.app.domain.model.AbandonedCapture
import com.nsvault.app.domain.model.RecorderState.Companion.isCapturing
import com.nsvault.app.domain.model.Recording
import com.nsvault.app.domain.model.VaultStats
import com.nsvault.app.domain.repository.RecorderRepository
import com.nsvault.app.domain.repository.VaultRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MonthGroup(
    val label: String,
    val recordings: List<Recording>,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val vaultRepository: VaultRepository,
    private val recorder: RecorderRepository,
) : ViewModel() {

    data class UiState(
        val stats: VaultStats = VaultStats.Empty,
        val recordings: List<Recording> = emptyList(),
        val groups: List<MonthGroup> = emptyList(),
        val recovery: AbandonedCapture? = null,
        val isRecording: Boolean = false,
    )

    private val recovery = MutableStateFlow<AbandonedCapture?>(null)

    val uiState: StateFlow<UiState> = combine(
        vaultRepository.stats,
        vaultRepository.recordings,
        recovery,
        recorder.state,
    ) { stats, recordings, pendingRecovery, recorderState ->
        UiState(
            stats = stats,
            recordings = recordings,
            groups = groupByMonth(recordings),
            recovery = pendingRecovery,
            isRecording = recorderState.isCapturing,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState())

    init {
        viewModelScope.launch {
            recovery.value = recorder.findAbandonedCapture()
        }
    }

    fun recoverCapture() {
        val capture = recovery.value ?: return
        viewModelScope.launch {
            recorder.recoverCapture(capture)
            recovery.value = null
        }
    }

    fun discardCapture() {
        val capture = recovery.value ?: return
        viewModelScope.launch {
            recorder.discardCapture(capture)
            recovery.value = null
        }
    }

    fun dismissRecovery() {
        recovery.value = null
    }

    fun toggleFavorite(recording: Recording) {
        viewModelScope.launch {
            vaultRepository.setFavorite(recording.uuid, !recording.isFavorite)
        }
    }

    fun deleteRecording(recording: Recording) {
        viewModelScope.launch {
            vaultRepository.delete(recording.uuid)
        }
    }

    private fun groupByMonth(recordings: List<Recording>): List<MonthGroup> {
        if (recordings.isEmpty()) return emptyList()
        val formatter = java.time.format.DateTimeFormatter
            .ofPattern("MMMM yyyy")
            .withZone(java.time.ZoneId.systemDefault())
        return recordings
            .groupBy { formatter.format(java.time.Instant.ofEpochMilli(it.createdAtEpochMs)) }
            .map { (label, items) -> MonthGroup(label, items) }
    }
}
