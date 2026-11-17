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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lalatube.offline.data.Video
import com.lalatube.offline.ui.components.ChannelAvatar
import com.lalatube.offline.ui.components.VideoGrid
import com.lalatube.offline.ui.components.channelColor

/**
 * Halaman kanal bergaya YouTube:
 *
 * 1. Bar atas: tombol kembali, cari, dan pindai ulang.
 * 2. Banner kanal (gradien warna kanal) selebar halaman.
 * 3. Avatar besar + nama kanal + jumlah video + tombol **Putar semua**.
 * 4. Bar tab dengan tab **Video** terpilih (garis bawah seperti YouTube).
 * 5. Grid berisi seluruh video dari folder kanal tersebut.
 *
 * Setiap subfolder flashdisk dianggap satu kanal, jadi isi halaman ini sama
 * dengan isi folder kanal (misalnya `Kartun`, `Lagu Anak`).
 */
@Composable
fun ChannelScreen(
    channel: String,
    videos: List<Video>,
    onPlayAll: () -> Unit,
    onPlay: (Video, List<Video>) -> Unit,
    onToggleFavorite: (Video) -> Unit,
    onSearch: () -> Unit,
    onRefresh: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Terbaru dulu seperti tab "Videos" di kanal YouTube.
    val ordered = videos.sortedWith(
        compareByDescending<Video> { it.addedAt }
            .thenByDescending { it.lastModified },
    )
    val avatarPath = ordered.firstNotNullOfOrNull { it.channelAvatarPath }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(start = 8.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Rounded.ArrowBack,
                    contentDescription = "Kembali",
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onSearch) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = "Cari",
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
            Spacer(Modifier.width(2.dp))
            IconButton(onClick = onRefresh) {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = "Pindai ulang",
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        VideoGrid(
            videos = ordered,
            onPlay = { onPlay(it, ordered) },
            onToggleFavorite = onToggleFavorite,
            modifier = Modifier.weight(1f),
            header = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    ChannelBanner(channel = channel, avatarPath = avatarPath)
                    ChannelInfo(
                        channel = channel,
                        videoCount = ordered.size,
                        avatarPath = avatarPath,
                        onPlayAll = onPlayAll,
                    )
                    ChannelTabs()
                }
            },
        )
    }
}

/** Banner kanal. LalaTube tidak punya gambar banner, jadi dipakai gradien warna kanal. */
@Composable
private fun ChannelBanner(channel: String, avatarPath: String?) {
    val color = channelColor(channel)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(150.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        color,
                        color.copy(alpha = 0.78f),
                        color.copy(alpha = 0.36f),
                    ),
                ),
            ),
    ) {
        // Dekorasi lingkaran lembut supaya banner tidak terlihat datar.
        Box(
            modifier = Modifier
                .size(260.dp)
                .align(Alignment.CenterEnd)
                .offset(x = 70.dp)
                .background(Color.White.copy(alpha = 0.10f), CircleShape),
        )
        Box(
            modifier = Modifier
                .size(150.dp)
                .align(Alignment.BottomStart)
                .offset(x = -40.dp, y = 50.dp)
                .background(Color.White.copy(alpha = 0.08f), CircleShape),
        )
        // Inisial kanal besar sebagai penanda visual.
        Text(
            text = channel.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?",
            color = Color.White.copy(alpha = 0.22f),
            fontSize = 96.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 40.dp),
        )
    }
}

@Composable
private fun ChannelInfo(
    channel: String,
    videoCount: Int,
    avatarPath: String?,
    onPlayAll: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        ChannelAvatar(
            channel = channel,
            size = 136,
            avatarPath = avatarPath,
        )
        Spacer(Modifier.width(18.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = channel,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "$videoCount video • langsung dari flashdisk",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "Tidak ada internet yang digunakan. Semua video diputar dari folder kanal ini.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(12.dp))
            // Tombol hitam bulat seperti tombol "Subscribe" di YouTube.
            Button(
                onClick = onPlayAll,
                enabled = videoCount > 0,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onSurface,
                    contentColor = MaterialTheme.colorScheme.surface,
                ),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = 22.dp,
                    vertical = 10.dp,
                ),
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(19.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Putar semua",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                )
            }
        }
    }
}

/** Bar tab kanal. LalaTube hanya punya satu jenis konten: video. */
@Composable
private fun ChannelTabs() {
    Column(modifier = Modifier.fillMaxWidth()) {
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Video",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .width(46.dp)
                        .height(3.dp)
                        .background(MaterialTheme.colorScheme.onSurface),
                )
            }
        }
        Spacer(Modifier.height(10.dp))
    }
}
