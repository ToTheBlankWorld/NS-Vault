package com.nsvault.app.feature.library

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nsvault.app.domain.model.Recording
import com.nsvault.app.domain.repository.VaultRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val vaultRepository: VaultRepository,
) : ViewModel() {

    data class MonthGroup(
        val label: String,
        val recordings: List<Recording>,
    )

    data class ImportState(
        val current: Int,
        val total: Int,
        val progress: Float,
    )

    enum class Filter { All, Favorites }

    private data class RecordingsAndFilter(
        val recordings: List<Recording>,
        val query: String,
        val filter: Filter,
    )

    data class UiState(
        val groups: List<MonthGroup> = emptyList(),
        val query: String = "",
        val filter: Filter = Filter.All,
        val hasAnyRecordings: Boolean = false,
        val importing: ImportState? = null,
        val importMessage: String? = null,
        val selected: Recording? = null,
    )

    private val query = MutableStateFlow("")
    private val filter = MutableStateFlow(Filter.All)
    private val importing = MutableStateFlow<ImportState?>(null)
    private val importMessage = MutableStateFlow<String?>(null)
    private val selected = MutableStateFlow<Recording?>(null)

    private val recordingsAndFilter: StateFlow<RecordingsAndFilter> = combine(
        vaultRepository.recordings, query, filter,
    ) { r, q, f -> RecordingsAndFilter(r, q, f) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RecordingsAndFilter(emptyList(), "", Filter.All))

    val uiState: StateFlow<UiState> = combine(
        recordingsAndFilter, importing, importMessage, selected,
    ) { raf, importState, message, selectedRecording ->
        UiState(
            groups = group(filter(raf.recordings, raf.query, raf.filter)),
            query = raf.query,
            filter = raf.filter,
            hasAnyRecordings = raf.recordings.isNotEmpty(),
            importing = importState,
            importMessage = message,
            selected = selectedRecording,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState())

    fun onFilterChange(value: Filter) {
        filter.value = value
    }

    fun onQueryChange(value: String) {
        query.value = value
    }

    fun onSelect(recording: Recording?) {
        selected.value = recording
    }

    fun rename(recording: Recording, title: String) {
        if (title.isBlank()) return
        viewModelScope.launch { vaultRepository.rename(recording.uuid, title) }
    }

    fun toggleFavorite(recording: Recording) {
        viewModelScope.launch {
            vaultRepository.setFavorite(recording.uuid, !recording.isFavorite)
        }
    }

    fun delete(recording: Recording) {
        viewModelScope.launch { vaultRepository.delete(recording.uuid) }
    }

    fun import(uris: List<Uri>) {
        if (uris.isEmpty() || importing.value != null) return
        viewModelScope.launch {
            var succeeded = 0
            uris.forEachIndexed { index, uri ->
                importing.value = ImportState(current = index + 1, total = uris.size, progress = 0f)
                val id = vaultRepository.importRecording(uri) { progress ->
                    importing.update { it?.copy(progress = progress) }
                }
                if (id != null) succeeded++
            }
            importing.value = null
            importMessage.value = when {
                succeeded == uris.size && succeeded == 1 -> "Recording imported and encrypted"
                succeeded == uris.size -> "$succeeded recordings imported and encrypted"
                succeeded > 0 -> "Imported $succeeded of ${uris.size} — some files couldn't be read"
                else -> "Couldn't import — files weren't readable audio"
            }
            delay(MESSAGE_VISIBLE_MS)
            importMessage.value = null
        }
    }

    private fun filter(recordings: List<Recording>, rawQuery: String, activeFilter: Filter): List<Recording> {
        val filtered = when (activeFilter) {
            Filter.All -> recordings
            Filter.Favorites -> recordings.filter { it.isFavorite }
        }
        val q = rawQuery.trim().lowercase()
        if (q.isEmpty()) return filtered
        return filtered.filter { recording ->
            recording.title.lowercase().contains(q) ||
                DATE_SEARCH.format(Instant.ofEpochMilli(recording.createdAtEpochMs))
                    .lowercase()
                    .contains(q)
        }
    }

    private fun group(recordings: List<Recording>): List<MonthGroup> =
        recordings
            .groupBy { MONTH_LABEL.format(Instant.ofEpochMilli(it.createdAtEpochMs)) }
            .map { (label, items) -> MonthGroup(label, items) }

    companion object {
        private const val MESSAGE_VISIBLE_MS = 3_500L

        private val MONTH_LABEL = DateTimeFormatter
            .ofPattern("MMMM yyyy")
            .withZone(ZoneId.systemDefault())

        /** Searchable date text: "13 july 2026 13 jul". */
        private val DATE_SEARCH = DateTimeFormatter
            .ofPattern("d MMMM yyyy d MMM")
            .withZone(ZoneId.systemDefault())
    }
}
