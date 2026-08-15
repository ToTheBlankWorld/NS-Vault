package com.nsvault.app.feature.player

import android.content.Intent
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Forward30
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import com.nsvault.app.core.common.TimeFormats
import com.nsvault.app.core.common.findFragmentActivity
import com.nsvault.app.designsystem.component.AuroraBackground
import com.nsvault.app.designsystem.component.ErrorState
import com.nsvault.app.designsystem.component.PlaybackWaveform
import com.nsvault.app.designsystem.component.RecordControlButton
import com.nsvault.app.designsystem.component.RecordControlStyle
import com.nsvault.app.designsystem.component.VaultDialog
import com.nsvault.app.designsystem.component.VaultTextButton
import com.nsvault.app.designsystem.component.VaultTopBar
import com.nsvault.app.designsystem.layout.VaultLayout
import com.nsvault.app.designsystem.layout.vaultContentWidth
import com.nsvault.app.designsystem.theme.Dimens
import com.nsvault.app.designsystem.theme.VaultColors
import kotlinx.coroutines.launch

@UnstableApi
@Composable
fun PlayerScreen(
    onBack: () -> Unit,
    viewModel: PlayerViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showShareConfirm by remember { mutableStateOf(false) }

    // Pause playback whenever the screen leaves the foreground.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) viewModel.pausePlayback()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    fun launchShare() {
        scope.launch {
            val uri = viewModel.exportForSharing() ?: return@launch
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "audio/*"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(send, "Share recording"))
        }
    }

    AuroraBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            VaultTopBar(
                title = "Now Playing",
                onBack = onBack,
                actions = {
                    if (uiState.recording != null && !uiState.error) {
                        IconButton(onClick = { showShareConfirm = true }) {
                            Icon(
                                imageVector = Icons.Rounded.IosShare,
                                contentDescription = "Share recording",
                                tint = VaultColors.TextSecondary,
                            )
                        }
                    }
                },
            )

            when {
                uiState.error -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    ErrorState(
                        icon = Icons.Rounded.ErrorOutline,
                        title = "Playback failed",
                        message = "This recording couldn't be decrypted or read. " +
                            "Try again, or check storage health.",
                        retryText = "Go back",
                        onRetry = onBack,
                    )
                }

                uiState.recording != null -> PlayerContent(
                    uiState = uiState,
                    onTogglePlay = viewModel::togglePlayPause,
                    onSkipBack = { viewModel.skip(-SKIP_BACK_MS) },
                    onSkipForward = { viewModel.skip(SKIP_FORWARD_MS) },
                    onScrub = viewModel::onScrub,
                    onScrubEnd = viewModel::onScrubEnd,
                    onCycleSpeed = viewModel::cycleSpeed,
                )
            }
        }
    }

    if (showShareConfirm) {
        val activity = context.findFragmentActivity()
        VaultDialog(
            title = "Share a decrypted copy?",
            message = "A decrypted copy of this recording will leave the vault and be handed " +
                "to the app you choose. The vault original stays encrypted.",
            confirmText = "Share",
            onConfirm = {
                showShareConfirm = false
                if (viewModel.canUseBiometric && activity != null) {
                    viewModel.authenticateForShare(activity) { launchShare() }
                } else {
                    launchShare()
                }
            },
            dismissText = "Cancel",
            onDismissRequest = { showShareConfirm = false },
            icon = Icons.Rounded.IosShare,
        )
    }
}

@Composable
private fun PlayerContent(
    uiState: PlayerViewModel.UiState,
    onTogglePlay: () -> Unit,
    onSkipBack: () -> Unit,
    onSkipForward: () -> Unit,
    onScrub: (Float) -> Unit,
    onScrubEnd: (Float) -> Unit,
    onCycleSpeed: () -> Unit,
) {
    val recording = uiState.recording ?: return

    Column(
        modifier = Modifier
            .vaultContentWidth(VaultLayout.FocusMaxWidth)
            .fillMaxSize()
            .padding(horizontal = Dimens.screenPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.weight(0.8f))

        Text(
            text = recording.title,
            style = MaterialTheme.typography.headlineSmall,
            color = VaultColors.TextPrimary,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(Dimens.spaceXxs))
        Text(
            text = TimeFormats.dateTime(recording.createdAtEpochMs),
            style = MaterialTheme.typography.bodyMedium,
            color = VaultColors.TextTertiary,
        )

        Spacer(modifier = Modifier.weight(1f))

        PlaybackWaveform(
            waveform = uiState.waveform,
            progress = uiState.progress,
            onScrub = onScrub,
            onScrubEnd = onScrubEnd,
            modifier = Modifier
                .fillMaxWidth()
                .height(Dimens.waveformHeight),
        )

        Spacer(modifier = Modifier.height(Dimens.spaceSm))

        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = TimeFormats.clock(uiState.positionMs),
                style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                color = VaultColors.TextSecondary,
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = TimeFormats.clock(uiState.durationMs),
                style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                color = VaultColors.TextTertiary,
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Row(
            horizontalArrangement = Arrangement.spacedBy(Dimens.spaceXl),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RecordControlButton(
                icon = Icons.Rounded.Replay10,
                contentDescription = "Back 10 seconds",
                onClick = onSkipBack,
                style = RecordControlStyle.Glass,
                size = Dimens.fabSize,
            )
            RecordControlButton(
                icon = if (uiState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = if (uiState.isPlaying) "Pause" else "Play",
                onClick = onTogglePlay,
                style = RecordControlStyle.Primary,
                size = Dimens.recordButtonSize,
            )
            RecordControlButton(
                icon = Icons.Rounded.Forward30,
                contentDescription = "Forward 30 seconds",
                onClick = onSkipForward,
                style = RecordControlStyle.Glass,
                size = Dimens.fabSize,
            )
        }

        Spacer(modifier = Modifier.height(Dimens.spaceMd))

        VaultTextButton(
            text = "Speed ${formatSpeed(uiState.speed)}",
            onClick = onCycleSpeed,
        )

        Spacer(modifier = Modifier.weight(0.8f))
    }
}

private fun formatSpeed(speed: Float): String =
    if (speed == speed.toInt().toFloat()) "${speed.toInt()}×" else "$speed×"

private const val SKIP_BACK_MS = 10_000L
private const val SKIP_FORWARD_MS = 30_000L
