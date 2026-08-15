package com.nsvault.app.feature.library

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DriveFileRenameOutline
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nsvault.app.core.common.ByteFormat
import com.nsvault.app.core.common.TimeFormats
import com.nsvault.app.designsystem.component.AuroraBackground
import com.nsvault.app.designsystem.component.EmptyState
import com.nsvault.app.designsystem.component.GlassCard
import com.nsvault.app.designsystem.component.RecordingCard
import com.nsvault.app.designsystem.component.VaultBottomSheet
import com.nsvault.app.designsystem.component.VaultDialog
import com.nsvault.app.designsystem.component.VaultPrimaryButton
import com.nsvault.app.designsystem.component.VaultProgressBar
import com.nsvault.app.designsystem.component.VaultSearchField
import com.nsvault.app.designsystem.component.VaultTextButton
import com.nsvault.app.designsystem.component.VaultTopBar
import com.nsvault.app.designsystem.layout.vaultContentWidth
import com.nsvault.app.designsystem.theme.Dimens
import com.nsvault.app.designsystem.theme.VaultColors
import com.nsvault.app.domain.model.Recording

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibraryScreen(
    onBack: () -> Unit,
    onOpenRecording: (Long) -> Unit,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var renameTarget by remember { mutableStateOf<Recording?>(null) }
    var deleteTarget by remember { mutableStateOf<Recording?>(null) }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments(),
    ) { uris -> viewModel.import(uris) }

    AuroraBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            VaultTopBar(
                title = "Library",
                onBack = onBack,
                actions = {
                    IconButton(onClick = { importLauncher.launch(arrayOf("audio/*")) }) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = "Import recordings",
                            tint = VaultColors.TextSecondary,
                        )
                    }
                },
            )

            Column(modifier = Modifier.vaultContentWidth().weight(1f)) {
                VaultSearchField(
                    value = uiState.query,
                    onValueChange = viewModel::onQueryChange,
                    placeholder = "Search recordings",
                    modifier = Modifier.padding(
                        horizontal = Dimens.screenPadding,
                        vertical = Dimens.spaceXs,
                    ),
                )

                FilterChips(
                    selected = uiState.filter,
                    onSelect = viewModel::onFilterChange,
                    modifier = Modifier.padding(horizontal = Dimens.screenPadding),
                )

                AnimatedVisibility(visible = uiState.importing != null || uiState.importMessage != null) {
                    ImportBanner(
                        importing = uiState.importing,
                        message = uiState.importMessage,
                    )
                }

                when {
                    !uiState.hasAnyRecordings -> Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        EmptyState(
                            icon = Icons.Rounded.GraphicEq,
                            title = "Your vault is empty",
                            message = "Record your first memory, or bring in recordings you already have.",
                            actionText = "Import recordings",
                            onAction = { importLauncher.launch(arrayOf("audio/*")) },
                        )
                    }

                    uiState.groups.isEmpty() -> Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        EmptyState(
                            icon = Icons.Rounded.GraphicEq,
                            title = "Nothing matches",
                            message = "No recordings match \"${uiState.query}\".",
                        )
                    }

                    else -> LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            start = Dimens.screenPadding,
                            end = Dimens.screenPadding,
                            bottom = Dimens.spaceXl,
                        ),
                        verticalArrangement = Arrangement.spacedBy(Dimens.spaceXs),
                    ) {
                        uiState.groups.forEach { group ->
                            stickyHeader(key = group.label) {
                                MonthHeader(label = group.label)
                            }
                            items(group.recordings, key = { it.uuid }) { recording ->
                                RecordingCard(
                                    title = recording.title,
                                    dateText = TimeFormats.dateTime(recording.createdAtEpochMs),
                                    durationText = TimeFormats.clock(recording.durationMs),
                                    isFavorite = recording.isFavorite,
                                    onClick = { onOpenRecording(recording.id) },
                                    onMore = { viewModel.onSelect(recording) },
                                    onLongClick = { viewModel.onSelect(recording) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    uiState.selected?.let { recording ->
        RecordingActionsSheet(
            recording = recording,
            onDismiss = { viewModel.onSelect(null) },
            onToggleFavorite = {
                viewModel.toggleFavorite(recording)
                viewModel.onSelect(null)
            },
            onRename = {
                viewModel.onSelect(null)
                renameTarget = recording
            },
            onDelete = {
                viewModel.onSelect(null)
                deleteTarget = recording
            },
        )
    }

    renameTarget?.let { recording ->
        RenameDialog(
            currentTitle = recording.title,
            onConfirm = { newTitle ->
                viewModel.rename(recording, newTitle)
                renameTarget = null
            },
            onDismiss = { renameTarget = null },
        )
    }

    deleteTarget?.let { recording ->
        VaultDialog(
            title = "Delete recording?",
            message = "\"${recording.title}\" will be erased from the vault permanently. " +
                "This cannot be undone.",
            confirmText = "Delete",
            onConfirm = {
                viewModel.delete(recording)
                deleteTarget = null
            },
            dismissText = "Cancel",
            onDismissRequest = { deleteTarget = null },
            icon = Icons.Rounded.DeleteOutline,
            destructive = true,
        )
    }
}

@Composable
private fun MonthHeader(label: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(VaultColors.Obsidian.copy(alpha = 0.94f))
            .padding(vertical = Dimens.spaceXs),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = VaultColors.TextSecondary,
        )
    }
}

@Composable
private fun ImportBanner(
    importing: LibraryViewModel.ImportState?,
    message: String?,
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.screenPadding, vertical = Dimens.spaceXxs),
        contentPadding = Dimens.spaceSm,
    ) {
        if (importing != null) {
            Text(
                text = "Importing ${importing.current} of ${importing.total}…",
                style = MaterialTheme.typography.bodySmall,
                color = VaultColors.TextSecondary,
            )
            Spacer(modifier = Modifier.height(Dimens.spaceXs))
            VaultProgressBar(progress = importing.progress, modifier = Modifier.fillMaxWidth())
        } else if (message != null) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = VaultColors.TextSecondary,
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
    onRename: () -> Unit,
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
            icon = Icons.Rounded.DriveFileRenameOutline,
            label = "Rename",
            tint = VaultColors.TextPrimary,
            onClick = onRename,
        )
        SheetAction(
            icon = Icons.Rounded.DeleteOutline,
            label = "Delete",
            tint = VaultColors.Error,
            onClick = onDelete,
        )
    }
}

@Composable
private fun RenameDialog(
    currentTitle: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var title by rememberSaveable(currentTitle) { mutableStateOf(currentTitle) }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.extraLarge)
                .background(VaultColors.SurfaceRaised)
                .padding(Dimens.spaceLg),
        ) {
            Text(
                text = "Rename recording",
                style = MaterialTheme.typography.titleLarge,
                color = VaultColors.TextPrimary,
            )
            Spacer(modifier = Modifier.height(Dimens.spaceMd))
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = VaultColors.TextPrimary,
                    unfocusedTextColor = VaultColors.TextPrimary,
                    focusedBorderColor = VaultColors.AuroraViolet,
                    unfocusedBorderColor = VaultColors.Hairline,
                    cursorColor = VaultColors.AuroraViolet,
                ),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(Dimens.spaceLg))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimens.spaceSm, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                VaultTextButton(text = "Cancel", onClick = onDismiss)
                VaultPrimaryButton(
                    text = "Save",
                    onClick = { onConfirm(title) },
                    enabled = title.isNotBlank(),
                )
            }
        }
    }
}

@Composable
private fun FilterChips(
    selected: LibraryViewModel.Filter,
    onSelect: (LibraryViewModel.Filter) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.selectableGroup().fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Dimens.spaceXs),
    ) {
        LibraryViewModel.Filter.entries.forEach { filter ->
            val isSelected = filter == selected
            Box(
                modifier = Modifier
                    .selectable(
                        selected = isSelected,
                        onClick = { onSelect(filter) },
                        role = androidx.compose.ui.semantics.Role.Tab,
                    )
                    .background(
                        color = if (isSelected) VaultColors.AuroraViolet.copy(alpha = 0.2f)
                        else VaultColors.SurfaceHigh,
                        shape = RoundedCornerShape(Dimens.spaceMd),
                    )
                    .padding(horizontal = Dimens.spaceMd, vertical = Dimens.spaceXs),
            ) {
                Text(
                    text = when (filter) {
                        LibraryViewModel.Filter.All -> "All"
                        LibraryViewModel.Filter.Favorites -> "Favorites"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isSelected) VaultColors.AuroraCyan else VaultColors.TextSecondary,
                )
            }
        }
    }
}

@Composable
private fun SheetAction(
    icon: ImageVector,
    label: String,
    tint: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .heightIn(min = Dimens.buttonHeight)
            .padding(horizontal = Dimens.spaceXs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(Dimens.iconSize),
        )
        Spacer(modifier = Modifier.width(Dimens.spaceMd))
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            color = tint,
        )
    }
}
