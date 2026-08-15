package com.nsvault.app.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import com.nsvault.app.designsystem.theme.VaultColors

/**
 * Scrolling live waveform: bars enter from the right and drift left,
 * mirrored around the vertical center, painted with the Aurora ramp.
 * The engine supplies already-smoothed levels, so bars never jump.
 *
 * @param levels newest level last, each in 0..1.
 * @param maxBars how many bars fit the view; also caps [levels].
 */
@Composable
fun LiveWaveform(
    levels: List<Float>,
    modifier: Modifier = Modifier,
    maxBars: Int = 48,
    active: Boolean = true,
) {
    val brush = Brush.verticalGradient(
        colors = listOf(
            VaultColors.AuroraViolet,
            VaultColors.AuroraIndigo,
            VaultColors.AuroraCyan,
        ),
    )

    Canvas(modifier = modifier) {
        val slot = size.width / maxBars
        val barWidth = slot * 0.55f
        val corner = CornerRadius(barWidth / 2f, barWidth / 2f)
        val centerY = size.height / 2f
        val minBar = size.height * 0.04f
        val maxBar = size.height * 0.92f

        val visible = levels.takeLast(maxBars)
        val startSlot = maxBars - visible.size

        visible.forEachIndexed { index, level ->
            val height = (minBar + (maxBar - minBar) * level.coerceIn(0f, 1f))
            val x = (startSlot + index) * slot + (slot - barWidth) / 2f
            drawRoundRect(
                brush = brush,
                topLeft = Offset(x, centerY - height / 2f),
                size = Size(barWidth, height),
                cornerRadius = corner,
                alpha = if (active) 1f else 0.35f,
            )
        }
    }
}
