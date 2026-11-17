package com.lalatube.offline.util

import com.lalatube.offline.data.Video

/**
 * Kunci urutan acak yang stabil untuk satu *seed* (satu sesi aplikasi).
 *
 * YouTube menampilkan video dengan urutan yang berbeda setiap dibuka. LalaTube
 * menirunya secara offline: urutan dihitung dari hash nama berkas ditambah seed
 * yang diganti setiap aplikasi dibuka atau folder dipindai ulang. Karena hasilnya
 * deterministik, posisi kartu tidak melompat saat daftar digulir, saat posisi
 * tontonan disimpan, atau saat favorit diubah.
 */
fun videoShuffleKey(seed: Long, uri: String): Long {
    var hash = uri.hashCode().toLong()
    hash = hash * 0x9E3779B1L + seed
    hash = hash xor (hash ushr 33)
    hash *= 0xC2B2AE3DL
    hash = hash xor (hash ushr 29)
    hash *= 0x27D4EB2FL
    hash = hash xor (hash ushr 32)
    return hash
}

/**
 * Mengacak urutan video memakai [seed] yang sama untuk seluruh sesi, sehingga
 * urutan berubah tiap aplikasi dibuka atau daftar dipindai ulang tetapi tetap
 * konsisten selama sesi berjalan.
 */
fun Iterable<Video>.shuffledForSession(seed: Long): List<Video> =
    sortedBy { videoShuffleKey(seed, it.uri) }
