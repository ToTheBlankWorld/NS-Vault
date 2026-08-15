package com.nsvault.app.domain.model

/**
 * Recording quality presets. All AAC mono at 44.1 kHz — bit rate is
 * what meaningfully moves the size/fidelity tradeoff for voice.
 */
enum class AudioQuality(
    val bitRate: Int,
    val label: String,
    val description: String,
) {
    STANDARD(
        bitRate = 96_000,
        label = "Standard",
        description = "Crisp voice, smallest files · ~42 MB per hour",
    ),
    HIGH(
        bitRate = 128_000,
        label = "High",
        description = "Recommended balance · ~56 MB per hour",
    ),
    STUDIO(
        bitRate = 192_000,
        label = "Studio",
        description = "Maximum fidelity · ~84 MB per hour",
    ),
}
