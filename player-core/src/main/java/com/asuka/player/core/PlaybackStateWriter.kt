package com.asuka.player.core

import androidx.media3.common.C
import androidx.media3.common.Player
import com.asuka.player.data.PlaybackStore

/**
 * Attaches to Player events and writes playback state into the store.
 * This is a lightweight, clean-room alternative and can be replaced by a use-case layer.
 */
class PlaybackStateWriter(
    private val store: PlaybackStore,
) : Player.Listener {

    private var currentMediaId: String? = null
    private var attachedPlayer: Player? = null

    fun attach(player: Player) {
        attachedPlayer = player
        currentMediaId = player.currentMediaItem?.mediaId
        player.addListener(this)
    }

    fun detach(player: Player) {
        player.removeListener(this)
        attachedPlayer = null
        currentMediaId = null
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        if (isPlaying) return
        val mediaId = currentMediaId ?: return
        val position = attachedPlayer?.currentPosition ?: return
        store.savePosition(mediaId, position)
    }

    override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
        currentMediaId = mediaItem?.mediaId
    }

    override fun onPlaybackParametersChanged(playbackParameters: androidx.media3.common.PlaybackParameters) {
        val mediaId = currentMediaId ?: return
        store.savePlaybackSpeed(mediaId, playbackParameters.speed)
    }

    override fun onPositionDiscontinuity(
        oldPosition: Player.PositionInfo,
        newPosition: Player.PositionInfo,
        reason: Int,
    ) {
        val mediaId = oldPosition.mediaItem?.mediaId ?: return
        store.savePosition(mediaId, oldPosition.positionMs)
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        if (playbackState == Player.STATE_ENDED) {
            val mediaId = currentMediaId ?: return
            store.savePosition(mediaId, 0L)
        }
    }

    override fun onTracksChanged(tracks: androidx.media3.common.Tracks) {
        val mediaId = currentMediaId ?: return
        var selectedAudio: Int? = null
        var selectedSubtitle: Int? = null
        var hasTextGroup = false

        tracks.groups.forEachIndexed { groupIndex, group ->
            if (group.type != C.TRACK_TYPE_AUDIO && group.type != C.TRACK_TYPE_TEXT) return@forEachIndexed
            if (group.type == C.TRACK_TYPE_TEXT) hasTextGroup = true
            val selectedTrackIndex = (0 until group.length).firstOrNull { trackIndex ->
                group.isTrackSelected(trackIndex)
            } ?: return@forEachIndexed

            val encoded = TrackIndexCodec.encode(groupIndex, selectedTrackIndex)
            when (group.type) {
                C.TRACK_TYPE_AUDIO -> if (selectedAudio == null) selectedAudio = encoded
                C.TRACK_TYPE_TEXT -> if (selectedSubtitle == null) selectedSubtitle = encoded
            }
        }

        selectedAudio?.let { store.saveAudioTrack(mediaId, it) }
        if (selectedSubtitle != null) {
            store.saveSubtitleTrack(mediaId, selectedSubtitle)
        } else if (hasTextGroup &&
            attachedPlayer?.trackSelectionParameters?.disabledTrackTypes?.contains(C.TRACK_TYPE_TEXT) == true
        ) {
            store.saveSubtitleTrack(mediaId, -1)
        }
    }
}
