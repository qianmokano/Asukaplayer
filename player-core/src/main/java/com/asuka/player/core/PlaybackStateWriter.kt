package com.asuka.player.core

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import com.asuka.player.data.PlaybackStore

/**
 * Attaches to Player events and writes playback state into the store.
 * This is a lightweight, clean-room alternative and can be replaced by a use-case layer.
 *
 * **Thread safety:** All Player.Listener callbacks are delivered on the application's main thread.
 * This class must only be used on the main thread.
 */
@OptIn(UnstableApi::class)
class PlaybackStateWriter(
    private val store: PlaybackStore,
) : Player.Listener {

    companion object {
        internal fun shouldSavePositionOnPause(isPlaying: Boolean, playbackState: Int): Boolean {
            if (isPlaying) return false
            return playbackState != Player.STATE_IDLE &&
                playbackState != Player.STATE_ENDED &&
                playbackState != Player.STATE_BUFFERING
        }
    }

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
        val player = attachedPlayer ?: return
        // During media item transitions the player briefly stops playing. At that point
        // currentMediaId already points to the *new* item (updated by onMediaItemTransition),
        // so saving the position here would associate a stale/zero position with the new
        // mediaId. Skip the save when the player is between items (IDLE/ENDED) or buffering
        // the next item.
        val state = player.playbackState
        if (!shouldSavePositionOnPause(isPlaying = isPlaying, playbackState = state)) return
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
        val mediaId = newPosition.mediaItem?.mediaId ?: oldPosition.mediaItem?.mediaId ?: return
        store.savePosition(mediaId, newPosition.positionMs)
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        if (playbackState == Player.STATE_ENDED) {
            val mediaId = currentMediaId ?: return
            store.savePosition(mediaId, 0L)
        }
    }

    override fun onTracksChanged(tracks: androidx.media3.common.Tracks) {
        // Why read from attachedPlayer?.currentMediaItem?.mediaId instead of currentMediaId:
        //
        // Media3 does NOT guarantee that onMediaItemTransition fires before onTracksChanged
        // when the media item changes. If onTracksChanged fires first:
        //   - currentMediaId still points to the OLD item (not yet updated)
        //   - attachedPlayer.currentMediaItem already reflects the NEW item
        //   - the `tracks` parameter already belongs to the NEW item
        // Using currentMediaId here would associate new-item tracks with the old mediaId.
        //
        // By reading directly from the player we get the mediaId that matches `tracks`,
        // regardless of the onMediaItemTransition ordering. In the converse case where
        // onMediaItemTransition fires first, both sources agree, so the result is the same.
        val mediaId = attachedPlayer?.currentMediaItem?.mediaId ?: return
        var selectedAudioId: String? = null
        var selectedSubtitleId: String? = null
        var hasTextGroup = false

        tracks.groups.forEachIndexed { groupIndex, group ->
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
}
