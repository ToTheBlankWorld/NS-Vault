package com.nsvault.app.feature.recorder

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nsvault.app.core.common.TimeFormats
import com.nsvault.app.core.common.findFragmentActivity
import com.nsvault.app.designsystem.component.AuroraBackground
import com.nsvault.app.designsystem.component.ErrorState
import com.nsvault.app.designsystem.component.GlassCard
import com.nsvault.app.designsystem.component.LiveWaveform
import com.nsvault.app.designsystem.component.RecordControlButton
import com.nsvault.app.designsystem.component.RecordControlStyle
import com.nsvault.app.designsystem.component.SuccessGlyph
import com.nsvault.app.designsystem.component.rememberVaultHaptics
import com.nsvault.app.designsystem.component.VaultDialog
import com.nsvault.app.designsystem.component.VaultLoadingIndicator
import com.nsvault.app.designsystem.component.VaultPrimaryButton
import com.nsvault.app.designsystem.component.VaultTextButton
import com.nsvault.app.designsystem.component.VaultProgressBar
import com.nsvault.app.designsystem.layout.VaultLayout
import com.nsvault.app.designsystem.layout.vaultContentWidth
import com.nsvault.app.designsystem.theme.Dimens
import com.nsvault.app.designsystem.theme.VaultColors
import com.nsvault.app.designsystem.theme.VaultGradients
import com.nsvault.app.designsystem.theme.VaultMotion
import com.nsvault.app.domain.model.RecorderError
import com.nsvault.app.domain.model.RecorderState
import com.nsvault.app.domain.model.RecorderState.Companion.isCapturing
import com.nsvault.app.domain.model.RecorderState.Companion.isFinalizing
import com.nsvault.app.domain.model.StorageWarning
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow

@Composable
fun RecorderScreen(
    onClose: () -> Unit,
    viewModel: RecorderViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasMicPermission = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.RECORD_AUDIO,
                ) == PackageManager.PERMISSION_GRANTED
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (hasMicPermission) {
        RecorderContent(onClose = onClose, viewModel = viewModel)
    } else {
        val activity = context.findFragmentActivity()
        var permanentlyDenied by rememberSaveable { mutableStateOf(false) }
        val launcher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions(),
        ) { result ->
            val granted = result[Manifest.permission.RECORD_AUDIO] == true
            hasMicPermission = granted
            if (!granted) {
                permanentlyDenied = activity
                    ?.shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO) == false
            }
        }
        MicPermissionScreen(
            permanentlyDenied = permanentlyDenied,
            onRequest = {
                val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    arrayOf(Manifest.permission.RECORD_AUDIO)
                }
                launcher.launch(permissions)
            },
            onNotNow = onClose,
        )
    }
}

@Composable
private fun RecorderContent(
    onClose: () -> Unit,
    viewModel: RecorderViewModel,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val storageWarning by viewModel.storageWarning.collectAsStateWithLifecycle()
    val saveProgress by viewModel.saveProgress.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val haptics = rememberVaultHaptics()

    var showCancelDialog by remember { mutableStateOf(false) }

    var autoStarted by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!autoStarted) {
            autoStarted = true
            viewModel.start()
        }
    }

    val levels = remember { mutableStateListOf<Float>() }
    LaunchedEffect(Unit) {
        try {
            viewModel.level.collect { value ->
                levels.add(value)
                if (levels.size > WAVE_HISTORY) levels.removeAt(0)
            }
        } catch (_: Exception) { }
    }

    LaunchedEffect(state) {
        when (state) {
            is RecorderState.Completed -> {
                haptics.success()
                delay(VaultMotion.SuccessHoldMs.toLong())
                viewModel.acknowledgeResult()
                onClose()
            }
            is RecorderState.Failed -> haptics.reject()
            else -> Unit
        }
    }

    AuroraBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = Dimens.screenPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onClose, enabled = !state.isFinalizing && state !is RecorderState.AwaitingTitle) {
                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowDown,
                        contentDescription = "Minimize — recording continues",
                        tint = VaultColors.TextSecondary,
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                if (stageOf(state) == Stage.Capture) {
                    StatusChip(state = state)
                }
                Spacer(modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.size(Dimens.minTouchTarget))
            }

            AnimatedVisibility(visible = storageWarning != null) {
                StorageBanner(warning = storageWarning)
            }

            if (error != null) {
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Rounded.ErrorOutline,
                            contentDescription = null,
                            tint = VaultColors.Error,
                            modifier = Modifier.size(Dimens.spaceXxl),
                        )
                        Spacer(modifier = Modifier.height(Dimens.spaceMd))
                        Text(
                            text = error ?: "Recording failed",
                            style = MaterialTheme.typography.bodyLarge,
                            color = VaultColors.TextSecondary,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(modifier = Modifier.height(Dimens.spaceLg))
                        VaultTextButton(text = "OK", onClick = { viewModel.clearError(); onClose() })
                    }
                }
            } else {
                AnimatedContent(
                    targetState = stageOf(state),
                    transitionSpec = {
                        fadeIn(VaultMotion.enter()) togetherWith fadeOut(VaultMotion.exit())
                    },
                    label = "recorderStage",
                    modifier = Modifier.weight(1f),
                ) { stage ->
                    when (stage) {
                        Stage.Capture -> CaptureStage(
                            state = state,
                            elapsedProvider = viewModel.elapsedMillis,
                            levels = levels,
                            levelProvider = viewModel.level,
                            onPause = viewModel::pause,
                            onResume = viewModel::resume,
                            onStop = viewModel::stop,
                            onCancel = { showCancelDialog = true },
                        )
                        Stage.Finalizing -> FinalizingStage(state = state, progress = saveProgress)
                        Stage.Done -> DoneStage()
                        Stage.Error -> ErrorStage(
                            state = state,
                            onAcknowledge = { viewModel.acknowledgeResult(); onClose() },
                        )
                    }
                }
            }
        }
    }

    if (showCancelDialog) {
        VaultDialog(
            title = "Discard recording?",
            message = "This recording will be deleted permanently.",
            confirmText = "Discard",
            onConfirm = { showCancelDialog = false; viewModel.cancel(); onClose() },
            dismissText = "Keep recording",
            onDismissRequest = { showCancelDialog = false },
            icon = Icons.Rounded.Close,
            destructive = true,
        )
    }

    if (state is RecorderState.AwaitingTitle) {
        NameRecordingDialog(
            onConfirm = viewModel::finalizeRecording,
            onDismiss = { viewModel.cancel(); onClose() },
        )
    }
}

@Composable
private fun NameRecordingDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val defaultTitle = remember {
        java.time.LocalDateTime.now().format(
            java.time.format.DateTimeFormatter.ofPattern("'Recording' d MMM yyyy, h:mm a")
        )
    }
    var title by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.extraLarge)
                .background(VaultColors.SurfaceRaised)
                .padding(Dimens.spaceLg),
        ) {
            Text(
                text = "Name your recording",
                style = MaterialTheme.typography.titleLarge,
                color = VaultColors.TextPrimary,
            )
            Spacer(modifier = Modifier.height(Dimens.spaceMd))
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                singleLine = true,
                placeholder = {
                    Text(text = defaultTitle, color = VaultColors.TextTertiary)
                },
                textStyle = MaterialTheme.typography.bodyLarge,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = VaultColors.TextPrimary,
                    unfocusedTextColor = VaultColors.TextPrimary,
                    focusedBorderColor = VaultColors.AuroraViolet,
                    unfocusedBorderColor = VaultColors.Hairline,
                    cursorColor = VaultColors.AuroraViolet,
                    focusedPlaceholderColor = VaultColors.TextTertiary,
                    unfocusedPlaceholderColor = VaultColors.TextTertiary,
                ),
                keyboardActions = KeyboardActions(
                    onDone = { onConfirm(if (title.isBlank()) defaultTitle else title) },
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(Dimens.spaceXxs))
            Text(
                text = "Press Enter to use the auto-generated name",
                style = MaterialTheme.typography.bodySmall,
                color = VaultColors.TextTertiary,
            )
            Spacer(modifier = Modifier.height(Dimens.spaceLg))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimens.spaceSm, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                VaultTextButton(text = "Discard", onClick = onDismiss)
                VaultPrimaryButton(
                    text = "Save",
                    onClick = { onConfirm(if (title.isBlank()) defaultTitle else title) },
                )
            }
        }
    }
}

private enum class Stage { Capture, Finalizing, Done, Error }

private fun stageOf(state: RecorderState): Stage = when {
    state is RecorderState.Completed -> Stage.Done
    state is RecorderState.Failed -> Stage.Error
    state.isFinalizing -> Stage.Finalizing
    else -> Stage.Capture
}

@Composable
private fun CaptureStage(
    state: RecorderState,
    elapsedProvider: StateFlow<Long>,
    levels: List<Float>,
    levelProvider: StateFlow<Float>,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    onCancel: () -> Unit,
) {
    val paused = state == RecorderState.Paused

    // The timer and mic orb read the high-frequency flows themselves, so
    // this stage recomposes only when pause state changes — not at 10-20 Hz.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .vaultContentWidth(VaultLayout.FocusMaxWidth),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(Dimens.spaceXl))

        TimerText(elapsedProvider = elapsedProvider)
        Text(
            text = if (paused) "Paused" else "Recording",
            style = MaterialTheme.typography.bodyMedium,
            color = VaultColors.TextTertiary,
        )

        Spacer(modifier = Modifier.weight(1f))

        MicOrb(levelProvider = levelProvider, paused = paused)

        Spacer(modifier = Modifier.weight(1f))

        LiveWaveform(
            levels = levels,
            active = !paused,
            modifier = Modifier
                .fillMaxWidth()
                .height(Dimens.waveformHeight),
        )

        Spacer(modifier = Modifier.weight(1f))

        Row(
            horizontalArrangement = Arrangement.spacedBy(Dimens.spaceXl),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RecordControlButton(
                icon = if (paused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                contentDescription = if (paused) "Resume" else "Pause",
                onClick = if (paused) onResume else onPause,
                style = RecordControlStyle.Glass,
            )
            RecordControlButton(
                icon = Icons.Rounded.Stop,
                contentDescription = "Stop and save",
                onClick = onStop,
                style = RecordControlStyle.Danger,
                size = Dimens.recordButtonSize,
            )
            RecordControlButton(
                icon = Icons.Rounded.Close,
                contentDescription = "Discard recording",
                onClick = onCancel,
                style = RecordControlStyle.Glass,
            )
        }

        Spacer(modifier = Modifier.height(Dimens.spaceXl))
    }
}

@Composable
private fun TimerText(elapsedProvider: StateFlow<Long>) {
    val elapsed by elapsedProvider.collectAsStateWithLifecycle()
    Text(
        text = TimeFormats.clock(elapsed),
        style = MaterialTheme.typography.displayLarge.copy(fontFeatureSettings = "tnum"),
        color = VaultColors.TextPrimary,
    )
}

@Composable
private fun MicOrb(levelProvider: StateFlow<Float>, paused: Boolean) {
    val level by levelProvider.collectAsStateWithLifecycle()
    val animatedLevel by animateFloatAsState(
        targetValue = if (paused) 0f else level,
        animationSpec = VaultMotion.SpringGentle,
        label = "orbLevel",
    )

    Box(contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(Dimens.recordButtonSize + Dimens.spaceXxl)
                .scale(0.8f + animatedLevel * 0.45f)
                .background(VaultGradients.RecordGlow, CircleShape),
        )
        Box(
            modifier = Modifier
                .size(Dimens.recordButtonSize + Dimens.spaceLg)
                .background(
                    brush = VaultGradients.Aurora,
                    shape = CircleShape,
                    alpha = if (paused) 0.4f else 1f,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.Mic,
                contentDescription = null,
                tint = VaultColors.OnAccent,
                modifier = Modifier.size(Dimens.spaceXl + Dimens.spaceXs),
            )
        }
    }
}

@Composable
private fun StatusChip(state: RecorderState) {
    val paused = state == RecorderState.Paused
    val pulse = rememberInfiniteTransition(label = "recPulse")
    val dotAlpha by pulse.animateFloat(
        initialValue = 1f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(VaultMotion.DurationExtraLong, easing = VaultMotion.EasingEmphasized),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "recDot",
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.spaceXs),
    ) {
        Box(
            modifier = Modifier
                .size(Dimens.spaceXs)
                .graphicsLayer { alpha = if (paused) 1f else dotAlpha }
                .background(
                    if (paused) VaultColors.TextTertiary else VaultColors.RecordingRed,
                    CircleShape,
                ),
        )
        Text(
            text = if (paused) "PAUSED" else "REC",
            style = MaterialTheme.typography.labelMedium,
            color = if (paused) VaultColors.TextTertiary else VaultColors.RecordingRed,
        )
    }
}

@Composable
private fun StorageBanner(warning: StorageWarning?) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = Dimens.spaceSm),
        contentPadding = Dimens.spaceSm,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.spaceXs),
        ) {
            Icon(
                imageVector = Icons.Rounded.WarningAmber,
                contentDescription = null,
                tint = if (warning == StorageWarning.Critical) VaultColors.Error else VaultColors.Gold,
                modifier = Modifier.size(Dimens.iconSize),
            )
            Text(
                text = when (warning) {
                    StorageWarning.Critical -> "Storage critically low — recording was saved safely"
                    else -> "Storage is running low"
                },
                style = MaterialTheme.typography.bodySmall,
                color = VaultColors.TextSecondary,
            )
        }
    }
}

@Composable
private fun FinalizingStage(
    state: RecorderState,
    progress: Float?,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .vaultContentWidth(VaultLayout.FocusMaxWidth),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        VaultLoadingIndicator()
        Spacer(modifier = Modifier.height(Dimens.spaceLg))
        AnimatedContent(
            targetState = state,
            transitionSpec = {
                fadeIn(VaultMotion.enter()) togetherWith fadeOut(VaultMotion.exit())
            },
            label = "finalizingLabel",
        ) { current ->
            Text(
                text = when (current) {
                    RecorderState.Stopping -> "Finishing recording…"
                    RecorderState.Encrypting -> "Encrypting…"
                    else -> "Verifying & saving…"
                },
                style = MaterialTheme.typography.titleMedium,
                color = VaultColors.TextSecondary,
            )
        }
        Spacer(modifier = Modifier.height(Dimens.spaceLg))
        VaultProgressBar(
            progress = progress ?: 0f,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.spaceXxl),
        )
    }
}

@Composable
private fun DoneStage() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        SuccessGlyph()
        Spacer(modifier = Modifier.height(Dimens.spaceLg))
        Text(
            text = "Saved to your vault",
            style = MaterialTheme.typography.headlineSmall,
            color = VaultColors.TextPrimary,
        )
    }
}

@Composable
private fun ErrorStage(
    state: RecorderState,
    onAcknowledge: () -> Unit,
) {
    val error = (state as? RecorderState.Failed)?.error
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        ErrorState(
            icon = Icons.Rounded.ErrorOutline,
            title = when (error) {
                RecorderError.MicrophoneUnavailable -> "Microphone unavailable"
                RecorderError.StorageFull -> "Not enough storage"
                RecorderError.SaveFailed -> "Couldn't save recording"
                else -> "Recording failed"
            },
            message = when (error) {
                RecorderError.MicrophoneUnavailable ->
                    "Another app may be using the microphone. Close it and try again."

                RecorderError.StorageFull ->
                    "Free up some space on this phone, then try again."

                RecorderError.SaveFailed ->
                    "Your audio was kept safe — NS Vault will offer to recover it on the home screen."

                else -> "Something interrupted the recording. Please try again."
            },
            retryText = "OK",
            onRetry = onAcknowledge,
        )
    }
}

private const val WAVE_HISTORY = 64
