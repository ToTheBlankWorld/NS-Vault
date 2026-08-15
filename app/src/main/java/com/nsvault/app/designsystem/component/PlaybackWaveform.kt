package com.nsvault.app.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import com.nsvault.app.designsystem.theme.VaultColors

/**
 * Seekable waveform: played bars carry the Aurora ramp, the rest stay
 * quiet. Tap or drag anywhere to scrub. When no preview exists
 * (imports), a uniform bar field acts as an honest seek bar.
 */
@Composable
fun PlaybackWaveform(
    waveform: FloatArray?,
    progress: Float,
    onScrub: (Float) -> Unit,
    onScrubEnd: (Float) -> Unit,
    modifier: Modifier = Modifier,
    barCount: Int = 56,
) {
    val playedBrush = Brush.verticalGradient(
        colors = listOf(
            VaultColors.AuroraViolet,
            VaultColors.AuroraIndigo,
            VaultColors.AuroraCyan,
        ),
    )

    Canvas(
        modifier = modifier
            .semantics {
                contentDescription = "Playback position"
                progressBarRangeInfo = ProgressBarRangeInfo(progress, 0f..1f)
            }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                    onScrub(fraction)
                    onScrubEnd(fraction)
                }
            }
            .pointerInput(Unit) {
                var fraction = 0f
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        fraction = (offset.x / size.width).coerceIn(0f, 1f)
                        onScrub(fraction)
                    },
                    onHorizontalDrag = { change, _ ->
                        fraction = (change.position.x / size.width).coerceIn(0f, 1f)
                        onScrub(fraction)
                    },
                    onDragEnd = { onScrubEnd(fraction) },
                )
            },
    ) {
        val slot = size.width / barCount
        val barWidth = slot * 0.55f
        val corner = CornerRadius(barWidth / 2f, barWidth / 2f)
        val centerY = size.height / 2f
        val minBar = size.height * 0.12f
        val maxBar = size.height * 0.95f
        val playedX = size.width * progress.coerceIn(0f, 1f)

        for (i in 0 until barCount) {
            val level = sampleLevel(waveform, i, barCount)
            val height = minBar + (maxBar - minBar) * level
            val x = i * slot + (slot - barWidth) / 2f
            val played = x + barWidth / 2f <= playedX
            if (played) {
                drawRoundRect(
                    brush = playedBrush,
                    topLeft = Offset(x, centerY - height / 2f),
                    size = Size(barWidth, height),
                    cornerRadius = corner,
                )
            } else {
                drawRoundRect(
                    color = VaultColors.SurfaceHigh,
                    topLeft = Offset(x, centerY - height / 2f),
                    size = Size(barWidth, height),
                    cornerRadius = corner,
                )
            }
        }
    }
}

private fun sampleLevel(waveform: FloatArray?, index: Int, barCount: Int): Float {
    if (waveform == null || waveform.isEmpty()) return 0.45f
    val start = index * waveform.size / barCount
    val end = ((index + 1) * waveform.size / barCount).coerceAtMost(waveform.size)
    var peak = 0f
    for (i in start until maxOf(end, start + 1)) {
        if (waveform[i] > peak) peak = waveform[i]
    }
    return peak.coerceIn(0.06f, 1f)
}
