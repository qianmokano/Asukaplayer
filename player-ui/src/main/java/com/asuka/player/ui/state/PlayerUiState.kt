package com.asuka.player.ui.state

/**
 * UI-only state container.
 */
data class PlayerUiState(
    val title: String = "",
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val errorMessage: String? = null,
)
