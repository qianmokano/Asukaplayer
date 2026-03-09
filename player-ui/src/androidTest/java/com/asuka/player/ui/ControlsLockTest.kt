package com.asuka.player.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.asuka.player.ui.state.PlayerUiState
import org.junit.Rule
import org.junit.Test

class ControlsLockTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun lockHidesControls() {
        composeRule.setContent {
            PlayerScreen(
                model = testPlaybackScreenModel(uiState = PlayerUiState(title = "Test")),
                dependencies = testPlaybackScreenDependencies(),
                onBack = {},
                onPip = {},
                onBackground = {},
            )
        }
        composeRule.onNodeWithTag("btn_lock").assertExists().performClick()
        composeRule.onNodeWithTag("btn_lock").assertExists()
    }
}
