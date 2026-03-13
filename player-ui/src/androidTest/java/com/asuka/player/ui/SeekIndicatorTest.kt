package com.asuka.player.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.asuka.player.ui.components.SeekIndicator
import com.asuka.player.ui.state.SeekState
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

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
                mediaId = null,
                durationMs = 120_000L,
                previewFrameProvider = null,
            )
        }

        composeRule.onNodeWithTag("seek_indicator_hud").assertExists()
        assertTrue(composeRule.onAllNodesWithTag("seek_indicator_preview").fetchSemanticsNodes().isEmpty())
        composeRule.onNodeWithText("00:15 / 02:00").assertExists()
    }
}
