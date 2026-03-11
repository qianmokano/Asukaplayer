package com.asuka.player.platform

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import com.asuka.player.contract.PersistedTrackSelection
import com.asuka.player.contract.PlaybackStore

@OptIn(UnstableApi::class)
class PlaybackStateWriter(
    private val store: PlaybackStore,
) : Player.Listener {
    companion object {
        const val POSITION_CHECKPOINT_INTERVAL_MS = 5_000L

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

    fun checkpoint(nowMs: Long, minIntervalMs: Long = POSITION_CHECKPOINT_INTERVAL_MS): Boolean {
        val player = attachedPlayer ?: return false
        if (!player.isPlaying) return false
        val mediaId = resolveCurrentMediaId(player) ?: return false
        val lastSavedForSameMedia = lastCheckpointMediaId == mediaId
        if (lastSavedForSameMedia && nowMs - lastCheckpointRealtimeMs < minIntervalMs) {
            return false
        }
        if (!saveCurrentPosition(player, mediaId, force = false)) return false
        lastCheckpointMediaId = mediaId
        lastCheckpointRealtimeMs = nowMs
        return true
    }

    fun flushCurrentPosition(): Boolean {
        val player = attachedPlayer ?: return false
        val mediaId = resolveCurrentMediaId(player) ?: return false
        return saveCurrentPosition(player, mediaId, force = true)
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        val player = attachedPlayer ?: return
        val state = player.playbackState
        if (!shouldSavePositionOnPause(isPlaying = isPlaying, playbackState = state)) return
        val mediaId = currentMediaId ?: return
        saveCurrentPosition(player, mediaId, force = false)
    }

    override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
        currentMediaId = mediaItem?.mediaId
        lastCheckpointMediaId = null
        lastCheckpointRealtimeMs = Long.MIN_VALUE
    }

    override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
        val mediaId = currentMediaId ?: return
        store.savePlaybackSpeed(mediaId, playbackParameters.speed)
    }

    override fun onPositionDiscontinuity(
        oldPosition: Player.PositionInfo,
        newPosition: Player.PositionInfo,
        reason: Int,
    ) {
        if (reason == Player.DISCONTINUITY_REASON_AUTO_TRANSITION) return
        val mediaId = newPosition.mediaItem?.mediaId ?: oldPosition.mediaItem?.mediaId ?: return
        store.savePosition(mediaId, newPosition.positionMs)
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        if (playbackState == Player.STATE_ENDED) {
            val mediaId = currentMediaId ?: return
            saveCurrentPosition(attachedPlayer ?: return, mediaId, force = true)
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

        selectedAudioId?.let { store.saveAudioTrackId(mediaId, it) }
        if (selectedSubtitleId != null) {
            store.saveSubtitleTrackId(mediaId, selectedSubtitleId)
        } else if (hasTextGroup &&
            attachedPlayer?.trackSelectionParameters?.disabledTrackTypes?.contains(C.TRACK_TYPE_TEXT) == true
        ) {
            store.saveSubtitleTrackId(mediaId, PersistedTrackSelection.DISABLED_SUBTITLE_ID)
        }
    }

    private fun resolveCurrentMediaId(player: Player): String? {
        return player.currentMediaItem?.mediaId ?: currentMediaId
    }

    private fun saveCurrentPosition(
        player: Player,
        mediaId: String,
        force: Boolean,
    ): Boolean {
        val playbackState = player.playbackState
        if (!force && (playbackState == Player.STATE_IDLE || playbackState == Player.STATE_ENDED)) {
            return false
        }
        val position = if (playbackState == Player.STATE_ENDED) {
            0L
        } else {
            player.currentPosition.coerceAtLeast(0L)
        }
        store.savePosition(mediaId, position)
        return true
    }
}
