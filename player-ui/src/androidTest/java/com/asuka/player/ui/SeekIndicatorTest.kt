package com.asuka.player.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.asuka.player.contract.PlaybackPreviewFrameProvider
import com.asuka.player.ui.components.SeekIndicator
import com.asuka.player.ui.state.SeekState
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.atomic.AtomicReference

class SeekIndicatorTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun seekIndicator_showsPreviewTimeAndDurationInHud() {
        val state = SeekState().apply {
            start(initialPositionMs = 10_000L)
            update(delta = 5_000L, previewPositionMs = 15_000L)
        }

        composeRule.setContent {
            SeekIndicator(
                seekState = state,
                playbackUri = null,
                durationMs = 120_000L,
                previewFrameProvider = null,
            )
        }

        composeRule.onNodeWithTag("seek_indicator_hud").assertExists()
        assertTrue(composeRule.onAllNodesWithTag("seek_indicator_preview").fetchSemanticsNodes().isEmpty())
        composeRule.onNodeWithText("00:15 / 02:00").assertExists()
    }

    @Test
    fun seekIndicator_requestsPreviewUsingPlaybackUri() {
        val state = SeekState().apply {
            start(initialPositionMs = 10_000L)
            update(delta = 5_000L, previewPositionMs = 15_000L)
        }
        val requestedPlaybackUri = AtomicReference<String?>(null)

        composeRule.setContent {
            SeekIndicator(
                seekState = state,
                playbackUri = "content://videos/current.mp4",
                durationMs = 120_000L,
                previewFrameProvider = object : PlaybackPreviewFrameProvider {
                    override suspend fun loadPreviewFrame(
                        playbackUri: String,
                        positionMs: Long,
                        maxWidthPx: Int,
                        maxHeightPx: Int,
                    ): ByteArray? {
                        requestedPlaybackUri.set(playbackUri)
                        return null
                    }
                },
            )
        }

        composeRule.waitForIdle()

        org.junit.Assert.assertEquals(
            "content://videos/current.mp4",
            requestedPlaybackUri.get(),
        )
    }
}
