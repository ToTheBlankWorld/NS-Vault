package com.nsvault.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nsvault.app.data.auth.SessionManager
import com.nsvault.app.domain.model.ThemeMode
import com.nsvault.app.domain.repository.AuthRepository
import com.nsvault.app.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

enum class RootUiState {
    /** Splash stays on screen until the first real state is known. */
    Loading,
    NeedsSetup,
    Locked,
    Unlocked,
}

@HiltViewModel
class MainViewModel @Inject constructor(
    authRepository: AuthRepository,
    sessionManager: SessionManager,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    val uiState: StateFlow<RootUiState> =
        combine(authRepository.isPinSet, sessionManager.isUnlocked) { pinSet, unlocked ->
            when {
                !pinSet -> RootUiState.NeedsSetup
                !unlocked -> RootUiState.Locked
                else -> RootUiState.Unlocked
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = RootUiState.Loading,
        )

    val themeMode: StateFlow<ThemeMode> = settingsRepository.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemeMode.DARK)
}
