package com.asuka.player.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.asuka.player.core.PlaybackController
import com.asuka.player.data.InMemoryPlaybackStore
import com.asuka.player.ui.state.PlayerUiState
import org.junit.Rule
import org.junit.Test

class ErrorOverlayTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val fakeController = object : PlaybackController {
        override fun play() {}
        override fun pause() {}
        override fun togglePlayPause() {}
        override fun seekTo(positionMs: Long) {}
        override fun seekBy(deltaMs: Long) {}
        override fun setPlaybackSpeed(speed: Float) {}
        override fun setSubtitleEnabled(enabled: Boolean) {}
        override fun addExternalSubtitle(uri: android.net.Uri, label: String?) {}
        override fun setVideoScaleMode(mode: com.asuka.player.core.VideoScaleMode) {}
        override fun setLoopMode(mode: com.asuka.player.core.LoopMode) {}
        override fun setShuffleEnabled(enabled: Boolean) {}
        override fun skipToNext() {}
        override fun skipToPrevious() {}
        override fun getRepeatMode() = com.asuka.player.core.LoopMode.OFF
        override fun isShuffleEnabled() = false
    }

    @Test
    fun errorOverlayButtonsExist() {
        composeRule.setContent {
            PlayerScreen(
                uiState = PlayerUiState(title = "Test", errorMessage = "Error"),
                player = null,
                controller = fakeController,
                bindings = null,
                store = InMemoryPlaybackStore(),
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
