package com.asuka.player.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.performClick
import com.asuka.player.ui.state.PlayerUiState
import kotlin.math.abs
import org.junit.Assert.assertTrue
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
        composeRule.onNodeWithTag("btn_unlock_controls").assertExists()
    }

    @Test
    fun lockAndUnlockButtons_shareSameHorizontalAnchor() {
        composeRule.setContent {
            PlayerScreen(
                model = testPlaybackScreenModel(uiState = PlayerUiState(title = "Test")),
                dependencies = testPlaybackScreenDependencies(),
                onBack = {},
                onPip = {},
                onBackground = {},
            )
        }

        val lockCenterX = composeRule.onNodeWithTag("btn_lock").fetchSemanticsNode().boundsInRoot.center.x
        composeRule.onNodeWithTag("btn_lock").performClick()
        composeRule.waitForIdle()
        val unlockCenterX = composeRule.onNodeWithTag("btn_unlock_controls").fetchSemanticsNode().boundsInRoot.center.x

        assertTrue(abs(lockCenterX - unlockCenterX) < 2f)
    }

    @Test
    fun lockButton_isAnchoredOnRightSide() {
        composeRule.setContent {
            PlayerScreen(
                model = testPlaybackScreenModel(uiState = PlayerUiState(title = "Test")),
                dependencies = testPlaybackScreenDependencies(),
                onBack = {},
                onPip = {},
                onBackground = {},
            )
        }

        val rootCenterX = composeRule.onRoot().fetchSemanticsNode().boundsInRoot.center.x
        val lockCenterX = composeRule.onNodeWithTag("btn_lock").fetchSemanticsNode().boundsInRoot.center.x

        assertTrue(lockCenterX > rootCenterX)
    }

    @Test
    fun lockedScreenShowsUnlockButtonOnlyAfterTap() {
        composeRule.mainClock.autoAdvance = false
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
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("btn_unlock_controls").assertExists()

        composeRule.mainClock.advanceTimeBy(3_100L)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("btn_unlock_controls").assertDoesNotExist()

        composeRule.onRoot().performTouchInput { click(center) }

        composeRule.onNodeWithTag("btn_unlock_controls").assertExists()
    }
}
