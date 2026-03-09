package com.asuka.player.ui

import androidx.media3.common.Player
import com.asuka.player.core.PlaybackController
import com.asuka.player.core.PlaybackRuntimeSettings
import com.asuka.player.ui.controller.PlaybackDeviceController
import com.asuka.player.ui.controller.PlaybackTrackSelectionController
import com.asuka.player.ui.controller.PlaybackTrackUiState
import com.asuka.player.ui.controller.PlaybackUiPersistence
import com.asuka.player.ui.state.PlayerUiState

data class PlaybackScreenModel(
    val uiState: PlayerUiState = PlayerUiState(),
    val surfacePlayer: Player? = null,
    val trackUiState: PlaybackTrackUiState = PlaybackTrackUiState(),
    val settings: PlaybackRuntimeSettings = PlaybackRuntimeSettings(),
    val isInPip: Boolean = false,
)

data class PlaybackScreenDependencies(
    val controller: PlaybackController,
    val trackSelectionController: PlaybackTrackSelectionController? = null,
    val playbackPersistence: PlaybackUiPersistence,
    val deviceController: PlaybackDeviceController,
)
