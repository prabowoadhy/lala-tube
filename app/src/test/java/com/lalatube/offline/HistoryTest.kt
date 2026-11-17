package com.lalatube.offline

import com.lalatube.offline.data.HistoryLimit
import com.lalatube.offline.data.Video
import com.lalatube.offline.data.prunedHistory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoryTest {

    private fun video(id: Int, playedAt: Long = 0L, position: Long = 30_000L) = Video(
        uri = "content://video/$id",
        title = "Video $id",
        channel = "Belajar",
        folder = "Belajar",
        durationMs = 120_000L,
        positionMs = position,
        lastPlayedAt = playedAt,
    )

    @Test
    fun riwayatDiBawahBatasTidakBerubah() {
        // Susun ulang agar setiap video punya cap waktu berbeda.
        val list = (1..HistoryLimit).map { video(it, playedAt = it.toLong()) }
        assertEquals(list, list.prunedHistory())
        // Daftar yang belum melewati batas dikembalikan apa adanya (tanpa salinan).
        assertSame(list, list.prunedHistory())
    }

    @Test
    fun riwayatMelebihiBatasDipangkas() {
        val total = HistoryLimit + 20
        val list = (1..total).map { video(it, playedAt = it.toLong()) }

        val pruned = list.prunedHistory()

        // Semua video tetap ada; hanya cap waktu riwayat terlama yang dilepas.
        assertEquals(total, pruned.size)
        val played = pruned.filter { it.lastPlayedAt > 0L }
        assertEquals(HistoryLimit, played.size)
        assertTrue(played.all { it.lastPlayedAt > (total - HistoryLimit).toLong() })
        // 20 riwayat terlama (cap waktu 1..20) sudah tidak punya riwayat.
        assertTrue(pruned.filter { it.lastPlayedAt == 0L }.all {
            it.uri.substringAfterLast('/').toInt() <= total - HistoryLimit
        })
    }

    @Test
    fun posisiTontonanTetapSetelahDipangkas() {
        val list = (1..HistoryLimit + 5).map {
            video(it, playedAt = it.toLong(), position = 42_000L)
        }

        // Video yang keluar dari riwayat tetap bisa dilanjutkan dari menit terakhir.
        assertTrue(list.prunedHistory().all { it.positionMs == 42_000L })
    }
}
