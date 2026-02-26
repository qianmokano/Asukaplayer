package com.asuka.player.ui.controller

import androidx.media3.common.Player
import androidx.media3.common.PlaybackException
import com.asuka.player.ui.state.PlayerUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/**
 * Lightweight state holder that observes MediaController and exposes UI state.
 */
class PlayerUiStateHolder(
    private val player: Player,
) : Player.Listener {

    private val _state = MutableStateFlow(PlayerUiState())
    val state: StateFlow<PlayerUiState> = _state
    private var tickerJob: Job? = null
    private var attached = false

    fun attach() {
        attached = true
        player.addListener(this)
        updateFromPlayer()
    }

    fun detach() {
        attached = false
        player.removeListener(this)
        tickerJob?.cancel()
        tickerJob = null
    }

    fun startProgressTicker(scope: CoroutineScope, intervalMs: Long = 500L) {
        tickerJob?.cancel()
        tickerJob = scope.launch {
            while (isActive) {
                updateFromPlayer()
                delay(intervalMs)
            }
        }
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        _state.update {
            it.copy(
                isPlaying = isPlaying,
                isBuffering = player.playbackState == Player.STATE_BUFFERING,
                errorMessage = if (isPlaying) null else it.errorMessage,
            )
        }
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        updateFromPlayer(
            clearError = playbackState == Player.STATE_BUFFERING || playbackState == Player.STATE_READY,
        )
    }

    override fun onEvents(player: Player, events: Player.Events) {
        if (events.contains(Player.EVENT_TIMELINE_CHANGED) ||
            events.contains(Player.EVENT_MEDIA_METADATA_CHANGED)
        ) {
            _state.update { it.copy(title = player.mediaMetadata.title?.toString() ?: "") }
        }
    }

    override fun onPositionDiscontinuity(oldPosition: Player.PositionInfo, newPosition: Player.PositionInfo, reason: Int) {
        updateFromPlayer()
    }

    override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
        _state.update { it.copy(title = mediaItem?.mediaMetadata?.title?.toString() ?: "") }
    }

    override fun onPlayerError(error: PlaybackException) {
        _state.update { it.copy(errorMessage = error.message ?: "Playback error") }
    }

    private fun updateFromPlayer(clearError: Boolean = false) {
        if (!attached) return
        _state.update {
            it.copy(
                isBuffering = player.playbackState == Player.STATE_BUFFERING,
                positionMs = player.currentPosition,
                durationMs = if (player.duration > 0) player.duration else 0L,
                bufferedMs = player.bufferedPosition,
                errorMessage = if (clearError) null else it.errorMessage,
            )
        }
    }
}
