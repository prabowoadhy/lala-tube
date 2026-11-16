package com.lalatube.offline.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lalatube.offline.UiState
import com.lalatube.offline.data.Video
import com.lalatube.offline.ui.components.ChannelChipRow
import com.lalatube.offline.ui.components.ContinueShelf
import com.lalatube.offline.ui.components.EmptyState
import com.lalatube.offline.ui.components.SectionHeading
import com.lalatube.offline.ui.components.VideoGrid

@Composable
fun HomeScreen(
    state: UiState,
    onPlay: (Video, List<Video>) -> Unit,
    onToggleFavorite: (Video) -> Unit,
    onPickFolder: () -> Unit,
    onRescan: () -> Unit,
    modifier: Modifier = Modifier,
    onScrollDirection: ((Boolean) -> Unit)? = null,
) {
    var selectedChannel by rememberSaveable { mutableStateOf<String?>(null) }

    val channels = state.videos
        .groupingBy { it.channel }
        .eachCount()
        .toList()
        .sortedWith(compareByDescending<Pair<String, Int>> { it.second }.thenBy { it.first })

    LaunchedEffect(channels) {
        if (selectedChannel != null && channels.none { it.first == selectedChannel }) {
            selectedChannel = null
        }
    }

    val visibleVideos = if (selectedChannel == null) {
        state.videos
    } else {
        state.videos.filter { it.channel == selectedChannel }
    }
    val continueVideos = visibleVideos
        .filter { it.canResume }
        .sortedByDescending { it.lastPlayedAt }

    if (!state.initialized) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    if (state.videos.isEmpty()) {
        when {
            state.isScanning -> {
                Column(
                    modifier = modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(18.dp))
                    Text(
                        text = if (state.scanTotal > 0) {
                            "Memindai video ${state.scanDone} dari ${state.scanTotal}…"
                        } else {
                            "Membaca folder video…"
                        },
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Thumbnail dibuat sekali saja agar pemindaian berikutnya cepat.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (state.scanTotal > 0) {
                        Spacer(Modifier.height(16.dp))
                        LinearProgressIndicator(
                            progress = {
                                state.scanDone.toFloat() / state.scanTotal.toFloat().coerceAtLeast(1f)
                            },
                            modifier = Modifier
                                .width(320.dp)
                                .height(5.dp),
                        )
                    }
                }
            }

            !state.hasAccess -> EmptyState(
                title = "Video offline, tanpa internet",
                description = "Colokkan flashdisk berisi video, lalu pilih foldernya. " +
                    "Setelah dipilih, LalaTube mengingat folder itu sampai flashdisk dicabut.",
                primaryLabel = "Pilih folder di flashdisk",
                onPrimary = onPickFolder,
                modifier = modifier,
            )

            else -> EmptyState(
                title = "Belum ada video di folder ini",
                description = "Masukkan file MP4, MKV, WEBM, atau format video lain ke folder " +
                    "yang sudah dipilih, lalu pindai ulang.",
                primaryLabel = "Pindai ulang",
                onPrimary = onRescan,
                secondaryLabel = "Pilih folder lain",
                onSecondary = onPickFolder,
                modifier = modifier,
            )
        }
        return
    }

    Column(modifier.fillMaxSize()) {
        if (!state.hasAccess) {
            AccessLostBanner(
                onPickFolder = onPickFolder,
                onRescan = onRescan,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
            )
        }
        Spacer(Modifier.height(2.dp))
        ChannelChipRow(
            channels = channels,
            selectedChannel = selectedChannel,
            onSelect = { selectedChannel = it },
            modifier = Modifier.padding(bottom = 4.dp),
        )
        key(selectedChannel) {
            VideoGrid(
                videos = visibleVideos,
                onPlay = { onPlay(it, visibleVideos) },
                onToggleFavorite = onToggleFavorite,
                modifier = Modifier.weight(1f),
                onScrollDirection = onScrollDirection,
                header = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        if (continueVideos.isNotEmpty()) {
                            Spacer(Modifier.height(6.dp))
                            SectionHeading(
                                title = "Lanjutkan menonton",
                                subtitle = "Lanjutkan dari posisi terakhir",
                            )
                            Spacer(Modifier.height(9.dp))
                            ContinueShelf(
                                videos = continueVideos,
                                onPlay = { onPlay(it, continueVideos) },
                            )
                            Spacer(Modifier.height(18.dp))
                        }
                        SectionHeading(
                            title = if (selectedChannel == null) "Video terbaru" else selectedChannel!!,
                            subtitle = "${visibleVideos.size} video • ${channels.size} kanal • offline",
                        )
                        Spacer(Modifier.height(4.dp))
                    }
                },
            )
        }
    }
}

@Composable
private fun AccessLostBanner(
    onPickFolder: () -> Unit,
    onRescan: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = RoundedCornerShape(14.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.WifiOff,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Akses flashdisk terputus",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                )
                Text(
                    text = "Colokkan kembali flashdisk, lalu pilih foldernya.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Spacer(Modifier.width(12.dp))
            Button(onClick = onRescan) {
                Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Coba lagi")
            }
            Spacer(Modifier.width(8.dp))
            Button(onClick = onPickFolder) {
                Text("Pilih folder")
            }
        }
    }
}
