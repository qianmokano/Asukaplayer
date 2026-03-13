package com.asuka.player.renderer.activity

import com.asuka.player.contract.PlaybackController
import com.asuka.player.contract.PlaybackTrackSelectionController
import com.asuka.player.render.api.PlaybackSurfaceState
import com.asuka.player.ui.controller.PlaybackTrackUiState
import com.asuka.player.ui.state.PlayerUiState

internal data class PlaybackHostState(
    val uiState: PlayerUiState = PlayerUiState(),
    val controller: PlaybackController? = null,
    val surfaceState: PlaybackSurfaceState? = null,
    val trackUiState: PlaybackTrackUiState = PlaybackTrackUiState(),
    val trackSelectionController: PlaybackTrackSelectionController? = null,
    val isConnectingController: Boolean = false,
    val controllerErrorMessage: String? = null,
)
