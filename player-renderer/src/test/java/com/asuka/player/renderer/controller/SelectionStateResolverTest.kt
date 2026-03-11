package com.asuka.player.renderer.controller

import androidx.media3.common.C
import com.asuka.player.contract.TrackIndexCodec
import com.asuka.player.platform.TrackSelectionStateReader
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SelectionStateResolverTest {

    @Test
    fun subtitleSelection_returnsDisabledSentinelWhenTextTracksAreDisabled() {
        val resolved = SelectionStateResolver.subtitleSelection(
            selected = null,
            subtitlesDisabled = true,
            hasTextTracks = true,
        )

        assertEquals(TrackIndexCodec.SUBTITLE_DISABLED, resolved)
    }

    @Test
    fun subtitleSelection_prefersExplicitTrackOverride() {
        val selected = TrackSelectionStateReader.Selected(
            type = C.TRACK_TYPE_TEXT,
            groupIndex = 2,
            trackIndex = 1,
        )

        val resolved = SelectionStateResolver.subtitleSelection(
            selected = selected,
            subtitlesDisabled = true,
            hasTextTracks = true,
        )

        assertEquals(TrackIndexCodec.encode(2, 1), resolved)
    }

    @Test
    fun subtitleSelection_returnsNullWithoutTracksOrDisableFlag() {
        val resolved = SelectionStateResolver.subtitleSelection(
            selected = null,
            subtitlesDisabled = false,
            hasTextTracks = false,
        )

        assertNull(resolved)
    }

    @Test
    fun audioSelection_encodesExplicitOverride() {
        val selected = TrackSelectionStateReader.Selected(
            type = C.TRACK_TYPE_AUDIO,
            groupIndex = 1,
            trackIndex = 3,
        )

        assertEquals(TrackIndexCodec.encode(1, 3), SelectionStateResolver.audioSelection(selected))
    }
}
