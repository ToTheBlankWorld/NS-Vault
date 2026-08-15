package com.nsvault.app.feature.settings

import android.net.Uri
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nsvault.app.domain.model.AudioQuality
import com.nsvault.app.domain.model.ThemeMode
import com.nsvault.app.domain.model.VaultStats
import com.nsvault.app.domain.repository.AuthRepository
import com.nsvault.app.domain.repository.SettingsRepository
import com.nsvault.app.domain.repository.VaultRepository
import com.nsvault.app.domain.usecase.SetBiometricEnabledUseCase
import com.nsvault.app.feature.auth.BiometricAuthenticator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    authRepository: AuthRepository,
    private val settingsRepository: SettingsRepository,
    private val vaultRepository: VaultRepository,
    private val setBiometricEnabledUseCase: SetBiometricEnabledUseCase,
    private val biometricAuthenticator: BiometricAuthenticator,
) : ViewModel() {

    data class ImportState(
        val current: Int,
        val total: Int,
        val progress: Float,
    )

    val biometricEnabled: StateFlow<Boolean> = authRepository.isBiometricEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val audioQuality: StateFlow<AudioQuality> = settingsRepository.audioQuality
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AudioQuality.HIGH)

    val themeMode: StateFlow<ThemeMode> = settingsRepository.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemeMode.DARK)

    val stats: StateFlow<VaultStats> = vaultRepository.stats
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), VaultStats.Empty)

    private val _freeBytes = MutableStateFlow(0L)
    val freeBytes: StateFlow<Long> = _freeBytes

    private val _importing = MutableStateFlow<ImportState?>(null)
    val importing: StateFlow<ImportState?> = _importing

    private val _importMessage = MutableStateFlow<String?>(null)
    val importMessage: StateFlow<String?> = _importMessage

    val biometricAvailable: Boolean
        get() = biometricAuthenticator.isAvailable

    init {
        viewModelScope.launch {
            _freeBytes.value = vaultRepository.availableStorageBytes()
        }
    }

    /** Enabling requires a fresh fingerprint confirmation; disabling doesn't. */
    fun onBiometricToggle(enabled: Boolean, activity: FragmentActivity?) {
        if (!enabled) {
            setBiometric(false)
            return
        }
        if (activity == null) return
        biometricAuthenticator.authenticate(
            activity = activity,
            title = "Confirm fingerprint",
            subtitle = "Verify once to enable fingerprint unlock",
            negativeText = "Cancel",
            onSuccess = { setBiometric(true) },
        )
    }

    fun setAudioQuality(quality: AudioQuality) {
        viewModelScope.launch { settingsRepository.setAudioQuality(quality) }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }

    fun import(uris: List<Uri>) {
        if (uris.isEmpty() || _importing.value != null) return
        viewModelScope.launch {
            var succeeded = 0
            uris.forEachIndexed { index, uri ->
                _importing.value = ImportState(current = index + 1, total = uris.size, progress = 0f)
                val id = vaultRepository.importRecording(uri) { progress ->
                    _importing.update { it?.copy(progress = progress) }
                }
                if (id != null) succeeded++
            }
            _importing.value = null
            _freeBytes.value = vaultRepository.availableStorageBytes()
            _importMessage.value = when {
                succeeded == uris.size && succeeded == 1 -> "Recording imported and encrypted"
                succeeded == uris.size -> "$succeeded recordings imported and encrypted"
                succeeded > 0 -> "Imported $succeeded of ${uris.size} — some files couldn't be read"
                else -> "Couldn't import — files weren't readable audio"
            }
            delay(MESSAGE_VISIBLE_MS)
            _importMessage.value = null
        }
    }

    private fun setBiometric(enabled: Boolean) {
        viewModelScope.launch { setBiometricEnabledUseCase(enabled) }
    }

    companion object {
        private const val MESSAGE_VISIBLE_MS = 3_500L
    }
}
