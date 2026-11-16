package com.lalatube.offline

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lalatube.offline.data.FolderAccessException
import com.lalatube.offline.data.LibraryRepository
import com.lalatube.offline.data.StorageAccess
import com.lalatube.offline.data.ThemeMode
import com.lalatube.offline.data.Video
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UiState(
    val initialized: Boolean = false,
    val videos: List<Video> = emptyList(),
    val treeName: String? = null,
    val treeUri: String? = null,
    val folderPath: String? = null,
    val hasAccess: Boolean = false,
    val isScanning: Boolean = false,
    val scanDone: Int = 0,
    val scanTotal: Int = 0,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val safeMode: Boolean = false,
    val crashReport: String? = null,
    val message: String? = null,
    val lastScanAt: Long = 0L,
    val storagePermissionGranted: Boolean = false,
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = LibraryRepository(application)
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    private var scanJob: Job? = null

    init {
        viewModelScope.launch {
            val saved = repository.loadSavedVideos().sortedByNewest()
            val tree = repository.treeUri
            val hasFolder = repository.hasFolder()
            val hasAccess = when {
                repository.mediaStoreMode -> true
                repository.folderPath != null -> true
                tree != null -> repository.hasPersistedPermission(tree)
                else -> false
            }
            _uiState.update {
                it.copy(
                    initialized = true,
                    // Katalog lama hanya ditampilkan kalau masih ada folder terpilih.
                    // Favorit/riwayat tetap tersimpan dan kembali saat folder dipilih lagi.
                    videos = if (hasFolder) saved else emptyList(),
                    treeName = if (hasFolder) repository.treeName else null,
                    treeUri = tree?.toString(),
                    folderPath = repository.folderPath,
                    hasAccess = hasAccess,
                    themeMode = repository.themeMode,
                    safeMode = repository.safeMode,
                    crashReport = readCrashReport(),
                    storagePermissionGranted = StorageAccess.canAccessStorage(
                        getApplication(),
                    ),
                )
            }
            if (hasAccess) scan(showToastOnError = false)
        }
    }

    /** Dipanggil setelah dialog izin media selesai. */
    fun refreshStoragePermission() {
        val granted = StorageAccess.canAccessStorage(getApplication())
        _uiState.update { it.copy(storagePermissionGranted = granted) }
        if (granted && repository.hasFolder()) scan(showToastOnError = false)
    }

    fun onFolderPicked(uri: Uri?) {
        if (uri == null) return
        viewModelScope.launch {
            // Jangan batalkan pemilihan folder hanya karena izin permanen ditolak:
            // izin sementara tetap bisa dipakai untuk memindai pada sesi ini.
            val persisted = runCatching { repository.takePersistedPermission(uri) }
                .getOrDefault(false)
            val name = repository.displayNameFor(uri)
            repository.storeTree(uri, name)
            _uiState.update {
                it.copy(
                    treeName = name,
                    treeUri = uri.toString(),
                    hasAccess = true,
                    message = null,
                )
            }
            scan(
                showToastOnError = true,
                warning = if (persisted) {
                    null
                } else {
                    "Folder bisa dibaca sekarang, tapi head unit tidak memberi izin permanen. " +
                        "Pilih ulang folder setelah aplikasi ditutup."
                },
            )
        }
    }

    fun rescan() {
        if (!repository.hasFolder()) {
            _uiState.update { it.copy(message = "Pilih folder video terlebih dahulu.") }
            return
        }
        scan(showToastOnError = true)
    }

    /** Folder dipilih lewat browser bawaan aplikasi (tanpa DocumentsUI). */
    fun onFolderPathPicked(path: String, name: String) {
        viewModelScope.launch {
            repository.setMediaStoreMode(false)
            repository.storeFolderPath(path, name)
            _uiState.update {
                it.copy(
                    treeName = name,
                    treeUri = null,
                    folderPath = path,
                    hasAccess = true,
                    message = null,
                )
            }
            scan(showToastOnError = true)
        }
    }

    /**
     * Mode cadangan: kumpulkan semua video yang dikenal MediaStore. Tidak butuh
     * akses folder, hanya izin baca media.
     */
    fun autoDetect() {
        viewModelScope.launch {
            repository.setMediaStoreMode(true)
            repository.forgetTreeKeepMode()
            _uiState.update {
                it.copy(
                    treeName = "Semua video di perangkat",
                    treeUri = null,
                    folderPath = null,
                    hasAccess = true,
                    message = null,
                )
            }
            scan(showToastOnError = true)
        }
    }

    private fun scan(showToastOnError: Boolean, warning: String? = null) {
        scanJob?.cancel()
        scanJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isScanning = true,
                    scanDone = 0,
                    scanTotal = 0,
                    message = null,
                )
            }
            try {
                val result = repository.scan(
                    onProgress = { done, total ->
                        _uiState.update { it.copy(scanDone = done, scanTotal = total) }
                    },
                    onPartial = { partial ->
                        // Daftar sudah tampil sebelum metadata/thumbnail selesai,
                        // jadi pemindaian panjang tidak terasa seperti macet.
                        val sorted = partial.sortedByNewest()
                        _uiState.update {
                            it.copy(
                                videos = sorted,
                                hasAccess = true,
                                scanDone = sorted.size,
                                scanTotal = sorted.size,
                            )
                        }
                    },
                )
                val sorted = result.videos.sortedByNewest()
                val path = repository.folderPath
                val tree = repository.treeUri
                when {
                    path != null -> repository.storeFolderPath(path, result.treeName)
                    tree != null -> repository.storeTree(tree, result.treeName)
                    repository.mediaStoreMode -> Unit // Mode cadangan tidak punya folder.
                    else -> throw IllegalStateException("Folder video belum dipilih.")
                }
                _uiState.update {
                    it.copy(
                        videos = sorted,
                        treeName = result.treeName,
                        folderPath = path,
                        hasAccess = true,
                        isScanning = false,
                        scanDone = sorted.size,
                        scanTotal = sorted.size,
                        lastScanAt = System.currentTimeMillis(),
                        message = warning,
                    )
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                val accessLost = exception is FolderAccessException ||
                    exception is SecurityException ||
                    (!repository.mediaStoreMode && repository.folderPath == null &&
                        !repository.hasPersistedPermission(repository.treeUri ?: Uri.EMPTY))
                _uiState.update {
                    it.copy(
                        isScanning = false,
                        hasAccess = if (accessLost) false else it.hasAccess,
                        message = when {
                            accessLost ->
                                "Akses folder hilang. Colokkan flashdisk lalu pilih foldernya lagi."
                            showToastOnError ->
                                "Gagal memindai: ${exception.localizedMessage ?: "kesalahan tidak diketahui"}"
                            else -> it.message
                        },
                    )
                }
            } catch (_: Throwable) {
                // OutOfMemoryError dan sejenisnya tidak boleh menutup aplikasi.
                _uiState.update {
                    it.copy(
                        isScanning = false,
                        message = "Pemindaian dihentikan karena memori penuh. " +
                            "Coba aktifkan Mode aman di Pengaturan, lalu pindai ulang.",
                    )
                }
            }
        }
    }

    fun updateProgress(uri: String, positionMs: Long, completed: Boolean) {
        val now = System.currentTimeMillis()
        val updated = _uiState.value.videos.map { video ->
            if (video.uri != uri) return@map video
            val duration = video.durationMs.coerceAtLeast(0L)
            val safePosition = if (completed || (duration > 0L && positionMs >= duration - 2_000L)) {
                0L
            } else {
                positionMs.coerceAtLeast(0L)
            }
            video.copy(
                positionMs = safePosition,
                completed = completed,
                lastPlayedAt = now,
            )
        }
        _uiState.update { it.copy(videos = updated) }
        viewModelScope.launch { repository.saveVideos(updated) }
    }

    fun toggleFavorite(uri: String) {
        val updated = _uiState.value.videos.map { video ->
            if (video.uri == uri) video.copy(favorite = !video.favorite) else video
        }
        _uiState.update { it.copy(videos = updated) }
        viewModelScope.launch { repository.saveVideos(updated) }
    }

    fun setThemeMode(mode: ThemeMode) {
        repository.setThemeMode(mode)
        _uiState.update { it.copy(themeMode = mode) }
    }

    fun setSafeMode(enabled: Boolean) {
        repository.setSafeMode(enabled)
        _uiState.update { it.copy(safeMode = enabled) }
    }

    fun clearCrashReport() {
        runCatching { crashLogFile().delete() }
        _uiState.update { it.copy(crashReport = null) }
    }

    fun forgetFolder() {
        scanJob?.cancel()
        repository.forgetTree()
        _uiState.update {
            it.copy(
                videos = emptyList(),
                treeName = null,
                treeUri = null,
                folderPath = null,
                hasAccess = false,
                isScanning = false,
                scanDone = 0,
                scanTotal = 0,
            )
        }
    }

    fun consumeMessage() {
        _uiState.update { it.copy(message = null) }
    }

    fun postMessage(text: String) {
        _uiState.update { it.copy(message = text) }
    }

    private fun crashLogFile(): File = File(getApplication<Application>().filesDir, "last-crash.txt")

    private fun readCrashReport(): String? = runCatching {
        val file = crashLogFile()
        if (!file.exists()) return@runCatching null
        file.readText().takeIf { it.isNotBlank() }
    }.getOrNull()
}

private fun List<Video>.sortedByNewest(): List<Video> = sortedWith(
    compareByDescending<Video> { it.lastModified }
        .thenByDescending { it.addedAt }
        .thenBy { it.title.lowercase() },
)
