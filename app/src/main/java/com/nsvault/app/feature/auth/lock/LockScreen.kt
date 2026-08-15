package com.nsvault.app.feature.auth.lock

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nsvault.app.core.common.findFragmentActivity
import com.nsvault.app.designsystem.component.AuroraBackground
import com.nsvault.app.designsystem.component.KeypadAction
import com.nsvault.app.designsystem.component.rememberVaultHaptics
import com.nsvault.app.designsystem.layout.VaultLayout
import com.nsvault.app.designsystem.layout.vaultContentWidth
import com.nsvault.app.designsystem.theme.Dimens
import com.nsvault.app.designsystem.theme.VaultColors
import com.nsvault.app.designsystem.theme.VaultGradients
import com.nsvault.app.feature.auth.components.PinEntrySection

@Composable
fun LockScreen(
    viewModel: LockViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val activity = LocalContext.current.findFragmentActivity()
    val haptics = rememberVaultHaptics()

    // Offer the fingerprint automatically once per lock.
    var autoPrompted by remember { mutableStateOf(false) }
    LaunchedEffect(state.biometricEnabled) {
        if (state.biometricEnabled && !autoPrompted && activity != null) {
            autoPrompted = true
            viewModel.promptBiometric(activity)
        }
    }
    LaunchedEffect(state.success) {
        if (state.success) haptics.success()
    }
    LaunchedEffect(state.errorPulse) {
        if (state.errorPulse > 0) haptics.reject()
    }

    val biometricAction =
        if (state.biometricEnabled && viewModel.biometricHardwareAvailable && activity != null) {
            KeypadAction(
                icon = Icons.Rounded.Fingerprint,
                contentDescription = "Unlock with fingerprint",
                onClick = { viewModel.promptBiometric(activity) },
            )
        } else {
            null
        }

    AuroraBackground {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .vaultContentWidth(VaultLayout.FocusMaxWidth)
                    .verticalScroll(rememberScrollState())
                    .padding(Dimens.screenPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(Dimens.fabSize)
                        .background(VaultGradients.Aurora, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.GraphicEq,
                        contentDescription = null,
                        tint = VaultColors.OnAccent,
                        modifier = Modifier.size(Dimens.iconSize),
                    )
                }
                Spacer(modifier = Modifier.height(Dimens.spaceSm))
                Text(
                    text = "NS Vault",
                    style = MaterialTheme.typography.titleMedium,
                    color = VaultColors.TextSecondary,
                )
                Spacer(modifier = Modifier.height(Dimens.spaceXl))
                PinEntrySection(
                    title = "Enter your PIN",
                    subtitle = state.message,
                    subtitleIsError = state.messageIsError,
                    length = state.pinLength.digits,
                    filled = state.enteredCount,
                    errorPulse = state.errorPulse,
                    success = state.success,
                    enabled = state.inputEnabled,
                    leftAction = biometricAction,
                    onDigit = viewModel::onDigit,
                    onBackspace = viewModel::onBackspace,
                )
            }
        }
    }
}
