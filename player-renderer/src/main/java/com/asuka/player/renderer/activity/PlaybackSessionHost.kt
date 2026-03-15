package com.asuka.player.renderer.activity

import android.content.ContentResolver
import android.content.Intent
import android.util.Log
import androidx.media3.common.Player
import com.asuka.player.contract.PlaybackController
import com.asuka.player.platform.PlaybackActivityDependencies
import com.asuka.player.platform.PlaybackControllerConnector
import com.asuka.player.ui.R
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

internal class PlaybackSessionHost(
    contentResolver: ContentResolver,
    cacheDir: File,
    scope: CoroutineScope,
    private val dependencies: PlaybackActivityDependencies,
    controllerContext: android.content.Context,
    controllerProvider: PlaybackControllerConnector =
        dependencies.createPlaybackControllerConnector(controllerContext.applicationContext),
    sharedState: MutableStateFlow<PlaybackHostState> = MutableStateFlow(PlaybackHostState()),
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
    private val _state = sharedState
    private val stateFeeds = PlaybackSessionStateFeeds(
        scope = scope,
        state = _state,
    )
    private val controllerConnection = PlaybackControllerConnection(
        scope = scope,
        dependencies = dependencies,
        controllerProvider = controllerProvider,
        titleResolver = mediaMetadataBridge::resolveTitle,
    )
    private val launchDriver = PlaybackSessionLaunchDriver(
        scope = scope,
        launchOrchestrator = launchOrchestrator,
        mediaControllerProvider = controllerConnection::currentMediaController,
        sessionCoordinatorProvider = controllerConnection::currentSessionCoordinator,
        applyArtwork = { mediaController, request, startIndex, targetUri ->
            mediaMetadataBridge.maybeLoadAndSetArtwork(
                controller = mediaController,
                mediaId = request.targetEntry.mediaId,
                index = startIndex,
                uri = targetUri,
            )
        },
    )

    val state: StateFlow<PlaybackHostState> = _state
    val currentPlayer: Player?
        get() = controllerConnection.currentPlayer()
    val currentController: PlaybackController?
        get() = controllerConnection.currentController()

    fun ensureControllerReady(launchIntent: Intent?) {
        launchDriver.updateIntent(
            intent = launchIntent,
            supersedeRequest = currentPlayer == null && launchIntent != null,
            clearSeekFallbackAttempts = currentPlayer == null && launchIntent != null,
        )
        connectOrReuseController()
    }

    fun onNewIntent(launchIntent: Intent) {
        val requestId = launchDriver.updateIntent(
            intent = launchIntent,
            supersedeRequest = true,
            clearSeekFallbackAttempts = true,
        )
        launchDriver.cancelPending()
        if (currentPlayer == null) {
            connectOrReuseController()
        } else {
            launchDriver.startCurrentRequest(requestId)
        }
    }

    fun onStop(retainSession: Boolean) {
        launchDriver.cancelPending()
        stateFeeds.clear()
        controllerConnection.clearRetainedSession(launchDriver.playbackListener)
        if (!retainSession) {
            controllerConnection.releaseAll(launchDriver.playbackListener)
            launchDriver.clearAppliedRequest()
            stateFeeds.resetToUiStateOnly()
        }
    }

    fun releaseAll() {
        launchDriver.cancelPending()
        stateFeeds.clear()
        controllerConnection.releaseAll(launchDriver.playbackListener)
        launchDriver.clearAppliedRequest()
        stateFeeds.resetToUiStateOnly()
    }

    private fun connectOrReuseController() {
        controllerConnection.connectOrReuse(
            onConnectingChanged = { isConnecting ->
                _state.update { current ->
                    current.copy(
                        isConnectingController = isConnecting,
                        controllerErrorMessage = if (isConnecting) null else current.controllerErrorMessage,
                    )
                }
            },
            onConnectionFailure = { error ->
                if (error is CancellationException) return@connectOrReuse
                Log.e(TAG, "failed to connect controller", error)
                _state.update { current ->
                    current.copy(
                        isConnectingController = false,
                        controllerErrorMessage = appContext.getString(R.string.playback_session_connection_failed),
                    )
                }
            },
            onConnected = { connection ->
                launchDriver.bindPlayer(connection.mediaController)
                stateFeeds.bind(connection)
                launchDriver.startCurrentRequest()
            },
        )
    }

    private companion object {
        private const val TAG = "AsukaPlayback"
    }
}
