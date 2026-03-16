package com.asuka.player.ui

import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.asuka.player.ui.controller.QueueActions
import com.asuka.player.ui.state.ControlsState
import com.asuka.player.ui.state.PlayerUiState
import kotlin.math.abs
import kotlin.time.Duration.Companion.seconds
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class GestureSeekOverlayTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun seekOnlyLayout_keepsSeekBarPositionAndHidesControlContent() {
        var normalSeekBarCenterY = 0f

        composeRule.setContent {
            val scope = rememberCoroutineScope()
            val controlsState = remember { ControlsState(scope = scope, autoHideDelay = 3.seconds) }
            PlayerScreenLayoutShell(
                controlsVisible = true,
                gestureSeekOverlayVisible = false,
                isInPip = false,
                settings = testPlaybackScreenModel().settings,
                uiState = PlayerUiState(
                    title = "Test",
                    durationMs = 120_000L,
                    positionMs = 30_000L,
                ),
                controller = TestPlaybackController,
                landscapeCutoutPadding = LandscapeCutoutPadding.None,
                displayedPositionMs = 45_000L,
                queueActions = QueueActions(TestPlaybackController),
                onBack = {},
                onPip = {},
                onBackground = {},
                onOpenOverlay = {},
                onRotate = {},
                controlsState = controlsState,
                onProgressBarSeekStart = {},
                onProgressBarSeekPreview = { _, _ -> },
                onProgressBarSeekEnd = {},
            )
        }
        composeRule.waitForIdle()
        normalSeekBarCenterY = composeRule.onNodeWithTag("bottom_seek_bar").fetchSemanticsNode().boundsInRoot.center.y

        composeRule.setContent {
            val scope = rememberCoroutineScope()
            val controlsState = remember { ControlsState(scope = scope, autoHideDelay = 3.seconds) }
            PlayerScreenLayoutShell(
                controlsVisible = true,
                gestureSeekOverlayVisible = true,
                isInPip = false,
                settings = testPlaybackScreenModel().settings,
                uiState = PlayerUiState(
                    title = "Test",
                    durationMs = 120_000L,
                    positionMs = 30_000L,
                ),
                controller = TestPlaybackController,
                landscapeCutoutPadding = LandscapeCutoutPadding.None,
                displayedPositionMs = 45_000L,
                queueActions = QueueActions(TestPlaybackController),
                onBack = {},
                onPip = {},
                onBackground = {},
                onOpenOverlay = {},
                onRotate = {},
                controlsState = controlsState,
                onProgressBarSeekStart = {},
                onProgressBarSeekPreview = { _, _ -> },
                onProgressBarSeekEnd = {},
            )
        }
        composeRule.waitForIdle()

        val seekOnlySeekBarCenterY = composeRule.onNodeWithTag("bottom_seek_bar").fetchSemanticsNode().boundsInRoot.center.y
        composeRule.onNodeWithTag("bottom_seek_bar").assertExists()
        composeRule.onNodeWithTag("btn_settings").assertDoesNotExist()
        composeRule.onNodeWithTag("btn_play_pause").assertDoesNotExist()
        composeRule.onNodeWithTag("btn_rotate").assertDoesNotExist()
        assertTrue(abs(normalSeekBarCenterY - seekOnlySeekBarCenterY) < 2f)
    }
}
