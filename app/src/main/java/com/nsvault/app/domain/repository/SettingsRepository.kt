package com.nsvault.app.domain.repository

import com.nsvault.app.domain.model.AudioQuality
import com.nsvault.app.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {

    val audioQuality: Flow<AudioQuality>

    suspend fun setAudioQuality(quality: AudioQuality)

    val themeMode: Flow<ThemeMode>

    suspend fun setThemeMode(mode: ThemeMode)
}
