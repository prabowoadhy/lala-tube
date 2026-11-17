package com.lalatube.offline.ui.screens

import android.graphics.Color as AndroidColor
import androidx.activity.compose.BackHandler
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.lalatube.offline.data.Video
import com.lalatube.offline.ui.components.ChannelAvatar
import com.lalatube.offline.ui.theme.YouTubeRed
import com.lalatube.offline.util.formatDuration
import com.lalatube.offline.util.videoShuffleKey
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    video: Video,
    queue: List<Video>,
    recommendations: List<Video>,
    shuffleSeed: Long,
    fullscreen: Boolean,
    onFullscreenChange: (Boolean) -> Unit,
    onBack: () -> Unit,
    onPlayVideo: (Video) -> Unit,
    onProgress: (uri: String, positionMs: Long, completed: Boolean) -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val currentIndex = queue.indexOfFirst { it.uri == video.uri }
    val nextVideo = if (currentIndex >= 0) queue.getOrNull(currentIndex + 1) else null
    // Seed per-video: kombinasi seed sesi + URI video yang diputar.
    // Setiap ganti video = urutan rekomendasi berbeda, tapi deterministik.
    val videoShuffleSeed = remember(video.uri, shuffleSeed) {
        videoShuffleKey(shuffleSeed, video.uri)
    }
    val suggestions = remember(recommendations, video.uri, videoShuffleSeed) {
        val seed = videoShuffleSeed
        recommendations
            .asSequence()
            .filter { it.uri != video.uri }
            // Video dari kanal yang sama didahulukan seperti YouTube, tetapi
            // urutannya diacak per video supaya tidak selalu sama.
            .sortedWith(
                compareBy<Video> { if (it.channel == video.channel) 0 else 1 }
                    .thenBy { videoShuffleKey(seed, it.uri) },
            )
            .toList()
    }

    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            setAudioAttributes(
                androidx.media3.common.AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                    .build(),
                true,
            )
            setHandleAudioBecomingNoisy(true)
            setWakeMode(C.WAKE_MODE_LOCAL)
            repeatMode = Player.REPEAT_MODE_OFF
        }
    }

    var isPlaying by remember { mutableStateOf(false) }
    var playbackState by remember { mutableIntStateOf(Player.STATE_IDLE) }
    var positionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(video.durationMs) }
    var controlsVisible by remember { mutableStateOf(false) }
    var isScrubbing by remember { mutableStateOf(false) }
    var scrubPosition by remember { mutableLongStateOf(0L) }
    var ended by remember { mutableStateOf(false) }
    var countdown by remember { mutableIntStateOf(-1) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val latestOnProgress by rememberUpdatedState(onProgress)
    val latestOnPlayVideo by rememberUpdatedState(onPlayVideo)
    val latestUri by rememberUpdatedState(video.uri)
    val latestEnded by rememberUpdatedState(ended)

    fun saveProgress(completed: Boolean) {
        val duration = player.duration.takeIf { it > 0L } ?: durationMs
        val position = player.currentPosition.coerceAtLeast(0L)
        val isCompleted = completed || (duration > 0L && position >= duration - 2_000L)
        latestOnProgress(latestUri, if (isCompleted) 0L else position, isCompleted)
    }

    fun leavePlayer() {
        saveProgress(latestEnded)
        onBack()
    }

    val stageBack: () -> Unit = {
        if (fullscreen) onFullscreenChange(false) else leavePlayer()
    }

    BackHandler { stageBack() }

    DisposableEffect(view) {
        val previous = view.keepScreenOn
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = previous }
    }

    // Mode layar penuh: sembunyikan status bar + navigation bar seperti YouTube,
    // lalu tampilkan lagi saat keluar. Di halaman tonton biasa kedua bar tetap
    // terlihat dan konten diberi jarak aman supaya tidak tertutup.
    LaunchedEffect(fullscreen) {
        val window = (context as? android.app.Activity)?.window ?: return@LaunchedEffect
        val controller = WindowCompat.getInsetsController(window, view)
        if (fullscreen) {
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
        }
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
                if (!playing && !ended && player.currentPosition > 1_000L) {
                    saveProgress(false)
                }
            }

            override fun onPlaybackStateChanged(state: Int) {
                playbackState = state
                if (state == Player.STATE_ENDED) {
                    ended = true
                    controlsVisible = true
                    durationMs = player.duration.takeIf { it > 0L } ?: durationMs
                    positionMs = durationMs
                    saveProgress(true)
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                errorMessage = friendlyPlaybackError(error)
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            if (!latestEnded) {
                saveProgress(false)
            }
            player.release()
        }
    }

    LaunchedEffect(video.uri) {
        player.setMediaItem(MediaItem.fromUri(video.uri))
        player.prepare()
        val resumeAt = if (video.completed) 0L else video.positionMs
        if (resumeAt > 0L) player.seekTo(resumeAt)
        player.playWhenReady = true
    }

    LaunchedEffect(player) {
        while (isActive) {
            if (player.duration > 0L) durationMs = player.duration
            if (!isScrubbing) positionMs = player.currentPosition.coerceAtLeast(0L)
            delay(350)
        }
    }

    LaunchedEffect(player) {
        while (isActive) {
            delay(15_000L)
            if (player.isPlaying) saveProgress(false)
        }
    }

    LaunchedEffect(isPlaying, controlsVisible, ended) {
        if (isPlaying && controlsVisible && !ended) {
            delay(3_800L)
            controlsVisible = false
        }
    }

    LaunchedEffect(ended, nextVideo?.uri) {
        if (ended && nextVideo != null) {
            for (tick in 6 downTo 1) {
                countdown = tick
                delay(1_000L)
            }
            countdown = -1
            if (ended) {
                saveProgress(true)
                latestOnPlayVideo(nextVideo)
            }
        } else {
            countdown = -1
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, player) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                player.pause()
                saveProgress(false)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val effectiveDuration = durationMs.coerceAtLeast(0L)
    val sliderPosition = if (isScrubbing) scrubPosition else positionMs
    val showControls = controlsVisible || ended || errorMessage != null

    val playerStage: @Composable (Modifier) -> Unit = { stageModifier ->
    Box(
        modifier = stageModifier
            .background(Color.Black),
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    setShutterBackgroundColor(AndroidColor.BLACK)
                    keepScreenOn = true
                    this.player = player
                }
            },
            update = { it.player = player },
            modifier = Modifier.fillMaxSize(),
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures {
                        controlsVisible = !controlsVisible
                    }
                },
        )

        if (playbackState == Player.STATE_BUFFERING && errorMessage == null) {
            CircularProgressIndicator(
                color = YouTubeRed,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(46.dp),
            )
        }

        if (!isPlaying && !ended && errorMessage == null && !controlsVisible &&
            playbackState == Player.STATE_READY
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(84.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = "Putar",
                    tint = Color.White,
                    modifier = Modifier.size(50.dp),
                )
            }
        }

        AnimatedVisibility(
            visible = showControls && errorMessage == null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Black.copy(alpha = 0.82f), Color.Transparent),
                            ),
                        )
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = { stageBack() }) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowBack,
                            contentDescription = "Kembali",
                            tint = Color.White,
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                    ChannelAvatar(channel = video.channel, size = 34, avatarPath = video.channelAvatarPath)
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = video.title,
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = "${video.channel} • offline",
                            color = Color.White.copy(alpha = 0.72f),
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                        )
                    }
                    IconButton(onClick = onToggleFavorite) {
                        Icon(
                            imageVector = if (video.favorite) Icons.Rounded.Favorite
                            else Icons.Rounded.FavoriteBorder,
                            contentDescription = if (video.favorite) {
                                "Hapus dari favorit"
                            } else {
                                "Tambah ke favorit"
                            },
                            tint = Color.White,
                        )
                    }
                    IconButton(onClick = { onFullscreenChange(!fullscreen) }) {
                        Icon(
                            imageVector = if (fullscreen) Icons.Rounded.FullscreenExit
                            else Icons.Rounded.Fullscreen,
                            contentDescription = if (fullscreen) {
                                "Keluar layar penuh"
                            } else {
                                "Layar penuh"
                            },
                            tint = Color.White,
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.88f)),
                            ),
                        )
                        .padding(horizontal = 26.dp, vertical = 10.dp),
                ) {
                    Slider(
                        value = sliderPosition.toFloat().coerceIn(
                            0f,
                            effectiveDuration.coerceAtLeast(1L).toFloat(),
                        ),
                        onValueChange = {
                            isScrubbing = true
                            scrubPosition = it.toLong()
                        },
                        onValueChangeFinished = {
                            player.seekTo(scrubPosition)
                            positionMs = scrubPosition
                            isScrubbing = false
                            if (ended) {
                                ended = false
                                player.play()
                            }
                        },
                        enabled = effectiveDuration > 0L,
                        valueRange = 0f..effectiveDuration.coerceAtLeast(1L).toFloat(),
                        colors = SliderDefaults.colors(
                            thumbColor = YouTubeRed,
                            activeTrackColor = YouTubeRed,
                            inactiveTrackColor = Color.White.copy(alpha = 0.34f),
                        ),
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = formatDuration(sliderPosition),
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                        )
                        Text(
                            text = formatDuration(effectiveDuration),
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                    Spacer(Modifier.height(2.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        PlayerControlButton(
                            icon = Icons.Rounded.Replay10,
                            description = "Mundur 10 detik",
                            onClick = {
                                player.seekTo((player.currentPosition - 10_000L).coerceAtLeast(0L))
                                if (ended) {
                                    ended = false
                                    player.play()
                                }
                            },
                        )
                        Spacer(Modifier.width(16.dp))
                        IconButton(
                            onClick = {
                                if (player.isPlaying) player.pause() else {
                                    if (ended) {
                                        player.seekTo(0)
                                        ended = false
                                    }
                                    player.play()
                                }
                            },
                            modifier = Modifier
                                .size(68.dp)
                                .background(Color.White, CircleShape),
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Rounded.Pause
                                else Icons.Rounded.PlayArrow,
                                contentDescription = if (isPlaying) "Jeda" else "Putar",
                                tint = Color.Black,
                                modifier = Modifier.size(42.dp),
                            )
                        }
                        Spacer(Modifier.width(16.dp))
                        PlayerControlButton(
                            icon = Icons.Rounded.Forward10,
                            description = "Maju 10 detik",
                            onClick = {
                                player.seekTo(
                                    (player.currentPosition + 10_000L)
                                        .coerceAtMost(effectiveDuration.coerceAtLeast(0L)),
                                )
                            },
                        )
                        if (nextVideo != null) {
                            Spacer(Modifier.width(22.dp))
                            PlayerControlButton(
                                icon = Icons.Rounded.SkipNext,
                                description = "Video berikutnya",
                                onClick = {
                                    saveProgress(false)
                                    latestOnPlayVideo(nextVideo)
                                },
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                }
            }
        }

        if (ended && errorMessage == null) {
            EndOverlay(
                countdown = countdown,
                hasNext = nextVideo != null,
                nextTitle = nextVideo?.title,
                onReplay = {
                    countdown = -1
                    ended = false
                    player.seekTo(0)
                    player.play()
                    controlsVisible = false
                },
                onNext = {
                    saveProgress(true)
                    nextVideo?.let(latestOnPlayVideo)
                },
                onBack = { stageBack() },
                modifier = Modifier.align(Alignment.Center),
            )
        }

        if (errorMessage != null) {
            ErrorOverlay(
                message = errorMessage!!,
                onRetry = {
                    errorMessage = null
                    player.prepare()
                    player.play()
                },
                onBack = { stageBack() },
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }
    }

    if (fullscreen) {
        playerStage(modifier.fillMaxSize())
    } else {
        WatchLayout(
            video = video,
            suggestions = suggestions,
            onBack = { leavePlayer() },
            onPlayVideo = onPlayVideo,
            onToggleFavorite = onToggleFavorite,
            onFullscreen = { onFullscreenChange(true) },
            playerStage = playerStage,
        )
    }
}

@Composable
private fun PlayerControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(54.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = Color.White,
            modifier = Modifier.size(31.dp),
        )
    }
}

@Composable
private fun EndOverlay(
    countdown: Int,
    hasNext: Boolean,
    nextTitle: String?,
    onReplay: () -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .widthIn(max = 460.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(Color.Black.copy(alpha = 0.82f))
            .padding(horizontal = 28.dp, vertical = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = if (hasNext) "Video berikutnya" else "Video selesai",
            color = Color.White,
            style = MaterialTheme.typography.titleLarge,
        )
        if (hasNext && !nextTitle.isNullOrBlank()) {
            Spacer(Modifier.height(5.dp))
            Text(
                text = nextTitle,
                color = Color.White.copy(alpha = 0.72f),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (hasNext && countdown > 0) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Diputar otomatis dalam $countdown detik",
                color = Color.White.copy(alpha = 0.6f),
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Spacer(Modifier.height(18.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(
                onClick = onReplay,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color.Black,
                ),
            ) {
                Icon(Icons.Rounded.Replay, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(7.dp))
                Text("Putar lagi")
            }
            if (hasNext) {
                Button(onClick = onNext) {
                    Text("Berikutnya")
                    Spacer(Modifier.width(7.dp))
                    Icon(Icons.Rounded.SkipNext, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            }
            TextButton(onClick = onBack) {
                Text("Kembali", color = Color.White)
            }
        }
    }
}

@Composable
private fun ErrorOverlay(
    message: String,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .widthIn(max = 520.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0xFF1F1F1F))
            .padding(horizontal = 28.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Rounded.Warning,
            contentDescription = null,
            tint = Color(0xFFFFB4AB),
            modifier = Modifier.size(42.dp),
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Video tidak bisa diputar",
            color = Color.White,
            style = MaterialTheme.typography.titleLarge,
        )
        Spacer(Modifier.height(7.dp))
        Text(
            text = message,
            color = Color.White.copy(alpha = 0.72f),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = onRetry) { Text("Coba lagi") }
            TextButton(onClick = onBack) { Text("Kembali", color = Color.White) }
        }
    }
}

private fun friendlyPlaybackError(error: PlaybackException): String {
    return when (error.errorCode) {
        PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND,
        PlaybackException.ERROR_CODE_IO_NO_PERMISSION,
        -> "File video tidak ditemukan atau akses flashdisk terputus. " +
            "Colokkan kembali flashdisk lalu pindai ulang folder."

        PlaybackException.ERROR_CODE_DECODING_FAILED,
        PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
        PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED,
        -> "Format atau codec video ini tidak didukung head unit. " +
            "Gunakan MP4 dengan video H.264 dan audio AAC."

        else -> "Terjadi masalah saat memutar video. Coba format MP4 (H.264/AAC). " +
            (error.localizedMessage?.takeIf { it.isNotBlank() } ?: "")
    }
}
