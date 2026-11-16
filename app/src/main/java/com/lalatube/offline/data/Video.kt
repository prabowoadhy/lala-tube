package com.lalatube.offline.data

data class Video(
    val uri: String,
    val title: String,
    val channel: String,
    val folder: String,
    val durationMs: Long = 0L,
    val sizeBytes: Long = 0L,
    val lastModified: Long = 0L,
    val width: Int = 0,
    val height: Int = 0,
    val thumbnailPath: String? = null,
    val positionMs: Long = 0L,
    val lastPlayedAt: Long = 0L,
    val completed: Boolean = false,
    val favorite: Boolean = false,
    val addedAt: Long = 0L,
) {
    val id: String get() = uri

    val progressFraction: Float
        get() {
            if (completed || durationMs <= 0L) return 0f
            return (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
        }

    val canResume: Boolean
        get() = !completed && durationMs > 0L &&
            positionMs in 5_000L until (durationMs - 4_000L).coerceAtLeast(5_001L)
}

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}
