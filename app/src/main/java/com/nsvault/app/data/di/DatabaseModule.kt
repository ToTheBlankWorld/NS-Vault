package com.nsvault.app.data.di

import android.content.Context
import androidx.room.Room
import com.nsvault.app.data.database.NSVaultDatabase
import com.nsvault.app.data.database.RecordingDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun providesDatabase(@ApplicationContext context: Context): NSVaultDatabase =
        Room.databaseBuilder(context, NSVaultDatabase::class.java, "nsvault.db")
            .addMigrations(NSVaultDatabase.MIGRATION_1_2, NSVaultDatabase.MIGRATION_2_3, NSVaultDatabase.MIGRATION_3_4)
            .build()

    @Provides
    fun providesRecordingDao(database: NSVaultDatabase): RecordingDao =
        database.recordingDao()
}
