package com.lalatube.offline.data

/**
 * Jumlah maksimum riwayat tontonan yang disimpan.
 *
 * Setiap video menyimpan satu cap waktu `lastPlayedAt`. Supaya daftar riwayat
 * dan berkas JSON tidak terus membesar, hanya [HistoryLimit] riwayat terbaru
 * yang dipertahankan; cap waktu riwayat yang lebih lama dilepas (jadi 0).
 */
const val HistoryLimit = 50

/**
 * Jumlah kartu riwayat yang ditampilkan per batch di halaman Riwayat.
 * Batch berikutnya dimuat otomatis saat pengguna menggulir sampai bawah.
 */
const val HistoryPageSize = 15

/**
 * Buang riwayat tontonan yang melebihi [limit] entri terbaru.
 *
 * Hanya `lastPlayedAt` yang di-reset ke 0; posisi tontonan (`positionMs`) dan
 * tanda favorit tidak diubah, sehingga video tetap bisa dilanjutkan dari
 * menit terakhirnya lewat beranda walaupun sudah keluar dari daftar riwayat.
 * Daftar dikembalikan apa adanya kalau jumlahnya belum melewati batas.
 */
fun List<Video>.prunedHistory(limit: Int = HistoryLimit): List<Video> {
    val played = filter { it.lastPlayedAt > 0L }
    if (played.size <= limit) return this
    val keep = played
        .sortedByDescending { it.lastPlayedAt }
        .take(limit)
        .mapTo(HashSet()) { it.uri }
    return map { video ->
        if (video.lastPlayedAt > 0L && video.uri !in keep) {
            video.copy(lastPlayedAt = 0L)
        } else {
            video
        }
    }
}
