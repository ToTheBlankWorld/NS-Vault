package com.nsvault.app.data.di

import com.nsvault.app.data.audio.RecorderRepositoryImpl
import com.nsvault.app.data.auth.AuthRepositoryImpl
import com.nsvault.app.data.settings.SettingsRepositoryImpl
import com.nsvault.app.data.vault.VaultRepositoryImpl
import com.nsvault.app.domain.repository.AuthRepository
import com.nsvault.app.domain.repository.RecorderRepository
import com.nsvault.app.domain.repository.SettingsRepository
import com.nsvault.app.domain.repository.VaultRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindsAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    abstract fun bindsRecorderRepository(impl: RecorderRepositoryImpl): RecorderRepository

    @Binds
    abstract fun bindsVaultRepository(impl: VaultRepositoryImpl): VaultRepository

    @Binds
    abstract fun bindsSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository
}
