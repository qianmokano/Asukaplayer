package com.asuka.player.core

import androidx.media3.common.C
import androidx.media3.common.Player
import com.asuka.player.data.PlaybackStore

/**
 * Attaches to Player events and writes playback state into the store.
 * This is a lightweight, clean-room alternative and can be replaced by a use-case layer.
 *
 * **Thread safety:** All Player.Listener callbacks are delivered on the application's main thread.
 * This class must only be used on the main thread.
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
        val player = attachedPlayer ?: return
        // During media item transitions the player briefly stops playing. At that point
        // currentMediaId already points to the *new* item (updated by onMediaItemTransition),
        // so saving the position here would associate a stale/zero position with the new
        // mediaId. Skip the save when the player is between items (IDLE/ENDED) or buffering
        // the next item.
        val state = player.playbackState
        if (state == Player.STATE_IDLE || state == Player.STATE_ENDED) return
        val mediaId = currentMediaId ?: return
        val position = player.currentPosition
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
        if (reason == Player.DISCONTINUITY_REASON_AUTO_TRANSITION) return
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
        // Guard: onTracksChanged can fire before onMediaItemTransition for the same item,
        // causing tracks to be saved against the wrong mediaId. Use the player's current
        // media item directly to guarantee we write to the correct key.
        val mediaId = attachedPlayer?.currentMediaItem?.mediaId ?: return
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
            store.saveSubtitleTrack(mediaId, TrackIndexCodec.SUBTITLE_DISABLED)
        }
    }
}
