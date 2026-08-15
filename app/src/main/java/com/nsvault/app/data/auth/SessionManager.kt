package com.nsvault.app.data.auth

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory unlock gate. The vault starts locked on every process
 * start, and locks immediately when the app is paused (app switch,
 * background, screen off). Nothing about the session is persisted.
 */
@Singleton
class SessionManager @Inject constructor() {

    private val _isUnlocked = MutableStateFlow(false)
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    fun unlock() {
        _isUnlocked.value = true
    }

    fun lock() {
        _isUnlocked.value = false
    }
}

/**
 * Bridges process lifecycle to the session gate. Registered on
 * [androidx.lifecycle.ProcessLifecycleOwner] at app start.
 * Locks immediately on pause (app switch / recent apps) and on
 * stop (fully backgrounded).
 */
@Singleton
class AppLockObserver @Inject constructor(
    private val sessionManager: SessionManager,
) : DefaultLifecycleObserver {

    override fun onPause(owner: LifecycleOwner) {
        sessionManager.lock()
    }
}
