package com.asuka.player.ui

import com.asuka.player.contract.PlaybackController
import com.asuka.player.contract.PlaybackDeviceController
import com.asuka.player.contract.PlaybackRuntimeSettings
import com.asuka.player.contract.PlaybackUiPersistence
import com.asuka.player.render.api.PlaybackSurfaceRenderer
import com.asuka.player.render.api.PlaybackSurfaceState
import com.asuka.player.ui.controller.PlaybackTrackSelectionController
import com.asuka.player.ui.controller.PlaybackTrackUiState
import com.asuka.player.ui.state.PlayerUiState

data class PlaybackScreenModel(
    val uiState: PlayerUiState = PlayerUiState(),
    val surfaceState: PlaybackSurfaceState? = null,
    val trackUiState: PlaybackTrackUiState = PlaybackTrackUiState(),
    val settings: PlaybackRuntimeSettings = PlaybackRuntimeSettings(),
    val isInPip: Boolean = false,
)

data class PlaybackScreenDependencies(
    val controller: PlaybackController,
    val trackSelectionController: PlaybackTrackSelectionController? = null,
    val playbackPersistence: PlaybackUiPersistence,
    val deviceController: PlaybackDeviceController,
    val surfaceRenderer: PlaybackSurfaceRenderer? = null,
)
