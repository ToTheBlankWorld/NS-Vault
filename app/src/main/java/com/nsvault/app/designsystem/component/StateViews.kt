package com.nsvault.app.designsystem.component

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import com.nsvault.app.designsystem.theme.Dimens
import com.nsvault.app.designsystem.theme.VaultColors
import com.nsvault.app.designsystem.theme.VaultGradients
import com.nsvault.app.designsystem.theme.VaultMotion

/**
 * Centered empty state: a quiet icon, a headline, and one supporting line.
 */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier.padding(Dimens.spaceXl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(Dimens.recordControlSize)
                .background(VaultColors.SurfaceRaised, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = VaultColors.AuroraCyan,
                modifier = Modifier.size(Dimens.spaceXl),
            )
        }
        Spacer(modifier = Modifier.height(Dimens.spaceLg))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = VaultColors.TextPrimary,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(Dimens.spaceXxs))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = VaultColors.TextTertiary,
            textAlign = TextAlign.Center,
        )
        if (actionText != null && onAction != null) {
            Spacer(modifier = Modifier.height(Dimens.spaceLg))
            VaultSecondaryButton(text = actionText, onClick = onAction)
        }
    }
}

/**
 * Centered error state with a retry affordance.
 */
@Composable
fun ErrorState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    retryText: String = "Try again",
    onRetry: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier.padding(Dimens.spaceXl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(Dimens.recordControlSize)
                .background(VaultColors.Error.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = VaultColors.Error,
                modifier = Modifier.size(Dimens.spaceXl),
            )
        }
        Spacer(modifier = Modifier.height(Dimens.spaceLg))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = VaultColors.TextPrimary,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(Dimens.spaceXxs))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = VaultColors.TextTertiary,
            textAlign = TextAlign.Center,
        )
        if (onRetry != null) {
            Spacer(modifier = Modifier.height(Dimens.spaceLg))
            VaultSecondaryButton(text = retryText, onClick = onRetry)
        }
    }
}

/**
 * Three aurora dots breathing in sequence — the vault's loading signature.
 */
@Composable
fun VaultLoadingIndicator(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "loading")
    val colors = listOf(
        VaultColors.AuroraViolet,
        VaultColors.AuroraIndigo,
        VaultColors.AuroraCyan,
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(Dimens.spaceXs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        colors.forEachIndexed { index, color ->
            val alpha by transition.animateFloat(
                initialValue = 0.25f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = VaultMotion.DurationLong,
                        easing = VaultMotion.EasingEmphasized,
                    ),
                    repeatMode = RepeatMode.Reverse,
                    initialStartOffset = StartOffset(index * VaultMotion.DurationShort),
                ),
                label = "dotAlpha$index",
            )
            Box(
                modifier = Modifier
                    .size(Dimens.spaceSm)
                    .graphicsLayer { this.alpha = alpha }
                    .background(color, CircleShape),
            )
        }
    }
}

/**
 * Celebration glyph: a gradient disc that springs in behind a check.
 * Used when a PIN is created, a recording is saved, and similar moments.
 */
@Composable
fun SuccessGlyph(
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "successHalo")
    val haloScale by transition.animateFloat(
        initialValue = VaultMotion.PulseMinScale,
        targetValue = VaultMotion.PulseMaxScale,
        animationSpec = infiniteRepeatable(
            animation = tween(VaultMotion.PulsePeriodMs / 2, easing = VaultMotion.EasingEmphasized),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "haloScale",
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(Dimens.recordButtonSize + Dimens.spaceLg)
                .scale(haloScale)
                .background(VaultColors.Success.copy(alpha = 0.15f), CircleShape),
        )
        Box(
            modifier = Modifier
                .size(Dimens.recordButtonSize)
                .background(VaultGradients.Aurora, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                tint = VaultColors.OnAccent,
                modifier = Modifier.size(Dimens.spaceXl),
            )
        }
    }
}
