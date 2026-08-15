package com.nsvault.app.designsystem.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.nsvault.app.designsystem.theme.VaultColors
import com.nsvault.app.designsystem.theme.VaultGradients
import com.nsvault.app.designsystem.theme.VaultMotion

/**
 * Slim determinate progress bar with an Aurora fill. Progress animates
 * smoothly between updates so streamed byte counts never look steppy.
 */
@Composable
fun VaultProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
) {
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = VaultMotion.enter(VaultMotion.DurationShort),
        label = "progressFill",
    )

    Box(
        modifier = modifier
            .height(4.dp)
            .clip(CircleShape)
            .background(VaultColors.SurfaceHigh),
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(animated)
                .clip(CircleShape)
                .background(VaultGradients.Aurora),
        )
    }
}
