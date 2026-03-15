package com.asuka.player.platform

import android.content.ContentResolver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Size
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PlaybackArtworkBridge(
    private val scope: CoroutineScope,
    private val backgroundDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val runOnUiThread: (Runnable) -> Unit = { runnable ->
        if (Looper.myLooper() == Looper.getMainLooper()) {
            runnable.run()
        } else {
            Handler(Looper.getMainLooper()).post(runnable)
        }
    },
    private val artworkLoader: suspend (Uri) -> ByteArray?,
) : Player.Listener {
    constructor(
        contentResolver: ContentResolver,
        scope: CoroutineScope,
        backgroundDispatcher: CoroutineDispatcher = Dispatchers.IO,
        runOnUiThread: (Runnable) -> Unit = { runnable ->
            if (Looper.myLooper() == Looper.getMainLooper()) {
                runnable.run()
            } else {
                Handler(Looper.getMainLooper()).post(runnable)
            }
        },
    ) : this(
        scope = scope,
        backgroundDispatcher = backgroundDispatcher,
        runOnUiThread = runOnUiThread,
        artworkLoader = ContentResolverArtworkLoader(contentResolver)::loadArtworkBytes,
    )

    private var player: Player? = null
    private val loadedArtworkMediaIds = LruSet<String>(MAX_ARTWORK_MEDIA_IDS)
    private val loadingArtworkMediaIds = mutableSetOf<String>()

    fun attach(player: Player) {
        if (this.player === player) {
            maybeLoadCurrentArtwork(player)
            return
        }
        detach()
        this.player = player
        player.addListener(this)
        maybeLoadCurrentArtwork(player)
    }

    fun detach() {
        player?.removeListener(this)
        player = null
        synchronized(loadingArtworkMediaIds) {
            loadingArtworkMediaIds.clear()
        }
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        maybeLoadCurrentArtwork(player ?: return, mediaItem)
    }

    private fun maybeLoadCurrentArtwork(
        player: Player,
        mediaItem: MediaItem? = player.currentMediaItem,
    ) {
        val item = mediaItem ?: return
        val uri = item.localConfiguration?.uri ?: return
        maybeLoadAndSetArtwork(
            player = player,
            mediaId = item.mediaId,
            index = player.currentMediaItemIndex,
            uri = uri,
        )
    }

    private fun maybeLoadAndSetArtwork(
        player: Player,
        mediaId: String,
        index: Int,
        uri: Uri,
    ) {
        val scheme = uri.scheme ?: return
        if (scheme != "content" && scheme != "file") return

        val currentItem = player.currentMediaItem
        if (currentItem?.mediaId == mediaId && currentItem.mediaMetadata.artworkData != null) {
            loadedArtworkMediaIds.add(mediaId)
            return
        }

        synchronized(loadingArtworkMediaIds) {
            if (loadedArtworkMediaIds.contains(mediaId)) return
            if (!loadingArtworkMediaIds.add(mediaId)) return
        }

        scope.launch(backgroundDispatcher) {
            val bytes = runCatching { artworkLoader(uri) }.getOrNull()
            if (bytes == null) {
                clearPending(mediaId)
                return@launch
            }
            runCatching {
                runOnUiThread(
                    Runnable {
                        try {
                            if (this@PlaybackArtworkBridge.player !== player) return@Runnable
                            if (player.currentMediaItemIndex != index) return@Runnable
                            val item = player.currentMediaItem ?: return@Runnable
                            if (item.mediaId != mediaId) return@Runnable
                            if (item.mediaMetadata.artworkData == null) {
                                val metadata = item.mediaMetadata.buildUpon()
                                    .setArtworkData(bytes, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
                                    .build()
                                player.replaceMediaItem(
                                    index,
                                    item.buildUpon()
                                        .setMediaMetadata(metadata)
                                        .build(),
                                )
                            }
                            loadedArtworkMediaIds.add(mediaId)
                        } finally {
                            clearPending(mediaId)
                        }
                    },
                )
            }.onFailure {
                clearPending(mediaId)
            }
        }
    }

    private fun clearPending(mediaId: String) {
        synchronized(loadingArtworkMediaIds) {
            loadingArtworkMediaIds.remove(mediaId)
        }
    }

    private class LruSet<K>(private val maxSize: Int) {
        private val map = object : LinkedHashMap<K, Unit>(maxSize, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, Unit>): Boolean {
                return size > maxSize
            }
        }

        @Synchronized
        fun add(key: K): Boolean {
            val existed = map.containsKey(key)
            map[key] = Unit
            return !existed
        }

        @Synchronized
        fun contains(key: K): Boolean = map.containsKey(key)
    }

    private class ContentResolverArtworkLoader(
        private val contentResolver: ContentResolver,
    ) {
        suspend fun loadArtworkBytes(uri: Uri): ByteArray? {
            val bitmap = loadArtworkBitmap(uri) ?: return null
            return ByteArrayOutputStream().use { output ->
                bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, output)
                output.toByteArray()
            }
        }

        private fun loadArtworkBitmap(uri: Uri): android.graphics.Bitmap? {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q && uri.scheme == "content") {
                runCatching {
                    return contentResolver.loadThumbnail(uri, Size(512, 512), null)
                }
            }
            val retriever = android.media.MediaMetadataRetriever()
            return try {
                if (uri.scheme == "file") {
                    val path = uri.path ?: return null
                    retriever.setDataSource(path)
                } else {
                    val pfd = runCatching { contentResolver.openFileDescriptor(uri, "r") }.getOrNull() ?: return null
                    pfd.use { retriever.setDataSource(it.fileDescriptor) }
                }
                retriever.getFrameAtTime(0, android.media.MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            } catch (_: Throwable) {
                null
            } finally {
                runCatching { retriever.release() }
            }
        }
    }

    private companion object {
        private const val MAX_ARTWORK_MEDIA_IDS = 200
    }
}
