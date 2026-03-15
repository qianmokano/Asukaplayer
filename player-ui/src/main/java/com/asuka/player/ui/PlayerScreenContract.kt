package com.asuka.player.ui

import com.asuka.player.contract.PlaybackController
import com.asuka.player.contract.PlaybackDeviceController
import com.asuka.player.contract.PlaybackPreviewFrameProvider
import com.asuka.player.contract.PlayerSettings
import com.asuka.player.contract.PlaybackTrackSelectionController
import com.asuka.player.contract.PlaybackUiPersistence
import com.asuka.player.render.api.PlaybackSurfaceRenderer
import com.asuka.player.render.api.PlaybackSurfaceState
import com.asuka.player.ui.controller.PlaybackTrackUiState

data class PlaybackScreenModel(
    val surfaceState: PlaybackSurfaceState? = null,
    val trackUiState: PlaybackTrackUiState = PlaybackTrackUiState(),
    val settings: PlayerSettings = PlayerSettings(),
    val isInPip: Boolean = false,
    val isControllerConnected: Boolean = true,
    val isPersistenceDegraded: Boolean = false,
)

data class PlaybackScreenDependencies(
    val controller: PlaybackController,
    val trackSelectionController: PlaybackTrackSelectionController? = null,
    val playbackPersistence: PlaybackUiPersistence,
    val previewFrameProvider: PlaybackPreviewFrameProvider? = null,
    val deviceController: PlaybackDeviceController,
    val surfaceRenderer: PlaybackSurfaceRenderer? = null,
)
