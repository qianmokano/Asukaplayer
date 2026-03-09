package com.asuka.player.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.asuka.player.ui.state.PlayerUiState
import org.junit.Rule
import org.junit.Test

class ErrorOverlayTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun errorOverlayButtonsExist() {
        composeRule.setContent {
            PlayerScreen(
                uiState = PlayerUiState(title = "Test", errorMessage = "Error"),
                player = null,
                controller = TestPlaybackController,
                bindings = null,
                playbackStateRepository = testPlaybackStateRepository(),
                onBack = {},
                onPip = {},
                onBackground = {},
            )
        }
        composeRule.onNodeWithTag("err_retry").assertExists()
        composeRule.onNodeWithTag("err_next").assertExists()
        composeRule.onNodeWithTag("err_dismiss").assertExists()
    }
}
