package com.nsvault.app.data.audio

import android.content.Intent
import android.content.pm.ServiceInfo
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.nsvault.app.core.common.VaultLog
import com.nsvault.app.domain.model.RecorderState
import com.nsvault.app.domain.model.RecorderState.Companion.isCapturing
import com.nsvault.app.domain.model.RecorderState.Companion.isFinalizing
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class RecordingService : LifecycleService() {

    @Inject
    lateinit var engine: RecordingEngine

    override fun onCreate() {
        super.onCreate()
        try {
            RecordingNotifications.ensureChannel(this)
            observeEngine()
        } catch (e: Exception) {
            VaultLog.e("RecService", "Service init failed", e)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        try {
            when (intent?.action) {
                ACTION_START -> {
                    if (goForeground()) {
                        engine.start()
                    } else {
                        VaultLog.e("RecService", "Foreground failed — stopping service")
                        stopSelf()
                    }
                }
                ACTION_PAUSE -> engine.pause()
                ACTION_RESUME -> engine.resume()
                ACTION_STOP -> engine.stop()
                ACTION_CANCEL -> engine.cancel()
            }
        } catch (e: Exception) {
            VaultLog.e("RecService", "Command failed: ${intent?.action}", e)
        }
        return START_NOT_STICKY
    }

    /** @return true if the service successfully went foreground */
    private fun goForeground(): Boolean {
        return try {
            ServiceCompat.startForeground(
                this,
                RecordingNotifications.NOTIFICATION_ID,
                RecordingNotifications.build(this, RecorderState.Preparing, 0),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE,
            )
            true
        } catch (e: Exception) {
            VaultLog.e("RecService", "Failed to go foreground", e)
            false
        }
    }

    private fun observeEngine() {
        lifecycleScope.launch {
            try {
                engine.state.collectLatest { state ->
                    when {
                        state.isCapturing -> updateNotification(state)
                        state.isFinalizing -> Unit
                        else -> {
                            ServiceCompat.stopForeground(
                                this@RecordingService,
                                ServiceCompat.STOP_FOREGROUND_REMOVE,
                            )
                            stopSelf()
                        }
                    }
                }
            } catch (e: Exception) {
                VaultLog.e("RecService", "Engine observer failed", e)
            }
        }
    }

    private fun updateNotification(state: RecorderState) {
        try {
            NotificationManagerCompat.from(this).apply {
                if (areNotificationsEnabled()) {
                    notify(
                        RecordingNotifications.NOTIFICATION_ID,
                        RecordingNotifications.build(this@RecordingService, state, engine.elapsedMillis.value),
                    )
                }
            }
        } catch (e: Exception) {
            VaultLog.e("RecService", "Notification update failed", e)
        }
    }

    companion object {
        const val ACTION_START = "com.nsvault.app.action.START_RECORDING"
        const val ACTION_PAUSE = "com.nsvault.app.action.PAUSE_RECORDING"
        const val ACTION_RESUME = "com.nsvault.app.action.RESUME_RECORDING"
        const val ACTION_STOP = "com.nsvault.app.action.STOP_RECORDING"
        const val ACTION_CANCEL = "com.nsvault.app.action.CANCEL_RECORDING"
    }
}
