package com.asuka.player.platform

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import com.asuka.player.contract.PersistedTrackSelection
import com.asuka.player.contract.PlaybackStore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

@OptIn(UnstableApi::class)
class PlaybackStateWriter(
    private val store: PlaybackStore,
    writeDispatcher: CoroutineDispatcher? = null,
) : Player.Listener {
    companion object {
        const val POSITION_CHECKPOINT_INTERVAL_MS = 5_000L
        const val RESUME_RESTART_THRESHOLD_MS = 10_000L
        private const val TAG = "PlaybackStateWriter"

        fun shouldSavePositionOnPause(isPlaying: Boolean, playbackState: Int): Boolean {
            if (isPlaying) return false
            return playbackState != Player.STATE_IDLE &&
                playbackState != Player.STATE_ENDED &&
                playbackState != Player.STATE_BUFFERING
        }
    }

    private var currentMediaId: String? = null
    private var attachedPlayer: Player? = null
    private var lastCheckpointMediaId: String? = null
    private var lastCheckpointRealtimeMs: Long = Long.MIN_VALUE
    private val writeQueue = SerialTaskQueue(
        dispatcher = writeDispatcher ?: Dispatchers.IO.limitedParallelism(1),
        tag = TAG,
    )

    fun attach(player: Player) {
        attachedPlayer = player
        currentMediaId = player.currentMediaItem?.mediaId
        player.addListener(this)
    }

    fun detach(player: Player) {
        player.removeListener(this)
        attachedPlayer = null
        currentMediaId = null
        lastCheckpointMediaId = null
        lastCheckpointRealtimeMs = Long.MIN_VALUE
    }

    fun close() {
        writeQueue.close()
    }

    suspend fun awaitIdle() {
        writeQueue.awaitIdle()
    }

    fun checkpoint(nowMs: Long, minIntervalMs: Long = POSITION_CHECKPOINT_INTERVAL_MS): Boolean {
        val player = attachedPlayer ?: return false
        if (!player.isPlaying) return false
        val mediaId = resolveCurrentMediaId(player) ?: return false
        val lastSavedForSameMedia = lastCheckpointMediaId == mediaId
        if (lastSavedForSameMedia && nowMs - lastCheckpointRealtimeMs < minIntervalMs) {
            return false
        }
        if (!saveCurrentPositionAsync(player, mediaId)) return false
        lastCheckpointMediaId = mediaId
        lastCheckpointRealtimeMs = nowMs
        return true
    }

    fun flushCurrentPosition(): Boolean {
        val player = attachedPlayer ?: return false
        val mediaId = resolveCurrentMediaId(player) ?: return false
        return enqueueCurrentPosition(player, mediaId)
    }

    suspend fun flushCurrentPositionAndAwait(): Boolean {
        val player = attachedPlayer ?: return false
        val mediaId = resolveCurrentMediaId(player) ?: return false
        return saveCurrentPositionAwaited(player, mediaId)
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        val player = attachedPlayer ?: return
        val state = player.playbackState
        if (!shouldSavePositionOnPause(isPlaying = isPlaying, playbackState = state)) return
        val mediaId = currentMediaId ?: return
        saveCurrentPositionAsync(player, mediaId)
    }

    override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
        currentMediaId = mediaItem?.mediaId
        lastCheckpointMediaId = null
        lastCheckpointRealtimeMs = Long.MIN_VALUE
    }

    override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
        val mediaId = currentMediaId ?: return
        dispatchWrite {
            store.savePlaybackSpeed(mediaId, playbackParameters.speed)
        }
    }

    override fun onPositionDiscontinuity(
        oldPosition: Player.PositionInfo,
        newPosition: Player.PositionInfo,
        reason: Int,
    ) {
        if (reason == Player.DISCONTINUITY_REASON_AUTO_TRANSITION) return
        val mediaId = newPosition.mediaItem?.mediaId ?: oldPosition.mediaItem?.mediaId ?: return
        val duration = attachedPlayer?.duration ?: C.TIME_UNSET
        val playbackState = attachedPlayer?.playbackState ?: Player.STATE_READY
        dispatchWrite {
            store.savePosition(
                mediaId,
                normalizePersistedPosition(
                    playbackState = playbackState,
                    positionMs = newPosition.positionMs,
                    durationMs = duration,
                ),
            )
        }
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        if (playbackState == Player.STATE_ENDED) {
            val mediaId = currentMediaId ?: return
            enqueueCurrentPosition(attachedPlayer ?: return, mediaId)
        }
    }

    override fun onTracksChanged(tracks: Tracks) {
        val mediaId = attachedPlayer?.currentMediaItem?.mediaId ?: return
        var selectedAudioId: String? = null
        var selectedSubtitleId: String? = null
        var hasTextGroup = false

        tracks.groups.forEachIndexed { _, group ->
            if (group.type != C.TRACK_TYPE_AUDIO && group.type != C.TRACK_TYPE_TEXT) return@forEachIndexed
            if (group.type == C.TRACK_TYPE_TEXT) hasTextGroup = true
            val selectedTrackIndex = (0 until group.length).firstOrNull { trackIndex ->
                group.isTrackSelected(trackIndex)
            } ?: return@forEachIndexed

            val stableId = TrackSelectionIdentity.create(
                type = group.type,
                format = group.mediaTrackGroup.getFormat(selectedTrackIndex),
            )
            when (group.type) {
                C.TRACK_TYPE_AUDIO -> if (selectedAudioId == null) selectedAudioId = stableId
                C.TRACK_TYPE_TEXT -> if (selectedSubtitleId == null) selectedSubtitleId = stableId
            }
        }

        selectedAudioId?.let { audioId ->
            dispatchWrite {
                store.saveAudioTrackId(mediaId, audioId)
            }
        }
        if (selectedSubtitleId != null) {
            val subtitleId = selectedSubtitleId
            dispatchWrite {
                store.saveSubtitleTrackId(mediaId, subtitleId)
            }
        } else if (hasTextGroup &&
            attachedPlayer?.trackSelectionParameters?.disabledTrackTypes?.contains(C.TRACK_TYPE_TEXT) == true
        ) {
            dispatchWrite {
                store.saveSubtitleTrackId(mediaId, PersistedTrackSelection.DISABLED_SUBTITLE_ID)
            }
        }
    }

    private fun resolveCurrentMediaId(player: Player): String? {
        return player.currentMediaItem?.mediaId ?: currentMediaId
    }

    private fun saveCurrentPositionAsync(
        player: Player,
        mediaId: String,
    ): Boolean {
        val playbackState = player.playbackState
        if (playbackState == Player.STATE_IDLE || playbackState == Player.STATE_ENDED) {
            return false
        }
        return enqueueCurrentPosition(player, mediaId)
    }

    private suspend fun saveCurrentPositionAwaited(
        player: Player,
        mediaId: String,
    ): Boolean {
        val position = resolveCurrentPosition(player)
        writeQueue.dispatchAndAwait {
            store.savePosition(mediaId, position)
        }
        return true
    }

    private fun enqueueCurrentPosition(
        player: Player,
        mediaId: String,
    ): Boolean {
        val position = resolveCurrentPosition(player)
        writeQueue.dispatch {
            store.savePosition(mediaId, position)
        }
        return true
    }

    private fun resolveCurrentPosition(player: Player): Long {
        return normalizePersistedPosition(
            playbackState = player.playbackState,
            positionMs = player.currentPosition,
            durationMs = player.duration,
        )
    }

    private fun normalizePersistedPosition(
        playbackState: Int,
        positionMs: Long,
        durationMs: Long,
    ): Long {
        val position = if (playbackState == Player.STATE_ENDED) {
            0L
        } else {
            positionMs.coerceAtLeast(0L)
        }
        if (durationMs == C.TIME_UNSET || durationMs <= 0L) return position

        // Treat near-finished media as fully watched so the next launch starts over.
        val remainingMs = (durationMs - position).coerceAtLeast(0L)
        return if (remainingMs < RESUME_RESTART_THRESHOLD_MS) 0L else position
    }

    private fun dispatchWrite(block: suspend () -> Unit) {
        writeQueue.dispatch(block)
    }
}
