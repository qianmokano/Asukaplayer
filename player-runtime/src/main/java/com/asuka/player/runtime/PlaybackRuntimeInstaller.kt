package com.asuka.player.runtime

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Bitmap.createScaledBitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.LruCache
import java.io.ByteArrayOutputStream
import com.asuka.player.contract.PlaybackPreviewFrameProvider
import com.asuka.player.contract.PlaybackSessionPlanner
import com.asuka.player.contract.PlaybackStateRepository
import com.asuka.player.contract.PlaybackStore
import com.asuka.player.contract.PlaybackUiPersistence
import com.asuka.player.contract.QueueHistoryRepository
import com.asuka.player.contract.QueueHistoryStore
import com.asuka.player.data.PlaybackPersistenceStores
import com.asuka.player.data.PlaybackPersistenceStoresFactory
import com.asuka.player.platform.PlaybackControllerConnectorFactory
import com.asuka.player.platform.PlaybackDeviceControllerFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

class PlaybackRuntimeFeature(
    application: Application,
    playbackBehaviorRepository: PlaybackBehaviorRepository,
    scope: CoroutineScope,
    controllerConnectorFactory: PlaybackControllerConnectorFactory,
    val playbackPlatformBindings: PlaybackPlatformBindings,
    persistenceStoresFactory: suspend (Context) -> PlaybackPersistenceStores = PlaybackPersistenceStoresFactory::create,
    private val nowMs: () -> Long = System::currentTimeMillis,
) {
    private val appContext = application.applicationContext
    private val persistenceResolver = PlaybackPersistenceResolver(
        context = appContext,
        createStores = persistenceStoresFactory,
        nowMs = nowMs,
    )

    val playbackStore: PlaybackStore = DeferredPlaybackStore(persistenceResolver)
    val queueHistoryStore: QueueHistoryStore = DeferredQueueHistoryStore(persistenceResolver)
    val queueHistoryRepository: QueueHistoryRepository = QueueHistoryRepository(queueHistoryStore)
    val playbackStateRepository: PlaybackStateRepository = PlaybackStateRepository(playbackStore)
    val playbackSessionPlanner: PlaybackSessionPlanner = PlaybackSessionPlanner(playbackStateRepository)
    val playbackUiPersistence: PlaybackUiPersistence = PlaybackStateUiPersistence(
        playbackStateRepository = playbackStateRepository,
        playbackBehaviorRepository = playbackBehaviorRepository,
        scope = scope,
    )
    val playbackPreviewFrameProvider: PlaybackPreviewFrameProvider = MediaMetadataPreviewFrameProvider(appContext)
    val playbackDeviceControllerFactory: PlaybackDeviceControllerFactory = DefaultPlaybackDeviceControllerFactory
    val playbackControllerConnectorFactory: PlaybackControllerConnectorFactory = controllerConnectorFactory
    val playbackLaunchCoordinator: PlaybackLaunchCoordinator by lazy(LazyThreadSafetyMode.NONE) {
        PlaybackLaunchCoordinator(PassthroughPlaybackUriResolver)
    }
    val persistenceDegraded: StateFlow<Boolean> = persistenceResolver.degraded

    init {
        scope.launch { persistenceResolver.resolve() }
    }
}


private class DeferredPlaybackStore(
    private val persistenceResolver: PlaybackPersistenceResolver,
) : PlaybackStore {
    override suspend fun recentMediaIds(limit: Int): List<String> =
        persistenceResolver.withPlaybackStore { it.recentMediaIds(limit) }

    override suspend fun loadPosition(mediaId: String): Long? =
        persistenceResolver.withPlaybackStore { it.loadPosition(mediaId) }

    override suspend fun savePosition(mediaId: String, positionMs: Long) {
        persistenceResolver.withPlaybackStore { it.savePosition(mediaId, positionMs) }
    }

    override suspend fun loadPlaybackSpeed(mediaId: String): Float? =
        persistenceResolver.withPlaybackStore { it.loadPlaybackSpeed(mediaId) }

    override suspend fun savePlaybackSpeed(mediaId: String, speed: Float) {
        persistenceResolver.withPlaybackStore { it.savePlaybackSpeed(mediaId, speed) }
    }

    override suspend fun loadAudioTrackId(mediaId: String): String? =
        persistenceResolver.withPlaybackStore { it.loadAudioTrackId(mediaId) }

    override suspend fun saveAudioTrackId(mediaId: String, trackId: String) {
        persistenceResolver.withPlaybackStore { it.saveAudioTrackId(mediaId, trackId) }
    }

    override suspend fun loadSubtitleTrackId(mediaId: String): String? =
        persistenceResolver.withPlaybackStore { it.loadSubtitleTrackId(mediaId) }

    override suspend fun saveSubtitleTrackId(mediaId: String, trackId: String) {
        persistenceResolver.withPlaybackStore { it.saveSubtitleTrackId(mediaId, trackId) }
    }

    override suspend fun loadZoom(mediaId: String): Float? =
        persistenceResolver.withPlaybackStore { it.loadZoom(mediaId) }

    override suspend fun saveZoom(mediaId: String, zoom: Float) {
        persistenceResolver.withPlaybackStore { it.saveZoom(mediaId, zoom) }
    }
}

private class DeferredQueueHistoryStore(
    private val persistenceResolver: PlaybackPersistenceResolver,
) : QueueHistoryStore {
    override suspend fun push(mediaId: String) {
        persistenceResolver.withQueueHistoryStore { it.push(mediaId) }
    }

    override suspend fun items(): List<String> =
        persistenceResolver.withQueueHistoryStore { it.items() }
}

private class MediaMetadataPreviewFrameProvider(
    private val context: Context,
) : PlaybackPreviewFrameProvider {
    private val cache = object : LruCache<String, ByteArray>(PREVIEW_CACHE_MAX_BYTES) {
        override fun sizeOf(key: String, value: ByteArray): Int = value.size
    }
    private val loadSemaphore = Semaphore(1)

    override suspend fun loadPreviewFrame(
        playbackUri: String,
        positionMs: Long,
        maxWidthPx: Int,
        maxHeightPx: Int,
    ): ByteArray? {
        if (playbackUri.isBlank() || maxWidthPx <= 0 || maxHeightPx <= 0) return null
        val bucketedPositionMs = bucketPreviewPosition(positionMs)
        val cacheKey = "$playbackUri@$bucketedPositionMs:${maxWidthPx}x$maxHeightPx"
        cache.get(cacheKey)?.let { return it }
        return withContext(Dispatchers.IO) {
            loadSemaphore.withPermit {
                cache.get(cacheKey)?.let { return@withPermit it }
                loadFrame(
                    playbackUri = playbackUri,
                    positionMs = bucketedPositionMs,
                    maxWidthPx = maxWidthPx,
                    maxHeightPx = maxHeightPx,
                )?.also { cache.put(cacheKey, it) }
            }
        }
    }

    private fun loadFrame(
        playbackUri: String,
        positionMs: Long,
        maxWidthPx: Int,
        maxHeightPx: Int,
    ): ByteArray? {
        val retriever = MediaMetadataRetriever()
        return runCatching {
            val uri = Uri.parse(playbackUri)
            when (uri.scheme?.lowercase()) {
                "content", "file", "android.resource" -> retriever.setDataSource(context, uri)
                else -> retriever.setDataSource(playbackUri, emptyMap())
            }
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()
                ?.coerceAtLeast(0L)
                ?: 0L
            val targetMs = if (durationMs > 0L) {
                positionMs.coerceIn(0L, durationMs)
            } else {
                positionMs.coerceAtLeast(0L)
            }
            val frame = retriever.getFrameAtTime(targetMs * 1000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: retriever.getFrameAtTime(targetMs * 1000L, MediaMetadataRetriever.OPTION_CLOSEST)
                ?: retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            frame
                ?.scaleToFit(maxWidthPx, maxHeightPx)
                ?.toCompressedJpeg()
        }.getOrNull().also {
            runCatching { retriever.release() }
        }
    }

    private fun Bitmap.scaleToFit(maxWidthPx: Int, maxHeightPx: Int): Bitmap {
        if (width <= maxWidthPx && height <= maxHeightPx) return this
        val scale = minOf(maxWidthPx.toFloat() / width.toFloat(), maxHeightPx.toFloat() / height.toFloat())
        val targetWidth = (width * scale).toInt().coerceAtLeast(1)
        val targetHeight = (height * scale).toInt().coerceAtLeast(1)
        return createScaledBitmap(this, targetWidth, targetHeight, true)
    }

    private fun Bitmap.toCompressedJpeg(): ByteArray {
        val stream = ByteArrayOutputStream()
        compress(Bitmap.CompressFormat.JPEG, 85, stream)
        return stream.toByteArray()
    }

    companion object {
        private const val PREVIEW_CACHE_MAX_BYTES = 24 * 1024 * 1024
        private const val PREVIEW_BUCKET_MS = 500L

        private fun bucketPreviewPosition(positionMs: Long): Long =
            ((positionMs.coerceAtLeast(0L) + PREVIEW_BUCKET_MS / 2L) / PREVIEW_BUCKET_MS) * PREVIEW_BUCKET_MS
    }
}
