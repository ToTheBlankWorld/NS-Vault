package com.nsvault.app.designsystem.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import com.nsvault.app.designsystem.theme.VaultMotion

/**
 * Shared press-feedback scale used by every tappable vault component,
 * so all buttons in the app compress with identical physics.
 */
@Composable
fun InteractionSource.animatePressScale(
    pressedScale: Float = VaultMotion.PressedScale,
): State<Float> {
    val pressed by collectIsPressedAsState()
    return animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = VaultMotion.SpringSnappy,
        label = "pressScale",
    )
}
