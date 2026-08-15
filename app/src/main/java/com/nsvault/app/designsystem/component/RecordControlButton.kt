package com.nsvault.app.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import com.nsvault.app.designsystem.theme.Dimens
import com.nsvault.app.designsystem.theme.VaultColors
import com.nsvault.app.designsystem.theme.VaultGradients

enum class RecordControlStyle {
    /** Aurora gradient — the main transport action. */
    Primary,

    /** Glass surface — secondary transport actions (pause, resume). */
    Glass,

    /** Recording red — stop and cancel. */
    Danger,
}

/**
 * Circular transport control for the recording and playback screens.
 */
@Composable
fun RecordControlButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: RecordControlStyle = RecordControlStyle.Glass,
    size: Dp = Dimens.recordControlSize,
    enabled: Boolean = true,
) {
    val interaction = remember { MutableInteractionSource() }
    val scale by interaction.animatePressScale(pressedScale = 0.9f)
    val haptics = rememberVaultHaptics()

    val background = when (style) {
        RecordControlStyle.Primary -> Modifier.background(VaultGradients.Aurora)
        RecordControlStyle.Glass -> Modifier
            .background(VaultColors.GlassFill)
            .border(Dimens.hairline, VaultGradients.GlassBorder, CircleShape)
        RecordControlStyle.Danger -> Modifier.background(VaultColors.RecordingRed.copy(alpha = 0.16f))
    }

    val tint = when (style) {
        RecordControlStyle.Primary -> VaultColors.OnAccent
        RecordControlStyle.Glass -> VaultColors.TextPrimary
        RecordControlStyle.Danger -> VaultColors.RecordingRed
    }

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .size(size)
            .clip(CircleShape)
            .then(background)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = { haptics.tap(); onClick() },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (enabled) tint else VaultColors.TextTertiary,
            modifier = Modifier.size(Dimens.iconSize + Dimens.spaceXxs),
        )
    }
}
