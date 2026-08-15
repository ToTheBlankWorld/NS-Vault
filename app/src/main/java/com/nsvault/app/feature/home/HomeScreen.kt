package com.nsvault.app.feature.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nsvault.app.core.common.ByteFormat
import com.nsvault.app.core.common.TimeFormats
import com.nsvault.app.designsystem.component.AuroraBackground
import com.nsvault.app.designsystem.component.EmptyState
import com.nsvault.app.designsystem.component.GlassCard
import com.nsvault.app.designsystem.component.RecordingCard
import com.nsvault.app.designsystem.component.StatCard
import com.nsvault.app.designsystem.component.VaultBottomSheet
import com.nsvault.app.designsystem.component.VaultDialog
import com.nsvault.app.designsystem.component.VaultTextButton
import com.nsvault.app.designsystem.layout.vaultContentWidth
import com.nsvault.app.designsystem.theme.Dimens
import com.nsvault.app.designsystem.theme.VaultColors
import com.nsvault.app.designsystem.theme.VaultGradients
import com.nsvault.app.designsystem.theme.VaultMotion
import com.nsvault.app.domain.model.Recording
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HomeScreen(
    onStartRecording: () -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenRecording: (Long) -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val greeting = remember { greetingForNow() }
    val today = remember {
        LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault()))
    }

    var selectedRecording by remember { mutableStateOf<Recording?>(null) }
    var deleteTarget by remember { mutableStateOf<Recording?>(null) }

    AuroraBackground {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = Dimens.screenPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(
                    modifier = Modifier
                        .vaultContentWidth()
                        .weight(1f),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = Dimens.spaceMd),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = greeting,
                                style = MaterialTheme.typography.headlineMedium,
                                color = VaultColors.TextPrimary,
                            )
                            Text(
                                text = today,
                                style = MaterialTheme.typography.bodyMedium,
                                color = VaultColors.TextTertiary,
                            )
                        }
                        Row {
                            IconButton(onClick = onOpenLibrary) {
                                Icon(
                                    imageVector = Icons.Rounded.LibraryMusic,
                                    contentDescription = "Library",
                                    tint = VaultColors.TextSecondary,
                                )
                            }
                            IconButton(onClick = onOpenSettings) {
                                Icon(
                                    imageVector = Icons.Rounded.Settings,
                                    contentDescription = "Settings",
                                    tint = VaultColors.TextSecondary,
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(Dimens.spaceLg))

                    Row(horizontalArrangement = Arrangement.spacedBy(Dimens.spaceXs)) {
                        StatCard(
                            label = "Recordings",
                            value = uiState.stats.recordingCount.toString(),
                            modifier = Modifier.weight(1f),
                        )
                        StatCard(
                            label = "Hours",
                            value = TimeFormats.decimalHours(uiState.stats.totalDurationMs),
                            modifier = Modifier.weight(1f),
                        )
                        StatCard(
                            label = "Storage",
                            value = ByteFormat.format(uiState.stats.totalSizeBytes),
                            valueStyle = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.weight(1f),
                        )
                    }

                    Spacer(modifier = Modifier.height(Dimens.spaceMd))

                    Text(
                        text = "All Recordings",
                        style = MaterialTheme.typography.titleMedium,
                        color = VaultColors.TextPrimary,
                    )
                    if (uiState.recordings.isNotEmpty()) {
                        Text(
                            text = "${uiState.recordings.size} recording${if (uiState.recordings.size != 1) "s" else ""}",
                            style = MaterialTheme.typography.bodySmall,
                            color = VaultColors.TextTertiary,
                        )
                    }

                    Spacer(modifier = Modifier.height(Dimens.spaceXs))

                    if (uiState.recordings.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center,
                        ) {
                            EmptyState(
                                icon = Icons.Rounded.GraphicEq,
                                title = "Your vault is empty",
                                message = "The first memory is one tap away",
                                actionText = "Record now",
                                onAction = onStartRecording,
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(Dimens.spaceXs),
                        ) {
                            items(uiState.recordings, key = { it.uuid }) { recording ->
                                RecordingCard(
                                    title = recording.title,
                                    dateText = TimeFormats.dateTime(recording.createdAtEpochMs),
                                    durationText = TimeFormats.clock(recording.durationMs),
                                    isFavorite = recording.isFavorite,
                                    onClick = { onOpenRecording(recording.id) },
                                    onMore = { selectedRecording = recording },
                                    onLongClick = { selectedRecording = recording },
                                )
                            }
                            item {
                                Spacer(modifier = Modifier.height(Dimens.recordButtonSize + Dimens.spaceXxl))
                            }
                        }
                    }
                }
            }

            RecordFAB(
                isRecording = uiState.isRecording,
                onClick = onStartRecording,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = Dimens.spaceXl),
            )
        }
    }

    selectedRecording?.let { recording ->
        RecordingActionsSheet(
            recording = recording,
            onDismiss = { selectedRecording = null },
            onToggleFavorite = {
                viewModel.toggleFavorite(recording)
                selectedRecording = null
            },
            onOpen = {
                selectedRecording = null
                onOpenRecording(recording.id)
            },
            onDelete = {
                selectedRecording = null
                deleteTarget = recording
            },
        )
    }

    deleteTarget?.let { recording ->
        VaultDialog(
            title = "Delete recording?",
            message = "\"${recording.title}\" will be erased from the vault permanently. This cannot be undone.",
            confirmText = "Delete",
            onConfirm = {
                viewModel.deleteRecording(recording)
                deleteTarget = null
            },
            dismissText = "Cancel",
            onDismissRequest = { deleteTarget = null },
            icon = Icons.Rounded.Delete,
            destructive = true,
        )
    }

    val recovery = uiState.recovery
    if (recovery != null) {
        VaultDialog(
            title = "Unsaved recording found",
            message = "A recording from ${TimeFormats.dateTime(recovery.createdAtEpochMs)} " +
                "(${ByteFormat.format(recovery.sizeBytes)}) wasn't saved. Recover it into your vault?",
            confirmText = "Recover",
            onConfirm = viewModel::recoverCapture,
            dismissText = "Delete",
            onDismiss = viewModel::discardCapture,
            onDismissRequest = viewModel::dismissRecovery,
            icon = Icons.Rounded.Restore,
        )
    }
}

@Composable
private fun RecordFAB(
    isRecording: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val breathing = rememberInfiniteTransition(label = "recordFAB")
    val haloScale by breathing.animateFloat(
        initialValue = VaultMotion.PulseMinScale,
        targetValue = VaultMotion.PulseMaxScale,
        animationSpec = infiniteRepeatable(
            animation = tween(VaultMotion.PulsePeriodMs, easing = VaultMotion.EasingEmphasized),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "haloScale",
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(Dimens.recordButtonSize + Dimens.spaceLg)
                .scale(haloScale)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            if (isRecording) VaultColors.RecordingRed.copy(alpha = 0.3f)
                            else VaultColors.AuroraViolet.copy(alpha = 0.25f),
                            Color.Transparent,
                        ),
                    ),
                    shape = CircleShape,
                ),
        )
        Box(
            modifier = Modifier
                .size(Dimens.recordButtonSize)
                .background(brush = VaultGradients.Aurora, shape = CircleShape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (isRecording) Icons.Rounded.GraphicEq else Icons.Rounded.Mic,
                contentDescription = if (isRecording) "Return to recording" else "Start recording",
                tint = VaultColors.OnAccent,
                modifier = Modifier.size(Dimens.spaceXl),
            )
        }
        if (isRecording) {
            Text(
                text = "Recording...",
                style = MaterialTheme.typography.labelSmall,
                color = VaultColors.RecordingRed,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(top = Dimens.spaceSm),
            )
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun RecordingActionsSheet(
    recording: Recording,
    onDismiss: () -> Unit,
    onToggleFavorite: () -> Unit,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
) {
    VaultBottomSheet(onDismissRequest = onDismiss) {
        Text(
            text = recording.title,
            style = MaterialTheme.typography.titleMedium,
            color = VaultColors.TextPrimary,
        )
        Text(
            text = TimeFormats.dateTime(recording.createdAtEpochMs) +
                " · " + TimeFormats.clock(recording.durationMs) +
                " · " + ByteFormat.format(recording.sizeBytes),
            style = MaterialTheme.typography.bodySmall,
            color = VaultColors.TextTertiary,
        )
        Spacer(modifier = Modifier.height(Dimens.spaceMd))
        SheetAction(
            icon = if (recording.isFavorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
            label = if (recording.isFavorite) "Remove from favorites" else "Add to favorites",
            tint = if (recording.isFavorite) VaultColors.Gold else VaultColors.TextPrimary,
            onClick = onToggleFavorite,
        )
        SheetAction(
            icon = Icons.Rounded.LibraryMusic,
            label = "Open",
            tint = VaultColors.TextPrimary,
            onClick = onOpen,
        )
        SheetAction(
            icon = Icons.Rounded.Delete,
            label = "Delete",
            tint = VaultColors.Error,
            onClick = onDelete,
        )
    }
}

@Composable
private fun SheetAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = Dimens.spaceXs, vertical = Dimens.spaceMd),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(Dimens.iconSize),
        )
        Spacer(modifier = Modifier.size(Dimens.spaceMd))
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            color = tint,
        )
    }
}

private fun greetingForNow(): String {
    val hour = LocalTime.now().hour
    return when {
        hour < 5 -> "Still awake?"
        hour < 12 -> "Good morning"
        hour < 17 -> "Good afternoon"
        else -> "Good evening"
    }
}
