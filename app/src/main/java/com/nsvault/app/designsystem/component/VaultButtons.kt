package com.nsvault.app.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import com.nsvault.app.designsystem.theme.Dimens
import com.nsvault.app.designsystem.theme.VaultColors
import com.nsvault.app.designsystem.theme.VaultGradients

/**
 * Filled pill button carrying the Aurora gradient. The single
 * highest-emphasis action on any screen.
 */
@Composable
fun VaultPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val scale by interaction.animatePressScale()
    val haptics = rememberVaultHaptics()

    Row(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .heightIn(min = Dimens.buttonHeight)
            .clip(CircleShape)
            .then(
                if (enabled) {
                    Modifier.background(VaultGradients.Aurora)
                } else {
                    Modifier.background(VaultColors.SurfaceHigh)
                },
            )
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = { haptics.tap(); onClick() },
            )
            .padding(horizontal = Dimens.spaceXl),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) VaultColors.OnAccent else VaultColors.TextTertiary,
                modifier = Modifier.size(Dimens.iconSize),
            )
            Spacer(modifier = Modifier.width(Dimens.spaceXs))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = if (enabled) VaultColors.OnAccent else VaultColors.TextTertiary,
        )
    }
}

/**
 * Glass pill button for medium-emphasis actions that sit alongside
 * a primary action.
 */
@Composable
fun VaultSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val scale by interaction.animatePressScale()
    val haptics = rememberVaultHaptics()

    Row(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .heightIn(min = Dimens.buttonHeight)
            .clip(CircleShape)
            .background(VaultColors.GlassFill)
            .border(Dimens.hairline, VaultGradients.GlassBorder, CircleShape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = { haptics.tap(); onClick() },
            )
            .padding(horizontal = Dimens.spaceXl),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) VaultColors.TextPrimary else VaultColors.TextTertiary,
                modifier = Modifier.size(Dimens.iconSize),
            )
            Spacer(modifier = Modifier.width(Dimens.spaceXs))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = if (enabled) VaultColors.TextPrimary else VaultColors.TextTertiary,
        )
    }
}

/**
 * Borderless low-emphasis action ("Not now", "Start over").
 */
@Composable
fun VaultTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val interaction = remember { MutableInteractionSource() }
    val scale by interaction.animatePressScale()
    val haptics = rememberVaultHaptics()

    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = if (enabled) VaultColors.TextSecondary else VaultColors.TextTertiary,
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = { haptics.tap(); onClick() },
            )
            .heightIn(min = Dimens.minTouchTarget)
            .padding(horizontal = Dimens.spaceMd, vertical = Dimens.spaceSm),
    )
}

/**
 * Circular Aurora floating action button.
 */
@Composable
fun VaultFab(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val scale by interaction.animatePressScale()
    val haptics = rememberVaultHaptics()

    Row(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .size(Dimens.fabSize)
            .clip(CircleShape)
            .background(VaultGradients.Aurora)
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = { haptics.tap(); onClick() },
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = VaultColors.OnAccent,
            modifier = Modifier.size(Dimens.iconSize),
        )
    }
}
