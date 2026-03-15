package com.asuka.player.renderer.activity

import androidx.media3.common.Player
import androidx.media3.session.MediaController
import com.asuka.player.contract.PlaybackController
import com.asuka.player.contract.PlaybackTrackSelectionController
import com.asuka.player.platform.PlaybackActivityDependencies
import com.asuka.player.platform.PlaybackControllerConnector
import com.asuka.player.renderer.controller.PlaybackSessionCoordinator
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch

internal data class PlaybackControllerConnectionSnapshot(
    val mediaController: MediaController,
    val playbackController: PlaybackController,
    val trackSelectionController: PlaybackTrackSelectionController,
    val sessionCoordinator: PlaybackSessionCoordinator,
)

internal class PlaybackControllerConnection(
    private val scope: CoroutineScope,
    private val dependencies: PlaybackActivityDependencies,
    private val controllerProvider: PlaybackControllerConnector,
    private val titleResolver: suspend (android.net.Uri) -> String?,
) {
    private var mediaController: MediaController? = null
    private var playbackController: PlaybackController? = null
    private var trackSelectionController: PlaybackTrackSelectionController? = null
    private var sessionCoordinator: PlaybackSessionCoordinator? = null
    private var initJob: Job? = null

    fun currentPlayer(): Player? = mediaController

    fun currentController(): PlaybackController? = playbackController

    fun currentMediaController(): MediaController? = mediaController

    fun currentSessionCoordinator(): PlaybackSessionCoordinator? = sessionCoordinator

    fun connectOrReuse(
        onConnectingChanged: (Boolean) -> Unit,
        onConnectionFailure: (Throwable) -> Unit,
        onConnected: (PlaybackControllerConnectionSnapshot) -> Unit,
    ) {
        mediaController?.let { existing ->
            ensureSession(existing)?.let(onConnected)
            return
        }
        if (initJob?.isActive == true) return

        onConnectingChanged(true)
        initJob = scope.launch {
            try {
                val resolved = controllerProvider.buildAsync().await()
                mediaController = resolved
                ensureSession(resolved)?.let(onConnected)
                onConnectingChanged(false)
            } catch (_: CancellationException) {
                onConnectingChanged(false)
            } catch (error: Throwable) {
                controllerProvider.release()
                onConnectionFailure(error)
                onConnectingChanged(false)
            } finally {
                initJob = null
            }
        }
    }

    fun clearRetainedSession(playbackListener: Player.Listener) {
        initJob?.cancel()
        initJob = null
        mediaController?.removeListener(playbackListener)
        sessionCoordinator?.detach()
        sessionCoordinator = null
    }

    fun releaseAll(playbackListener: Player.Listener) {
        clearRetainedSession(playbackListener)
        mediaController?.pause()
        playbackController?.release()
        controllerProvider.release()
        mediaController = null
        playbackController = null
        trackSelectionController = null
    }

    private fun ensureSession(
        mediaController: MediaController,
    ): PlaybackControllerConnectionSnapshot? {
        if (playbackController == null) {
            playbackController = controllerProvider.asPlaybackController(mediaController)
        }
        if (trackSelectionController == null) {
            trackSelectionController = controllerProvider.asTrackSelectionController(mediaController)
        }
        if (sessionCoordinator == null) {
            val currentTrackSelectionController = trackSelectionController ?: return null
            sessionCoordinator = PlaybackSessionCoordinator(
                mediaController = mediaController,
                trackSelectionController = currentTrackSelectionController,
                sessionPlanner = dependencies.playbackSessionPlanner,
                titleResolver = titleResolver,
            ).also { it.attach() }
        }
        return PlaybackControllerConnectionSnapshot(
            mediaController = mediaController,
            playbackController = playbackController ?: return null,
            trackSelectionController = trackSelectionController ?: return null,
            sessionCoordinator = sessionCoordinator ?: return null,
        )
    }
}
