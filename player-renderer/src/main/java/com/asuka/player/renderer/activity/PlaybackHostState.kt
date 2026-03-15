package com.asuka.player.renderer.activity

import com.asuka.player.contract.PlaybackController
import com.asuka.player.contract.PlaybackTrackSelectionController
import com.asuka.player.contract.PlayerSettings
import com.asuka.player.render.api.PlaybackSurfaceState
import com.asuka.player.ui.controller.PlaybackTrackUiState

internal data class PlaybackHostState(
    val controller: PlaybackController? = null,
    val surfaceState: PlaybackSurfaceState? = null,
    val trackUiState: PlaybackTrackUiState = PlaybackTrackUiState(),
    val trackSelectionController: PlaybackTrackSelectionController? = null,
    val isConnectingController: Boolean = false,
    val controllerErrorMessage: String? = null,
    val runtimeSettings: PlayerSettings = PlayerSettings(),
    val isInPictureInPicture: Boolean = false,
    val isPersistenceDegraded: Boolean = false,
)
