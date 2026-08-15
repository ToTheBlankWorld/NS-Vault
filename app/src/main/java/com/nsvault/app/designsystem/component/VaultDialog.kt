package com.nsvault.app.designsystem.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.window.Dialog
import com.nsvault.app.designsystem.theme.Dimens
import com.nsvault.app.designsystem.theme.VaultColors
import com.nsvault.app.designsystem.theme.VaultGradients
import com.nsvault.app.designsystem.theme.VaultMotion

/**
 * Themed dialog with the vault's scale-and-fade entrance. Destructive
 * confirmations set [destructive] so the confirm action reads in red.
 */
@Composable
fun VaultDialog(
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    dismissText: String? = null,
    /** Distinct action for the dismiss button; falls back to [onDismissRequest]. */
    onDismiss: (() -> Unit)? = null,
    icon: ImageVector? = null,
    destructive: Boolean = false,
) {
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }

    val scale by animateFloatAsState(
        targetValue = if (entered) 1f else VaultMotion.DialogEnterScaleFrom,
        animationSpec = VaultMotion.SpringGentle,
        label = "dialogScale",
    )
    val alpha by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = VaultMotion.enter(VaultMotion.DurationShort),
        label = "dialogAlpha",
    )

    Dialog(onDismissRequest = onDismissRequest) {
        Column(
            modifier = modifier
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    this.alpha = alpha
                }
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.extraLarge)
                .background(VaultColors.SurfaceRaised)
                .border(Dimens.hairline, VaultGradients.GlassBorder, MaterialTheme.shapes.extraLarge)
                .padding(Dimens.spaceLg),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (destructive) VaultColors.Error else VaultColors.AuroraIndigo,
                    modifier = Modifier.size(Dimens.spaceXl),
                )
                Spacer(modifier = Modifier.height(Dimens.spaceMd))
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = VaultColors.TextPrimary,
            )
            Spacer(modifier = Modifier.height(Dimens.spaceXs))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = VaultColors.TextSecondary,
            )
            Spacer(modifier = Modifier.height(Dimens.spaceLg))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimens.spaceSm, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (dismissText != null) {
                    VaultTextButton(text = dismissText, onClick = onDismiss ?: onDismissRequest)
                }
                if (destructive) {
                    VaultTextButton(text = confirmText, onClick = onConfirm)
                } else {
                    VaultPrimaryButton(text = confirmText, onClick = onConfirm)
                }
            }
        }
    }
}
