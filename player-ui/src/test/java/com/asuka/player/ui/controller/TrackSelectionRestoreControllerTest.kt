package com.asuka.player.ui.controller

import com.asuka.player.core.TrackIndexCodec
import com.asuka.player.core.TrackSelectionRestoreRequest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TrackSelectionRestoreControllerTest {

    @Test
    fun applyIfReady_waitsUntilTrackGroupsExist() {
        var currentMediaId = "media-1"
        var trackGroupCount = 0
        val audioSelections = mutableListOf<Pair<Int, Int>>()
        val subtitleSelections = mutableListOf<Pair<Int, Int>>()
        var subtitlesDisabled = false
        val controller = TrackSelectionRestoreController(
            currentMediaIdProvider = { currentMediaId },
            trackGroupCountProvider = { trackGroupCount },
            applyAudioTrack = { groupIndex, trackIndex -> audioSelections += groupIndex to trackIndex },
            applySubtitleTrack = { groupIndex, trackIndex -> subtitleSelections += groupIndex to trackIndex },
            disableSubtitles = { subtitlesDisabled = true },
        )

        controller.schedule(
            TrackSelectionRestoreRequest(
                mediaId = "media-1",
                audioTrackIndex = TrackIndexCodec.encode(1, 2),
                subtitleTrackIndex = TrackIndexCodec.encode(3, 4),
            ),
        )

        assertFalse(controller.applyIfReady())
        assertTrue(audioSelections.isEmpty())
        assertTrue(subtitleSelections.isEmpty())
        assertFalse(subtitlesDisabled)

        trackGroupCount = 4

        assertTrue(controller.applyIfReady())
        assertEquals(listOf(1 to 2), audioSelections)
        assertEquals(listOf(3 to 4), subtitleSelections)
        assertFalse(subtitlesDisabled)
    }

    @Test
    fun applyIfReady_ignoresPendingRestoreForDifferentMedia() {
        var currentMediaId = "media-1"
        val audioSelections = mutableListOf<Pair<Int, Int>>()
        val controller = TrackSelectionRestoreController(
            currentMediaIdProvider = { currentMediaId },
            trackGroupCountProvider = { 2 },
            applyAudioTrack = { groupIndex, trackIndex -> audioSelections += groupIndex to trackIndex },
            applySubtitleTrack = { _, _ -> error("unexpected subtitle restore") },
            disableSubtitles = { error("unexpected subtitle disable") },
        )

        controller.schedule(
            TrackSelectionRestoreRequest(
                mediaId = "media-2",
                audioTrackIndex = TrackIndexCodec.encode(0, 1),
                subtitleTrackIndex = null,
            ),
        )

        assertFalse(controller.applyIfReady())
        assertTrue(audioSelections.isEmpty())

        currentMediaId = "media-2"

        assertTrue(controller.applyIfReady())
        assertEquals(listOf(0 to 1), audioSelections)
    }

    @Test
    fun applyIfReady_restoresDisabledSubtitlesSentinel() {
        val audioSelections = mutableListOf<Pair<Int, Int>>()
        val subtitleSelections = mutableListOf<Pair<Int, Int>>()
        var subtitlesDisabled = false
        val controller = TrackSelectionRestoreController(
            currentMediaIdProvider = { "media-1" },
            trackGroupCountProvider = { 1 },
            applyAudioTrack = { groupIndex, trackIndex -> audioSelections += groupIndex to trackIndex },
            applySubtitleTrack = { groupIndex, trackIndex -> subtitleSelections += groupIndex to trackIndex },
            disableSubtitles = { subtitlesDisabled = true },
        )

        controller.schedule(
            TrackSelectionRestoreRequest(
                mediaId = "media-1",
                audioTrackIndex = null,
                subtitleTrackIndex = TrackIndexCodec.SUBTITLE_DISABLED,
            ),
        )

        assertTrue(controller.applyIfReady())
        assertTrue(audioSelections.isEmpty())
        assertTrue(subtitleSelections.isEmpty())
        assertTrue(subtitlesDisabled)
    }
}
