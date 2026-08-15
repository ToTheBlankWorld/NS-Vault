package com.nsvault.app.feature.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Password
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nsvault.app.BuildConfig
import com.nsvault.app.core.common.ByteFormat
import com.nsvault.app.core.common.TimeFormats
import com.nsvault.app.core.common.findFragmentActivity
import com.nsvault.app.designsystem.component.AuroraBackground
import com.nsvault.app.designsystem.component.GlassCard
import com.nsvault.app.designsystem.component.SettingsRow
import com.nsvault.app.designsystem.component.SettingsSwitchRow
import com.nsvault.app.designsystem.component.VaultBottomSheet
import com.nsvault.app.designsystem.component.VaultProgressBar
import com.nsvault.app.designsystem.component.VaultTopBar
import com.nsvault.app.designsystem.layout.vaultContentWidth
import com.nsvault.app.designsystem.theme.Dimens
import com.nsvault.app.designsystem.theme.VaultColors
import com.nsvault.app.domain.model.AudioQuality
import com.nsvault.app.domain.model.ThemeMode

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onChangePin: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val biometricEnabled by viewModel.biometricEnabled.collectAsStateWithLifecycle()
    val audioQuality by viewModel.audioQuality.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    val freeBytes by viewModel.freeBytes.collectAsStateWithLifecycle()
    val importing by viewModel.importing.collectAsStateWithLifecycle()
    val importMessage by viewModel.importMessage.collectAsStateWithLifecycle()
    val activity = LocalContext.current.findFragmentActivity()

    var showQualitySheet by remember { mutableStateOf(false) }
    var showThemeSheet by remember { mutableStateOf(false) }

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
            VaultTopBar(title = "Settings", onBack = onBack)
            Column(
                modifier = Modifier
                    .vaultContentWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Dimens.screenPadding),
            ) {
                AnimatedVisibility(visible = importing != null || importMessage != null) {
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = Dimens.spaceSm),
                        contentPadding = Dimens.spaceSm,
                    ) {
                        val currentImport = importing
                        if (currentImport != null) {
                            Text(
                                text = "Importing ${currentImport.current} of ${currentImport.total}…",
                                style = MaterialTheme.typography.bodySmall,
                                color = VaultColors.TextSecondary,
                            )
                            Spacer(modifier = Modifier.height(Dimens.spaceXs))
                            VaultProgressBar(
                                progress = currentImport.progress,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        } else if (importMessage != null) {
                            Text(
                                text = importMessage.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = VaultColors.TextSecondary,
                            )
                        }
                    }
                }

                SectionLabel("Security")
                GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = Dimens.spaceXs) {
                    SettingsRow(
                        icon = Icons.Rounded.Password,
                        title = "Change PIN",
                        subtitle = "Update your vault PIN",
                        onClick = onChangePin,
                    )
                    if (viewModel.biometricAvailable) {
                        SettingsSwitchRow(
                            icon = Icons.Rounded.Fingerprint,
                            title = "Fingerprint unlock",
                            subtitle = "Open the vault with a touch",
                            checked = biometricEnabled,
                            onCheckedChange = { enabled ->
                                viewModel.onBiometricToggle(enabled, activity)
                            },
                        )
                    }
                }

                SectionLabel("Appearance")
                GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = Dimens.spaceXs) {
                    SettingsRow(
                        icon = Icons.Rounded.DarkMode,
                        title = "Theme",
                        subtitle = themeMode.label,
                        onClick = { showThemeSheet = true },
                    )
                }

                SectionLabel("Vault")
                GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = Dimens.spaceXs) {
                    SettingsRow(
                        icon = Icons.Rounded.FileDownload,
                        title = "Import recordings",
                        subtitle = "Encrypt existing audio files into the vault",
                        onClick = { importLauncher.launch(arrayOf("audio/*")) },
                    )
                    SettingsRow(
                        icon = Icons.Rounded.GraphicEq,
                        title = "Audio quality",
                        subtitle = audioQuality.label,
                        onClick = { showQualitySheet = true },
                    )
                    SettingsRow(
                        icon = Icons.Rounded.Storage,
                        title = "Storage",
                        subtitle = "${stats.recordingCount} recordings · " +
                            "${TimeFormats.decimalHours(stats.totalDurationMs)} h · " +
                            "${ByteFormat.format(stats.totalSizeBytes)} used · " +
                            "${ByteFormat.format(freeBytes)} free",
                        trailing = {},
                    )
                }

                SectionLabel("Privacy")
                GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = Dimens.spaceXs) {
                    SettingsRow(
                        icon = Icons.Rounded.WifiOff,
                        title = "Fully offline",
                        subtitle = "NS Vault has no internet permission — recordings can never leave this phone",
                        trailing = {},
                    )
                }

                SectionLabel("About")
                GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = Dimens.spaceXs) {
                    SettingsRow(
                        icon = Icons.Rounded.Info,
                        title = "Version",
                        subtitle = BuildConfig.VERSION_NAME,
                        trailing = {},
                    )
                }
                Spacer(modifier = Modifier.height(Dimens.spaceXl))
            }
        }
    }

    if (showThemeSheet) {
        VaultBottomSheet(onDismissRequest = { showThemeSheet = false }) {
            Text(
                text = "Theme",
                style = MaterialTheme.typography.titleMedium,
                color = VaultColors.TextPrimary,
            )
            Text(
                text = "Choose your preferred appearance",
                style = MaterialTheme.typography.bodySmall,
                color = VaultColors.TextTertiary,
            )
            Spacer(modifier = Modifier.height(Dimens.spaceMd))
            ThemeMode.entries.forEach { mode ->
                ThemeOption(
                    mode = mode,
                    selected = mode == themeMode,
                    onClick = {
                        viewModel.setThemeMode(mode)
                        showThemeSheet = false
                    },
                )
            }
        }
    }

    if (showQualitySheet) {
        VaultBottomSheet(onDismissRequest = { showQualitySheet = false }) {
            Text(
                text = "Audio quality",
                style = MaterialTheme.typography.titleMedium,
                color = VaultColors.TextPrimary,
            )
            Text(
                text = "Applies to new recordings",
                style = MaterialTheme.typography.bodySmall,
                color = VaultColors.TextTertiary,
            )
            Spacer(modifier = Modifier.height(Dimens.spaceMd))
            AudioQuality.entries.forEach { quality ->
                QualityOption(
                    quality = quality,
                    selected = quality == audioQuality,
                    onClick = {
                        viewModel.setAudioQuality(quality)
                        showQualitySheet = false
                    },
                )
            }
        }
    }
}

@Composable
private fun QualityOption(
    quality: AudioQuality,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.RadioButton, onClick = onClick)
            .heightIn(min = Dimens.buttonHeight)
            .padding(horizontal = Dimens.spaceXs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = quality.label,
                style = MaterialTheme.typography.titleSmall,
                color = if (selected) VaultColors.AuroraViolet else VaultColors.TextPrimary,
            )
            Text(
                text = quality.description,
                style = MaterialTheme.typography.bodySmall,
                color = VaultColors.TextTertiary,
            )
        }
        if (selected) {
            Spacer(modifier = Modifier.width(Dimens.spaceSm))
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = "Selected",
                tint = VaultColors.AuroraViolet,
                modifier = Modifier.size(Dimens.iconSize),
            )
        }
    }
}

@Composable
private fun ThemeOption(
    mode: ThemeMode,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.RadioButton, onClick = onClick)
            .heightIn(min = Dimens.buttonHeight)
            .padding(horizontal = Dimens.spaceXs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = mode.label,
            style = MaterialTheme.typography.titleSmall,
            color = if (selected) VaultColors.AuroraViolet else VaultColors.TextPrimary,
            modifier = Modifier.weight(1f),
        )
        if (selected) {
            Spacer(modifier = Modifier.width(Dimens.spaceSm))
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = "Selected",
                tint = VaultColors.AuroraViolet,
                modifier = Modifier.size(Dimens.iconSize),
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = VaultColors.TextTertiary,
        modifier = Modifier.padding(
            top = Dimens.spaceLg,
            bottom = Dimens.spaceXs,
            start = Dimens.spaceXxs,
        ),
    )
}
