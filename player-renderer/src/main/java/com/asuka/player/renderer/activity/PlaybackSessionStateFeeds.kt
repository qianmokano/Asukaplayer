package com.asuka.player.renderer.activity

import com.asuka.player.renderer.Media3PlaybackSurfaceState
import com.asuka.player.renderer.controller.PlaybackTrackUiStateHolder
import com.asuka.player.renderer.controller.PlayerUiStateHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class PlaybackSessionStateFeeds(
    private val scope: CoroutineScope,
    private val state: MutableStateFlow<PlaybackHostState>,
) {
    private var uiStateHolder: PlayerUiStateHolder? = null
    private var trackUiStateHolder: PlaybackTrackUiStateHolder? = null
    private var uiStateFeedJob: Job? = null
    private var trackUiStateFeedJob: Job? = null

    fun bind(connection: PlaybackControllerConnectionSnapshot) {
        if (uiStateHolder == null) {
            val holder = PlayerUiStateHolder(connection.mediaController)
            holder.attach()
            holder.startProgressTicker(scope)
            uiStateHolder = holder
            uiStateFeedJob?.cancel()
            uiStateFeedJob = scope.launch {
                holder.state.collect { uiState ->
                    state.update { current -> current.copy(uiState = uiState) }
                }
            }
        }
        if (trackUiStateHolder == null) {
            val holder = PlaybackTrackUiStateHolder(connection.mediaController)
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
                controller = connection.playbackController,
                surfaceState = Media3PlaybackSurfaceState(connection.mediaController),
                trackSelectionController = connection.trackSelectionController,
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
    }

    fun resetToUiStateOnly() {
        state.value = PlaybackHostState(uiState = state.value.uiState)
    }
}
