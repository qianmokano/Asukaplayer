package com.asuka.player.core

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi

/**
 * Minimal facade to set audio/subtitle track indexes via TrackSelectionParameters.
 *
 * **Null semantics differ intentionally between audio and subtitle:**
 * - [setAudioTrack]`(null, null)` — clears any manual override and restores **auto-selection**.
 *   Audio has no meaningful "off" state in most content, so null means "let the player choose".
 * - [setSubtitleTrack]`(null, null)` — clears any override and **disables** the text track type.
 *   Subtitles are off by default; null means "no subtitles".
 *
 * The asymmetry mirrors the real-world default: a user who hasn't touched subtitles expects none,
 * whereas a user who hasn't touched audio expects the best available audio stream.
 */
@OptIn(UnstableApi::class)
class TrackSelectionFacade(private val player: Player) {

    /** Sets a specific audio track override. Pass `null` for both params to restore auto-selection. */
    fun setAudioTrack(groupIndex: Int?, trackIndex: Int?) {
        val builder = player.trackSelectionParameters.buildUpon()
        builder.clearOverridesOfType(C.TRACK_TYPE_AUDIO)
        if (groupIndex == null || trackIndex == null) {
            player.trackSelectionParameters = builder.build()
            return
        }
        val group = player.currentTracks.groups.getOrNull(groupIndex)?.mediaTrackGroup
        if (group == null) {
            // Requested group no longer exists; apply the cleared-overrides state as-is.
            player.trackSelectionParameters = builder.build()
            return
        }
        if (trackIndex !in 0 until group.length) {
            // Requested track no longer exists; apply the cleared-overrides state as-is.
            builder.setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)
            player.trackSelectionParameters = builder.build()
            return
        }
        val override = androidx.media3.common.TrackSelectionOverride(group, listOf(trackIndex))
        builder.addOverride(override)
        builder.setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)
        player.trackSelectionParameters = builder.build()
    }

    /** Sets a specific subtitle track override. Pass `null` for both params to **disable** subtitles. */
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
            // Requested group no longer exists; apply the cleared-overrides state as-is.
            player.trackSelectionParameters = builder.build()
            return
        }
        if (trackIndex !in 0 until group.length) {
            // Requested track no longer exists; apply the cleared-overrides state as-is.
            builder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
            player.trackSelectionParameters = builder.build()
            return
        }
        val override = androidx.media3.common.TrackSelectionOverride(group, listOf(trackIndex))
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
