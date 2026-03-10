package com.asuka.player.core

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class PlaybackRuntimeSettings(
    val playerSettings: PlayerSettings = PlayerSettings(),
    val keepSessionConnectionInBackground: Boolean = true,
) : Parcelable {
    val seekGestureEnabled: Boolean
        get() = playerSettings.seekGestureEnabled

    val brightnessGestureEnabled: Boolean
        get() = playerSettings.brightnessGestureEnabled

    val volumeGestureEnabled: Boolean
        get() = playerSettings.volumeGestureEnabled

    val zoomGestureEnabled: Boolean
        get() = playerSettings.zoomGestureEnabled

    val panGestureEnabled: Boolean
        get() = playerSettings.panGestureEnabled

    val doubleTapGestureEnabled: Boolean
        get() = playerSettings.doubleTapGestureEnabled

    val doubleTapAction: PlayerSettings.DoubleTapAction
        get() = playerSettings.doubleTapAction

    val longPressGestureEnabled: Boolean
        get() = playerSettings.longPressGestureEnabled

    val seekIncrementSec: Int
        get() = playerSettings.seekIncrementSec

    val seekSensitivity: Float
        get() = playerSettings.seekSensitivity

    val longPressSpeed: Float
        get() = playerSettings.longPressSpeed

    val controllerTimeoutSec: Int
        get() = playerSettings.controllerTimeoutSec

    val hideButtonsBackground: Boolean
        get() = playerSettings.hideButtonsBackground

    val resumePlayback: Boolean
        get() = playerSettings.resumePlayback

    val defaultPlaybackSpeed: Float
        get() = playerSettings.defaultPlaybackSpeed

    val autoplay: Boolean
        get() = playerSettings.autoplay

    val autoPip: Boolean
        get() = playerSettings.autoPip

    val autoBackgroundPlay: Boolean
        get() = playerSettings.autoBackgroundPlay

    val rememberBrightness: Boolean
        get() = playerSettings.rememberBrightness

    val rememberSelections: Boolean
        get() = playerSettings.rememberSelections

    companion object {
        const val EXTRA_KEY = "player_runtime_settings"
    }
}
