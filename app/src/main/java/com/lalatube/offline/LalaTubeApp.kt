package com.lalatube.offline

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.Lifecycle
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.offset
import com.lalatube.offline.data.StorageAccess
import com.lalatube.offline.data.ThemeMode
import com.lalatube.offline.data.Video
import com.lalatube.offline.ui.components.LalaTubeLogo
import com.lalatube.offline.ui.screens.ChannelScreen
import com.lalatube.offline.ui.screens.FavoritesScreen
import com.lalatube.offline.ui.screens.FolderPickerScreen
import com.lalatube.offline.ui.screens.HistoryScreen
import com.lalatube.offline.ui.screens.HomeScreen
import com.lalatube.offline.ui.screens.PlayerScreen
import com.lalatube.offline.ui.screens.SearchScreen
import com.lalatube.offline.ui.screens.SettingsScreen
import com.lalatube.offline.ui.theme.LalaTubeTheme

private object Tabs {
    const val HOME = "home"
    const val FAVORITES = "favorites"
    const val HISTORY = "history"
    const val SETTINGS = "settings"
}

private data class NavDestination(
    val tab: String,
    val label: String,
    /** Ikon saat menu tidak dipilih: tanpa isi (outline). */
    val icon: ImageVector,
    /** Ikon saat menu aktif: terisi penuh seperti YouTube. */
    val activeIcon: ImageVector,
)

private val destinations = listOf(
    NavDestination(
        Tabs.HOME,
        "Beranda",
        Icons.Outlined.Home,
        Icons.Rounded.Home,
    ),
    NavDestination(
        Tabs.FAVORITES,
        "Favorit",
        Icons.Outlined.FavoriteBorder,
        Icons.Rounded.Favorite,
    ),
    NavDestination(
        Tabs.HISTORY,
        "Riwayat",
        Icons.Outlined.History,
        Icons.Rounded.History,
    ),
    NavDestination(
        Tabs.SETTINGS,
        "Pengaturan",
        Icons.Outlined.Settings,
        Icons.Rounded.Settings,
    ),
)

@Composable
fun LalaTubeApp(viewModel: MainViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf(Tabs.HOME) }
    var searchOpen by rememberSaveable { mutableStateOf(false) }
    var pickerOpen by rememberSaveable { mutableStateOf(false) }
    var playerId by rememberSaveable { mutableStateOf<String?>(null) }
    var playerFullscreen by rememberSaveable { mutableStateOf(false) }
    var channelName by rememberSaveable { mutableStateOf<String?>(null) }
    var playerQueue by remember { mutableStateOf<List<String>>(emptyList()) }
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        viewModel.refreshStoragePermission()
        if (!granted) {
            viewModel.postMessage(
                "Izin baca file ditolak. Buka Pengaturan lalu izinkan akses file untuk LalaTube.",
            )
        }
    }

    // Pemilih folder bawaan aplikasi. Jauh lebih andal di head unit yang tidak
    // menyertakan DocumentsUI.
    val openBuiltInPicker = {
        pickerOpen = true
        if (!state.storagePermissionGranted) {
            runCatching { permissionLauncher.launch(StorageAccess.mediaPermission()) }
        }
    }
    val pickFolder = { openBuiltInPicker() }

    LaunchedEffect(state.message) {
        state.message?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.consumeMessage()
        }
    }

    // Saat pengguna kembali dari halaman izin Android, status izin disegarkan
    // supaya daftar folder langsung bisa dibuka tanpa menutup aplikasi.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refreshStoragePermission()
    }

    val openPlayer: (Video, List<Video>) -> Unit = { video, queue ->
        playerQueue = queue.map { it.uri }
        playerFullscreen = false
        playerId = video.uri
    }

    // Buka halaman kanal dari halaman tonton (avatar / nama kanal diketuk).
    val openChannel: (String) -> Unit = { channel ->
        channelName = channel
    }

    val activeVideo = playerId?.let { id -> state.videos.firstOrNull { it.uri == id } }
    val queueFromSelection = playerQueue
        .mapNotNull { id -> state.videos.firstOrNull { it.uri == id } }
    val activeQueue = if (queueFromSelection.any { it.uri == activeVideo?.uri }) {
        queueFromSelection
    } else {
        state.videos
    }

    BackHandler(enabled = pickerOpen) { pickerOpen = false }
    BackHandler(enabled = !pickerOpen && activeVideo == null && searchOpen) { searchOpen = false }
    BackHandler(
        enabled = !pickerOpen && activeVideo == null && !searchOpen && channelName != null,
    ) { channelName = null }
    BackHandler(enabled = !pickerOpen && activeVideo == null && !searchOpen && tab != Tabs.HOME) {
        tab = Tabs.HOME
    }

    LalaTubeTheme(themeMode = state.themeMode) {
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            if (pickerOpen) {
                Surface(
                    color = MaterialTheme.colorScheme.background,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    FolderPickerScreen(
                        hasPermission = StorageAccess.hasAllFilesAccess() ||
                            state.storagePermissionGranted,
                        onRequestPermission = {
                            if (!StorageAccess.hasAllFilesAccess() &&
                                StorageAccess.openAllFilesAccess(context)
                            ) {
                                viewModel.postMessage(
                                    "Izinkan akses file untuk LalaTube, lalu kembali ke aplikasi.",
                                )
                            } else {
                                runCatching {
                                    permissionLauncher.launch(StorageAccess.mediaPermission())
                                }.onFailure {
                                    viewModel.postMessage(
                                        "Head unit tidak menampilkan dialog izin. " +
                                            "Buka Pengaturan > Aplikasi > LalaTube > Izin.",
                                    )
                                }
                            }
                        },
                        onAutoDetect = {
                            pickerOpen = false
                            tab = Tabs.HOME
                            viewModel.autoDetect()
                        },
                        onBack = { pickerOpen = false },
                        onFolderChosen = { path, name ->
                            pickerOpen = false
                            tab = Tabs.HOME
                            viewModel.onFolderPathPicked(path, name)
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            } else if (activeVideo != null) {
                key(activeVideo.uri) {
                    PlayerScreen(
                        video = activeVideo,
                        queue = activeQueue,
                        recommendations = state.videos,
                        shuffleSeed = state.shuffleSeed,
                        fullscreen = playerFullscreen,
                        onFullscreenChange = { playerFullscreen = it },
                        onBack = {
                            playerId = null
                            playerFullscreen = false
                        },
                        onPlayVideo = { next ->
                            playerId = next.uri
                        },
                        onProgress = viewModel::updateProgress,
                        onToggleFavorite = { viewModel.toggleFavorite(activeVideo.uri) },
                        onOpenChannel = { channel ->
                            // Pindah ke halaman kanal; pemutar ditutup tetapi
                            // posisi tontonan tetap disimpan saat keluar.
                            playerId = null
                            playerFullscreen = false
                            channelName = channel
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            } else {
                if (playerId != null) {
                    LaunchedEffect(state.initialized) {
                        if (state.initialized) playerId = null
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.safeDrawing),
                ) {
                    Scaffold(
                        containerColor = MaterialTheme.colorScheme.background,
                        contentWindowInsets = WindowInsets(0, 0, 0, 0),
                        snackbarHost = {
                            SnackbarHost(
                                hostState = snackbarHostState,
                                modifier = Modifier.padding(bottom = 66.dp),
                            )
                        },
                    ) { _ ->
                        if (searchOpen) {
                            SearchScreen(
                                videos = state.videos,
                                onPlay = openPlayer,
                                onToggleFavorite = { viewModel.toggleFavorite(it.uri) },
                                onBack = { searchOpen = false },
                                modifier = Modifier.fillMaxSize(),
                            )
                        } else if (channelName != null) {
                            Column(modifier = Modifier.fillMaxSize()) {
                                ChannelScreen(
                                    channel = channelName!!,
                                    videos = state.videos.filter {
                                        it.channel == channelName
                                    },
                                    onPlayAll = {
                                        val list = state.videos
                                            .filter { it.channel == channelName }
                                            .sortedWith(
                                                compareByDescending<Video> { it.addedAt }
                                                    .thenByDescending { it.lastModified },
                                            )
                                        list.firstOrNull()?.let { first ->
                                            openPlayer(first, list)
                                        }
                                    },
                                    onPlay = openPlayer,
                                    onToggleFavorite = {
                                        viewModel.toggleFavorite(it.uri)
                                    },
                                    onSearch = { searchOpen = true },
                                    onRefresh = viewModel::rescan,
                                    onBack = { channelName = null },
                                    modifier = Modifier.weight(1f),
                                )
                                AppBottomBar(
                                    selected = Tabs.HOME,
                                    onSelect = {
                                        channelName = null
                                        tab = it
                                    },
                                )
                            }
                        } else {
                            Column(modifier = Modifier.fillMaxSize()) {
                                Column {
                                    AppTopBar(
                                        isScanning = state.isScanning,
                                        onSearch = { searchOpen = true },
                                        onRefresh = viewModel::rescan,
                                    )
                                    if (state.isScanning) {
                                        val progress = if (state.scanTotal > 0) {
                                            state.scanDone.toFloat() / state.scanTotal.toFloat()
                                        } else {
                                            null
                                        }
                                        if (progress == null) {
                                            LinearProgressIndicator(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(2.dp),
                                            )
                                        } else {
                                            LinearProgressIndicator(
                                                progress = { progress },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(2.dp),
                                            )
                                        }
                                    }
                                }

                                Box(modifier = Modifier.weight(1f)) {
                                    when (tab) {
                                        Tabs.FAVORITES -> FavoritesScreen(
                                            videos = state.videos,
                                            onPlay = openPlayer,
                                            onToggleFavorite = { viewModel.toggleFavorite(it.uri) },
                                            onPickFolder = pickFolder,
                                            modifier = Modifier.fillMaxSize(),
                                        )

                                        Tabs.HISTORY -> HistoryScreen(
                                            videos = state.videos,
                                            onPlay = openPlayer,
                                            onToggleFavorite = { viewModel.toggleFavorite(it.uri) },
                                            modifier = Modifier.fillMaxSize(),
                                        )

                                        Tabs.SETTINGS -> SettingsScreen(
                                            state = state,
                                            onPickFolder = pickFolder,
                                            onRescan = viewModel::rescan,
                                            onAutoDetect = viewModel::autoDetect,
                                            onForgetFolder = viewModel::forgetFolder,
                                            onThemeChange = { mode: ThemeMode ->
                                                viewModel.setThemeMode(mode)
                                            },
                                            onSafeModeChange = viewModel::setSafeMode,
                                            onClearCrashReport = viewModel::clearCrashReport,
                                            modifier = Modifier.fillMaxSize(),
                                        )

                                        else -> HomeScreen(
                                            state = state,
                                            onPlay = openPlayer,
                                            onToggleFavorite = { viewModel.toggleFavorite(it.uri) },
                                            onPickFolder = pickFolder,
                                            onRescan = viewModel::rescan,
                                            modifier = Modifier.fillMaxSize(),
                                        )
                                    }
                                }

                                AppBottomBar(
                                    selected = tab,
                                    onSelect = { tab = it },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppTopBar(
    isScanning: Boolean,
    onSearch: () -> Unit,
    onRefresh: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(start = 24.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LalaTubeLogo()
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onSearch) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = "Cari",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(24.dp),
                )
            }
            Spacer(Modifier.width(2.dp))
            IconButton(onClick = onRefresh) {
                if (isScanning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(19.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = "Pindai ulang",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        }
    }
}

/**
 * Navigasi bawah seperti aplikasi YouTube Android: deretan menu di tengah
 * layar, ikon kecil dengan label tipis, dan garis pemisah tipis di atasnya.
 */
@Composable
private fun AppBottomBar(
    selected: String,
    onSelect: (String) -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column {
            HorizontalDivider(
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    // Bar bawah dibuat lebih rapat: hanya setinggi ikon + label
                    // (48dp), seperti bottom bar YouTube Android.
                    .height(48.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                destinations.forEach { destination ->
                    val active = selected == destination.tab
                    val interactionSource = remember { MutableInteractionSource() }
                    val focused by interactionSource.collectIsFocusedAsState()
                    Column(
                        modifier = Modifier
                            .width(104.dp)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (focused) {
                                    MaterialTheme.colorScheme.surfaceContainerHigh
                                } else {
                                    Color.Transparent
                                },
                            )
                            .selectable(
                                selected = active,
                                role = Role.Tab,
                                interactionSource = interactionSource,
                                indication = null,
                                onClick = { onSelect(destination.tab) },
                            )
                            // Geser isi sedikit ke atas seperti YouTube: jarak
                            // ikon ke garis atas lebih rapat daripada ke bawah.
                            .padding(bottom = 2.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            // Tidak dipilih = ikon garis (nofill); dipilih = ikon
                            // terisi penuh dengan warna hitam seperti YouTube.
                            imageVector = if (active) {
                                destination.activeIcon
                            } else {
                                destination.icon
                            },
                            contentDescription = null,
                            tint = if (active) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(27.dp),
                        )
                        Spacer(Modifier.height(1.dp))
                        Text(
                            text = destination.label,
                            // Font menu bawah sama dengan aplikasi YouTube:
                            // Roboto Medium 12sp (tinggi baris 16sp). Saat aktif
                            // hanya warnanya yang berubah, teks tidak ditebalkan.
                            style = MaterialTheme.typography.labelMedium,
                            color = if (active) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                }
            }
        }
    }
}
