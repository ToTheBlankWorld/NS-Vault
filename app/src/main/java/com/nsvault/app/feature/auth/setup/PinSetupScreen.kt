package com.nsvault.app.feature.auth.setup

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nsvault.app.core.common.findFragmentActivity
import com.nsvault.app.designsystem.component.AuroraBackground
import com.nsvault.app.designsystem.component.SuccessGlyph
import com.nsvault.app.designsystem.component.VaultPrimaryButton
import com.nsvault.app.designsystem.component.VaultTextButton
import com.nsvault.app.designsystem.component.rememberVaultHaptics
import com.nsvault.app.designsystem.layout.VaultLayout
import com.nsvault.app.designsystem.layout.vaultContentWidth
import com.nsvault.app.designsystem.theme.Dimens
import com.nsvault.app.designsystem.theme.VaultColors
import com.nsvault.app.designsystem.theme.VaultGradients
import com.nsvault.app.designsystem.theme.VaultMotion
import com.nsvault.app.domain.model.PinLength
import com.nsvault.app.feature.auth.components.PinEntrySection

@Composable
fun PinSetupScreen(
    viewModel: PinSetupViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val activity = LocalContext.current.findFragmentActivity()
    val haptics = rememberVaultHaptics()

    LaunchedEffect(state.step, state.errorPulse) {
        when {
            state.step == PinSetupViewModel.Step.Success -> haptics.success()
            state.mismatch && state.errorPulse > 0 -> haptics.reject()
        }
    }

    AuroraBackground {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            contentAlignment = Alignment.Center,
        ) {
            AnimatedContent(
                targetState = state.step,
                transitionSpec = {
                    (
                        fadeIn(VaultMotion.enter()) +
                            slideInVertically(VaultMotion.enter()) { it / 20 }
                        ) togetherWith fadeOut(VaultMotion.exit())
                },
                label = "setupStep",
            ) { step ->
                Column(
                    modifier = Modifier
                        .vaultContentWidth(VaultLayout.FocusMaxWidth)
                        .verticalScroll(rememberScrollState())
                        .padding(Dimens.screenPadding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    when (step) {
                        PinSetupViewModel.Step.Welcome -> WelcomeStep(
                            selectedLength = state.pinLength,
                            onLengthSelected = viewModel::onLengthSelected,
                            onContinue = viewModel::onBegin,
                        )

                        PinSetupViewModel.Step.Enter -> PinEntrySection(
                            title = "Create your PIN",
                            subtitle = "It unlocks everything in your vault",
                            length = state.pinLength.digits,
                            filled = state.enteredCount,
                            onDigit = viewModel::onDigit,
                            onBackspace = viewModel::onBackspace,
                        )

                        PinSetupViewModel.Step.Confirm -> PinEntrySection(
                            title = "Confirm your PIN",
                            subtitle = if (state.mismatch) {
                                "PINs didn't match — try again"
                            } else {
                                "Enter it once more"
                            },
                            subtitleIsError = state.mismatch,
                            length = state.pinLength.digits,
                            filled = state.enteredCount,
                            errorPulse = state.errorPulse,
                            onDigit = viewModel::onDigit,
                            onBackspace = viewModel::onBackspace,
                            footer = {
                                if (state.mismatch) {
                                    Spacer(modifier = Modifier.height(Dimens.spaceMd))
                                    VaultTextButton(
                                        text = "Start over",
                                        onClick = viewModel::onStartOver,
                                    )
                                }
                            },
                        )

                        PinSetupViewModel.Step.Biometric -> BiometricStep(
                            onEnable = {
                                val host = activity
                                if (host != null) {
                                    viewModel.onEnableBiometric(host)
                                } else {
                                    viewModel.onSkipBiometric()
                                }
                            },
                            onSkip = viewModel::onSkipBiometric,
                        )

                        PinSetupViewModel.Step.Success -> SuccessStep()
                    }
                }
            }
        }
    }
}

@Composable
private fun WelcomeStep(
    selectedLength: PinLength,
    onLengthSelected: (PinLength) -> Unit,
    onContinue: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(Dimens.recordButtonSize)
            .background(VaultGradients.Aurora, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Rounded.GraphicEq,
            contentDescription = null,
            tint = VaultColors.OnAccent,
            modifier = Modifier.size(Dimens.spaceXl),
        )
    }
    Spacer(modifier = Modifier.height(Dimens.spaceLg))
    Text(
        text = "Welcome to NS Vault",
        style = MaterialTheme.typography.headlineMedium,
        color = VaultColors.TextPrimary,
        textAlign = TextAlign.Center,
    )
    Spacer(modifier = Modifier.height(Dimens.spaceXs))
    Text(
        text = "A private home for your spoken memories.\nEverything stays on this phone, sealed by your PIN.",
        style = MaterialTheme.typography.bodyMedium,
        color = VaultColors.TextSecondary,
        textAlign = TextAlign.Center,
    )
    Spacer(modifier = Modifier.height(Dimens.spaceXl))
    Row(horizontalArrangement = Arrangement.spacedBy(Dimens.spaceSm)) {
        PinLength.entries.forEach { length ->
            LengthChip(
                length = length,
                selected = length == selectedLength,
                onClick = { onLengthSelected(length) },
            )
        }
    }
    Spacer(modifier = Modifier.height(Dimens.spaceXl))
    VaultPrimaryButton(
        text = "Create PIN",
        onClick = onContinue,
    )
}

@Composable
private fun LengthChip(
    length: PinLength,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val label = "${length.digits} digits"
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .then(
                if (selected) {
                    Modifier.background(VaultGradients.AuroraSoft)
                } else {
                    Modifier
                        .background(VaultColors.GlassFill)
                        .border(Dimens.hairline, VaultGradients.GlassBorder, CircleShape)
                },
            )
            .clickable(role = Role.RadioButton, onClick = onClick)
            .heightIn(min = Dimens.minTouchTarget)
            .padding(horizontal = Dimens.spaceLg, vertical = Dimens.spaceSm),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) VaultColors.TextPrimary else VaultColors.TextSecondary,
        )
    }
}

@Composable
private fun BiometricStep(
    onEnable: () -> Unit,
    onSkip: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(Dimens.recordButtonSize)
            .background(VaultColors.SurfaceRaised, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Rounded.Fingerprint,
            contentDescription = null,
            tint = VaultColors.AuroraCyan,
            modifier = Modifier.size(Dimens.spaceXl + Dimens.spaceXs),
        )
    }
    Spacer(modifier = Modifier.height(Dimens.spaceLg))
    Text(
        text = "Unlock with fingerprint?",
        style = MaterialTheme.typography.headlineSmall,
        color = VaultColors.TextPrimary,
        textAlign = TextAlign.Center,
    )
    Spacer(modifier = Modifier.height(Dimens.spaceXs))
    Text(
        text = "Open your vault with a touch.\nYour PIN always works as a fallback.",
        style = MaterialTheme.typography.bodyMedium,
        color = VaultColors.TextSecondary,
        textAlign = TextAlign.Center,
    )
    Spacer(modifier = Modifier.height(Dimens.spaceXl))
    VaultPrimaryButton(
        text = "Enable fingerprint",
        onClick = onEnable,
        icon = Icons.Rounded.Fingerprint,
    )
    Spacer(modifier = Modifier.height(Dimens.spaceSm))
    VaultTextButton(text = "Not now", onClick = onSkip)
}

@Composable
private fun SuccessStep() {
    SuccessGlyph()
    Spacer(modifier = Modifier.height(Dimens.spaceLg))
    Text(
        text = "Your vault is ready",
        style = MaterialTheme.typography.headlineSmall,
        color = VaultColors.TextPrimary,
        textAlign = TextAlign.Center,
    )
}
