package com.lalatube.offline

import com.lalatube.offline.data.friendlyTitle
import com.lalatube.offline.util.codecLabel
import com.lalatube.offline.util.formatDuration
import com.lalatube.offline.util.formatFileSize
import org.junit.Assert.assertEquals
import org.junit.Test

class FormatTest {
    @Test
    fun durationsAreFormattedLikeYouTube() {
        assertEquals("0:00", formatDuration(0L))
        assertEquals("0:09", formatDuration(9_900L))
        assertEquals("4:05", formatDuration(245_000L))
        assertEquals("1:02:03", formatDuration(3_723_000L))
    }

    @Test
    fun fileSizesAreReadable() {
        assertEquals("1.0 GB", formatFileSize(1024L * 1024L * 1024L))
        assertEquals("5 MB", formatFileSize(5L * 1024L * 1024L))
    }

    @Test
    fun youtubeIdsAndQualitySuffixesAreRemoved() {
        assertEquals(
            "Wheels on the Bus",
            friendlyTitle("Wheels on the Bus [dQw4w9WgXcQ].mp4"),
        )
        assertEquals(
            "Wheels on the Bus",
            friendlyTitle("Wheels on the Bus (1080p).mp4"),
        )
        assertEquals("Video Seru", friendlyTitle("Video_Seru.mkv"))
    }

    @Test
    fun codecMimesGetFriendlyNames() {
        assertEquals("AV1", codecLabel("video/av01"))
        assertEquals("H.264", codecLabel("video/avc"))
        assertEquals("H.265", codecLabel("video/hevc"))
        assertEquals("VP9", codecLabel("video/x-vnd.on2.vp9"))
        assertEquals("", codecLabel(""))
        assertEquals("X-FOO", codecLabel("video/x-foo"))
    }
}
