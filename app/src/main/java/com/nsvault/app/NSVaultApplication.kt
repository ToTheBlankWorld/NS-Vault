package com.nsvault.app

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.nsvault.app.data.auth.AppLockObserver
import com.nsvault.app.data.auth.SessionManager
import com.nsvault.app.data.vault.VaultMigrator
import com.nsvault.app.domain.repository.VaultRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class NSVaultApplication : Application() {

    @Inject
    lateinit var appLockObserver: AppLockObserver

    @Inject
    lateinit var sessionManager: SessionManager

    @Inject
    lateinit var vaultMigrator: VaultMigrator

    @Inject
    lateinit var vaultRepository: VaultRepository

    override fun onCreate() {
        super.onCreate()
        ProcessLifecycleOwner.get().lifecycle.addObserver(appLockObserver)

        ProcessLifecycleOwner.get().lifecycleScope.launch {
            vaultRepository.clearShareCache()
            vaultMigrator.encryptLegacyPlaintext()
        }

        // The vault locks the instant the screen turns off.
        ContextCompat.registerReceiver(
            this,
            object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    sessionManager.lock()
                }
            },
            IntentFilter(Intent.ACTION_SCREEN_OFF),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
    }
}
