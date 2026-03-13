package com.asuka.player.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.asuka.player.ui.components.VerticalAdjustIndicator
import com.asuka.player.ui.state.VolumeBrightnessState
import org.junit.Rule
import org.junit.Test

class VerticalAdjustIndicatorTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun volumeIndicator_showsGraphicHudWithVolumeLabel() {
        val state = VolumeBrightnessState().apply {
            start(
                mode = VolumeBrightnessState.Mode.VOLUME,
                currentVolume = 54,
                currentBrightness = 31,
            )
            updateVolume(72)
        }

        composeRule.setContent {
            VerticalAdjustIndicator(state = state)
        }

        composeRule.onNodeWithTag("vertical_adjust_indicator").assertExists()
        composeRule.onNodeWithTag("vertical_adjust_meter").assertExists()
    }

    @Test
    fun brightnessIndicator_showsGraphicHudWithBrightnessLabel() {
        val state = VolumeBrightnessState().apply {
            start(
                mode = VolumeBrightnessState.Mode.BRIGHTNESS,
                currentVolume = 54,
                currentBrightness = 31,
            )
            updateBrightness(31)
        }

        composeRule.setContent {
            VerticalAdjustIndicator(state = state)
        }

        composeRule.onNodeWithTag("vertical_adjust_indicator").assertExists()
        composeRule.onNodeWithTag("vertical_adjust_meter").assertExists()
    }
}
