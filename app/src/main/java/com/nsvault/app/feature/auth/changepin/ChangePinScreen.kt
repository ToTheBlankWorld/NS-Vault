package com.nsvault.app.feature.auth.changepin

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nsvault.app.designsystem.component.AuroraBackground
import com.nsvault.app.designsystem.component.SuccessGlyph
import com.nsvault.app.designsystem.component.VaultTextButton
import com.nsvault.app.designsystem.component.VaultTopBar
import com.nsvault.app.designsystem.layout.VaultLayout
import com.nsvault.app.designsystem.layout.vaultContentWidth
import com.nsvault.app.designsystem.theme.Dimens
import com.nsvault.app.designsystem.theme.VaultColors
import com.nsvault.app.designsystem.theme.VaultMotion
import com.nsvault.app.feature.auth.components.PinEntrySection
import kotlinx.coroutines.delay

@Composable
fun ChangePinScreen(
    onBack: () -> Unit,
    onDone: () -> Unit,
    viewModel: ChangePinViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.step) {
        if (state.step == ChangePinViewModel.Step.Success) {
            delay(VaultMotion.SuccessHoldMs.toLong())
            onDone()
        }
    }

    AuroraBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
        ) {
            VaultTopBar(title = "Change PIN", onBack = onBack)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
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
                    label = "changePinStep",
                ) { step ->
                    Column(
                        modifier = Modifier
                            .vaultContentWidth(VaultLayout.FocusMaxWidth)
                            .verticalScroll(rememberScrollState())
                            .padding(Dimens.screenPadding),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        when (step) {
                            ChangePinViewModel.Step.VerifyCurrent -> PinEntrySection(
                                title = "Enter current PIN",
                                subtitle = state.message,
                                subtitleIsError = state.messageIsError,
                                length = state.activeLength.digits,
                                filled = state.enteredCount,
                                errorPulse = state.errorPulse,
                                enabled = state.inputEnabled,
                                onDigit = viewModel::onDigit,
                                onBackspace = viewModel::onBackspace,
                            )

                            ChangePinViewModel.Step.Enter -> PinEntrySection(
                                title = "Create new PIN",
                                subtitle = "Choose a PIN you haven't used before",
                                length = state.activeLength.digits,
                                filled = state.enteredCount,
                                enabled = state.inputEnabled,
                                onDigit = viewModel::onDigit,
                                onBackspace = viewModel::onBackspace,
                                footer = {
                                    Spacer(modifier = Modifier.height(Dimens.spaceMd))
                                    Row(horizontalArrangement = Arrangement.spacedBy(Dimens.spaceSm)) {
                                        com.nsvault.app.domain.model.PinLength.entries.forEach { length ->
                                            VaultTextButton(
                                                text = "${length.digits} digits" +
                                                    if (length == state.newLength) " ✓" else "",
                                                onClick = { viewModel.onNewLengthSelected(length) },
                                            )
                                        }
                                    }
                                },
                            )

                            ChangePinViewModel.Step.Confirm -> PinEntrySection(
                                title = "Confirm new PIN",
                                subtitle = state.message ?: "Enter it once more",
                                subtitleIsError = state.messageIsError,
                                length = state.activeLength.digits,
                                filled = state.enteredCount,
                                errorPulse = state.errorPulse,
                                enabled = state.inputEnabled,
                                onDigit = viewModel::onDigit,
                                onBackspace = viewModel::onBackspace,
                                footer = {
                                    Spacer(modifier = Modifier.height(Dimens.spaceMd))
                                    VaultTextButton(
                                        text = "Start over",
                                        onClick = viewModel::onStartOver,
                                    )
                                },
                            )

                            ChangePinViewModel.Step.Success -> {
                                SuccessGlyph()
                                Spacer(modifier = Modifier.height(Dimens.spaceLg))
                                Text(
                                    text = "PIN updated",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = VaultColors.TextPrimary,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
