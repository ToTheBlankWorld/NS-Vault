package com.nsvault.app.core.common

import java.util.Locale

object ByteFormat {

    /** Human-readable size: `640 KB`, `12.4 MB`, `1.2 GB`. */
    fun format(bytes: Long): String = when {
        bytes >= GB -> String.format(Locale.US, "%.1f GB", bytes / GB.toDouble())
        bytes >= MB -> String.format(Locale.US, "%.1f MB", bytes / MB.toDouble())
        bytes >= KB -> String.format(Locale.US, "%.0f KB", bytes / KB.toDouble())
        else -> "$bytes B"
    }

    private const val KB = 1024L
    private const val MB = KB * 1024
    private const val GB = MB * 1024
}
