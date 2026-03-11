package com.asuka.player.renderer.controller

import androidx.media3.common.C
import com.asuka.player.contract.PersistedTrackSelection
import com.asuka.player.platform.TrackInfoReader
import com.asuka.player.contract.TrackSelectionRestoreRequest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TrackSelectionRestoreControllerTest {
    private fun track(
        groupIndex: Int,
        trackIndex: Int,
        type: Int,
        selectionId: String,
    ) = TrackInfoReader.TrackInfo(
        groupIndex = groupIndex,
        trackIndex = trackIndex,
        type = type,
        label = selectionId,
        language = null,
        selectionId = selectionId,
    )

    @Test
    fun applyIfReady_waitsUntilTracksAreReady() {
        var currentMediaId = "media-1"
        var tracksReady = false
        val tracks = listOf(
            track(groupIndex = 7, trackIndex = 1, type = C.TRACK_TYPE_AUDIO, selectionId = "audio-main"),
            track(groupIndex = 9, trackIndex = 0, type = C.TRACK_TYPE_TEXT, selectionId = "subtitle-main"),
        )
        val audioSelections = mutableListOf<Pair<Int, Int>>()
        val subtitleSelections = mutableListOf<Pair<Int, Int>>()
        var subtitlesDisabled = false
        val controller = TrackSelectionRestoreController(
            currentMediaIdProvider = { currentMediaId },
            tracksReadyProvider = { tracksReady },
            availableTracksProvider = { tracks },
            applyAudioTrack = { groupIndex, trackIndex -> audioSelections += groupIndex to trackIndex },
            applySubtitleTrack = { groupIndex, trackIndex -> subtitleSelections += groupIndex to trackIndex },
            disableSubtitles = { subtitlesDisabled = true },
        )

        controller.schedule(
            TrackSelectionRestoreRequest(
                mediaId = "media-1",
                audioTrackSelection = PersistedTrackSelection("audio-main"),
                subtitleTrackSelection = PersistedTrackSelection("subtitle-main"),
            ),
        )

        assertFalse(controller.applyIfReady())
        assertTrue(audioSelections.isEmpty())
        assertTrue(subtitleSelections.isEmpty())
        assertFalse(subtitlesDisabled)

        tracksReady = true

        assertTrue(controller.applyIfReady())
        assertEquals(listOf(7 to 1), audioSelections)
        assertEquals(listOf(9 to 0), subtitleSelections)
        assertFalse(subtitlesDisabled)
    }

    @Test
    fun applyIfReady_ignoresPendingRestoreForDifferentMedia() {
        var currentMediaId = "media-1"
        val audioSelections = mutableListOf<Pair<Int, Int>>()
        val controller = TrackSelectionRestoreController(
            currentMediaIdProvider = { currentMediaId },
            tracksReadyProvider = { true },
            availableTracksProvider = {
                listOf(track(groupIndex = 4, trackIndex = 2, type = C.TRACK_TYPE_AUDIO, selectionId = "audio-other"))
            },
            applyAudioTrack = { groupIndex, trackIndex -> audioSelections += groupIndex to trackIndex },
            applySubtitleTrack = { _, _ -> error("unexpected subtitle restore") },
            disableSubtitles = { error("unexpected subtitle disable") },
        )

        controller.schedule(
            TrackSelectionRestoreRequest(
                mediaId = "media-2",
                audioTrackSelection = PersistedTrackSelection("audio-other"),
                subtitleTrackSelection = null,
            ),
        )

        assertFalse(controller.applyIfReady())
        assertTrue(audioSelections.isEmpty())

        currentMediaId = "media-2"

        assertTrue(controller.applyIfReady())
        assertEquals(listOf(4 to 2), audioSelections)
    }

    @Test
    fun applyIfReady_restoresDisabledSubtitlesSentinel() {
        val audioSelections = mutableListOf<Pair<Int, Int>>()
        val subtitleSelections = mutableListOf<Pair<Int, Int>>()
        var subtitlesDisabled = false
        val controller = TrackSelectionRestoreController(
            currentMediaIdProvider = { "media-1" },
            tracksReadyProvider = { true },
            availableTracksProvider = { emptyList() },
            applyAudioTrack = { groupIndex, trackIndex -> audioSelections += groupIndex to trackIndex },
            applySubtitleTrack = { groupIndex, trackIndex -> subtitleSelections += groupIndex to trackIndex },
            disableSubtitles = { subtitlesDisabled = true },
        )

        controller.schedule(
            TrackSelectionRestoreRequest(
                mediaId = "media-1",
                audioTrackSelection = null,
                subtitleTrackSelection = PersistedTrackSelection.disabledSubtitle(),
            ),
        )

        assertTrue(controller.applyIfReady())
        assertTrue(audioSelections.isEmpty())
        assertTrue(subtitleSelections.isEmpty())
        assertTrue(subtitlesDisabled)
    }

    @Test
    fun applyIfReady_matchesStableIdsEvenWhenTrackPositionsChange() {
        val audioSelections = mutableListOf<Pair<Int, Int>>()
        val subtitleSelections = mutableListOf<Pair<Int, Int>>()
        val controller = TrackSelectionRestoreController(
            currentMediaIdProvider = { "media-1" },
            tracksReadyProvider = { true },
            availableTracksProvider = {
                listOf(
                    track(groupIndex = 5, trackIndex = 3, type = C.TRACK_TYPE_AUDIO, selectionId = "audio-main"),
                    track(groupIndex = 8, trackIndex = 1, type = C.TRACK_TYPE_TEXT, selectionId = "subtitle-main"),
                )
            },
            applyAudioTrack = { groupIndex, trackIndex -> audioSelections += groupIndex to trackIndex },
            applySubtitleTrack = { groupIndex, trackIndex -> subtitleSelections += groupIndex to trackIndex },
            disableSubtitles = { error("unexpected subtitle disable") },
        )

        controller.schedule(
            TrackSelectionRestoreRequest(
                mediaId = "media-1",
                audioTrackSelection = PersistedTrackSelection("audio-main"),
                subtitleTrackSelection = PersistedTrackSelection("subtitle-main"),
            ),
        )

        assertTrue(controller.applyIfReady())
        assertEquals(listOf(5 to 3), audioSelections)
        assertEquals(listOf(8 to 1), subtitleSelections)
    }
}
