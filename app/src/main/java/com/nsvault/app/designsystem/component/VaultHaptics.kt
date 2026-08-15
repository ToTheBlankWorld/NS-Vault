package com.nsvault.app.designsystem.component

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView

/**
 * One haptic vocabulary for the whole app, so every interaction of the
 * same kind feels identical. Backed by the platform view so it honors
 * the user's system haptic setting.
 */
class VaultHaptics(private val view: View) {

    /** Light tick for a routine tap (buttons, keypad, controls). */
    fun tap() {
        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }

    /** A discrete tick for scrubbing / stepping. */
    fun tick() {
        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
    }

    /** Positive confirmation (unlock, PIN created, recording saved). */
    fun success() {
        view.performHapticFeedback(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                HapticFeedbackConstants.CONFIRM
            } else {
                HapticFeedbackConstants.KEYBOARD_TAP
            },
        )
    }

    /** Rejection (wrong PIN, failure). */
    fun reject() {
        view.performHapticFeedback(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                HapticFeedbackConstants.REJECT
            } else {
                HapticFeedbackConstants.LONG_PRESS
            },
        )
    }
}

@Composable
fun rememberVaultHaptics(): VaultHaptics {
    val view = LocalView.current
    return remember(view) { VaultHaptics(view) }
}
