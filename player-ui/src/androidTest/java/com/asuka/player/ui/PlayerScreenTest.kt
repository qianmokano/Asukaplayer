package com.asuka.player.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import com.asuka.player.ui.state.PlayerUiState
import kotlin.math.abs
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PlayerScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun controlsVisible_onInitialRender() {
        composeRule.setContent {
            PlayerScreen(
                model = testPlaybackScreenModel(uiState = PlayerUiState(title = "Test")),
                dependencies = testPlaybackScreenDependencies(),
                onBack = {},
                onPip = {},
                onBackground = {},
            )
        }
        composeRule.onNodeWithTag("btn_settings").assertExists()
    }

    @Test
    fun overlayOpens_onSpeedClick() {
        composeRule.setContent {
            PlayerScreen(
                model = testPlaybackScreenModel(uiState = PlayerUiState(title = "Test")),
                dependencies = testPlaybackScreenDependencies(),
                onBack = {},
                onPip = {},
                onBackground = {},
            )
        }
        composeRule.onNodeWithTag("btn_speed").assertExists().performClick()
        composeRule.waitForIdle()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag("overlay_root").fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun settingsOverlayOpens_onSettingsClick() {
        composeRule.setContent {
            PlayerScreen(
                model = testPlaybackScreenModel(uiState = PlayerUiState(title = "Test")),
                dependencies = testPlaybackScreenDependencies(),
                onBack = {},
                onPip = {},
                onBackground = {},
            )
        }
        composeRule.onNodeWithTag("btn_settings").assertExists().performClick()
        composeRule.waitForIdle()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag("overlay_root").fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun settingsOverlayNavigates_toLoopModePanel() {
        composeRule.setContent {
            PlayerScreen(
                model = testPlaybackScreenModel(uiState = PlayerUiState(title = "Test")),
                dependencies = testPlaybackScreenDependencies(),
                onBack = {},
                onPip = {},
                onBackground = {},
            )
        }

        composeRule.onNodeWithTag("btn_settings").assertExists().performClick()
        composeRule.onNodeWithTag("settings_menu_loop_mode").assertExists().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("overlay_root").assertExists()
    }

    @Test
    fun bottomBarHostsPlaybackControlsOnLeft() {
        composeRule.setContent {
            PlayerScreen(
                model = testPlaybackScreenModel(uiState = PlayerUiState(title = "Test")),
                dependencies = testPlaybackScreenDependencies(),
                onBack = {},
                onPip = {},
                onBackground = {},
            )
        }

        val rootBounds = composeRule.onRoot().fetchSemanticsNode().boundsInRoot
        val playPauseCenter = composeRule.onNodeWithTag("btn_play_pause").fetchSemanticsNode().boundsInRoot.center
        val nextCenter = composeRule.onNodeWithTag("btn_next").fetchSemanticsNode().boundsInRoot.center

        composeRule.onNodeWithTag("btn_prev").assertDoesNotExist()
        assertTrue(playPauseCenter.y > rootBounds.center.y)
        assertTrue(nextCenter.x > playPauseCenter.x)
    }

    @Test
    fun subtitleSpeedAndRotateButtons_areRightAligned_withRotateAtFarRight() {
        composeRule.setContent {
            PlayerScreen(
                model = testPlaybackScreenModel(uiState = PlayerUiState(title = "Test")),
                dependencies = testPlaybackScreenDependencies(),
                onBack = {},
                onPip = {},
                onBackground = {},
            )
        }

        val rootBounds = composeRule.onRoot().fetchSemanticsNode().boundsInRoot
        val nextCenter = composeRule.onNodeWithTag("btn_next").fetchSemanticsNode().boundsInRoot.center
        val subsCenter = composeRule.onNodeWithTag("btn_subs").fetchSemanticsNode().boundsInRoot.center
        val speedCenter = composeRule.onNodeWithTag("btn_speed").fetchSemanticsNode().boundsInRoot.center
        val rotateCenter = composeRule.onNodeWithTag("btn_rotate").fetchSemanticsNode().boundsInRoot.center

        assertTrue(subsCenter.x > rootBounds.center.x)
        assertTrue(speedCenter.x > subsCenter.x)
        assertTrue(rotateCenter.x > speedCenter.x)
        assertTrue(subsCenter.x > nextCenter.x)
    }

    @Test
    fun bufferingShowsLoadingRingAroundPlayPauseButton() {
        composeRule.setContent {
            PlayerScreen(
                model = testPlaybackScreenModel(
                    uiState = PlayerUiState(
                        title = "Test",
                        isBuffering = true,
                    ),
                ),
                dependencies = testPlaybackScreenDependencies(),
                onBack = {},
                onPip = {},
                onBackground = {},
            )
        }

        composeRule.onNodeWithTag("play_pause_loading_ring").assertExists()
    }
}
