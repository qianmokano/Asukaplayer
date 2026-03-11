package com.asuka.player.platform

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.util.UnstableApi

@OptIn(UnstableApi::class)
class TrackSelectionFacade(private val player: Player) {
    fun setAudioTrack(groupIndex: Int?, trackIndex: Int?) {
        val builder = player.trackSelectionParameters.buildUpon()
        builder.clearOverridesOfType(C.TRACK_TYPE_AUDIO)
        if (groupIndex == null || trackIndex == null) {
            player.trackSelectionParameters = builder.build()
            return
        }
        val group = player.currentTracks.groups.getOrNull(groupIndex)?.mediaTrackGroup
        if (group == null) {
            player.trackSelectionParameters = builder.build()
            return
        }
        if (trackIndex !in 0 until group.length) {
            builder.setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)
            player.trackSelectionParameters = builder.build()
            return
        }
        val override = TrackSelectionOverride(group, listOf(trackIndex))
        builder.addOverride(override)
        builder.setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)
        player.trackSelectionParameters = builder.build()
    }

    fun setSubtitleTrack(groupIndex: Int?, trackIndex: Int?) {
        val builder = player.trackSelectionParameters.buildUpon()
        builder.clearOverridesOfType(C.TRACK_TYPE_TEXT)
        if (groupIndex == null || trackIndex == null) {
            builder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
            player.trackSelectionParameters = builder.build()
            return
        }
        val group = player.currentTracks.groups.getOrNull(groupIndex)?.mediaTrackGroup
        if (group == null) {
            player.trackSelectionParameters = builder.build()
            return
        }
        if (trackIndex !in 0 until group.length) {
            builder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
            player.trackSelectionParameters = builder.build()
            return
        }
        val override = TrackSelectionOverride(group, listOf(trackIndex))
        builder.addOverride(override)
        builder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
        player.trackSelectionParameters = builder.build()
    }

    fun disableSubtitles() {
        val builder = player.trackSelectionParameters.buildUpon()
        builder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
        player.trackSelectionParameters = builder.build()
    }
}
