package com.asuka.player.platform

import androidx.media3.common.Player
import com.asuka.player.contract.PlaybackTrackSelectionController

class DefaultPlaybackTrackSelectionController(player: Player) : PlaybackTrackSelectionController {
    private val trackSelectionFacade = TrackSelectionFacade(player)

    override fun setAudioTrack(groupIndex: Int, trackIndex: Int) {
        trackSelectionFacade.setAudioTrack(groupIndex, trackIndex)
    }

    override fun setSubtitleTrack(groupIndex: Int, trackIndex: Int) {
        trackSelectionFacade.setSubtitleTrack(groupIndex, trackIndex)
    }

    override fun disableSubtitles() {
        trackSelectionFacade.disableSubtitles()
    }
}
