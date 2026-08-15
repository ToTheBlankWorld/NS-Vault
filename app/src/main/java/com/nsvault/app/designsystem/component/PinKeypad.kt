package com.nsvault.app.designsystem.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import com.nsvault.app.designsystem.theme.Dimens
import com.nsvault.app.designsystem.theme.VaultColors
import com.nsvault.app.designsystem.theme.VaultGradients
import com.nsvault.app.designsystem.theme.VaultMotion
import kotlin.math.roundToInt

/** Optional bottom-left keypad slot (e.g. fingerprint). */
data class KeypadAction(
    val icon: ImageVector,
    val contentDescription: String,
    val onClick: () -> Unit,
)

/**
 * The vault's numeric keypad: large circular glass keys with press
 * physics and key-click haptics. Shared by PIN setup, lock, and
 * change-PIN flows so entering a PIN feels identical everywhere.
 */
@Composable
fun PinKeypad(
    onDigit: (Int) -> Unit,
    onBackspace: () -> Unit,
    modifier: Modifier = Modifier,
    leftAction: KeypadAction? = null,
    enabled: Boolean = true,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(Dimens.keypadRowSpacing),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        listOf(listOf(1, 2, 3), listOf(4, 5, 6), listOf(7, 8, 9)).forEach { rowDigits ->
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.keypadKeySpacing)) {
                rowDigits.forEach { digit ->
                    DigitKey(digit = digit, enabled = enabled, onDigit = onDigit)
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.keypadKeySpacing)) {
            if (leftAction != null) {
                IconKey(
                    icon = leftAction.icon,
                    contentDescription = leftAction.contentDescription,
                    enabled = enabled,
                    onClick = leftAction.onClick,
                )
            } else {
                Spacer(modifier = Modifier.size(Dimens.keypadKeySize))
            }
            DigitKey(digit = 0, enabled = enabled, onDigit = onDigit)
            IconKey(
                icon = Icons.AutoMirrored.Rounded.Backspace,
                contentDescription = "Delete",
                enabled = enabled,
                onClick = onBackspace,
            )
        }
    }
}

@Composable
private fun DigitKey(
    digit: Int,
    enabled: Boolean,
    onDigit: (Int) -> Unit,
) {
    KeypadKey(
        enabled = enabled,
        contentDescription = digit.toString(),
        onClick = { onDigit(digit) },
    ) {
        Text(
            text = digit.toString(),
            style = MaterialTheme.typography.headlineMedium,
            color = VaultColors.TextPrimary,
        )
    }
}

@Composable
private fun IconKey(
    icon: ImageVector,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    KeypadKey(
        enabled = enabled,
        contentDescription = contentDescription,
        onClick = onClick,
        showSurface = false,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = VaultColors.TextSecondary,
            modifier = Modifier.size(Dimens.iconSize + Dimens.spaceXxs),
        )
    }
}

@Composable
private fun KeypadKey(
    enabled: Boolean,
    contentDescription: String,
    onClick: () -> Unit,
    showSurface: Boolean = true,
    content: @Composable () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val scale by interaction.animatePressScale(pressedScale = 0.92f)
    val haptics = rememberVaultHaptics()

    Box(
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .size(Dimens.keypadKeySize)
            .clip(CircleShape)
            .then(
                if (showSurface) {
                    Modifier
                        .background(VaultColors.GlassFill)
                        .border(Dimens.hairline, VaultGradients.GlassBorder, CircleShape)
                } else {
                    Modifier
                },
            )
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = {
                    haptics.tap()
                    onClick()
                },
            )
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
private fun Dot(
    isFilled: Boolean,
    fillColor: androidx.compose.ui.graphics.Color,
) {
    Box(
        modifier = Modifier
            .size(Dimens.pinDotSize)
            .clip(CircleShape)
            .border(Dimens.hairline, VaultColors.TextTertiary, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedVisibility(
            visible = isFilled,
            enter = scaleIn(VaultMotion.SpringBouncy) + fadeIn(VaultMotion.enter(VaultMotion.DurationInstant)),
            exit = scaleOut(VaultMotion.exit(VaultMotion.DurationInstant)) + fadeOut(VaultMotion.exit(VaultMotion.DurationInstant)),
        ) {
            Box(
                modifier = Modifier
                    .size(Dimens.pinDotSize)
                    .background(fillColor, CircleShape),
            )
        }
    }
}

/**
 * PIN progress dots with pop-in fills, an error shake driven by
 * [errorPulse] increments, and a success recolor.
 */
@Composable
fun PinDots(
    length: Int,
    filled: Int,
    modifier: Modifier = Modifier,
    errorPulse: Int = 0,
    success: Boolean = false,
) {
    val shakeOffset = remember { Animatable(0f) }
    var showError by remember { mutableStateOf(false) }

    LaunchedEffect(errorPulse) {
        if (errorPulse > 0) {
            showError = true
            shakeOffset.animateTo(targetValue = 0f, animationSpec = VaultMotion.shake())
            showError = false
        }
    }

    val fillColor = when {
        success -> VaultColors.Success
        showError -> VaultColors.Error
        else -> VaultColors.AuroraViolet
    }

    Row(
        modifier = modifier
            .offset { IntOffset(x = shakeOffset.value.roundToInt(), y = 0) }
            .semantics {
                contentDescription = "$filled of $length digits entered"
            },
        horizontalArrangement = Arrangement.spacedBy(Dimens.spaceMd),
    ) {
        repeat(length) { index ->
            Dot(isFilled = index < filled, fillColor = fillColor)
        }
    }
}
