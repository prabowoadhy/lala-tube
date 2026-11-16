package com.lalatube.offline.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.storage.StorageManager
import android.provider.Settings
import java.io.File

/** Satu volume penyimpanan yang bisa dijelajahi (internal atau flashdisk). */
data class StorageVolumeInfo(
    val label: String,
    val path: String,
    val isRemovable: Boolean,
)

/** Satu subfolder di dalam browser folder. */
data class BrowserEntry(
    val name: String,
    val path: String,
    val videoCount: Int,
)

/**
 * Menjelajahi penyimpanan langsung lewat [File], tanpa DocumentsUI.
 *
 * Banyak head unit Android (terutama unit aftermarket) tidak menyertakan aplikasi
 * pemilih folder bawaan Android, sehingga `ACTION_OPEN_DOCUMENT_TREE` gagal dengan
 * ActivityNotFoundException. Dengan membaca folder langsung, aplikasi tetap bisa
 * dipakai asalkan izin "Akses semua file" atau izin media sudah diberikan.
 */
object StorageAccess {

    private val IGNORED_DIRS = setOf(
        "Android",
        "LOST.DIR",
        "System Volume Information",
        "\$RECYCLE.BIN",
        "lost+found",
    )

    /** Folder yang isinya tidak perlu ditampilkan ke pengguna. */
    fun isIgnored(name: String): Boolean =
        name.startsWith(".") || name in IGNORED_DIRS

    fun hasAllFilesAccess(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            runCatching { Environment.isExternalStorageManager() }.getOrDefault(false)
        } else {
            true
        }

    /** Izin baca media. Di Android 13+ memakai READ_MEDIA_VIDEO. */
    fun hasMediaPermission(context: Context): Boolean {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            android.Manifest.permission.READ_MEDIA_VIDEO
        } else {
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        }
        return runCatching {
            context.checkSelfPermission(permission) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
        }.getOrDefault(false)
    }

    /** Media permission yang harus diminta lewat dialog runtime. */
    fun mediaPermission(): String =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            android.Manifest.permission.READ_MEDIA_VIDEO
        } else {
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        }

    /** Cukup untuk membaca video di flashdisk? */
    fun canAccessStorage(context: Context): Boolean =
        hasAllFilesAccess() || hasMediaPermission(context)

    /**
     * Halaman pengaturan "Akses semua file". Dikembalikan null kalau head unit tidak
     * punya halaman tersebut, supaya pemanggil bisa memakai cadangan lain.
     */
    fun allFilesAccessIntent(context: Context): Intent? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null
        val perApp = Intent(
            Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
            Uri.parse("package:${context.packageName}"),
        )
        val generic = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
        val details = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:${context.packageName}"),
        )
        val manager = context.packageManager
        for (candidate in listOf(perApp, generic, details)) {
            if (runCatching { manager.resolveActivity(candidate, 0) }.getOrNull() != null) {
                return candidate
            }
        }
        return null
    }

    fun openAllFilesAccess(context: Context): Boolean {
        val intent = allFilesAccessIntent(context) ?: return false
        return runCatching {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            true
        }.getOrDefault(false)
    }

    /**
     * Daftar volume yang bisa dipilih. Volume yang dilaporkan [StorageManager]
     * digabung dengan hasil pemindaian `/storage` karena sebagian head unit tidak
     * mendaftarkan flashdisk ke StorageManager.
     */
    fun volumes(context: Context): List<StorageVolumeInfo> {
        val found = LinkedHashMap<String, StorageVolumeInfo>()

        val manager = context.getSystemService(Context.STORAGE_SERVICE) as? StorageManager
        if (manager != null) {
            val reported = runCatching { manager.storageVolumes }.getOrNull().orEmpty()
            for (volume in reported) {
                val directory = runCatching {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        volume.directory
                    } else {
                        @Suppress("DEPRECATION")
                        volume.javaClass.getMethod("getPath").invoke(volume) as? File
                    }
                }.getOrNull() ?: continue
                val path = directory.absolutePath
                val label = runCatching { volume.getDescription(context) }
                    .getOrNull()
                    ?.takeIf { it.isNotBlank() }
                    ?: directory.name
                found[path] = StorageVolumeInfo(
                    label = if (volume.isRemovable) "Flashdisk · $label" else label,
                    path = path,
                    isRemovable = volume.isRemovable,
                )
            }
        }

        for (file in scanStorageDir()) {
            val path = file.absolutePath
            if (found.containsKey(path)) continue
            found[path] = StorageVolumeInfo(
                label = rootLabel(file),
                path = path,
                isRemovable = !path.contains("/emulated/"),
            )
        }

        return found.values.sortedWith(
            compareByDescending<StorageVolumeInfo> { it.isRemovable }.thenBy { it.label },
        )
    }

    private fun rootLabel(file: File): String {
        val name = file.name
        return when {
            name == "0" -> "Penyimpanan internal"
            name.all { it.isDigit() } -> "Penyimpanan internal"
            // Volume lepas biasanya muncul sebagai UUID, mis. 1A2B-3C4D.
            name.length == 9 && name[4] == '-' -> "Flashdisk ($name)"
            else -> name
        }
    }

    private fun scanStorageDir(): List<File> {
        val result = mutableListOf<File>()
        val roots = listOf(
            File("/storage"),
            File("/mnt/media_rw"),
            File("/mnt/usb"),
            File("/mnt/usb_storage"),
            File("/mnt/extsd"),
        )
        for (root in roots) {
            val children = runCatching { root.listFiles() }.getOrNull() ?: continue
            for (child in children) {
                val isDirectory = runCatching { child.isDirectory }.getOrDefault(false)
                if (!isDirectory) continue
                when (child.name) {
                    "self", "emulated" -> {
                        // /storage/emulated/0 adalah penyimpanan internal sebenarnya.
                        val inner = runCatching { child.listFiles() }.getOrNull().orEmpty()
                        for (nested in inner) {
                            if (runCatching { nested.isDirectory }.getOrDefault(false)) {
                                result.add(nested)
                            }
                        }
                    }
                    else -> result.add(child)
                }
            }
        }
        return result.distinctBy { it.absolutePath }
    }

    /**
     * Daftar subfolder di [path]. Mengembalikan null kalau folder tidak bisa dibaca
     * sama sekali, supaya UI bisa membedakan "kosong" dari "tidak diizinkan".
     */
    fun listFolders(path: String): List<BrowserEntry>? {
        val directory = File(path)
        val children = runCatching { directory.listFiles() }.getOrNull() ?: return null
        return children
            .asSequence()
            .filter { !isIgnored(it.name) }
            .filter { runCatching { it.isDirectory }.getOrDefault(false) }
            .filter { runCatching { it.canRead() }.getOrDefault(false) }
            .map { folder ->
                BrowserEntry(
                    name = folder.name,
                    path = folder.absolutePath,
                    // Cukup untuk pratinjau. Hitungan penuh dilakukan saat memindai.
                    videoCount = countVideos(folder, maxDepth = 2, limit = 300),
                )
            }
            .filter { it.videoCount > 0 }
            .sortedBy { it.name.lowercase() }
            .toList()
    }

    /** Jumlah video di dalam [directory] beserta subfoldernya. */
    fun countVideos(directory: File, maxDepth: Int, limit: Int): Int {
        var count = 0
        fun walk(folder: File, depth: Int) {
            if (count >= limit || depth > maxDepth) return
            val children = runCatching { folder.listFiles() }.getOrNull() ?: return
            for (child in children) {
                if (count >= limit) return
                if (isIgnored(child.name)) continue
                val isDirectory = runCatching { child.isDirectory }.getOrDefault(false)
                if (isDirectory) {
                    walk(child, depth + 1)
                } else if (isVideoName(child.name)) {
                    count++
                }
            }
        }
        walk(directory, 0)
        return count
    }

    fun isVideoName(name: String): Boolean {
        val extension = name.substringAfterLast('.', "").lowercase()
        return extension.isNotEmpty() && extension in LibraryRepository.VIDEO_EXTENSIONS
    }

    /** Nama folder untuk ditampilkan, mis. "LalaTube". */
    fun displayName(path: String): String =
        File(path).name.takeIf { it.isNotBlank() } ?: "Folder video"
}
