package com.asuka.player.renderer.activity

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import com.asuka.player.platform.PlaybackActivityDependencies
import com.asuka.player.contract.PlaybackController
import com.asuka.player.render.api.PlaybackSurfaceState
import com.asuka.player.renderer.Media3PlaybackSurfaceState
import com.asuka.player.renderer.controller.PlaybackSessionCoordinator
import com.asuka.player.renderer.controller.PlaybackTrackUiStateHolder
import com.asuka.player.renderer.controller.PlayerUiStateHolder
import com.asuka.player.ui.R
import com.asuka.player.ui.controller.PlaybackControllerConnector
import com.asuka.player.ui.controller.PlaybackTrackSelectionController
import com.asuka.player.ui.controller.PlaybackTrackUiState
import com.asuka.player.ui.state.PlayerUiState
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch

internal data class PlaybackHostState(
    val uiState: PlayerUiState = PlayerUiState(),
    val controller: PlaybackController? = null,
    val surfaceState: PlaybackSurfaceState? = null,
    val trackUiState: PlaybackTrackUiState = PlaybackTrackUiState(),
    val trackSelectionController: PlaybackTrackSelectionController? = null,
    val isConnectingController: Boolean = false,
    val controllerErrorMessage: String? = null,
)

internal class PlaybackSessionHost(
    private val contentResolver: ContentResolver,
    cacheDir: File,
    private val scope: CoroutineScope,
    private val dependencies: PlaybackActivityDependencies,
    controllerContext: android.content.Context,
    private val controllerProvider: PlaybackControllerConnector =
        dependencies.createPlaybackControllerConnector(controllerContext.applicationContext),
) {
    private val appContext = controllerContext.applicationContext
    private val mediaMetadataBridge = PlaybackSessionMediaMetadataBridge(
        contentResolver = contentResolver,
        scope = scope,
    )
    private val launchOrchestrator = PlaybackLaunchOrchestrator(
        contentResolver = contentResolver,
        cacheDir = cacheDir,
        scope = scope,
        runtimeSettingsSource = dependencies.playbackRuntimeSettingsSource,
    )
    private val _state = MutableStateFlow(PlaybackHostState())

    val state: StateFlow<PlaybackHostState> = _state
    val currentPlayer: Player?
        get() = mediaController
    val currentController: PlaybackController?
        get() = playbackController

    private var mediaController: MediaController? = null
    private var playbackController: PlaybackController? = null
    private var stateHolder: PlayerUiStateHolder? = null
    private var trackUiStateHolder: PlaybackTrackUiStateHolder? = null
    private var trackSelectionController: PlaybackTrackSelectionController? = null
    private var initJob: Job? = null
    private var uiStateFeedJob: Job? = null
    private var trackUiStateFeedJob: Job? = null
    private var sessionCoordinator: PlaybackSessionCoordinator? = null

    private val seekFallbackListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState != Player.STATE_READY) return
            val mc = mediaController ?: return
            val currentUri = mc.currentMediaItem?.localConfiguration?.uri ?: return
            launchOrchestrator.handlePlaybackReady(
                currentUri = currentUri,
                isSeekable = mc.isCurrentMediaItemSeekable,
            ) { copiedUri ->
                startSingleMedia(copiedUri)
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            val mc = mediaController ?: return
            val currentUri = mc.currentMediaItem?.localConfiguration?.uri ?: launchOrchestrator.currentIntentData()
            launchOrchestrator.handlePlaybackError(
                currentUri = currentUri,
                error = error,
            ) { copiedUri ->
                startSingleMedia(copiedUri)
            }
        }
    }

    fun ensureControllerReady(
        launchIntent: Intent?,
    ) {
        launchOrchestrator.updateIntent(launchIntent)
        mediaController?.let { attachToResolvedController(it) } ?: connectController()
    }

    fun onNewIntent(
        launchIntent: Intent,
    ) {
        launchOrchestrator.updateIntent(launchIntent, clearSeekFallbackAttempts = true)
        if (mediaController == null) {
            ensureControllerReady(launchIntent)
        } else {
            scope.launch { startSingleMedia(launchIntent.data) }
        }
    }

    fun onStop(retainSession: Boolean) {
        launchOrchestrator.cancelPending()
        uiStateFeedJob?.cancel()
        uiStateFeedJob = null
        trackUiStateFeedJob?.cancel()
        trackUiStateFeedJob = null
        // Always cancel initJob: if retaining the session, ensureControllerReady() on the next
        // resume will reconnect cleanly rather than racing against an in-flight connection that
        // might recreate stateHolder / sessionCoordinator over the state we just cleared.
        initJob?.cancel()
        initJob = null
        stateHolder?.detach()
        stateHolder = null
        trackUiStateHolder?.detach()
        trackUiStateHolder = null
        sessionCoordinator?.detach()
        sessionCoordinator = null
        mediaController?.removeListener(seekFallbackListener)

        if (!retainSession) {
            mediaController?.pause()
            controllerProvider.release()
            mediaController = null
            playbackController = null
            trackSelectionController = null
            _state.value = PlaybackHostState(uiState = _state.value.uiState)
        }
    }

    fun releaseAll() {
        launchOrchestrator.cancelPending()
        uiStateFeedJob?.cancel()
        uiStateFeedJob = null
        trackUiStateFeedJob?.cancel()
        trackUiStateFeedJob = null
        initJob?.cancel()
        initJob = null
        stateHolder?.detach()
        stateHolder = null
        trackUiStateHolder?.detach()
        trackUiStateHolder = null
        sessionCoordinator?.detach()
        sessionCoordinator = null
        mediaController?.removeListener(seekFallbackListener)
        controllerProvider.release()
        mediaController = null
        playbackController = null
        trackSelectionController = null
        _state.value = PlaybackHostState(uiState = _state.value.uiState)
    }

    private fun connectController() {
        if (initJob?.isActive == true) return
        _state.update { current ->
            current.copy(
                isConnectingController = true,
                controllerErrorMessage = null,
            )
        }
        initJob = scope.launch {
            try {
                val mc = controllerProvider.buildAsync().await()
                mediaController = mc
                attachToResolvedController(mc)
                startSingleMedia(launchOrchestrator.currentIntentData())
            } catch (_: CancellationException) {
                // Lifecycle moved on; connection will be retried on next start.
                _state.update { current ->
                    current.copy(isConnectingController = false)
                }
            } catch (error: Throwable) {
                Log.e(TAG, "failed to connect controller", error)
                controllerProvider.release()
                _state.update { current ->
                    current.copy(
                        isConnectingController = false,
                        controllerErrorMessage = appContext.getString(R.string.playback_session_connection_failed),
                    )
                }
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
        if (trackSelectionController == null) {
            trackSelectionController = controllerProvider.asTrackSelectionController(mc)
        }
        if (sessionCoordinator == null) {
            val currentTrackSelectionController = trackSelectionController ?: return
            sessionCoordinator = PlaybackSessionCoordinator(
                mediaController = mc,
                trackSelectionController = currentTrackSelectionController,
                sessionPlanner = dependencies.playbackSessionPlanner,
                titleResolver = mediaMetadataBridge::resolveTitle,
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
        if (trackUiStateHolder == null) {
            val holder = PlaybackTrackUiStateHolder(mc)
            holder.attach()
            trackUiStateHolder = holder
            trackUiStateFeedJob?.cancel()
            trackUiStateFeedJob = scope.launch {
                holder.state.collect { trackUiState ->
                    _state.update { current -> current.copy(trackUiState = trackUiState) }
                }
            }
        }
        _state.update { current ->
            current.copy(
                controller = playbackController,
                surfaceState = Media3PlaybackSurfaceState(mc),
                trackSelectionController = trackSelectionController,
                isConnectingController = false,
                controllerErrorMessage = null,
            )
        }
    }

    private suspend fun startSingleMedia(uri: Uri?) {
        val controller = mediaController ?: return
        launchOrchestrator.startPlayback(
            targetUri = uri,
            sessionStarter = { targetUri, launchIntent, autoplay, policy ->
                sessionCoordinator?.start(
                    targetUri = targetUri,
                    launchIntent = launchIntent,
                    autoplay = autoplay,
                    policy = policy,
                )?.let { startResult ->
                    PlaybackLaunchResult(
                        targetEntry = startResult.targetEntry,
                        plan = startResult.plan,
                    )
                }
            },
            applyArtwork = { targetEntry, startIndex, targetUri ->
                mediaMetadataBridge.maybeLoadAndSetArtwork(
                    controller = controller,
                    mediaId = targetEntry.mediaId,
                    index = startIndex,
                    uri = targetUri,
                )
            },
        )
    }

    private companion object {
        private const val TAG = "AsukaPlayback"
    }
}
