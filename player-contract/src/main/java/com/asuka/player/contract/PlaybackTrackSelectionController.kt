package com.asuka.player.contract

interface PlaybackTrackSelectionController {
    fun setAudioTrack(groupIndex: Int, trackIndex: Int)
    fun setSubtitleTrack(groupIndex: Int, trackIndex: Int)
    fun disableSubtitles()
}
