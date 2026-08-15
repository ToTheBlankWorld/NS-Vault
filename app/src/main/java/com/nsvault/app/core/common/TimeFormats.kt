package com.nsvault.app.core.common

import java.util.Locale

/**
 * Shared time formatting used by cards, timers, and the player.
 */
object TimeFormats {

    /** `42:07` under an hour, `1:02:33` above it. */
    fun clock(totalMillis: Long): String {
        val totalSeconds = totalMillis / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%02d:%02d", minutes, seconds)
        }
    }

    /** Total listening time as decimal hours for dashboard stats: `12.5`. */
    fun decimalHours(totalMillis: Long): String {
        val hours = totalMillis / 3_600_000.0
        return String.format(Locale.US, "%.1f", hours)
    }

    /** `14 Jul · 10:15 PM` in the device time zone. */
    fun dateTime(epochMillis: Long): String =
        DATE_TIME.format(java.time.Instant.ofEpochMilli(epochMillis))

    private val DATE_TIME = java.time.format.DateTimeFormatter
        .ofPattern("d MMM · h:mm a")
        .withZone(java.time.ZoneId.systemDefault())
}
