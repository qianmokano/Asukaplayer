package com.asuka.player.ui.activity

import com.asuka.player.core.PlaybackRuntimeSettings
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlaybackActivityBehaviorTest {

    @Test
    fun pictureInPictureTransition_updatesRetentionAndReceiverFlags() {
        val behavior = PlaybackActivityBehavior(
            initialSettings = PlaybackRuntimeSettings(
                keepSessionConnectionInBackground = false,
                autoBackgroundPlay = false,
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
                keepSessionConnectionInBackground = false,
                autoBackgroundPlay = false,
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
                autoPip = false,
                rememberBrightness = false,
                keepSessionConnectionInBackground = false,
                autoBackgroundPlay = false,
            ),
        )

        assertFalse(behavior.shouldAutoEnterPictureInPictureOnUserLeave())
        assertFalse(behavior.shouldRememberBrightness())
        assertFalse(behavior.shouldRetainSessionOnStop())

        behavior.onRuntimeSettingsChanged(
            PlaybackRuntimeSettings(
                autoPip = true,
                rememberBrightness = true,
                keepSessionConnectionInBackground = true,
                autoBackgroundPlay = false,
            ),
        )

        assertTrue(behavior.shouldAutoEnterPictureInPictureOnUserLeave())
        assertTrue(behavior.shouldRememberBrightness())
        assertTrue(behavior.shouldRetainSessionOnStop())
    }
}
