package com.lalatube.offline.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lalatube.offline.data.Video
import com.lalatube.offline.ui.components.ChannelAvatar
import com.lalatube.offline.ui.components.DurationBadge
import com.lalatube.offline.ui.components.VideoThumbnail
import com.lalatube.offline.ui.components.WatchProgressBar
import com.lalatube.offline.ui.theme.YouTubeRed
import com.lalatube.offline.util.codecLabel
import com.lalatube.offline.util.formatDuration

/**
 * Halaman tonton bergaya YouTube: player di kiri, informasi video di bawahnya,
 * dan rekomendasi di sidebar kanan. Fullscreen ditangani oleh PlayerScreen.
 */
@Composable
fun WatchLayout(
    video: Video,
    suggestions: List<Video>,
    onBack: () -> Unit,
    onPlayVideo: (Video) -> Unit,
    onToggleFavorite: () -> Unit,
    onFullscreen: () -> Unit,
    onOpenChannel: (String) -> Unit,
    playerStage: @Composable (Modifier) -> Unit,
) {
    // Status bar atas + navigation bar bawah harus tetap terlihat seperti YouTube.
    // Pemutar & konten dimulai tepat di bawah status bar (y=96px di head unit),
    // jadi tombol menu status bar tidak menumpuk di atas video.
    val density = LocalDensity.current
    val statusBarTop = with(density) {
        WindowInsets.statusBars.getTop(this).toDp()
    }
    val navBarBottom = with(density) {
        WindowInsets.navigationBars.getBottom(this).toDp()
    }

    // Latar hitam di belakang status bar: YouTube memakai status bar hitam
    // (ikon putih) di halaman tonton, tetapi beranda/kanal tetap putih.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = statusBarTop)
                .background(MaterialTheme.colorScheme.background),
        ) {
        val sidebarWidth = if (maxWidth >= 1000.dp) 360.dp else 300.dp
        // Lebar video mengikuti kolom kiri agar menempel ke tepi layar, tetapi
        // tingginya dibatasi sekitar 58% tinggi layar seperti YouTube, supaya
        // judul, kanal, dan tombol Favorit/Layar penuh terlihat tanpa menggulir.
        val contentWidth = (maxWidth - sidebarWidth - 1.dp).coerceAtLeast(160.dp)
        val videoHeight = minOf(contentWidth * 9f / 16f, maxHeight * 0.58f)

        Row(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            ) {
                // Seperti YouTube: pemutar menempel ke tepi atas & kiri layar,
                // tanpa padding dan tanpa sudut membulat. Video 16:9 di dalamnya
                // diberi bilah hitam di sisi saat kotak lebih lebar.
                playerStage(
                    Modifier
                        .fillMaxWidth()
                        .height(videoHeight),
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(start = 16.dp, end = 16.dp)
                        .padding(top = 12.dp, bottom = 12.dp + navBarBottom),
                ) {

                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = video.title,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(10.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Avatar + nama kanal bisa diketuk untuk membuka halaman
                        // kanal (seluruh video di folder kanal itu).
                        val channelInteraction = remember { MutableInteractionSource() }
                        val channelFocused by channelInteraction.collectIsFocusedAsState()
                        val channelShape = RoundedCornerShape(8.dp)
                        // weight(1f) penting: tanpa ini baris kanal (yang punya
                        // teks weight(1f) di dalamnya) memakan seluruh lebar
                        // sehingga tombol Favorit/Layar penuh terjepit, teksnya
                        // membungkus ke bawah, dan barisnya jadi sangat tinggi.
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clip(channelShape)
                                .background(
                                    if (channelFocused) {
                                        MaterialTheme.colorScheme.surfaceContainer
                                    } else {
                                        androidx.compose.ui.graphics.Color.Transparent
                                    },
                                )
                                .clickable(
                                    interactionSource = channelInteraction,
                                    indication = LocalIndication.current,
                                    onClick = { onOpenChannel(video.channel) },
                                )
                                .padding(horizontal = 6.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            ChannelAvatar(
                                channel = video.channel,
                                size = 38,
                                avatarPath = video.channelAvatarPath,
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = video.channel,
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = "${formatDuration(video.durationMs)} • offline",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                        FilledTonalButton(onClick = onToggleFavorite) {
                            Icon(
                                imageVector = if (video.favorite) Icons.Rounded.Favorite
                                else Icons.Rounded.FavoriteBorder,
                                contentDescription = if (video.favorite) {
                                    "Hapus dari favorit"
                                } else {
                                    "Tambah ke favorit"
                                },
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(if (video.favorite) "Tersimpan" else "Favorit")
                        }
                        Spacer(Modifier.width(8.dp))
                        OutlinedButton(onClick = onFullscreen) {
                            Icon(
                                imageVector = Icons.Rounded.Fullscreen,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Layar penuh")
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "Video diputar langsung dari penyimpanan. Tidak ada internet yang digunakan.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (video.softwareOnly) {
                        Spacer(Modifier.height(10.dp))
                        SoftwareCodecNotice(video.codecMime)
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }

            RecommendationSidebar(
                videos = suggestions,
                onPlay = onPlayVideo,
                modifier = Modifier
                    .width(sidebarWidth)
                    .fillMaxHeight(),
            )
        }
        }
    }
}

/**
 * Catatan pada halaman tonton saat codec video hanya bisa didekode lewat CPU.
 * Ini penyebab umum pemutaran patah-patah di head unit.
 */
@Composable
private fun SoftwareCodecNotice(codecMime: String) {
    val label = codecLabel(codecMime).ifBlank { "codec ini" }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            imageVector = Icons.Rounded.Warning,
            contentDescription = null,
            tint = Color(0xFFB26A00),
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Codec $label tanpa dukungan hardware",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "Head unit ini tidak punya pemutar $label khusus, jadi video " +
                    "didekode lewat CPU dan bisa terasa patah-patah. Paling lancar: " +
                    "ubah file ke MP4 H.264 (misalnya 720p atau 1080p).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RecommendationSidebar(
    videos: List<Video>,
    onPlay: (Video) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val navBarBottom = with(density) {
        WindowInsets.navigationBars.getBottom(this).toDp()
    }
    Column(
        modifier = modifier.background(MaterialTheme.colorScheme.surface),
    ) {
        // Seperti YouTube: tanpa judul bagian, kartu rekomendasi langsung tampil
        // dengan thumbnail besar di atas dan keterangan video di bawahnya.
        if (videos.isEmpty()) {
            Text(
                text = "Belum ada video lain di folder ini.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(
                    start = 12.dp,
                    end = 12.dp,
                    top = 10.dp,
                    bottom = 10.dp + navBarBottom,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(videos, key = { it.uri }) { item ->
                    RecommendationCard(
                        video = item,
                        onClick = { onPlay(item) },
                    )
                }
            }
        }
    }
}

@Composable
private fun RecommendationCard(
    video: Video,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(12.dp)

    // Satu kolom dua baris: thumbnail di atas, nama kanal & judul di bawah.
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                if (focused) MaterialTheme.colorScheme.surfaceContainer else Color.Transparent,
            )
            .border(
                BorderStroke(2.dp, if (focused) YouTubeRed else Color.Transparent),
                shape,
            )
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick,
            ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(12.dp)),
        ) {
            VideoThumbnail(
                path = video.thumbnailPath,
                title = video.title,
                modifier = Modifier.fillMaxSize(),
            )
            DurationBadge(
                text = formatDuration(video.durationMs),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp),
            )
            if (video.progressFraction > 0f) {
                WatchProgressBar(
                    fraction = video.progressFraction,
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            ChannelAvatar(channel = video.channel, size = 34, avatarPath = video.channelAvatarPath)
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        lineHeight = 18.sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(3.dp))
                // Subjudul satu baris ala YouTube: kanal, durasi, lalu status unduhan.
                // Bila tidak muat, teks turun ke baris berikutnya.
                Text(
                    text = "${video.channel} • ${formatDuration(video.durationMs)} • offline",
                    style = MaterialTheme.typography.bodySmall.copy(lineHeight = 16.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
