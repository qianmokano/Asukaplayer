package com.asuka.player.renderer.activity

import androidx.media3.common.Player
import com.asuka.player.contract.PlaybackController
import com.asuka.player.contract.PlaybackTrackSelectionController
import com.asuka.player.render.api.PlaybackSurfaceState
import com.asuka.player.renderer.Media3PlaybackSurfaceState
import com.asuka.player.renderer.controller.PlaybackTrackUiStateHolder
import com.asuka.player.renderer.controller.PlayerUiStateHolder
import com.asuka.player.ui.state.PlayerUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class PlaybackSessionStateFeeds(
    private val scope: CoroutineScope,
    private val state: MutableStateFlow<PlaybackHostState>,
) {
    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var boundControllerIdentity: Any? = null
    private var uiStateHolder: PlayerUiStateHolder? = null
    private var trackUiStateHolder: PlaybackTrackUiStateHolder? = null
    private var uiStateFeedJob: Job? = null
    private var trackUiStateFeedJob: Job? = null

    fun bind(connection: PlaybackControllerConnectionSnapshot) {
        bindResolvedSession(
            player = connection.mediaController,
            controllerIdentity = connection.mediaController,
            playbackController = connection.playbackController,
            trackSelectionController = connection.trackSelectionController,
            surfaceState = Media3PlaybackSurfaceState(connection.mediaController),
        )
    }

    internal fun bindResolvedSession(
        player: Player,
        controllerIdentity: Any,
        playbackController: PlaybackController,
        trackSelectionController: PlaybackTrackSelectionController,
        surfaceState: PlaybackSurfaceState,
    ) {
        if (boundControllerIdentity !== controllerIdentity) {
            clear()
            boundControllerIdentity = controllerIdentity
        }
        if (uiStateHolder == null) {
            val holder = PlayerUiStateHolder(player)
            holder.attach()
            holder.startProgressTicker(scope)
            uiStateHolder = holder
            uiStateFeedJob?.cancel()
            uiStateFeedJob = scope.launch {
                holder.state.collect { uiState -> _uiState.value = uiState }
            }
        }
        if (trackUiStateHolder == null) {
            val holder = PlaybackTrackUiStateHolder(player)
            holder.attach()
            trackUiStateHolder = holder
            trackUiStateFeedJob?.cancel()
            trackUiStateFeedJob = scope.launch {
                holder.state.collect { trackUiState ->
                    state.update { current -> current.copy(trackUiState = trackUiState) }
                }
            }
        }
        state.update { current ->
            current.copy(
                controller = playbackController,
                surfaceState = surfaceState,
                trackSelectionController = trackSelectionController,
                isConnectingController = false,
                controllerErrorMessage = null,
            )
        }
    }

    fun clear() {
        uiStateFeedJob?.cancel()
        uiStateFeedJob = null
        trackUiStateFeedJob?.cancel()
        trackUiStateFeedJob = null
        uiStateHolder?.detach()
        uiStateHolder = null
        trackUiStateHolder?.detach()
        trackUiStateHolder = null
        boundControllerIdentity = null
    }

    fun resetToDisconnected() {
        clear()
        state.value = PlaybackHostState()
    }
}
