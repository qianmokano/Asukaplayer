package com.asuka.player.renderer.activity

import com.asuka.player.contract.PlayerSettings
import com.asuka.player.contract.PlaybackRuntimeSettings
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlaybackActivityBehaviorTest {

    @Test
    fun pictureInPictureTransition_updatesRetentionAndReceiverFlags() {
        val behavior = PlaybackActivityBehavior(
            initialSettings = PlaybackRuntimeSettings(
                playerSettings = PlayerSettings(
                    autoBackgroundPlay = false,
                ),
                keepSessionConnectionInBackground = false,
            ),
        )

        val entered = behavior.onPictureInPictureModeChanged(true)
        assertTrue(entered.isInPictureInPicture)
        assertTrue(entered.shouldRegisterReceiver)
        assertTrue(entered.shouldAttachPlayStateListener)
        assertTrue(behavior.shouldRetainSessionOnStop())

        val exited = behavior.onPictureInPictureModeChanged(false)
        assertFalse(exited.isInPictureInPicture)
        assertFalse(exited.shouldRegisterReceiver)
        assertFalse(exited.shouldAttachPlayStateListener)
        assertFalse(behavior.shouldRetainSessionOnStop())
    }

    @Test
    fun backgroundPlaybackRequest_isClearedOnStart() {
        val behavior = PlaybackActivityBehavior(
            initialSettings = PlaybackRuntimeSettings(
                playerSettings = PlayerSettings(
                    autoBackgroundPlay = false,
                ),
                keepSessionConnectionInBackground = false,
            ),
        )

        behavior.onBackgroundPlaybackRequested()
        assertTrue(behavior.shouldRetainSessionOnStop())

        behavior.onStart()
        assertFalse(behavior.shouldRetainSessionOnStop())
    }

    @Test
    fun runtimeSettingsChanges_updateBehaviorFlags() {
        val behavior = PlaybackActivityBehavior(
            initialSettings = PlaybackRuntimeSettings(
                playerSettings = PlayerSettings(
                    autoPip = false,
                    rememberBrightness = false,
                    autoBackgroundPlay = false,
                ),
                keepSessionConnectionInBackground = false,
            ),
        )

        assertFalse(behavior.shouldAutoEnterPictureInPictureOnUserLeave())
        assertFalse(behavior.shouldRememberBrightness())
        assertFalse(behavior.shouldRetainSessionOnStop())

        behavior.onRuntimeSettingsChanged(
            PlaybackRuntimeSettings(
                playerSettings = PlayerSettings(
                    autoPip = true,
                    rememberBrightness = true,
                    autoBackgroundPlay = false,
                ),
                keepSessionConnectionInBackground = true,
            ),
        )

        assertTrue(behavior.shouldAutoEnterPictureInPictureOnUserLeave())
        assertTrue(behavior.shouldRememberBrightness())
        assertTrue(behavior.shouldRetainSessionOnStop())
    }
}
