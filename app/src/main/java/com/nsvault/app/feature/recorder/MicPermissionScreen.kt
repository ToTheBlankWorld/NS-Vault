package com.nsvault.app.feature.recorder

import android.content.Intent
import android.net.Uri
import android.provider.Settings
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
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import com.nsvault.app.designsystem.component.AuroraBackground
import com.nsvault.app.designsystem.component.VaultPrimaryButton
import com.nsvault.app.designsystem.component.VaultTextButton
import com.nsvault.app.designsystem.layout.VaultLayout
import com.nsvault.app.designsystem.layout.vaultContentWidth
import com.nsvault.app.designsystem.theme.Dimens
import com.nsvault.app.designsystem.theme.VaultColors
import com.nsvault.app.designsystem.theme.VaultGradients

/**
 * Premium microphone-permission explanation. Shown instead of firing
 * system dialogs blindly; once the system says "never ask again" it
 * routes to app settings rather than nagging.
 */
@Composable
fun MicPermissionScreen(
    permanentlyDenied: Boolean,
    onRequest: () -> Unit,
    onNotNow: () -> Unit,
) {
    val context = LocalContext.current

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
                        .size(Dimens.recordButtonSize)
                        .background(VaultGradients.Aurora, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Mic,
                        contentDescription = null,
                        tint = VaultColors.OnAccent,
                        modifier = Modifier.size(Dimens.spaceXl),
                    )
                }
                Spacer(modifier = Modifier.height(Dimens.spaceLg))
                Text(
                    text = "Your voice, your vault",
                    style = MaterialTheme.typography.headlineMedium,
                    color = VaultColors.TextPrimary,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(Dimens.spaceXs))
                Text(
                    text = if (permanentlyDenied) {
                        "Microphone access is turned off for NS Vault. " +
                            "Enable it in system settings to start recording."
                    } else {
                        "NS Vault needs the microphone to record. " +
                            "Audio is stored only on this phone — the app has no internet access at all."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = VaultColors.TextSecondary,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(Dimens.spaceXl))
                if (permanentlyDenied) {
                    VaultPrimaryButton(
                        text = "Open settings",
                        onClick = {
                            context.startActivity(
                                Intent(
                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    Uri.fromParts("package", context.packageName, null),
                                ),
                            )
                        },
                    )
                } else {
                    VaultPrimaryButton(
                        text = "Allow microphone",
                        onClick = onRequest,
                        icon = Icons.Rounded.Mic,
                    )
                }
                Spacer(modifier = Modifier.height(Dimens.spaceSm))
                VaultTextButton(text = "Not now", onClick = onNotNow)
            }
        }
    }
}
