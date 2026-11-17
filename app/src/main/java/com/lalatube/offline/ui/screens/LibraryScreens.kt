package com.lalatube.offline.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lalatube.offline.data.HistoryLimit
import com.lalatube.offline.data.HistoryPageSize
import com.lalatube.offline.data.Video
import com.lalatube.offline.ui.components.EmptyState
import com.lalatube.offline.ui.components.SectionHeading
import com.lalatube.offline.ui.components.VideoGrid

@Composable
fun FavoritesScreen(
    videos: List<Video>,
    onPlay: (Video, List<Video>) -> Unit,
    onToggleFavorite: (Video) -> Unit,
    onPickFolder: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val favorites = videos.filter { it.favorite }
    if (favorites.isEmpty()) {
        EmptyState(
            title = "Belum ada favorit",
            description = "Tekan menu tiga titik pada video, lalu pilih “Tambah ke favorit” " +
                "agar video kesayangan mudah ditemukan.",
            primaryLabel = if (videos.isEmpty()) "Pilih folder video" else null,
            onPrimary = if (videos.isEmpty()) onPickFolder else null,
            icon = {
                androidx.compose.material3.Icon(
                    imageVector = Icons.Rounded.FavoriteBorder,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(2.dp),
                )
            },
            modifier = modifier,
        )
        return
    }
    VideoGrid(
        videos = favorites,
        onPlay = { onPlay(it, favorites) },
        onToggleFavorite = onToggleFavorite,
        modifier = modifier.fillMaxSize(),
        header = {
            Column {
                Spacer(Modifier.height(8.dp))
                SectionHeading(
                    title = "Favorit",
                    subtitle = "${favorites.size} video yang disimpan di perangkat ini",
                )
                Spacer(Modifier.height(10.dp))
            }
        },
    )
}

@Composable
fun HistoryScreen(
    videos: List<Video>,
    onPlay: (Video, List<Video>) -> Unit,
    onToggleFavorite: (Video) -> Unit,
    modifier: Modifier = Modifier,
) {
    val history = videos
        .filter { it.lastPlayedAt > 0L }
        .sortedByDescending { it.lastPlayedAt }
    if (history.isEmpty()) {
        EmptyState(
            title = "Belum ada riwayat",
            description = "Video yang sudah atau sedang ditonton akan muncul di sini.",
            icon = {
                androidx.compose.material3.Icon(
                    imageVector = Icons.Rounded.History,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            },
            modifier = modifier,
        )
        return
    }

    // Tampilkan bertahap: mulai [HistoryPageSize] kartu, lalu tambah satu batch
    // lagi setiap kali gulir sampai bawah. Riwayat panjang jadi tetap ringan
    // karena kartu (termasuk thumbnail) hanya dibuat saat benar-benar terlihat.
    var visibleCount by remember { mutableIntStateOf(HistoryPageSize) }
    val pagedHistory = history.take(visibleCount)

    VideoGrid(
        videos = pagedHistory,
        onPlay = { onPlay(it, history) },
        onToggleFavorite = onToggleFavorite,
        modifier = modifier.fillMaxSize(),
        hasMore = visibleCount < history.size,
        onLoadMore = {
            visibleCount = (visibleCount + HistoryPageSize).coerceAtMost(history.size)
        },
        header = {
            Column {
                Spacer(Modifier.height(8.dp))
                SectionHeading(
                    title = "Riwayat tontonan",
                    subtitle = "${history.size} video pernah diputar • " +
                        "maksimal $HistoryLimit riwayat disimpan",
                )
                Spacer(Modifier.height(10.dp))
            }
        },
    )
}