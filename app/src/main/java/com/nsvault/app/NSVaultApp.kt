package com.nsvault.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nsvault.app.designsystem.theme.VaultMotion
import com.nsvault.app.feature.auth.lock.LockScreen
import com.nsvault.app.feature.auth.setup.PinSetupScreen
import com.nsvault.app.navigation.NSVaultNavHost

/**
 * Root of the UI. The main app renders beneath an authentication layer:
 * PIN setup on first launch, the lock screen whenever the session is
 * locked. Unlocking dissolves the layer to reveal the vault.
 */
@Composable
fun NSVaultApp(viewModel: MainViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        if (state == RootUiState.Locked || state == RootUiState.Unlocked) {
            NSVaultNavHost()
        }

        AnimatedVisibility(
            visible = state == RootUiState.NeedsSetup,
            enter = fadeIn(VaultMotion.enter()),
            exit = fadeOut(VaultMotion.exit(VaultMotion.DurationMedium)) +
                scaleOut(
                    animationSpec = VaultMotion.exit(VaultMotion.DurationMedium),
                    targetScale = 1.05f,
                ),
        ) {
            PinSetupScreen()
        }

        AnimatedVisibility(
            visible = state == RootUiState.Locked,
            enter = fadeIn(VaultMotion.enter(VaultMotion.DurationShort)),
            exit = fadeOut(VaultMotion.exit(VaultMotion.DurationMedium)) +
                scaleOut(
                    animationSpec = VaultMotion.exit(VaultMotion.DurationMedium),
                    targetScale = 1.05f,
                ),
        ) {
            LockScreen()
        }
    }
}
