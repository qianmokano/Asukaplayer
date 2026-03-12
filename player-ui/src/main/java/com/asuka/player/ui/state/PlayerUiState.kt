package com.asuka.player.ui.state

import com.asuka.player.contract.LoopMode

/**
 * UI-only state container.
 */
data class PlayerUiState(
    val title: String = "",
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val repeatMode: LoopMode = LoopMode.OFF,
    val shuffleEnabled: Boolean = false,
    val errorMessage: String? = null,
)
