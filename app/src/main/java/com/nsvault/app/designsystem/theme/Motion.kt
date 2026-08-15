package com.nsvault.app.designsystem.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.KeyframesSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

/**
 * The single source of truth for motion in NS Vault.
 *
 * Every animation in the app — screen transitions, button presses,
 * dialogs, waveforms, pulses — must derive its timing from these tokens.
 * No duration, easing, or spring constant may be hardcoded at a call site.
 */
object VaultMotion {

    // ── Easings ────────────────────────────────────────────────────────────
    val EasingEmphasized = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val EasingDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
    val EasingAccelerate = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

    // ── Durations (ms) ─────────────────────────────────────────────────────
    const val DurationInstant = 100
    const val DurationShort = 200
    const val DurationMedium = 350
    const val DurationLong = 500
    const val DurationExtraLong = 800

    // ── Springs ────────────────────────────────────────────────────────────
    /** Default spring for content settling into place. */
    val SpringGentle: SpringSpec<Float> = spring(
        dampingRatio = 0.85f,
        stiffness = Spring.StiffnessMediumLow,
    )

    /** Playful spring for celebratory moments (PIN dot pops, success glyphs). */
    val SpringBouncy: SpringSpec<Float> = spring(
        dampingRatio = 0.55f,
        stiffness = Spring.StiffnessMedium,
    )

    /** Critically damped spring for direct manipulation (button presses). */
    val SpringSnappy: SpringSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessHigh,
    )

    // ── Standard tweens ────────────────────────────────────────────────────
    fun <T> enter(durationMillis: Int = DurationMedium): TweenSpec<T> =
        tween(durationMillis, easing = EasingDecelerate)

    fun <T> exit(durationMillis: Int = DurationShort): TweenSpec<T> =
        tween(durationMillis, easing = EasingAccelerate)

    fun <T> emphasized(durationMillis: Int = DurationMedium): TweenSpec<T> =
        tween(durationMillis, easing = EasingEmphasized)

    // ── Button press ───────────────────────────────────────────────────────
    const val PressedScale = 0.96f

    // ── Error shake ────────────────────────────────────────────────────────
    /** Horizontal-offset keyframes for rejection feedback (wrong PIN, etc.). */
    fun shake(): KeyframesSpec<Float> = keyframes {
        durationMillis = 420
        0f at 0
        (-14f) at 60
        12f at 130
        (-8f) at 200
        6f at 270
        (-3f) at 340
        0f at 420
    }

    // ── Recording pulse ────────────────────────────────────────────────────
    const val PulsePeriodMs = 2200
    const val PulseMinScale = 1f
    const val PulseMaxScale = 1.12f

    // ── Waveform (Phase 3) ─────────────────────────────────────────────────
    /** Cadence at which live amplitude is sampled and animated. */
    const val WaveSampleMs = 50
    const val WaveBarSettleMs = 120

    // ── Dialogs & sheets ───────────────────────────────────────────────────
    const val DialogEnterScaleFrom = 0.92f

    // ── Success moments ────────────────────────────────────────────────────
    const val SuccessHoldMs = 1200
}
