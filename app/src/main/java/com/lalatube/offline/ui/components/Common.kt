package com.lalatube.offline.ui.components

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items as lazyRowItems
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lalatube.offline.data.Video
import com.lalatube.offline.ui.theme.YouTubeRed
import com.lalatube.offline.util.codecLabel
import com.lalatube.offline.util.formatDuration
import com.lalatube.offline.util.progressDescription
import com.lalatube.offline.util.relativeDate

@Composable
fun LalaTubeLogo(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(width = 38.dp, height = 27.dp)
                .background(YouTubeRed, RoundedCornerShape(7.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.PlayArrow,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = "LalaTube",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.9).sp,
            ),
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
fun OfflineBadge(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.WifiOff,
                contentDescription = null,
                modifier = Modifier.size(13.dp),
            )
            Text("Offline", style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
fun ChannelAvatar(
    channel: String,
    size: Int = 36,
    modifier: Modifier = Modifier,
) {
    val color = channelColor(channel)
    Box(
        modifier = modifier
            .size(size.dp)
            .background(color, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = channel.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?",
            color = Color.White,
            fontSize = (size * 0.42f).sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

private fun channelColor(channel: String): Color {
    val palette = listOf(
        Color(0xFFB3261E),
        Color(0xFF7D5260),
        Color(0xFF006A60),
        Color(0xFF4A5C92),
        Color(0xFF7A5900),
        Color(0xFF8B5000),
        Color(0xFF3F5F90),
        Color(0xFF6C4B8B),
        Color(0xFF2F6B4F),
        Color(0xFF8A3B52),
    )
    return palette[(channel.hashCode() and 0x7FFFFFFF) % palette.size]
}

@Composable
fun DurationBadge(
    text: String,
    modifier: Modifier = Modifier,
) {
    if (text.isBlank()) return
    Box(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.78f), RoundedCornerShape(5.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/**
 * Penanda kecil di pojok thumbnail untuk video yang hanya bisa didekode lewat CPU.
 * Head unit yang lemah akan memutar video seperti ini secara patah-patah.
 */
@Composable
fun SoftwareCodecBadge(
    codecMime: String,
    modifier: Modifier = Modifier,
) {
    val label = codecLabel(codecMime).ifBlank { "Codec" }
    Row(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.72f), RoundedCornerShape(5.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Icon(
            imageVector = Icons.Rounded.Warning,
            contentDescription = null,
            tint = Color(0xFFFFC107),
            modifier = Modifier.size(12.dp),
        )
        Text(
            text = label,
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
fun WatchProgressBar(
    fraction: Float,
    modifier: Modifier = Modifier,
    trackColor: Color = Color.White.copy(alpha = 0.35f),
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(3.dp)
            .background(trackColor),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .fillMaxHeight()
                .background(YouTubeRed),
        )
    }
}

@Composable
fun VideoCard(
    video: Video,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(14.dp)

    Column(
        modifier = modifier
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
            )
            .padding(6.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(11.dp)),
        ) {
            VideoThumbnail(
                path = video.thumbnailPath,
                title = video.title,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.26f)),
                        ),
                    ),
            )
            if (video.favorite) {
                Icon(
                    imageVector = Icons.Rounded.Favorite,
                    contentDescription = "Favorit",
                    tint = Color.White,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(18.dp),
                )
            }
            if (video.softwareOnly) {
                SoftwareCodecBadge(
                    codecMime = video.codecMime,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(7.dp),
                )
            }
            DurationBadge(
                text = formatDuration(video.durationMs),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(7.dp),
            )
            if (video.progressFraction > 0f) {
                WatchProgressBar(
                    fraction = video.progressFraction,
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }

        Spacer(Modifier.height(9.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            ChannelAvatar(channel = video.channel, size = 36)
            Spacer(Modifier.width(9.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        lineHeight = 20.sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    minLines = 2,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = video.channel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = buildString {
                        append(if (video.completed) "Sudah ditonton" else "Offline")
                        if (video.canResume) {
                            append(" • ")
                            append(progressDescription(video.positionMs, video.durationMs))
                        } else if (video.addedAt > 0L) {
                            append(" • ")
                            append(relativeDate(video.addedAt))
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = "Opsi video",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                DropdownMenu(
                    expanded = menuOpen,
                    onDismissRequest = { menuOpen = false },
                ) {
                    DropdownMenuItem(
                        text = { Text("Putar sekarang") },
                        leadingIcon = {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                        },
                        onClick = {
                            menuOpen = false
                            onClick()
                        },
                    )
                    DropdownMenuItem(
                        text = {
                            Text(if (video.favorite) "Hapus dari favorit" else "Tambah ke favorit")
                        },
                        leadingIcon = {
                            Icon(
                                if (video.favorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                contentDescription = null,
                            )
                        },
                        onClick = {
                            menuOpen = false
                            onToggleFavorite()
                        },
                    )
                }
            }
        }
    }
}

@Composable
fun WideVideoCard(
    video: Video,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(13.dp)
    Column(
        modifier = modifier
            .width(300.dp)
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
            )
            .padding(6.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(10.dp)),
        ) {
            VideoThumbnail(
                path = video.thumbnailPath,
                title = video.title,
                modifier = Modifier.fillMaxSize(),
            )
            if (video.softwareOnly) {
                SoftwareCodecBadge(
                    codecMime = video.codecMime,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(7.dp),
                )
            }
            DurationBadge(
                text = formatDuration(video.durationMs),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(7.dp),
            )
            WatchProgressBar(
                fraction = video.progressFraction,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
        Spacer(Modifier.height(7.dp))
        Text(
            text = video.title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            minLines = 2,
        )
        Text(
            text = "${video.channel} • ${progressDescription(video.positionMs, video.durationMs)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun ChannelFilterChip(
    channel: String,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        modifier = modifier.heightIn(min = 38.dp),
        shape = CircleShape,
        leadingIcon = {
            ChannelAvatar(channel = channel, size = 23)
        },
        label = {
            Text(
                text = label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.onSurface,
            selectedLabelColor = MaterialTheme.colorScheme.surface,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            labelColor = MaterialTheme.colorScheme.onSurface,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = Color.Transparent,
            selectedBorderColor = Color.Transparent,
        ),
    )
}

@Composable
fun SectionHeading(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (!subtitle.isNullOrBlank()) {
            Spacer(Modifier.height(3.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun EmptyState(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    primaryLabel: String? = null,
    onPrimary: (() -> Unit)? = null,
    secondaryLabel: String? = null,
    onSecondary: (() -> Unit)? = null,
    icon: @Composable () -> Unit = {
        Icon(
            imageVector = Icons.Rounded.VideoLibrary,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(42.dp),
        )
    },
) {
    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        // Layar head unit dan ponsel dalam mode lanskap sering hanya punya
        // ruang vertikal ±200dp. Tata letak ringkas menjaga tombol utama
        // tetap terlihat tanpa perlu menggulir.
        val compact = maxHeight < 360.dp

        val actions: @Composable () -> Unit = {
            if (primaryLabel != null && onPrimary != null) {
                Spacer(Modifier.height(if (compact) 10.dp else 18.dp))
                Button(onClick = onPrimary) {
                    Icon(
                        imageVector = Icons.Rounded.FolderOpen,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(primaryLabel)
                }
            }
            if (secondaryLabel != null && onSecondary != null) {
                Spacer(Modifier.height(if (compact) 2.dp else 8.dp))
                androidx.compose.material3.TextButton(onClick = onSecondary) {
                    Text(secondaryLabel)
                }
            }
        }

        if (compact) {
            Row(
                modifier = Modifier
                    .widthIn(max = 700.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            MaterialTheme.colorScheme.primaryContainer,
                            CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    icon()
                }
                Spacer(Modifier.width(16.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.Start,
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    actions()
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .widthIn(max = 580.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 28.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(92.dp)
                        .background(
                            MaterialTheme.colorScheme.primaryContainer,
                            CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    icon()
                }
                Spacer(Modifier.height(18.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
                Spacer(Modifier.height(7.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
                actions()
            }
        }
    }
}

@Composable
fun VideoGrid(
    videos: List<Video>,
    onPlay: (Video) -> Unit,
    onToggleFavorite: (Video) -> Unit,
    modifier: Modifier = Modifier,
    header: (@Composable () -> Unit)? = null,
    emptyContent: (@Composable () -> Unit)? = null,
    onScrollDirection: ((scrollingDown: Boolean) -> Unit)? = null,
) {
    if (videos.isEmpty() && emptyContent != null) {
        Box(modifier = modifier.fillMaxSize()) {
            emptyContent()
        }
        return
    }

    val gridState = rememberLazyGridState()

    // Melaporkan arah gulir supaya header bisa disembunyikan seperti YouTube.
    if (onScrollDirection != null) {
        LaunchedEffect(gridState, onScrollDirection) {
            var lastIndex = gridState.firstVisibleItemIndex
            var lastOffset = gridState.firstVisibleItemScrollOffset
            snapshotFlow {
                gridState.firstVisibleItemIndex to gridState.firstVisibleItemScrollOffset
            }.collect { (index, offset) ->
                val movedDown = index > lastIndex || (index == lastIndex && offset > lastOffset)
                val atTop = index == 0 && offset <= 8
                when {
                    atTop -> onScrollDirection(false)
                    movedDown -> onScrollDirection(true)
                    index < lastIndex || offset < lastOffset -> onScrollDirection(false)
                }
                lastIndex = index
                lastOffset = offset
            }
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        // 2K (sekitar 1280dp) = 4 kolom, layar lebih lebar dapat 5 kolom.
        val columns = videoGridColumns(maxWidth)
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            state = gridState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (header != null) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    header()
                }
            }
            items(videos, key = { it.uri }) { video ->
                VideoCard(
                    video = video,
                    onClick = { onPlay(video) },
                    onToggleFavorite = { onToggleFavorite(video) },
                )
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                Spacer(Modifier.height(18.dp))
            }
        }
    }
}

/** Jumlah kolom mengikuti lebar layar, disamakan dengan aplikasi YouTube. */
private fun videoGridColumns(width: Dp): Int = when {
    width >= 1800.dp -> 4
    width >= 700.dp -> 3
    else -> 2
}

@Composable
fun ContinueShelf(
    videos: List<Video>,
    onPlay: (Video) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        lazyRowItems(videos, key = { "resume_${it.uri}" }) { video ->
            WideVideoCard(video = video, onClick = { onPlay(video) })
        }
    }
}

@Composable
fun ChannelChipRow(
    channels: List<Pair<String, Int>>,
    selectedChannel: String?,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        item {
            FilterChip(
                selected = selectedChannel == null,
                onClick = { onSelect(null) },
                modifier = Modifier.heightIn(min = 38.dp),
                shape = CircleShape,
                label = { Text("Semua") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.onSurface,
                    selectedLabelColor = MaterialTheme.colorScheme.surface,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selectedChannel == null,
                    borderColor = Color.Transparent,
                    selectedBorderColor = Color.Transparent,
                ),
            )
        }
        lazyRowItems(channels, key = { it.first }) { (channel, count) ->
            ChannelFilterChip(
                channel = channel,
                label = if (count > 0) "$channel · $count" else channel,
                selected = selectedChannel == channel,
                onClick = { onSelect(if (selectedChannel == channel) null else channel) },
            )
        }
    }
}

@Composable
fun BackHeader(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.Rounded.ArrowBack, contentDescription = "Kembali")
        }
        Spacer(Modifier.width(4.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

