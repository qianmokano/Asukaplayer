package com.asuka.player.runtime

import com.asuka.player.contract.PlayerSettings
import com.asuka.player.data.PlayerSettingsRecord

internal fun PlayerSettingsRecord.toPlayerSettings(): PlayerSettings {
    return PlayerSettings(
        seekGestureEnabled = seekGestureEnabled,
        brightnessGestureEnabled = brightnessGestureEnabled,
        volumeGestureEnabled = volumeGestureEnabled,
        zoomGestureEnabled = zoomGestureEnabled,
        panGestureEnabled = panGestureEnabled,
        doubleTapGestureEnabled = doubleTapGestureEnabled,
        doubleTapAction = doubleTapAction.toDoubleTapAction(),
        longPressGestureEnabled = longPressGestureEnabled,
        seekBackwardSec = seekBackwardSec,
        seekForwardSec = seekForwardSec,
        seekSensitivity = seekSensitivity,
        longPressSpeed = longPressSpeed,
        controllerTimeoutSec = controllerTimeoutSec,
        hideButtonsBackground = hideButtonsBackground,
        resumePlayback = resumePlayback,
        defaultPlaybackSpeed = defaultPlaybackSpeed,
        autoplay = autoplay,
        autoPip = autoPip,
        autoBackgroundPlay = autoBackgroundPlay,
        rememberBrightness = rememberBrightness,
        rememberVolume = rememberVolume,
        rememberSelections = rememberSelections,
    )
}

internal fun PlayerSettings.toPlayerSettingsRecord(): PlayerSettingsRecord {
    return PlayerSettingsRecord(
        seekGestureEnabled = seekGestureEnabled,
        brightnessGestureEnabled = brightnessGestureEnabled,
        volumeGestureEnabled = volumeGestureEnabled,
        zoomGestureEnabled = zoomGestureEnabled,
        panGestureEnabled = panGestureEnabled,
        doubleTapGestureEnabled = doubleTapGestureEnabled,
        doubleTapAction = doubleTapAction.value,
        longPressGestureEnabled = longPressGestureEnabled,
        seekBackwardSec = seekBackwardSec,
        seekForwardSec = seekForwardSec,
        seekSensitivity = seekSensitivity,
        longPressSpeed = longPressSpeed,
        controllerTimeoutSec = controllerTimeoutSec,
        hideButtonsBackground = hideButtonsBackground,
        resumePlayback = resumePlayback,
        defaultPlaybackSpeed = defaultPlaybackSpeed,
        autoplay = autoplay,
        autoPip = autoPip,
        autoBackgroundPlay = autoBackgroundPlay,
        rememberBrightness = rememberBrightness,
        rememberVolume = rememberVolume,
        rememberSelections = rememberSelections,
    )
}

private fun String?.toDoubleTapAction(): PlayerSettings.DoubleTapAction {
    return when (this) {
        "seek" -> PlayerSettings.DoubleTapAction.Seek
        "toggle_play_pause" -> PlayerSettings.DoubleTapAction.TogglePlayPause
        "both" -> PlayerSettings.DoubleTapAction.Both
        else -> PlayerSettings.DoubleTapAction.TogglePlayPause
    }
}

private val PlayerSettings.DoubleTapAction.value: String
    get() = when (this) {
        PlayerSettings.DoubleTapAction.Seek -> "seek"
        PlayerSettings.DoubleTapAction.TogglePlayPause -> "toggle_play_pause"
        PlayerSettings.DoubleTapAction.Both -> "both"
    }
