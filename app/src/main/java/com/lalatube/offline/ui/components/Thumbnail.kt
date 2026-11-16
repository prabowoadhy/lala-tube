package com.lalatube.offline.ui.components

import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ThumbnailBitmapCache {
    // Batasi cache berdasarkan ukuran bitmap sebenarnya, bukan jumlah entri.
    // Sebelumnya 1 entri dihitung 1 sehingga ratusan thumbnail bisa memenuhi RAM.
    private val maxBytes = (Runtime.getRuntime().maxMemory() / 8L)
        .coerceIn(8L * 1024 * 1024, 48L * 1024 * 1024)
        .toInt()

    private val cache = object : LruCache<String, ImageBitmap>(maxBytes) {
        override fun sizeOf(key: String, value: ImageBitmap): Int =
            value.width * value.height * 4
    }

    fun peek(path: String?): ImageBitmap? = path?.let { cache.get(it) }

    suspend fun load(path: String?): ImageBitmap? {
        if (path.isNullOrBlank()) return null
        cache.get(path)?.let { return it }
        return withContext(Dispatchers.IO) {
            val file = File(path)
            if (!file.exists()) return@withContext null
            runCatching {
                BitmapFactory.decodeFile(path)?.asImageBitmap()?.also { cache.put(path, it) }
            }.getOrNull()
        }
    }

    fun clear() = cache.evictAll()
}

@Composable
fun VideoThumbnail(
    path: String?,
    title: String,
    modifier: Modifier = Modifier,
) {
    val bitmap by produceState(
        initialValue = ThumbnailBitmapCache.peek(path),
        key1 = path,
    ) {
        value = ThumbnailBitmapCache.load(path)
    }

    Box(
        modifier = modifier.background(placeholderBrush(title)),
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Icon(
                imageVector = Icons.Rounded.PlayArrow,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.9f),
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxSize(0.26f),
            )
        }
    }
}

fun placeholderBrush(seed: String): Brush {
    val hue = ((seed.hashCode() and 0x7FFFFFFF) % 360).toFloat()
    val first = Color.hsv(hue, 0.32f, 0.56f)
    val second = Color.hsv((hue + 32f) % 360f, 0.42f, 0.30f)
    return Brush.linearGradient(listOf(first, second))
}

@Composable
fun ThumbnailFallbackBackground(title: String, modifier: Modifier = Modifier) {
    Box(modifier.background(placeholderBrush(title))) {
        Icon(
            imageVector = Icons.Rounded.PlayArrow,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.9f),
            modifier = Modifier.fillMaxSize(0.3f),
        )
    }
}
