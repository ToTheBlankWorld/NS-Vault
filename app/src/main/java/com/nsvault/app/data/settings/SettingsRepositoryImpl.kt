package com.nsvault.app.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.nsvault.app.domain.model.AudioQuality
import com.nsvault.app.domain.model.ThemeMode
import com.nsvault.app.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {

    override val audioQuality: Flow<AudioQuality> = dataStore.data
        .map { prefs ->
            prefs[KEY_AUDIO_QUALITY]
                ?.let { stored -> AudioQuality.entries.firstOrNull { it.name == stored } }
                ?: AudioQuality.HIGH
        }
        .distinctUntilChanged()

    override suspend fun setAudioQuality(quality: AudioQuality) {
        dataStore.edit { it[KEY_AUDIO_QUALITY] = quality.name }
    }

    override val themeMode: Flow<ThemeMode> = dataStore.data
        .map { prefs ->
            prefs[KEY_THEME_MODE]
                ?.let { stored -> ThemeMode.entries.firstOrNull { it.name == stored } }
                ?: ThemeMode.DARK
        }
        .distinctUntilChanged()

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[KEY_THEME_MODE] = mode.name }
    }

    companion object {
        private val KEY_AUDIO_QUALITY = stringPreferencesKey("audio_quality")
        private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
    }
}
