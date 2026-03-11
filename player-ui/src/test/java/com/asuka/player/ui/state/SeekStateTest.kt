package com.asuka.player.ui.state

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SeekStateTest {

    @Test
    fun startAndUpdate_trackPreviewPosition() {
        val state = SeekState()

        state.start(12_000L)
        state.update(delta = 3_000L, previewPositionMs = 15_000L)

        assertTrue(state.seeking)
        assertEquals(3_000L, state.deltaMs)
        assertEquals(15_000L, state.previewPositionMs)
    }

    @Test
    fun end_clearsSeekPreview() {
        val state = SeekState()

        state.start(12_000L)
        state.update(delta = -2_000L, previewPositionMs = 10_000L)
        state.end()

        assertFalse(state.seeking)
        assertEquals(0L, state.deltaMs)
        assertEquals(0L, state.previewPositionMs)
    }
}
