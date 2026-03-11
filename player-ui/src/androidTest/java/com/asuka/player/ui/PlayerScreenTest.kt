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
        composeRule.onNodeWithTag("btn_speed").assertExists()
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
    fun middleControls_areVerticallyCenteredInViewport() {
        composeRule.setContent {
            PlayerScreen(
                model = testPlaybackScreenModel(uiState = PlayerUiState(title = "Test")),
                dependencies = testPlaybackScreenDependencies(),
                onBack = {},
                onPip = {},
                onBackground = {},
            )
        }

        val rootCenterY = composeRule.onRoot().fetchSemanticsNode().boundsInRoot.center.y
        val playPauseCenterY = composeRule.onNodeWithTag("btn_play_pause").fetchSemanticsNode().boundsInRoot.center.y

        assertTrue(abs(rootCenterY - playPauseCenterY) < 2f)
    }
}
