package com.lalatube.offline.data

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaCodecList
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.documentfile.provider.DocumentFile
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import kotlin.math.max
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class ScanResult(
    val videos: List<Video>,
    val treeName: String,
)

/** Folder/izin tidak bisa dipakai. Ditampilkan sebagai "akses hilang" di UI. */
class FolderAccessException(message: String) : Exception(message)

/**
 * Menyimpan katalog video di penyimpanan internal aplikasi. File asli tidak pernah
 * disalin dari flashdisk; aplikasi hanya menyimpan URI, judul, durasi, dan thumbnail.
 */
class LibraryRepository(private val context: Context) {

    companion object {
        private const val PREFS = "lalatube_prefs"
        private const val KEY_TREE_URI = "tree_uri"
        private const val KEY_TREE_NAME = "tree_name"
        private const val KEY_FOLDER_PATH = "folder_path"
        private const val KEY_MEDIA_STORE = "media_store_mode"
        private const val KEY_THEME = "theme_mode"
        private const val KEY_SAFE_MODE = "safe_mode"
        private const val THUMB_WIDTH = 640
        private const val THUMB_HEIGHT = 360
        private const val AVATAR_SIZE = 160
        private const val MAX_DEPTH = 8
        private const val MAX_FILES = 5_000

        /** Nama file gambar profil selain nama folder kanal. */
        private val AVATAR_ALIASES = listOf(
            "avatar", "kanal", "channel", "logo", "profil", "profile", "channel-avatar",
        )

        val VIDEO_EXTENSIONS = setOf(
            "mp4", "m4v", "mkv", "webm", "mov", "avi", "3gp", "ts", "m2ts",
            "mpg", "mpeg", "flv", "wmv", "ogv", "divx", "vob",
        )
        private val IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp")
    }

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val libraryFile = File(context.filesDir, "library.json")
    private val thumbDir = File(context.cacheDir, "thumbs").apply { mkdirs() }
    private val avatarDir = File(context.cacheDir, "avatars").apply { mkdirs() }
    private val writeMutex = Mutex()

    /** Daftar codec sistem, dibangun sekali lalu dipakai ulang saat memindai. */
    private val codecList by lazy {
        runCatching { MediaCodecList(MediaCodecList.REGULAR_CODECS) }.getOrNull()
    }
    private val codecInfoByName by lazy {
        codecList?.codecInfos
            ?.associateBy { it.name.lowercase() }
            .orEmpty()
    }

    var treeUri: Uri? = prefs.getString(KEY_TREE_URI, null)?.let(Uri::parse)
        private set

    var treeName: String? = prefs.getString(KEY_TREE_NAME, null)
        private set

    /**
     * Folder yang dipilih lewat browser bawaan aplikasi. Kalau ini terisi, pemindaian
     * memakai akses file langsung dan tidak bergantung pada DocumentsUI/SAF.
     */
    var folderPath: String? = prefs.getString(KEY_FOLDER_PATH, null)
        private set

    /**
     * Mode cadangan: katalog dibangun dari MediaStore, bukan dari folder tertentu.
     * Berguna kalau head unit hanya memberi izin media (bukan akses semua file),
     * sehingga daftar isi direktori ditolak tetapi file video tetap bisa dibaca.
     */
    var mediaStoreMode: Boolean = prefs.getBoolean(KEY_MEDIA_STORE, false)
        private set

    fun setMediaStoreMode(enabled: Boolean) {
        mediaStoreMode = enabled
        prefs.edit().putBoolean(KEY_MEDIA_STORE, enabled).apply()
    }

    var themeMode: ThemeMode = runCatching {
        ThemeMode.valueOf(prefs.getString(KEY_THEME, null) ?: ThemeMode.SYSTEM.name)
    }.getOrDefault(ThemeMode.SYSTEM)
        private set

    /**
     * Mode aman melewati pembuatan thumbnail dari frame video. Dipakai kalau head unit
     * menutup aplikasi sendiri saat memindai (decoder video bermasalah / RAM kecil).
     */
    var safeMode: Boolean = prefs.getBoolean(KEY_SAFE_MODE, false)
        private set

    fun setThemeMode(mode: ThemeMode) {
        themeMode = mode
        prefs.edit().putString(KEY_THEME, mode.name).apply()
    }

    fun setSafeMode(enabled: Boolean) {
        safeMode = enabled
        prefs.edit().putBoolean(KEY_SAFE_MODE, enabled).apply()
    }

    fun hasPersistedPermission(uri: Uri): Boolean {
        val target = uri.toString()
        return context.contentResolver.persistedUriPermissions.any {
            it.isReadPermission && it.uri.toString() == target
        }
    }

    /**
     * Mengambil izin baca permanen untuk folder. Beberapa DocumentsUI bawaan head unit
     * menolak flag tertentu, jadi dicoba beberapa kombinasi dan hasilnya dilaporkan.
     * Kalau gagal, izin sementara tetap berlaku sampai aplikasi ditutup sehingga
     * pemindaian masih bisa dilakukan pada sesi ini.
     */
    fun takePersistedPermission(uri: Uri): Boolean {
        val resolver = context.contentResolver
        val read = Intent.FLAG_GRANT_READ_URI_PERMISSION
        val readWrite = read or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        if (runCatching { resolver.takePersistableUriPermission(uri, read) }.isSuccess) {
            return true
        }
        if (runCatching { resolver.takePersistableUriPermission(uri, readWrite) }.isSuccess) {
            return true
        }
        return false
    }

    fun storeTree(uri: Uri, name: String) {
        treeUri = uri
        treeName = name
        folderPath = null
        prefs.edit()
            .putString(KEY_TREE_URI, uri.toString())
            .putString(KEY_TREE_NAME, name)
            .remove(KEY_FOLDER_PATH)
            .apply()
    }

    /** Menyimpan folder hasil browser bawaan. Izin SAF lama dilepas. */
    fun storeFolderPath(path: String, name: String) {
        releaseTreePermission()
        folderPath = path
        treeUri = null
        treeName = name
        prefs.edit()
            .putString(KEY_FOLDER_PATH, path)
            .putString(KEY_TREE_NAME, name)
            .remove(KEY_TREE_URI)
            .apply()
    }

    /** Hapus folder terpilih tetapi pertahankan mode MediaStore. */
    fun forgetTreeKeepMode() {
        releaseTreePermission()
        folderPath = null
        treeUri = null
        prefs.edit()
            .remove(KEY_TREE_URI)
            .remove(KEY_FOLDER_PATH)
            .apply()
    }

    private fun releaseTreePermission() {
        treeUri?.let { uri ->
            runCatching {
                context.contentResolver.releasePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
        }
    }

    /** Apakah sudah ada folder yang bisa dipindai, dari sumber mana pun. */
    fun hasFolder(): Boolean = mediaStoreMode || folderPath != null || treeUri != null

    suspend fun displayNameFor(uri: Uri): String = withContext(Dispatchers.IO) {
        runCatching { DocumentFile.fromTreeUri(context, uri)?.name }.getOrNull()
            ?.takeIf { it.isNotBlank() }
            ?: "Folder video"
    }

    fun forgetTree() {
        releaseTreePermission()
        treeUri = null
        treeName = null
        folderPath = null
        mediaStoreMode = false
        prefs.edit()
            .remove(KEY_TREE_URI)
            .remove(KEY_TREE_NAME)
            .remove(KEY_FOLDER_PATH)
            .remove(KEY_MEDIA_STORE)
            .apply()
    }

    suspend fun loadSavedVideos(): List<Video> = withContext(Dispatchers.IO) {
        readLibrary().values.toList()
    }

    suspend fun saveVideos(videos: List<Video>) = withContext(Dispatchers.IO) {
        writeVideos(videos)
    }

    /**
     * Memindai folder yang dipilih dalam dua tahap:
     *
     * 1. Daftar file dikumpulkan dan langsung disimpan/ditampilkan. Tahap ini tidak
     *    menyentuh decoder video sehingga aman dan cepat.
     * 2. Metadata dan thumbnail dilengkapi satu per satu. Setiap file dibungkus
     *    try/catch Throwable supaya satu video bermasalah (atau kehabisan memori)
     *    tidak menutup aplikasi. Hasilnya disimpan bertahap.
     *
     * Metadata yang tidak berubah dipakai ulang supaya pemindaian berikutnya cepat.
     */
    suspend fun scan(
        onProgress: (done: Int, total: Int) -> Unit,
        onPartial: (List<Video>) -> Unit = {},
    ): ScanResult = withContext(Dispatchers.IO) {
        val path = folderPath
        val old = readLibrary()
        val found = mutableListOf<FoundFile>()
        var rootName = "Video Offline"

        if (mediaStoreMode) {
            // Cadangan: MediaStore hanya butuh izin media, tidak butuh akses folder.
            found.addAll(collectFromMediaStore())
            rootName = "Flashdisk"
        } else if (path != null) {
            // Mode browser bawaan: akses file langsung, tanpa SAF/DocumentsUI.
            val directory = File(path)
            if (!directory.isDirectory) {
                throw FolderAccessException(
                    "Folder tidak ditemukan. Colokkan flashdisk lalu pilih foldernya lagi.",
                )
            }
            // Bedakan "folder kosong" dari "folder gagal dibaca" supaya katalog lama
            // tidak terhapus hanya karena flashdisk sedang tidak siap.
            val children = runCatching { directory.listFiles() }.getOrNull()
                ?: throw FolderAccessException(
                    "Folder tidak dapat dibaca. Beri izin akses file, lalu pilih ulang foldernya.",
                )
            collectFromDirectory(directory, "", found, 0, children)
            rootName = directory.name.takeIf { it.isNotBlank() } ?: "Folder video"
        } else {
            val uri = treeUri ?: throw IllegalStateException("Folder video belum dipilih.")
            val root = openTree(uri)
                ?: throw FolderAccessException(
                    "Folder tidak dapat dibuka. Pilih ulang folder di flashdisk.",
                )
            val readable = runCatching { root.exists() && root.canRead() }.getOrDefault(false)
            if (!readable) {
                throw FolderAccessException(
                    "Folder tidak dapat dibaca. Colokkan flashdisk lalu pilih foldernya lagi.",
                )
            }
            val rootChildren = runCatching { root.listFiles() }.getOrNull()
                ?: throw FolderAccessException(
                    "Folder tidak dapat dibaca. Colokkan flashdisk lalu pilih foldernya lagi.",
                )
            collectVideos(root, "", found, 0, rootChildren)
            rootName = root.name?.takeIf { it.isNotBlank() } ?: "Folder video"
        }
        onProgress(0, found.size)

        // Satu gambar profil per kanal cukup; disalin sekali lalu dipakai semua
        // video di kanal tersebut.
        val avatarCache = HashMap<String, String?>()
        fun avatarFor(channel: String): String? {
            if (avatarCache.containsKey(channel)) return avatarCache[channel]
            val source = found.firstOrNull {
                it.channel == channel && (it.avatarFile != null || it.avatarUri != null)
            }
            val saved = source?.let { saveChannelAvatar(it) }
            avatarCache[channel] = saved
            return saved
        }

        val result = ArrayList<Video>(found.size)
        val pending = ArrayList<Int>()
        found.forEachIndexed { index, item ->
            val previous = old[item.uri.toString()]
            val cacheValid = previous != null &&
                previous.lastModified == item.lastModified &&
                previous.sizeBytes == item.size &&
                previous.codecMime.isNotBlank() &&
                (previous.thumbnailPath == null || File(previous.thumbnailPath).exists())

            val video = if (previous != null && cacheValid) {
                previous.copy(
                    title = item.title,
                    channel = item.channel,
                    folder = item.folder,
                    addedAt = item.lastModified.takeIf { it > 0L } ?: previous.addedAt,
                )
            } else {
                baseVideo(item, previous)
            }
            result.add(video.copy(channelAvatarPath = avatarFor(item.channel)))
            if (!cacheValid) pending.add(index)
        }

        // Simpan dan tampilkan daftar lebih dulu. Kalau tahap metadata gagal di
        // tengah jalan, video tetap muncul dan tidak hilang dari layar.
        writeVideos(result)
        onPartial(result)

        var sinceSave = 0
        pending.forEachIndexed { position, index ->
            coroutineContext.ensureActive()
            val item = found[index]
            val previous = old[item.uri.toString()]
            result[index] = enrichVideo(item, previous, result[index])
            sinceSave++
            if (sinceSave >= 20) {
                writeVideos(result)
                onPartial(result)
                sinceSave = 0
            }
            onProgress(position + 1, pending.size)
        }
        writeVideos(result)

        val keep = result.mapNotNull { it.thumbnailPath }.toSet()
        runCatching {
            thumbDir.listFiles()?.forEach { file ->
                if (file.isFile && file.absolutePath !in keep) file.delete()
            }
        }

        ScanResult(
            videos = result,
            treeName = rootName,
        )
    }

    /**
     * Membaca semua video yang dikenal MediaStore. Dipakai sebagai mode cadangan
     * ketika aplikasi hanya diberi izin media, bukan akses folder penuh.
     * Subfolder pertama dianggap nama kanal, sama seperti mode folder.
     */
    private fun collectFromMediaStore(): List<FoundFile> {
        val result = mutableListOf<FoundFile>()
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATE_MODIFIED,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
            MediaStore.Video.Media.RELATIVE_PATH,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
        )
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }
        val cursor = runCatching {
            context.contentResolver.query(
                collection,
                projection,
                null,
                null,
                "${MediaStore.Video.Media.DATE_MODIFIED} DESC",
            )
        }.getOrNull() ?: return result

        cursor.use { data ->
            val idColumn = data.getColumnIndex(MediaStore.Video.Media._ID)
            val nameColumn = data.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME)
            val sizeColumn = data.getColumnIndex(MediaStore.Video.Media.SIZE)
            val modifiedColumn = data.getColumnIndex(MediaStore.Video.Media.DATE_MODIFIED)
            val pathColumn = data.getColumnIndex(MediaStore.Video.Media.RELATIVE_PATH)
            val bucketColumn = data.getColumnIndex(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
            if (idColumn < 0 || nameColumn < 0) return result

            while (data.moveToNext() && result.size < MAX_FILES) {
                val id = data.getLong(idColumn)
                val name = data.getString(nameColumn) ?: continue
                val relative = if (pathColumn >= 0) data.getString(pathColumn).orEmpty() else ""
                val bucket = if (bucketColumn >= 0) {
                    data.getString(bucketColumn)?.takeIf { it.isNotBlank() }
                } else {
                    null
                }
                val channel = bucket ?: "Video Offline"
                val uri = ContentUris.withAppendedId(collection, id)
                val modifiedSeconds = if (modifiedColumn >= 0) {
                    data.getLong(modifiedColumn)
                } else {
                    0L
                }
                result.add(
                    FoundFile(
                        uri = uri,
                        file = null,
                        title = friendlyTitle(name),
                        channel = channel,
                        folder = relative.trimEnd('/').ifBlank { "Video Offline" },
                        lastModified = modifiedSeconds * 1_000L,
                        size = if (sizeColumn >= 0) data.getLong(sizeColumn) else 0L,
                        poster = null,
                        posterFile = null,
                    ),
                )
            }
        }
        return result
    }

    private fun openTree(uri: Uri): DocumentFile? =
        runCatching { DocumentFile.fromTreeUri(context, uri) }.getOrNull()

    private fun collectVideos(
        directory: DocumentFile,
        relativePath: String,
        output: MutableList<FoundFile>,
        depth: Int,
        prelisted: Array<DocumentFile>? = null,
        avatarUri: DocumentFile? = null,
    ) {
        if (depth > MAX_DEPTH || output.size >= MAX_FILES) return
        val children = prelisted
            ?: runCatching { directory.listFiles() }.getOrElse { emptyArray() }
        if (children.isEmpty()) return

        val images = children
            .asSequence()
            .filter {
                runCatching { it.isFile }.getOrDefault(false) &&
                    it.extensionLower() in IMAGE_EXTENSIONS
            }
            .associateBy { it.baseNameLower() }

        // Folder kanal boleh berisi satu gambar profil dengan nama sama seperti
        // foldernya (mis. `Babatigara/Babatigara.jpg`) atau nama umum di bawah.
        val folderAvatar = if (depth == 1 && relativePath.isNotBlank()) {
            findAvatar(images, relativePath.substringBefore('/'))
        } else {
            null
        }

        for (child in children) {
            if (output.size >= MAX_FILES) return
            val name = runCatching { child.name }.getOrNull() ?: continue
            if (name.startsWith(".")) continue

            val isDirectory = runCatching { child.isDirectory }.getOrDefault(false)
            if (isDirectory) {
                val childPath = if (relativePath.isBlank()) name else "$relativePath/$name"
                collectVideos(
                    directory = child,
                    relativePath = childPath,
                    output = output,
                    depth = depth + 1,
                    avatarUri = folderAvatar ?: avatarUri,
                )
                continue
            }

            val isFile = runCatching { child.isFile }.getOrDefault(false)
            if (!isFile || !child.isVideo()) continue
            val poster = images[child.baseNameLower()]?.uri
            val channel = when {
                relativePath.isBlank() -> "Video Offline"
                else -> relativePath.substringBefore('/')
            }
            val avatar = folderAvatar ?: avatarUri
            output.add(
                FoundFile(
                    uri = child.uri,
                    file = null,
                    title = friendlyTitle(name),
                    channel = channel,
                    folder = relativePath.ifBlank { "Video Offline" },
                    lastModified = runCatching { child.lastModified() }.getOrDefault(0L),
                    size = runCatching { child.length() }.getOrDefault(0L),
                    poster = poster,
                    posterFile = null,
                    avatarUri = avatar?.uri,
                    avatarStamp = avatar?.let {
                        runCatching { it.lastModified() }.getOrDefault(0L)
                    } ?: 0L,
                ),
            )
        }
    }

    /**
     * Varian [collectVideos] untuk folder yang diakses langsung lewat [File].
     * URI video dibentuk sebagai `file://...` sehingga ExoPlayer dan thumbnail
     * bisa memakainya tanpa ContentResolver.
     */
    private fun collectFromDirectory(
        directory: File,
        relativePath: String,
        output: MutableList<FoundFile>,
        depth: Int,
        prelisted: Array<File>? = null,
        avatarFile: File? = null,
    ) {
        if (depth > MAX_DEPTH || output.size >= MAX_FILES) return
        val children = prelisted
            ?: runCatching { directory.listFiles() }.getOrNull()
            ?: return
        if (children.isEmpty()) return

        val posters = children
            .asSequence()
            .filter { !it.name.startsWith(".") }
            .filter { it.isFile && it.extensionLower() in IMAGE_EXTENSIONS }
            .associateBy { it.baseNameLower() }

        // Foto profil kanal: nama file sama dengan nama folder, misalnya
        // `Babatigara/Babatigara.jpg`. Nama umum seperti `avatar.jpg` juga diterima.
        val folderAvatar = if (depth == 1 && relativePath.isNotBlank()) {
            findAvatar(posters, relativePath.substringBefore('/'))
        } else {
            null
        }

        for (child in children) {
            if (output.size >= MAX_FILES) return
            val name = child.name
            if (StorageAccess.isIgnored(name)) continue

            if (runCatching { child.isDirectory }.getOrDefault(false)) {
                val childPath = if (relativePath.isBlank()) name else "$relativePath/$name"
                collectFromDirectory(
                    directory = child,
                    relativePath = childPath,
                    output = output,
                    depth = depth + 1,
                    avatarFile = folderAvatar ?: avatarFile,
                )
                continue
            }
            if (!runCatching { child.isFile }.getOrDefault(false)) continue
            if (name.substringAfterLast('.', "").lowercase() !in VIDEO_EXTENSIONS) continue

            val poster = posters[child.baseNameLower()]
            val channel = if (relativePath.isBlank()) {
                "Video Offline"
            } else {
                relativePath.substringBefore('/')
            }
            val avatar = folderAvatar ?: avatarFile
            output.add(
                FoundFile(
                    uri = Uri.fromFile(child),
                    file = child,
                    title = friendlyTitle(name),
                    channel = channel,
                    folder = relativePath.ifBlank { "Video Offline" },
                    lastModified = runCatching { child.lastModified() }.getOrDefault(0L),
                    size = runCatching { child.length() }.getOrDefault(0L),
                    poster = null,
                    posterFile = poster,
                    avatarFile = avatar,
                    avatarStamp = avatar?.let {
                        runCatching { it.lastModified() }.getOrDefault(0L)
                    } ?: 0L,
                ),
            )
        }
    }

    /** Mencari gambar profil kanal di dalam daftar gambar satu folder. */
    private fun <T> findAvatar(images: Map<String, T>, channel: String): T? {
        val direct = images[channel.trim().lowercase()]
        if (direct != null) return direct
        for (alias in AVATAR_ALIASES) {
            images[alias]?.let { return it }
        }
        return null
    }

    /** Video dasar tanpa menyentuh decoder: judul, kanal, dan data lama. */
    private fun baseVideo(item: FoundFile, previous: Video?): Video {
        val existingThumb = previous?.thumbnailPath?.takeIf { File(it).exists() }
        return Video(
            uri = item.uri.toString(),
            title = item.title,
            channel = item.channel,
            folder = item.folder,
            durationMs = previous?.durationMs ?: 0L,
            sizeBytes = item.size,
            lastModified = item.lastModified,
            width = previous?.width ?: 0,
            height = previous?.height ?: 0,
            codecMime = previous?.codecMime.orEmpty(),
            softwareOnly = previous?.softwareOnly ?: false,
            thumbnailPath = existingThumb,
            positionMs = previous?.positionMs ?: 0L,
            lastPlayedAt = previous?.lastPlayedAt ?: 0L,
            completed = previous?.completed ?: false,
            favorite = previous?.favorite ?: false,
            addedAt = item.lastModified.takeIf { it > 0L } ?: (previous?.addedAt ?: 0L),
        )
    }

    private fun enrichVideo(item: FoundFile, previous: Video?, base: Video): Video {
        val metadata = readMetadata(item)
        val thumbnail = if (safeMode) {
            base.thumbnailPath
        } else {
            createThumbnail(item, metadata.durationMs) ?: base.thumbnailPath
        }
        return base.copy(
            durationMs = metadata.durationMs.takeIf { it > 0L } ?: base.durationMs,
            width = metadata.width.takeIf { it > 0 } ?: base.width,
            height = metadata.height.takeIf { it > 0 } ?: base.height,
            codecMime = metadata.codecMime.ifBlank { base.codecMime },
            softwareOnly = metadata.softwareOnly,
            thumbnailPath = thumbnail,
            positionMs = previous?.positionMs ?: base.positionMs,
            lastPlayedAt = previous?.lastPlayedAt ?: base.lastPlayedAt,
            completed = previous?.completed ?: base.completed,
            favorite = previous?.favorite ?: base.favorite,
            addedAt = item.lastModified.takeIf { it > 0L } ?: base.addedAt,
        )
    }

    private data class VideoMetadata(
        val durationMs: Long,
        val width: Int,
        val height: Int,
        val codecMime: String = "",
        val softwareOnly: Boolean = false,
    )

    /**
     * Membaca durasi/resolusi lewat MediaExtractor (tanpa decode frame). Jauh lebih
     * ringan dan stabil dibanding MediaMetadataRetriever di head unit RAM kecil.
     * File descriptor dipakai supaya provider tidak perlu membuka ulang.
     */
    private fun readMetadata(item: FoundFile): VideoMetadata {
        var durationMs = 0L
        var width = 0
        var height = 0
        var codecMime = ""
        runCatching {
            openDescriptor(item)?.use { pfd ->
                val extractor = MediaExtractor()
                try {
                    extractor.setDataSource(pfd.fileDescriptor)
                    for (index in 0 until extractor.trackCount) {
                        val format = runCatching { extractor.getTrackFormat(index) }.getOrNull()
                            ?: continue
                        val mime = runCatching { format.getString(MediaFormat.KEY_MIME) }
                            .getOrNull() ?: continue
                        if (!mime.startsWith("video/")) continue
                        codecMime = mime
                        if (format.containsKey(MediaFormat.KEY_DURATION)) {
                            val value = runCatching {
                                format.getLong(MediaFormat.KEY_DURATION)
                            }.getOrDefault(0L)
                            if (value > 0L) durationMs = value / 1_000L
                        }
                        width = runCatching { format.getInteger(MediaFormat.KEY_WIDTH) }
                            .getOrDefault(0)
                        height = runCatching { format.getInteger(MediaFormat.KEY_HEIGHT) }
                            .getOrDefault(0)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                            format.containsKey(MediaFormat.KEY_ROTATION)
                        ) {
                            val rotation = runCatching {
                                format.getInteger(MediaFormat.KEY_ROTATION)
                            }.getOrDefault(0)
                            if (rotation == 90 || rotation == 270) {
                                val temp = width
                                width = height
                                height = temp
                            }
                        }
                        break
                    }
                } finally {
                    runCatching { extractor.release() }
                }
            }
        }
        val softwareOnly = codecMime.isNotBlank() &&
            !hasHardwareDecoder(codecMime, width, height)
        return VideoMetadata(durationMs, width, height, codecMime, softwareOnly)
    }

    /**
     * Apakah head unit punya decoder yang dipercepat hardware untuk codec ini.
     * Video AV1/VP9 yang besar sering hanya punya decoder software (c2.android.*),
     * dan itu penyebab utama pemutaran patah-patah di SoC hemat daya.
     */
    private fun hasHardwareDecoder(mime: String, width: Int, height: Int): Boolean = runCatching {
        val list = codecList ?: return@runCatching false
        if (width <= 0 || height <= 0) return@runCatching false
        val format = MediaFormat.createVideoFormat(mime, width, height)
        val decoderName = list.findDecoderForFormat(format) ?: return@runCatching false
        val info = codecInfoByName[decoderName.lowercase()] ?: return@runCatching false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            info.isHardwareAccelerated
        } else {
            !info.name.startsWith("OMX.google.", ignoreCase = true) &&
                !info.name.startsWith("c2.android.", ignoreCase = true)
        }
    }.getOrDefault(false)

    private fun createThumbnail(item: FoundFile, durationMs: Long): String? {
        val filename = "thumb_${shortHash(item.uri.toString())}_${item.lastModified}.jpg"
        val output = File(thumbDir, filename)
        if (output.exists() && output.length() > 0L) return output.absolutePath

        var bitmap: Bitmap? = null
        try {
            bitmap = item.posterFile?.let { decodePosterFile(it) }
                ?: item.poster?.let { decodePoster(it) }
                ?: decodeFrame(item, durationMs)
            if (bitmap == null) return null
            val cropped = runCatching { centerCrop(bitmap, THUMB_WIDTH, THUMB_HEIGHT) }
                .getOrNull() ?: return null
            return try {
                FileOutputStream(output).use { stream ->
                    cropped.compress(Bitmap.CompressFormat.JPEG, 86, stream)
                }
                output.absolutePath
            } catch (_: Throwable) {
                output.delete()
                null
            } finally {
                if (cropped !== bitmap) runCatching { cropped.recycle() }
            }
        } catch (_: Throwable) {
            // OutOfMemoryError / kegagalan decoder tidak boleh menutup aplikasi.
            output.delete()
            return null
        } finally {
            runCatching { bitmap?.takeIf { !it.isRecycled }?.recycle() }
        }
    }

    /**
     * Menyalin gambar profil kanal ke cache aplikasi dalam ukuran kecil, supaya
     * tampilannya ringan dan tetap bisa dibaca baik lewat akses file langsung
     * maupun SAF. Hasilnya `null` kalau gambar tidak ada atau gagal dibaca.
     */
    private fun saveChannelAvatar(item: FoundFile): String? {
        if (item.avatarFile == null && item.avatarUri == null) return null
        val filename = "avatar_${shortHash(item.channel)}_${item.avatarStamp}.jpg"
        val output = File(avatarDir, filename)
        if (output.exists() && output.length() > 0L) return output.absolutePath

        var bitmap: Bitmap? = null
        try {
            bitmap = item.avatarFile?.let { decodePosterFile(it) }
                ?: item.avatarUri?.let { decodePoster(it) }
                ?: return null
            val cropped = runCatching { centerCrop(bitmap, AVATAR_SIZE, AVATAR_SIZE) }
                .getOrNull() ?: return null
            return try {
                FileOutputStream(output).use { stream ->
                    cropped.compress(Bitmap.CompressFormat.JPEG, 88, stream)
                }
                // Versi lama dengan cap waktu berbeda tidak dipakai lagi.
                avatarDir.listFiles()?.forEach { file ->
                    if (file.isFile && file.name.startsWith("avatar_${shortHash(item.channel)}_") &&
                        file.absolutePath != output.absolutePath
                    ) {
                        file.delete()
                    }
                }
                output.absolutePath
            } catch (_: Throwable) {
                output.delete()
                null
            } finally {
                if (cropped !== bitmap) runCatching { cropped.recycle() }
            }
        } catch (_: Throwable) {
            output.delete()
            return null
        } finally {
            runCatching { bitmap?.takeIf { !it.isRecycled }?.recycle() }
        }
    }

    private fun decodeFrame(item: FoundFile, durationMs: Long): Bitmap? = runCatching {
        openDescriptor(item)?.use { pfd ->
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(pfd.fileDescriptor)
                val timeUs = if (durationMs > 0L) {
                    (durationMs * 250L).coerceIn(0L, 6_000_000L)
                } else {
                    1_000_000L
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                    // Minta frame yang sudah diperkecil: menghindari bitmap 4K di RAM.
                    retriever.getScaledFrameAtTime(
                        timeUs,
                        MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                        THUMB_WIDTH,
                        THUMB_HEIGHT,
                    )
                } else {
                    retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                }
            } finally {
                runCatching { retriever.release() }
            }
        }
    }.getOrNull()

    /**
     * Membuka file untuk MediaExtractor/MediaMetadataRetriever. Untuk video dari
     * browser bawaan dipakai [FileInputStream] langsung; untuk SAF dipakai
     * ContentResolver. Keduanya sama-sama memberi FileDescriptor.
     */
    private fun openDescriptor(item: FoundFile): android.os.ParcelFileDescriptor? {
        val file = item.file
        if (file != null) {
            return runCatching {
                android.os.ParcelFileDescriptor.open(
                    file,
                    android.os.ParcelFileDescriptor.MODE_READ_ONLY,
                )
            }.getOrNull()
        }
        return runCatching {
            context.contentResolver.openFileDescriptor(item.uri, "r")
        }.getOrNull()
    }

    private fun decodePosterFile(file: File): Bitmap? = runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        var sample = 1
        while (bounds.outWidth / sample > THUMB_WIDTH * 2 ||
            bounds.outHeight / sample > THUMB_HEIGHT * 2
        ) {
            sample *= 2
        }
        val options = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        BitmapFactory.decodeFile(file.absolutePath, options)
    }.getOrNull()

    private fun File.extensionLower(): String =
        name.substringAfterLast('.', "").lowercase()

    private fun File.baseNameLower(): String =
        name.substringBeforeLast('.', name).lowercase()

    private fun decodePoster(uri: Uri): Bitmap? {
        val resolver = context.contentResolver
        return runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            var sample = 1
            while (bounds.outWidth / sample > THUMB_WIDTH * 2 ||
                bounds.outHeight / sample > THUMB_HEIGHT * 2
            ) {
                sample *= 2
            }
            val options = BitmapFactory.Options().apply {
                inSampleSize = sample
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
        }.getOrNull()
    }

    private fun centerCrop(source: Bitmap, targetWidth: Int, targetHeight: Int): Bitmap {
        if (source.width <= 0 || source.height <= 0) return source
        val scale = max(
            targetWidth / source.width.toFloat(),
            targetHeight / source.height.toFloat(),
        )
        val scaledWidth = (source.width * scale).toInt().coerceAtLeast(1)
        val scaledHeight = (source.height * scale).toInt().coerceAtLeast(1)
        val scaled = if (scaledWidth == source.width && scaledHeight == source.height) {
            source
        } else {
            Bitmap.createScaledBitmap(source, scaledWidth, scaledHeight, true)
        }
        val left = ((scaledWidth - targetWidth) / 2).coerceAtLeast(0)
        val top = ((scaledHeight - targetHeight) / 2).coerceAtLeast(0)
        val cropped = Bitmap.createBitmap(
            scaled,
            left.coerceAtMost((scaledWidth - targetWidth).coerceAtLeast(0)),
            top.coerceAtMost((scaledHeight - targetHeight).coerceAtLeast(0)),
            targetWidth.coerceAtMost(scaledWidth),
            targetHeight.coerceAtMost(scaledHeight),
        )
        if (scaled !== cropped && scaled !== source) scaled.recycle()
        if (source !== cropped && source !== scaled) source.recycle()
        return cropped
    }

    private fun readLibrary(): LinkedHashMap<String, Video> {
        val result = LinkedHashMap<String, Video>()
        if (!libraryFile.exists()) return result
        return runCatching {
            val root = JSONObject(libraryFile.readText())
            val array = root.optJSONArray("videos") ?: JSONArray()
            for (index in 0 until array.length()) {
                val json = array.optJSONObject(index) ?: continue
                val uri = json.optString("uri").takeIf { it.isNotBlank() } ?: continue
                result[uri] = Video(
                    uri = uri,
                    title = json.optString("title", "Video"),
                    channel = json.optString("channel", "Video Offline"),
                    folder = json.optString("folder", "Video Offline"),
                    durationMs = json.optLong("duration", 0L),
                    sizeBytes = json.optLong("size", 0L),
                    lastModified = json.optLong("lastModified", 0L),
                    width = json.optInt("width", 0),
                    height = json.optInt("height", 0),
                    codecMime = json.optString("codec").takeIf { it.isNotBlank() }.orEmpty(),
                    softwareOnly = json.optBoolean("softwareOnly", false),
                    thumbnailPath = json.optString("thumbnail").takeIf { it.isNotBlank() },
                    channelAvatarPath = json.optString("channelAvatar").takeIf { it.isNotBlank() },
                    positionMs = json.optLong("position", 0L),
                    lastPlayedAt = json.optLong("lastPlayedAt", 0L),
                    completed = json.optBoolean("completed", false),
                    favorite = json.optBoolean("favorite", false),
                    addedAt = json.optLong("addedAt", 0L),
                )
            }
            result
        }.getOrDefault(result)
    }

    private suspend fun writeVideos(videos: List<Video>) {
        writeMutex.withLock {
            val array = JSONArray()
            videos.forEach { video ->
                array.put(
                    JSONObject().apply {
                        put("uri", video.uri)
                        put("title", video.title)
                        put("channel", video.channel)
                        put("folder", video.folder)
                        put("duration", video.durationMs)
                        put("size", video.sizeBytes)
                        put("lastModified", video.lastModified)
                        put("width", video.width)
                        put("height", video.height)
                        put("codec", video.codecMime)
                        put("softwareOnly", video.softwareOnly)
                        put("thumbnail", video.thumbnailPath ?: "")
                        put("channelAvatar", video.channelAvatarPath ?: "")
                        put("position", video.positionMs)
                        put("lastPlayedAt", video.lastPlayedAt)
                        put("completed", video.completed)
                        put("favorite", video.favorite)
                        put("addedAt", video.addedAt)
                    },
                )
            }
            val root = JSONObject().apply {
                put("version", 1)
                put("updatedAt", System.currentTimeMillis())
                put("videos", array)
            }
            val temp = File(libraryFile.parentFile, "${libraryFile.name}.tmp")
            temp.writeText(root.toString())
            if (!temp.renameTo(libraryFile)) {
                libraryFile.writeText(root.toString())
                temp.delete()
            }
        }
    }

    private fun DocumentFile.isVideo(): Boolean {
        val mime = runCatching { type }.getOrNull()
        if (mime != null && mime.startsWith("video/")) return true
        return extensionLower() in VIDEO_EXTENSIONS
    }

    private fun DocumentFile.extensionLower(): String =
        runCatching { name }.getOrNull()
            ?.substringAfterLast('.', "")
            ?.lowercase()
            .orEmpty()

    private fun DocumentFile.baseNameLower(): String {
        val value = runCatching { name }.getOrNull() ?: return ""
        return value.substringBeforeLast('.', value).lowercase()
    }

    private fun shortHash(value: String): String {
        val digest = MessageDigest.getInstance("SHA-1").digest(value.toByteArray())
        return digest.take(10).joinToString("") { "%02x".format(it) }
    }

    private data class FoundFile(
        val uri: Uri,
        /** Terisi kalau video ditemukan lewat akses file langsung (bukan SAF). */
        val file: File?,
        val title: String,
        val channel: String,
        val folder: String,
        val lastModified: Long,
        val size: Long,
        val poster: Uri?,
        val posterFile: File?,
        /** Gambar profil kanal (kalau ada), untuk mode akses file langsung. */
        val avatarFile: File? = null,
        /** Gambar profil kanal untuk mode SAF/DocumentFile. */
        val avatarUri: Uri? = null,
        /** Waktu ubah gambar profil, dipakai sebagai bagian nama cache. */
        val avatarStamp: Long = 0L,
    )
}

fun friendlyTitle(filename: String): String {
    var value = filename.substringBeforeLast('.', filename)
    value = value.replace(Regex("""\s*\[[A-Za-z0-9_-]{11}]\s*$"""), "")
    value = value.replace(Regex("""\s*\((?:[0-9]{3,4}p|HD|FHD|UHD)\)\s*$""", RegexOption.IGNORE_CASE), "")
    value = value.replace('_', ' ').trim()
    return value.ifBlank { "Video" }
}
