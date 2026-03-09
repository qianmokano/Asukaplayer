package com.asuka.player.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.asuka.player.ui.state.PlayerUiState
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
}
