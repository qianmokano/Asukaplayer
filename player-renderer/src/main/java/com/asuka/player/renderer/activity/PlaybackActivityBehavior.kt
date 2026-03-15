package com.asuka.player.renderer.activity

import com.asuka.player.contract.PlayerSettings
import com.asuka.player.renderer.controller.BackgroundPlaybackPolicy

internal data class PictureInPictureTransition(
    val isInPictureInPicture: Boolean,
    val shouldRegisterReceiver: Boolean,
    val shouldAttachPlayStateListener: Boolean,
)

internal class PlaybackActivityBehavior(
    private val backgroundPolicy: BackgroundPlaybackPolicy = BackgroundPlaybackPolicy(),
    initialSettings: PlayerSettings = PlayerSettings(),
) {
    private var runtimeSettings: PlayerSettings = initialSettings

    init {
        syncPolicy()
    }

    fun currentSettings(): PlayerSettings = runtimeSettings

    fun onRuntimeSettingsChanged(settings: PlayerSettings) {
        runtimeSettings = settings
        syncPolicy()
    }

    fun onStart() {
        syncPolicy()
        backgroundPolicy.clearManualBackgroundPlaybackRequest()
    }

    fun onPictureInPictureModeChanged(active: Boolean): PictureInPictureTransition {
        backgroundPolicy.setPictureInPicture(active)
        return PictureInPictureTransition(
            isInPictureInPicture = active,
            shouldRegisterReceiver = active,
            shouldAttachPlayStateListener = active,
        )
    }

    fun onEnterPictureInPictureRequested(): Boolean {
        backgroundPolicy.setPictureInPicture(true)
        return true
    }

    fun onBackgroundPlaybackRequested() {
        backgroundPolicy.requestBackgroundPlayback()
    }

    fun shouldAutoEnterPictureInPictureOnUserLeave(): Boolean = runtimeSettings.autoPip

    fun shouldRememberBrightness(): Boolean = runtimeSettings.rememberBrightness

    fun shouldRetainSessionOnStop(): Boolean = backgroundPolicy.shouldRetainSession()

    private fun syncPolicy() {
        backgroundPolicy.update(
            retainControllerConnection = runtimeSettings.keepSessionConnectionInBackground,
            autoBackgroundPlaybackEnabled = runtimeSettings.autoBackgroundPlay,
        )
    }
}
