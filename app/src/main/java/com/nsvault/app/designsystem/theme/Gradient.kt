package com.nsvault.app.designsystem.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

object VaultGradients {

    val Aurora = Brush.linearGradient(
        colors = listOf(
            VaultColors.AuroraViolet,
            VaultColors.AuroraIndigo,
            VaultColors.AuroraCyan,
        ),
    )

    val AuroraSoft = Brush.linearGradient(
        colors = listOf(
            VaultColors.AuroraViolet.copy(alpha = 0.85f),
            VaultColors.AuroraIndigo.copy(alpha = 0.85f),
        ),
    )

    val GlassBorder = Brush.verticalGradient(
        colors = listOf(
            VaultColors.GlassBorderTop,
            VaultColors.GlassBorderBottom,
        ),
    )

    val RecordGlow = Brush.radialGradient(
        colors = listOf(
            VaultColors.RecordingRed.copy(alpha = 0.35f),
            Color.Transparent,
        ),
    )
}
