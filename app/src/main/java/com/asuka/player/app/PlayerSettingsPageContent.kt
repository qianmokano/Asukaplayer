package com.asuka.player.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.asuka.player.R
import java.util.Locale

@Composable
internal fun DoubleTapActionOptionRow(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton,
            )
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
internal fun PlayerSettingsPlaceholderPageContent(
    modifier: Modifier = Modifier,
    playerSettings: PlayerSettingsConfig,
    onPlayerSettingsChange: (PlayerSettingsConfig) -> Unit,
) {
    val gestureSeekEnabled = playerSettings.seekGestureEnabled
    val gestureBrightnessEnabled = playerSettings.brightnessGestureEnabled
    val gestureVolumeEnabled = playerSettings.volumeGestureEnabled
    val gestureZoomEnabled = playerSettings.zoomGestureEnabled
    val gesturePanEnabled = playerSettings.panGestureEnabled
    val gestureDoubleTapEnabled = playerSettings.doubleTapGestureEnabled
    val doubleTapAction = playerSettings.doubleTapAction
    val doubleTapActionLabel = when (doubleTapAction) {
        DoubleTapActionSetting.TogglePlayPause -> stringResource(R.string.action_play_pause)
        DoubleTapActionSetting.Both -> stringResource(R.string.action_seek_and_play_pause)
        DoubleTapActionSetting.Seek -> stringResource(R.string.action_seek)
    }
    val gestureLongPressEnabled = playerSettings.longPressGestureEnabled
    val seekIncrementSec = playerSettings.seekIncrementSec.toFloat()
    val seekSensitivity = playerSettings.seekSensitivity
    val longPressSpeed = playerSettings.longPressSpeed
    val controllerTimeoutSec = playerSettings.controllerTimeoutSec.toFloat()
    val hideButtonsBackground = playerSettings.hideButtonsBackground
    val resumePlaybackEnabled = playerSettings.resumePlayback
    val autoplayEnabled = playerSettings.autoplay
    val autoPipEnabled = playerSettings.autoPip
    val backgroundPlayEnabled = playerSettings.autoBackgroundPlay
    val rememberBrightnessEnabled = playerSettings.rememberBrightness
    val rememberSelectionsEnabled = playerSettings.rememberSelections
    val defaultPlaybackSpeed = playerSettings.defaultPlaybackSpeed
    var showDoubleTapActionDialog by remember { mutableStateOf(false) }
    var showLongPressSpeedDialog by remember { mutableStateOf(false) }
    var editingLongPressSpeed by remember { mutableFloatStateOf(longPressSpeed) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            bottom = 24.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
        ),
    ) {
        item { Spacer(modifier = Modifier.size(12.dp)) }

        item {
            SplicedColumnGroup(title = stringResource(R.string.settings_group_gesture)) {
                item {
                    SettingsToggleItem(
                        icon = Icons.Rounded.Swipe,
                        title = stringResource(R.string.gesture_seek_title),
                        description = stringResource(R.string.gesture_seek_desc),
                        checked = gestureSeekEnabled,
                        onCheckedChange = { onPlayerSettingsChange(playerSettings.copy(seekGestureEnabled = it)) },
                    )
                }
                item {
                    SettingsToggleItem(
                        icon = Icons.Rounded.SwipeVertical,
                        title = stringResource(R.string.gesture_brightness_title),
                        description = stringResource(R.string.gesture_brightness_desc),
                        checked = gestureBrightnessEnabled,
                        onCheckedChange = { onPlayerSettingsChange(playerSettings.copy(brightnessGestureEnabled = it)) },
                    )
                }
                item {
                    SettingsToggleItem(
                        icon = Icons.Rounded.SwipeVertical,
                        title = stringResource(R.string.gesture_volume_title),
                        description = stringResource(R.string.gesture_volume_desc),
                        checked = gestureVolumeEnabled,
                        onCheckedChange = { onPlayerSettingsChange(playerSettings.copy(volumeGestureEnabled = it)) },
                    )
                }
                item {
                    SettingsToggleItem(
                        icon = Icons.Rounded.Pinch,
                        title = stringResource(R.string.gesture_zoom_title),
                        description = stringResource(R.string.gesture_zoom_desc),
                        checked = gestureZoomEnabled,
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
                        description = if (gestureZoomEnabled) stringResource(R.string.gesture_pan_desc_enabled) else stringResource(R.string.gesture_pan_desc_disabled),
                        checked = gesturePanEnabled,
                        enabled = gestureZoomEnabled,
                        onCheckedChange = { onPlayerSettingsChange(playerSettings.copy(panGestureEnabled = it && gestureZoomEnabled)) },
                    )
                }
                item {
                    SettingsToggleNavigationItem(
                        icon = Icons.Rounded.DoubleArrow,
                        title = stringResource(R.string.gesture_double_tap_title),
                        description = if (gestureDoubleTapEnabled) stringResource(R.string.gesture_double_tap_desc_on, doubleTapActionLabel) else stringResource(R.string.gesture_double_tap_desc_off),
                        checked = gestureDoubleTapEnabled,
                        onCheckedChange = { enabled ->
                            onPlayerSettingsChange(playerSettings.copy(doubleTapGestureEnabled = enabled))
                        },
                        onClick = { showDoubleTapActionDialog = true },
                    )
                }
                item {
                    SettingsToggleNavigationItem(
                        icon = Icons.Rounded.TouchApp,
                        title = stringResource(R.string.gesture_long_press_title),
                        description = String.format(Locale.US, stringResource(R.string.gesture_long_press_desc), longPressSpeed),
                        checked = gestureLongPressEnabled,
                        onCheckedChange = { onPlayerSettingsChange(playerSettings.copy(longPressGestureEnabled = it)) },
                        onClick = {
                            editingLongPressSpeed = longPressSpeed
                            showLongPressSpeedDialog = true
                        },
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
                        enabled = gestureSeekEnabled,
                        onValueChange = { onPlayerSettingsChange(playerSettings.copy(seekIncrementSec = it.toInt().coerceIn(1, 60))) },
                        onReset = { onPlayerSettingsChange(playerSettings.copy(seekIncrementSec = 10)) },
                    )
                }
                item {
                    SettingsSliderItem(
                        icon = Icons.Rounded.FastForward,
                        title = stringResource(R.string.gesture_seek_sensitivity_title),
                        description = String.format(Locale.US, "%.1f", seekSensitivity),
                        value = seekSensitivity,
                        valueRange = 0.1f..2.0f,
                        enabled = gestureSeekEnabled,
                        onValueChange = { onPlayerSettingsChange(playerSettings.copy(seekSensitivity = (it * 10).toInt() / 10f)) },
                        onReset = { onPlayerSettingsChange(playerSettings.copy(seekSensitivity = 1.0f)) },
                    )
                }
            }
        }

        item {
            SplicedColumnGroup(title = stringResource(R.string.settings_group_ui)) {
                item {
                    SettingsSliderItem(
                        icon = Icons.Rounded.Timer,
                        title = stringResource(R.string.settings_controller_timeout_title),
                        description = stringResource(R.string.settings_controller_timeout_desc, controllerTimeoutSec.toInt()),
                        value = controllerTimeoutSec,
                        valueRange = 1f..60f,
                        steps = 58,
                        onValueChange = { onPlayerSettingsChange(playerSettings.copy(controllerTimeoutSec = it.toInt().coerceIn(1, 60))) },
                        onReset = { onPlayerSettingsChange(playerSettings.copy(controllerTimeoutSec = 3)) },
                    )
                }
                item {
                    SettingsToggleItem(
                        icon = Icons.Rounded.HideSource,
                        title = stringResource(R.string.settings_hide_button_bg_title),
                        description = stringResource(R.string.settings_hide_button_bg_desc),
                        checked = hideButtonsBackground,
                        onCheckedChange = { onPlayerSettingsChange(playerSettings.copy(hideButtonsBackground = it)) },
                    )
                }
            }
        }

        item {
            SplicedColumnGroup(title = stringResource(R.string.settings_group_playback)) {
                item {
                    SettingsToggleItem(
                        icon = Icons.Rounded.ResetTv,
                        title = stringResource(R.string.settings_resume_title),
                        description = stringResource(R.string.settings_resume_desc),
                        checked = resumePlaybackEnabled,
                        onCheckedChange = { onPlayerSettingsChange(playerSettings.copy(resumePlayback = it)) },
                    )
                }
                item {
                    SettingsSliderItem(
                        icon = Icons.Rounded.Speed,
                        title = stringResource(R.string.settings_default_speed_title),
                        description = String.format(Locale.US, "%.1fx", defaultPlaybackSpeed),
                        value = defaultPlaybackSpeed,
                        valueRange = 0.2f..4.0f,
                        onValueChange = { onPlayerSettingsChange(playerSettings.copy(defaultPlaybackSpeed = (it * 10).toInt() / 10f)) },
                        onReset = { onPlayerSettingsChange(playerSettings.copy(defaultPlaybackSpeed = 1.0f)) },
                    )
                }
                item {
                    SettingsToggleItem(
                        icon = Icons.Rounded.PlayCircle,
                        title = stringResource(R.string.settings_autoplay_title),
                        description = stringResource(R.string.settings_autoplay_desc),
                        checked = autoplayEnabled,
                        onCheckedChange = { onPlayerSettingsChange(playerSettings.copy(autoplay = it)) },
                    )
                }
                item {
                    SettingsToggleItem(
                        icon = Icons.Rounded.PictureInPictureAlt,
                        title = stringResource(R.string.settings_auto_pip_title),
                        description = stringResource(R.string.settings_auto_pip_desc),
                        checked = autoPipEnabled,
                        onCheckedChange = { onPlayerSettingsChange(playerSettings.copy(autoPip = it)) },
                    )
                }
                item {
                    SettingsToggleItem(
                        icon = Icons.Rounded.Headset,
                        title = stringResource(R.string.settings_background_play_title),
                        description = stringResource(R.string.settings_background_play_desc),
                        checked = backgroundPlayEnabled,
                        onCheckedChange = { onPlayerSettingsChange(playerSettings.copy(autoBackgroundPlay = it)) },
                    )
                }
                item {
                    SettingsToggleItem(
                        icon = Icons.Rounded.BrightnessHigh,
                        title = stringResource(R.string.settings_remember_brightness_title),
                        description = stringResource(R.string.settings_remember_brightness_desc),
                        checked = rememberBrightnessEnabled,
                        onCheckedChange = { onPlayerSettingsChange(playerSettings.copy(rememberBrightness = it)) },
                    )
                }
                item {
                    SettingsToggleItem(
                        icon = Icons.Rounded.DoneAll,
                        title = stringResource(R.string.settings_remember_tracks_title),
                        description = stringResource(R.string.settings_remember_tracks_desc),
                        checked = rememberSelectionsEnabled,
                        onCheckedChange = { onPlayerSettingsChange(playerSettings.copy(rememberSelections = it)) },
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.size(6.dp)) }
    }

    if (showDoubleTapActionDialog) {
        AlertDialog(
            onDismissRequest = { showDoubleTapActionDialog = false },
            title = { Text(text = stringResource(R.string.dialog_double_tap_action_title)) },
            text = {
                Column {
                    HorizontalDivider()
                    LazyColumn(
                        modifier = Modifier.selectableGroup(),
                        contentPadding = PaddingValues(vertical = 8.dp),
                    ) {
                        item {
                            DoubleTapActionOptionRow(
                                text = stringResource(R.string.action_seek),
                                selected = doubleTapAction == DoubleTapActionSetting.Seek,
                                onClick = {
                                    onPlayerSettingsChange(
                                        playerSettings.copy(doubleTapGestureEnabled = true, doubleTapAction = DoubleTapActionSetting.Seek),
                                    )
                                    showDoubleTapActionDialog = false
                                },
                            )
                        }
                        item {
                            DoubleTapActionOptionRow(
                                text = stringResource(R.string.action_play_pause),
                                selected = doubleTapAction == DoubleTapActionSetting.TogglePlayPause,
                                onClick = {
                                    onPlayerSettingsChange(
                                        playerSettings.copy(doubleTapGestureEnabled = true, doubleTapAction = DoubleTapActionSetting.TogglePlayPause),
                                    )
                                    showDoubleTapActionDialog = false
                                },
                            )
                        }
                        item {
                            DoubleTapActionOptionRow(
                                text = stringResource(R.string.action_seek_and_play_pause),
                                selected = doubleTapAction == DoubleTapActionSetting.Both,
                                onClick = {
                                    onPlayerSettingsChange(
                                        playerSettings.copy(doubleTapGestureEnabled = true, doubleTapAction = DoubleTapActionSetting.Both),
                                    )
                                    showDoubleTapActionDialog = false
                                },
                            )
                        }
                    }
                    HorizontalDivider()
                }
            },
            dismissButton = {
                TextButton(onClick = { showDoubleTapActionDialog = false }) {
                    Text(text = stringResource(R.string.dialog_cancel))
                }
            },
            confirmButton = {},
        )
    }

    if (showLongPressSpeedDialog) {
        AlertDialog(
            onDismissRequest = { showLongPressSpeedDialog = false },
            title = { Text(text = stringResource(R.string.dialog_long_press_title)) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = String.format(Locale.US, "%.1fx", editingLongPressSpeed),
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                    )
                    Slider(
                        value = editingLongPressSpeed,
                        onValueChange = { value ->
                            editingLongPressSpeed = ((value * 10).toInt() / 10f).coerceIn(0.2f, 4.0f)
                        },
                        valueRange = 0.2f..4.0f,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showLongPressSpeedDialog = false }) {
                    Text(text = stringResource(R.string.dialog_cancel))
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onPlayerSettingsChange(
                            playerSettings.copy(longPressSpeed = editingLongPressSpeed.coerceIn(0.2f, 4.0f)),
                        )
                        showLongPressSpeedDialog = false
                    },
                ) {
                    Text(text = stringResource(R.string.dialog_done))
                }
            },
        )
    }
}
