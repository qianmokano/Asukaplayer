package com.asuka.player.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.LruCache
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.scale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

import com.asuka.player.app.VIDEO_ITEM_THUMB_HEIGHT
import com.asuka.player.app.VIDEO_ITEM_THUMB_WIDTH
import com.asuka.player.app.VIDEO_PAGE_CORNER_RADIUS

internal object VideoThumbnailCache {
    // Use 1/8 of the available heap rather than a fixed constant so the cache scales
    // appropriately across low-memory (256 MB heap → ~32 MB) and high-memory devices.
    private val maxBytes = (Runtime.getRuntime().maxMemory() / 8).toInt()
    private val cache = object : LruCache<String, Bitmap>(maxBytes) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }
    val loadSemaphore = Semaphore(2)

    fun get(key: String): Bitmap? = cache.get(key)

    fun put(key: String, bitmap: Bitmap) {
        cache.put(key, bitmap)
    }
}

internal const val VIDEO_THUMB_VERSION = 2
internal const val INITIAL_THUMB_WARMUP_LIMIT = 80
private const val VIDEO_THUMB_MAX_CACHE_BYTES = 120L * 1024L * 1024L
private const val VIDEO_THUMB_MAX_CACHE_FILES = 2_000
private const val VIDEO_THUMB_MAX_AGE_MS = 30L * 24L * 60L * 60L * 1000L

@Composable
internal fun rememberVideoThumbnail(
    uri: Uri?,
    thumbnailId: Long?,
    allowLoad: Boolean = true,
): ImageBitmap? {
    val context = LocalContext.current
    val cacheKey = thumbnailCacheKey(thumbnailId = thumbnailId, uri = uri)
    val cached = cacheKey?.let { VideoThumbnailCache.get(it) }
    val bitmap by produceState<Bitmap?>(initialValue = cached, cacheKey, allowLoad) {
        if (cacheKey == null || uri == null || value != null || !allowLoad) return@produceState
        value = withContext(Dispatchers.IO) {
            VideoThumbnailCache.loadSemaphore.withPermit {
                loadOrCreateVideoThumbnail(
                    context = context,
                    uri = uri,
                    thumbnailId = thumbnailId,
                )
            }
        }?.also { loaded ->
            VideoThumbnailCache.put(cacheKey, loaded)
        }
    }
    return bitmap?.asImageBitmap()
}

internal fun loadOrCreateVideoThumbnail(
    context: Context,
    uri: Uri,
    thumbnailId: Long?,
): Bitmap? {
    val cachedFile = thumbnailId?.let { videoThumbnailFile(context, it) }
    cachedFile?.takeIf { it.exists() }?.let { file ->
        BitmapFactory.decodeFile(file.absolutePath)?.let { return it }
    }

    val generated = loadVideoThumbnail(context, uri) ?: return null
    if (cachedFile != null) {
        runCatching {
            pruneVideoThumbnailCache(context)
            FileOutputStream(cachedFile).use { out ->
                generated.compress(Bitmap.CompressFormat.JPEG, 88, out)
            }
            pruneVideoThumbnailCache(context)
        }
    }
    return generated
}

internal fun videoThumbnailFile(context: Context, thumbnailId: Long): File {
    val dir = videoThumbnailCacheDir(context).apply { mkdirs() }
    return File(dir, "${thumbnailId}_v$VIDEO_THUMB_VERSION.jpg")
}

internal fun thumbnailCacheKey(thumbnailId: Long?, uri: Uri?): String? {
    return when {
        thumbnailId != null -> "${thumbnailId}_v$VIDEO_THUMB_VERSION"
        uri != null -> "u_${uri.hashCode()}_v$VIDEO_THUMB_VERSION"
        else -> null
    }
}

internal suspend fun warmupInitialThumbnails(
    context: Context,
    videos: List<LocalVideoItem>,
    limit: Int,
) {
    videos.take(limit.coerceAtLeast(1)).forEach { video ->
        VideoThumbnailCache.loadSemaphore.withPermit {
            ensureThumbnailFile(
                context = context,
                uri = video.uri,
                thumbnailId = video.id,
            )
        }
    }
}

internal fun ensureThumbnailFile(
    context: Context,
    uri: Uri,
    thumbnailId: Long,
) {
    val cachedFile = videoThumbnailFile(context, thumbnailId)
    if (cachedFile.exists()) return
    val generated = loadVideoThumbnail(context, uri) ?: return
    runCatching {
        pruneVideoThumbnailCache(context)
        FileOutputStream(cachedFile).use { out ->
            generated.compress(Bitmap.CompressFormat.JPEG, 88, out)
        }
        pruneVideoThumbnailCache(context)
    }
}

internal fun loadVideoThumbnail(context: Context, uri: Uri): Bitmap? {
    val fromFrame = runCatching {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()
                ?.coerceAtLeast(0L)
                ?: 0L
            val targetTimeUs = (durationMs * 1000L / 3L).coerceAtLeast(0L)
            val frame = retriever.getFrameAtTime(targetTimeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            frame?.let { limitBitmapEdge(it, maxEdge = 960) }
        } finally {
            retriever.release()
        }
    }.getOrNull()
    return fromFrame
}

internal fun limitBitmapEdge(bitmap: Bitmap, maxEdge: Int): Bitmap {
    if (maxEdge <= 0) return bitmap
    val width = bitmap.width
    val height = bitmap.height
    val longest = maxOf(width, height)
    if (longest <= maxEdge) return bitmap
    val scale = maxEdge.toFloat() / longest.toFloat()
    val targetWidth = (width * scale).toInt().coerceAtLeast(1)
    val targetHeight = (height * scale).toInt().coerceAtLeast(1)
    return bitmap.scale(targetWidth, targetHeight)
}

internal fun pruneVideoThumbnailCache(
    context: Context,
    maxBytes: Long = VIDEO_THUMB_MAX_CACHE_BYTES,
    maxFiles: Int = VIDEO_THUMB_MAX_CACHE_FILES,
    maxAgeMs: Long = VIDEO_THUMB_MAX_AGE_MS,
    nowMs: Long = System.currentTimeMillis(),
) {
    val dir = videoThumbnailCacheDir(context)
    val files = dir.listFiles()
        ?.filter { it.isFile }
        ?: return

    val remaining = files
        .onEach { file ->
            if (nowMs - file.lastModified() > maxAgeMs) {
                file.delete()
            }
        }
        .filter { it.exists() }
        .sortedWith(compareBy<File> { it.lastModified() }.thenBy { it.name })
        .toMutableList()

    var totalBytes = remaining.sumOf { it.length() }
    while (remaining.isNotEmpty() && (totalBytes > maxBytes || remaining.size > maxFiles)) {
        val oldest = remaining.removeAt(0)
        totalBytes -= oldest.length()
        oldest.delete()
    }
}

private fun videoThumbnailCacheDir(context: Context): File =
    File(context.cacheDir, "video_thumb_cache")

@Composable
internal fun VideoThumbOrIcon(
    icon: ImageVector,
    thumbnailUri: Uri?,
    thumbnailId: Long?,
    allowThumbnailLoad: Boolean = true,
    durationLabel: String?,
    progressFraction: Float?,
    selected: Boolean,
) {
    val thumb = rememberVideoThumbnail(
        uri = thumbnailUri,
        thumbnailId = thumbnailId,
        allowLoad = allowThumbnailLoad,
    )
    val shouldUseThumbnailSlot = thumbnailUri != null || thumbnailId != null
    if (shouldUseThumbnailSlot) {
        Crossfade(
            targetState = thumb,
            animationSpec = tween(durationMillis = 220),
            label = "VideoThumbCrossfade",
        ) { image ->
            Box(
                modifier = Modifier
                    .size(width = VIDEO_ITEM_THUMB_WIDTH, height = VIDEO_ITEM_THUMB_HEIGHT)
                    .clip(RoundedCornerShape(VIDEO_PAGE_CORNER_RADIUS)),
            ) {
                if (image != null) {
                    Image(
                        bitmap = image,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize(),
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        )
                    }
                }
                if (!durationLabel.isNullOrBlank()) {
                    Text(
                        text = durationLabel,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = Color.White,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 3.dp, bottom = if ((progressFraction ?: 0f) > 0f) 8.dp else 3.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.Black.copy(alpha = 0.58f))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
                val clampedProgress = progressFraction?.coerceIn(0f, 1f)?.takeIf { it > 0f }
                if (clampedProgress != null) {
                    val playedProgressShape = RoundedCornerShape(
                        topStart = 0.dp,
                        bottomStart = 0.dp,
                        topEnd = 999.dp,
                        bottomEnd = 999.dp,
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .height(4.dp)
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.92f)),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(clampedProgress)
                                .height(4.dp)
                                .clip(playedProgressShape)
                                .background(MaterialTheme.colorScheme.primary),
                        )
                    }
                }
            }
        }
    } else {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}
