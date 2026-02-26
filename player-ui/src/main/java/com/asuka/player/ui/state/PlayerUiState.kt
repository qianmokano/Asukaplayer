package com.asuka.player.ui.state

import com.asuka.player.core.VideoScaleMode

/**
 * UI-only state container.
 */
data class PlayerUiState(
    val title: String = "",
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val bufferedMs: Long = 0L,
    val controlsVisible: Boolean = true,
    val controlsLocked: Boolean = false,
    val errorMessage: String? = null,
    val scaleMode: VideoScaleMode = VideoScaleMode.FIT,
)
