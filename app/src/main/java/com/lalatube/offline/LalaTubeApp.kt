package com.lalatube.offline

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
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
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
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
    val icon: ImageVector,
)

private val destinations = listOf(
    NavDestination(Tabs.HOME, "Beranda", Icons.Rounded.Home),
    NavDestination(Tabs.FAVORITES, "Favorit", Icons.Rounded.Favorite),
    NavDestination(Tabs.HISTORY, "Riwayat", Icons.Rounded.History),
    NavDestination(Tabs.SETTINGS, "Pengaturan", Icons.Rounded.Settings),
)

@Composable
fun LalaTubeApp(viewModel: MainViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf(Tabs.HOME) }
    var searchOpen by rememberSaveable { mutableStateOf(false) }
    var pickerOpen by rememberSaveable { mutableStateOf(false) }
    var playerId by rememberSaveable { mutableStateOf<String?>(null) }
    var playerFullscreen by rememberSaveable { mutableStateOf(false) }
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
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            } else {
                if (playerId != null) {
                    LaunchedEffect(state.initialized) {
                        if (state.initialized) playerId = null
                    }
                }

                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.safeDrawing),
                ) {
                    val wide = maxWidth >= 840.dp
                    Scaffold(
                        containerColor = MaterialTheme.colorScheme.background,
                        contentWindowInsets = WindowInsets(0, 0, 0, 0),
                        snackbarHost = {
                            SnackbarHost(
                                hostState = snackbarHostState,
                                modifier = Modifier.padding(
                                    bottom = if (wide) 16.dp else 86.dp,
                                ),
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
                                    } else {
                                        HorizontalDivider(
                                            color = MaterialTheme.colorScheme.outlineVariant,
                                        )
                                    }
                                }

                                Row(modifier = Modifier.weight(1f)) {
                                    if (wide) {
                                        AppNavigationRail(
                                            selected = tab,
                                            onSelect = { tab = it },
                                        )
                                        HorizontalDivider(
                                            modifier = Modifier
                                                .width(1.dp)
                                                .fillMaxHeight(),
                                            color = MaterialTheme.colorScheme.outlineVariant,
                                        )
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
                                }

                                if (!wide) {
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
                .height(62.dp)
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LalaTubeLogo()
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onSearch) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = "Cari",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(25.dp),
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

@Composable
private fun AppNavigationRail(
    selected: String,
    onSelect: (String) -> Unit,
) {
    NavigationRail(
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxHeight(),
    ) {
        Spacer(Modifier.height(8.dp))
        destinations.forEach { destination ->
            NavigationRailItem(
                selected = selected == destination.tab,
                onClick = { onSelect(destination.tab) },
                icon = {
                    Icon(
                        imageVector = destination.icon,
                        contentDescription = null,
                    )
                },
                label = { Text(destination.label) },
                alwaysShowLabel = true,
            )
        }
        Spacer(Modifier.weight(1f))
        Text(
            text = "OFFLINE",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 18.dp),
        )
    }
}

@Composable
private fun AppBottomBar(
    selected: String,
    onSelect: (String) -> Unit,
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        modifier = Modifier.height(72.dp),
    ) {
        destinations.forEach { destination ->
            NavigationBarItem(
                selected = selected == destination.tab,
                onClick = { onSelect(destination.tab) },
                icon = {
                    Icon(
                        imageVector = destination.icon,
                        contentDescription = null,
                    )
                },
                label = { Text(destination.label) },
            )
        }
    }
}
