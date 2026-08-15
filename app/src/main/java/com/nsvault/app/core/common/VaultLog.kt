package com.nsvault.app.core.common

import android.util.Log
import com.nsvault.app.BuildConfig

/**
 * The only logging surface allowed in NS Vault.
 *
 * Rules — enforced by review, stated here for every future feature:
 *  - Messages must NEVER contain PINs, keys, IVs, hashes, file names,
 *    file paths, recording titles, durations, or any vault metadata.
 *  - Log the *category* of an event, never its content:
 *    "encryption failed" is fine; "encrypting 2026-07-14_x.m4a" is not.
 *  - Debug/info logs are stripped from release builds entirely.
 */
object VaultLog {

    fun d(tag: String, message: String) {
        if (BuildConfig.DEBUG) Log.d(prefix(tag), message)
    }

    fun i(tag: String, message: String) {
        if (BuildConfig.DEBUG) Log.i(prefix(tag), message)
    }

    fun w(tag: String, message: String) {
        Log.w(prefix(tag), message)
    }

    /**
     * [throwable] is logged with class name only in release builds so
     * exception messages cannot leak paths or content.
     */
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        if (BuildConfig.DEBUG) {
            Log.e(prefix(tag), message, throwable)
        } else {
            val cause = throwable?.let { " (${it.javaClass.simpleName})" }.orEmpty()
            Log.e(prefix(tag), message + cause)
        }
    }

    private fun prefix(tag: String) = "NSVault.$tag"
}
