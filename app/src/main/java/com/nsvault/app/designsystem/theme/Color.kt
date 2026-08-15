package com.nsvault.app.designsystem.theme

import androidx.compose.ui.graphics.Color

/**
 * NS Vault color tokens. The app is deliberately dark-only:
 * a vault is a night-time object, and a single theme keeps the
 * brand language exact on every device.
 */
object VaultColors {
    // Canvas
    val Obsidian = Color(0xFF0B0D12)
    val Surface = Color(0xFF12151D)
    val SurfaceRaised = Color(0xFF171B25)
    val SurfaceHigh = Color(0xFF1D2230)

    // Hairlines and glass
    val Hairline = Color(0x14FFFFFF)
    val GlassFill = Color(0x0AFFFFFF)
    val GlassBorderTop = Color(0x1AFFFFFF)
    val GlassBorderBottom = Color(0x05FFFFFF)

    // Aurora accent ramp
    val AuroraViolet = Color(0xFF7C5CFF)
    val AuroraIndigo = Color(0xFF5B8CFF)
    val AuroraCyan = Color(0xFF4ADEDE)

    // Content
    val TextPrimary = Color(0xFFF2F4F8)
    val TextSecondary = Color(0xFFA8B0C0)
    val TextTertiary = Color(0xFF5C6474)
    val OnAccent = Color(0xFF0B0D12)

    // Semantic
    val Gold = Color(0xFFE8C468)
    val RecordingRed = Color(0xFFFF5C6C)
    val Success = Color(0xFF4ADE80)
    val Error = Color(0xFFFF6B7A)
}
