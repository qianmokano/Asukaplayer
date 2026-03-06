package com.asuka.player.ui.activity

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import android.util.Size
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import com.asuka.player.core.PlaybackController
import com.asuka.player.core.PlaybackCoreGraph
import com.asuka.player.core.PlaybackStartupPolicy
import com.asuka.player.core.SeekFallbackCopier
import com.asuka.player.core.impl.Media3PlaybackController
import com.asuka.player.ui.PlayerRuntimeSettings
import com.asuka.player.ui.controller.ControllerBindings
import com.asuka.player.ui.controller.ControllerProvider
import com.asuka.player.ui.controller.PlaybackSessionCoordinator
import com.asuka.player.ui.controller.PlayerUiStateHolder
import com.asuka.player.ui.state.PlayerUiState
import java.io.ByteArrayOutputStream
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal data class PlaybackHostState(
    val uiState: PlayerUiState = PlayerUiState(),
    val controller: PlaybackController? = null,
    val player: Player? = null,
    val bindings: ControllerBindings? = null,
)

internal class PlaybackSessionHost(
    private val contentResolver: ContentResolver,
    cacheDir: File,
    private val scope: CoroutineScope,
    private val graph: PlaybackCoreGraph,
    controllerContext: android.content.Context,
) {
    private class LruSet<K>(private val maxSize: Int) {
        private val map = object : LinkedHashMap<K, Unit>(maxSize, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, Unit>): Boolean {
                return size > maxSize
            }
        }

        fun add(key: K): Boolean {
            val existed = map.containsKey(key)
            map[key] = Unit
            return !existed
        }
    }

    private val controllerProvider = ControllerProvider(controllerContext.applicationContext)
    private val seekFallbackCopier = SeekFallbackCopier(contentResolver, cacheDir)
    private val artworkSetForMediaIds = LruSet<String>(MAX_ARTWORK_MEDIA_IDS)
    private val _state = MutableStateFlow(PlaybackHostState())

    val state: StateFlow<PlaybackHostState> = _state
    val currentPlayer: Player?
        get() = mediaController
    val currentController: PlaybackController?
        get() = playbackController

    private var mediaController: MediaController? = null
    private var playbackController: Media3PlaybackController? = null
    private var stateHolder: PlayerUiStateHolder? = null
    private var bindings: ControllerBindings? = null
    private var initJob: Job? = null
    private var seekFallbackJob: Job? = null
    private var uiStateFeedJob: Job? = null
    private var sessionCoordinator: PlaybackSessionCoordinator? = null
    private var currentIntent: Intent? = null
    private var runtimeSettings: PlayerRuntimeSettings = PlayerRuntimeSettings()
    private val seekFallbackAttemptedUris = mutableSetOf<String>()

    private val seekFallbackListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState != Player.STATE_READY) return
            val mc = mediaController ?: return
            val currentUri = mc.currentMediaItem?.localConfiguration?.uri ?: return
            if (mc.isCurrentMediaItemSeekable) return
            trySeekFallback(currentUri, "not_seekable_ready")
        }

        override fun onPlayerError(error: PlaybackException) {
            val mc = mediaController ?: return
            val currentUri = mc.currentMediaItem?.localConfiguration?.uri ?: currentIntent?.data ?: return
            trySeekFallback(currentUri, "player_error_${error.errorCode}")
        }
    }

    fun ensureControllerReady(
        launchIntent: Intent?,
        runtimeSettings: PlayerRuntimeSettings,
    ) {
        currentIntent = launchIntent
        this.runtimeSettings = runtimeSettings
        mediaController?.let { attachToResolvedController(it) } ?: connectController()
    }

    fun onNewIntent(
        launchIntent: Intent,
        runtimeSettings: PlayerRuntimeSettings,
    ) {
        currentIntent = launchIntent
        this.runtimeSettings = runtimeSettings
        seekFallbackAttemptedUris.clear()
        if (mediaController == null) {
            ensureControllerReady(launchIntent, runtimeSettings)
        } else {
            scope.launch { startSingleMedia(launchIntent.data) }
        }
    }

    fun onStop(retainSession: Boolean) {
        seekFallbackJob?.cancel()
        seekFallbackJob = null
        uiStateFeedJob?.cancel()
        uiStateFeedJob = null
        stateHolder?.detach()
        stateHolder = null
        sessionCoordinator?.detach()
        sessionCoordinator = null
        mediaController?.removeListener(seekFallbackListener)

        if (!retainSession) {
            initJob?.cancel()
            initJob = null
            mediaController?.pause()
            controllerProvider.release()
            mediaController = null
            playbackController = null
            bindings = null
            _state.value = PlaybackHostState(uiState = _state.value.uiState)
        }
    }

    fun releaseAll() {
        seekFallbackJob?.cancel()
        seekFallbackJob = null
        uiStateFeedJob?.cancel()
        uiStateFeedJob = null
        initJob?.cancel()
        initJob = null
        stateHolder?.detach()
        stateHolder = null
        sessionCoordinator?.detach()
        sessionCoordinator = null
        mediaController?.removeListener(seekFallbackListener)
        controllerProvider.release()
        mediaController = null
        playbackController = null
        bindings = null
        _state.value = PlaybackHostState(uiState = _state.value.uiState)
    }

    private fun connectController() {
        if (initJob?.isActive == true) return
        initJob = scope.launch {
            try {
                val mc = controllerProvider.buildAsync().await()
                mediaController = mc
                attachToResolvedController(mc)
                startSingleMedia(currentIntent?.data)
            } catch (_: CancellationException) {
                // Lifecycle moved on; connection will be retried on next start.
            } catch (error: Throwable) {
                Log.e(TAG, "failed to connect controller", error)
            } finally {
                initJob = null
            }
        }
    }

    private fun attachToResolvedController(mc: MediaController) {
        mc.removeListener(seekFallbackListener)
        mc.addListener(seekFallbackListener)

        if (playbackController == null) {
            playbackController = controllerProvider.asPlaybackController(mc)
        }
        if (bindings == null) {
            bindings = ControllerBindings.from(mc)
        }
        if (sessionCoordinator == null) {
            val currentBindings = bindings ?: return
            sessionCoordinator = PlaybackSessionCoordinator(
                mediaController = mc,
                controllerBindings = currentBindings,
                sessionPlanner = graph.playbackSessionPlanner,
                titleResolver = ::resolveTitleForNotification,
            ).also { it.attach() }
        }
        if (stateHolder == null) {
            val holder = PlayerUiStateHolder(mc)
            holder.attach()
            holder.startProgressTicker(scope)
            stateHolder = holder
            uiStateFeedJob?.cancel()
            uiStateFeedJob = scope.launch {
                holder.state.collect { uiState ->
                    _state.update { current -> current.copy(uiState = uiState) }
                }
            }
        }
        _state.update { current ->
            current.copy(
                controller = playbackController,
                player = mc,
                bindings = bindings,
            )
        }
    }

    private suspend fun startSingleMedia(uri: Uri?) {
        val controller = mediaController ?: return
        val target = uri ?: return
        val plan = sessionCoordinator?.start(
            targetUri = target,
            launchIntent = currentIntent,
            autoplay = runtimeSettings.autoplay,
            policy = PlaybackStartupPolicy(
                resumePlayback = runtimeSettings.resumePlayback,
                defaultPlaybackSpeed = runtimeSettings.defaultPlaybackSpeed,
                rememberTrackSelections = runtimeSettings.rememberSelections,
            ),
        ) ?: return

        maybeLoadAndSetArtwork(
            controller = controller,
            mediaId = target.toString(),
            index = plan.queue.startIndex,
            uri = target,
        )
    }

    private fun resolveTitleForNotification(uri: Uri): String? {
        val fromContent = if (uri.scheme == "content") {
            runCatching {
                contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                    ?.use { cursor ->
                        val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (idx >= 0 && cursor.moveToFirst()) cursor.getString(idx) else null
                    }
            }.getOrNull()
        } else {
            null
        }
        val fromPath = uri.lastPathSegment
        return fromContent?.takeIf { it.isNotBlank() }
            ?: fromPath?.takeIf { it.isNotBlank() }
            ?: uri.toString().takeIf { it.isNotBlank() }
    }

    private fun maybeLoadAndSetArtwork(
        controller: MediaController,
        mediaId: String,
        index: Int,
        uri: Uri,
    ) {
        val scheme = uri.scheme ?: return
        if (scheme != "content" && scheme != "file") return
        if (!artworkSetForMediaIds.add(mediaId)) return
        scope.launch(Dispatchers.IO) {
            val bitmap = loadArtworkBitmap(uri) ?: return@launch
            val bytes = ByteArrayOutputStream().use { baos ->
                bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, baos)
                baos.toByteArray()
            }
            withContext(Dispatchers.Main) {
                val current = controller.currentMediaItemIndex
                if (current != index) return@withContext
                val item = controller.currentMediaItem ?: return@withContext
                if (item.mediaId != mediaId) return@withContext
                val metadata = item.mediaMetadata.buildUpon()
                    .setArtworkData(bytes, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
                    .build()
                controller.replaceMediaItem(index, item.buildUpon().setMediaMetadata(metadata).build())
            }
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

    private fun trySeekFallback(currentUri: Uri, reason: String) {
        if (currentUri.scheme != "content") return
        val key = currentUri.toString()
        if (!seekFallbackAttemptedUris.add(key)) return
        if (seekFallbackJob?.isActive == true) return
        seekFallbackJob = scope.launch {
            val copiedUri = withContext(Dispatchers.IO) {
                seekFallbackCopier.copy(currentUri, checkSize = true)
            } ?: return@launch
            Log.i(TAG, "fallback[$reason] src=${currentUri.authority} dst=${copiedUri.lastPathSegment}")
            currentIntent = Intent(currentIntent).apply { data = copiedUri }
            startSingleMedia(copiedUri)
        }
    }

    private companion object {
        private const val TAG = "AsukaPlayback"
        private const val MAX_ARTWORK_MEDIA_IDS = 200
    }
}
