package com.lalatube.offline.util

import java.util.Locale
import kotlin.math.max

fun formatDuration(durationMs: Long): String {
    if (durationMs <= 0L) return "0:00"
    val totalSeconds = max(0L, durationMs / 1000L)
    val seconds = totalSeconds % 60
    val minutes = (totalSeconds / 60) % 60
    val hours = totalSeconds / 3600
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%d:%02d", minutes, seconds)
    }
}

fun formatFileSize(bytes: Long): String {
    if (bytes <= 0L) return "-"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1.0 -> String.format(Locale.US, "%.1f GB", gb)
        mb >= 1.0 -> String.format(Locale.US, "%.0f MB", mb)
        else -> String.format(Locale.US, "%.0f KB", kb)
    }
}

fun relativeDate(timestamp: Long): String {
    if (timestamp <= 0L) return ""
    val diff = System.currentTimeMillis() - timestamp
    val day = 24L * 60L * 60L * 1000L
    return when {
        diff < 60L * 60L * 1000L -> "Baru saja"
        diff < day -> "Hari ini"
        diff < 2 * day -> "Kemarin"
        diff < 7 * day -> "${diff / day} hari lalu"
        diff < 30 * day -> "${diff / (7 * day)} minggu lalu"
        diff < 365 * day -> "${diff / (30 * day)} bulan lalu"
        else -> "${diff / (365 * day)} tahun lalu"
    }
}

fun progressDescription(positionMs: Long, durationMs: Long): String {
    if (durationMs <= 0L) return "Offline"
    val remaining = (durationMs - positionMs).coerceAtLeast(0L)
    return "Sisa ${formatDuration(remaining)}"
}
