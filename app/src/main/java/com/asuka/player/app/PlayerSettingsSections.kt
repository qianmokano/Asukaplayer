package com.asuka.player.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BrightnessHigh
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.DoubleArrow
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.Headset
import androidx.compose.material.icons.rounded.HideSource
import androidx.compose.material.icons.rounded.PictureInPictureAlt
import androidx.compose.material.icons.rounded.PanToolAlt
import androidx.compose.material.icons.rounded.Pinch
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.ResetTv
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Swipe
import androidx.compose.material.icons.rounded.SwipeVertical
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.asuka.player.contract.PlayerSettings
import com.asuka.player.R
import java.util.Locale

@Composable
internal fun PlayerGestureSettingsGroup(
    playerSettings: PlayerSettings,
    onPlayerSettingsChange: (PlayerSettings) -> Unit,
    onOpenDoubleTapAction: () -> Unit,
    onOpenLongPressSpeed: () -> Unit,
) {
    val doubleTapActionLabel = when (playerSettings.doubleTapAction) {
        PlayerSettings.DoubleTapAction.TogglePlayPause -> stringResource(R.string.action_play_pause)
        PlayerSettings.DoubleTapAction.Both -> stringResource(R.string.action_seek_and_play_pause)
        PlayerSettings.DoubleTapAction.Seek -> stringResource(R.string.action_seek)
    }
    val seekIncrementSec = playerSettings.seekIncrementSec.toFloat()

    SplicedColumnGroup(title = stringResource(R.string.settings_group_gesture)) {
        item {
            SettingsToggleItem(
                icon = Icons.Rounded.Swipe,
                title = stringResource(R.string.gesture_seek_title),
                description = stringResource(R.string.gesture_seek_desc),
                checked = playerSettings.seekGestureEnabled,
                onCheckedChange = { onPlayerSettingsChange(playerSettings.copy(seekGestureEnabled = it)) },
            )
        }
        item {
            SettingsToggleItem(
                icon = Icons.Rounded.SwipeVertical,
                title = stringResource(R.string.gesture_brightness_title),
                description = stringResource(R.string.gesture_brightness_desc),
                checked = playerSettings.brightnessGestureEnabled,
                onCheckedChange = { onPlayerSettingsChange(playerSettings.copy(brightnessGestureEnabled = it)) },
            )
        }
        item {
            SettingsToggleItem(
                icon = Icons.Rounded.SwipeVertical,
                title = stringResource(R.string.gesture_volume_title),
                description = stringResource(R.string.gesture_volume_desc),
                checked = playerSettings.volumeGestureEnabled,
                onCheckedChange = { onPlayerSettingsChange(playerSettings.copy(volumeGestureEnabled = it)) },
            )
        }
        item {
            SettingsToggleItem(
                icon = Icons.Rounded.Pinch,
                title = stringResource(R.string.gesture_zoom_title),
                description = stringResource(R.string.gesture_zoom_desc),
                checked = playerSettings.zoomGestureEnabled,
                onCheckedChange = { enabled ->
                    onPlayerSettingsChange(
                        playerSettings.copy(
                            zoomGestureEnabled = enabled,
                            panGestureEnabled = if (enabled) playerSettings.panGestureEnabled else false,
                        ),
                    )
                },
            )
        }
        item {
            SettingsToggleItem(
                icon = Icons.Rounded.PanToolAlt,
                title = stringResource(R.string.gesture_pan_title),
                description = if (playerSettings.zoomGestureEnabled) {
                    stringResource(R.string.gesture_pan_desc_enabled)
                } else {
                    stringResource(R.string.gesture_pan_desc_disabled)
                },
                checked = playerSettings.panGestureEnabled,
                enabled = playerSettings.zoomGestureEnabled,
                onCheckedChange = {
                    onPlayerSettingsChange(
                        playerSettings.copy(
                            panGestureEnabled = it && playerSettings.zoomGestureEnabled,
                        ),
                    )
                },
            )
        }
        item {
            SettingsToggleNavigationItem(
                icon = Icons.Rounded.DoubleArrow,
                title = stringResource(R.string.gesture_double_tap_title),
                description = if (playerSettings.doubleTapGestureEnabled) {
                    stringResource(R.string.gesture_double_tap_desc_on, doubleTapActionLabel)
                } else {
                    stringResource(R.string.gesture_double_tap_desc_off)
                },
                checked = playerSettings.doubleTapGestureEnabled,
                onCheckedChange = { enabled ->
                    onPlayerSettingsChange(playerSettings.copy(doubleTapGestureEnabled = enabled))
                },
                onClick = onOpenDoubleTapAction,
            )
        }
        item {
            SettingsToggleNavigationItem(
                icon = Icons.Rounded.TouchApp,
                title = stringResource(R.string.gesture_long_press_title),
                description = String.format(
                    Locale.US,
                    stringResource(R.string.gesture_long_press_desc),
                    playerSettings.longPressSpeed,
                ),
                checked = playerSettings.longPressGestureEnabled,
                onCheckedChange = { onPlayerSettingsChange(playerSettings.copy(longPressGestureEnabled = it)) },
                onClick = onOpenLongPressSpeed,
            )
        }
        item {
            SettingsSliderItem(
                icon = Icons.Rounded.Replay10,
                title = stringResource(R.string.gesture_seek_increment_title),
                description = stringResource(R.string.gesture_seek_increment_desc, seekIncrementSec.toInt()),
                value = seekIncrementSec,
                valueRange = 1f..60f,
                steps = 58,
                enabled = playerSettings.seekGestureEnabled,
                onValueChange = {
                    onPlayerSettingsChange(playerSettings.copy(seekIncrementSec = it.toInt().coerceIn(1, 60)))
                },
                onReset = { onPlayerSettingsChange(playerSettings.copy(seekIncrementSec = 10)) },
            )
        }
        item {
            SettingsSliderItem(
                icon = Icons.Rounded.FastForward,
                title = stringResource(R.string.gesture_seek_sensitivity_title),
                description = String.format(Locale.US, "%.1f", playerSettings.seekSensitivity),
                value = playerSettings.seekSensitivity,
                valueRange = 0.1f..2.0f,
                enabled = playerSettings.seekGestureEnabled,
                onValueChange = {
                    onPlayerSettingsChange(playerSettings.copy(seekSensitivity = (it * 10).toInt() / 10f))
                },
                onReset = { onPlayerSettingsChange(playerSettings.copy(seekSensitivity = 1.0f)) },
            )
        }
    }
}

@Composable
internal fun PlayerUiSettingsGroup(
    playerSettings: PlayerSettings,
    onPlayerSettingsChange: (PlayerSettings) -> Unit,
) {
    val controllerTimeoutSec = playerSettings.controllerTimeoutSec.toFloat()

    SplicedColumnGroup(title = stringResource(R.string.settings_group_ui)) {
        item {
            SettingsSliderItem(
                icon = Icons.Rounded.Timer,
                title = stringResource(R.string.settings_controller_timeout_title),
                description = stringResource(R.string.settings_controller_timeout_desc, controllerTimeoutSec.toInt()),
                value = controllerTimeoutSec,
                valueRange = 1f..60f,
                steps = 58,
                onValueChange = {
                    onPlayerSettingsChange(playerSettings.copy(controllerTimeoutSec = it.toInt().coerceIn(1, 60)))
                },
                onReset = { onPlayerSettingsChange(playerSettings.copy(controllerTimeoutSec = 3)) },
            )
        }
        item {
            SettingsToggleItem(
                icon = Icons.Rounded.HideSource,
                title = stringResource(R.string.settings_hide_button_bg_title),
                description = stringResource(R.string.settings_hide_button_bg_desc),
                checked = playerSettings.hideButtonsBackground,
                onCheckedChange = { onPlayerSettingsChange(playerSettings.copy(hideButtonsBackground = it)) },
            )
        }
    }
}

@Composable
internal fun PlayerPlaybackSettingsGroup(
    playerSettings: PlayerSettings,
    onPlayerSettingsChange: (PlayerSettings) -> Unit,
) {
    SplicedColumnGroup(title = stringResource(R.string.settings_group_playback)) {
        item {
            SettingsToggleItem(
                icon = Icons.Rounded.ResetTv,
                title = stringResource(R.string.settings_resume_title),
                description = stringResource(R.string.settings_resume_desc),
                checked = playerSettings.resumePlayback,
                onCheckedChange = { onPlayerSettingsChange(playerSettings.copy(resumePlayback = it)) },
            )
        }
        item {
            SettingsSliderItem(
                icon = Icons.Rounded.Speed,
                title = stringResource(R.string.settings_default_speed_title),
                description = String.format(Locale.US, "%.1fx", playerSettings.defaultPlaybackSpeed),
                value = playerSettings.defaultPlaybackSpeed,
                valueRange = 0.2f..4.0f,
                onValueChange = {
                    onPlayerSettingsChange(playerSettings.copy(defaultPlaybackSpeed = (it * 10).toInt() / 10f))
                },
                onReset = { onPlayerSettingsChange(playerSettings.copy(defaultPlaybackSpeed = 1.0f)) },
            )
        }
        item {
            SettingsToggleItem(
                icon = Icons.Rounded.PlayCircle,
                title = stringResource(R.string.settings_autoplay_title),
                description = stringResource(R.string.settings_autoplay_desc),
                checked = playerSettings.autoplay,
                onCheckedChange = { onPlayerSettingsChange(playerSettings.copy(autoplay = it)) },
            )
        }
        item {
            SettingsToggleItem(
                icon = Icons.Rounded.PictureInPictureAlt,
                title = stringResource(R.string.settings_auto_pip_title),
                description = stringResource(R.string.settings_auto_pip_desc),
                checked = playerSettings.autoPip,
                onCheckedChange = { onPlayerSettingsChange(playerSettings.copy(autoPip = it)) },
            )
        }
        item {
            SettingsToggleItem(
                icon = Icons.Rounded.Headset,
                title = stringResource(R.string.settings_background_play_title),
                description = stringResource(R.string.settings_background_play_desc),
                checked = playerSettings.autoBackgroundPlay,
                onCheckedChange = { onPlayerSettingsChange(playerSettings.copy(autoBackgroundPlay = it)) },
            )
        }
        item {
            SettingsToggleItem(
                icon = Icons.Rounded.BrightnessHigh,
                title = stringResource(R.string.settings_remember_brightness_title),
                description = stringResource(R.string.settings_remember_brightness_desc),
                checked = playerSettings.rememberBrightness,
                onCheckedChange = { onPlayerSettingsChange(playerSettings.copy(rememberBrightness = it)) },
            )
        }
        item {
            SettingsToggleItem(
                icon = Icons.Rounded.DoneAll,
                title = stringResource(R.string.settings_remember_tracks_title),
                description = stringResource(R.string.settings_remember_tracks_desc),
                checked = playerSettings.rememberSelections,
                onCheckedChange = { onPlayerSettingsChange(playerSettings.copy(rememberSelections = it)) },
            )
        }
    }
}

@Composable
internal fun PlayerSettingsListFooter() {
    Spacer(modifier = Modifier.size(6.dp))
}
