package com.asuka.player.app

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.rounded.BrightnessHigh
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.DoubleArrow
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.Headset
import androidx.compose.material.icons.rounded.HideSource
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.AutoAwesomeMotion
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PanToolAlt
import androidx.compose.material.icons.rounded.PictureInPictureAlt
import androidx.compose.material.icons.rounded.Pinch
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Refresh
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.asuka.player.R
import java.util.Locale

@Composable
internal fun SettingsPageContent(
    modifier: Modifier = Modifier,
    appVersion: String,
    hapticFeedbackEnabled: Boolean,
    onHapticFeedbackEnabledChange: (Boolean) -> Unit,
    onOpenPlayer: () -> Unit,
    onOpenTheme: () -> Unit,
    onOpenMotion: () -> Unit,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            bottom = 24.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
        ),
    ) {
        item { Spacer(modifier = Modifier.size(12.dp)) }

        item {
            SplicedColumnGroup(title = stringResource(R.string.settings_group_playback)) {
                item {
                    SettingsNavigationItem(
                        icon = Icons.Rounded.PlayCircle,
                        title = stringResource(R.string.settings_player_title),
                        description = stringResource(R.string.settings_player_desc),
                        onClick = onOpenPlayer,
                    )
                }
            }
        }

        item {
            SplicedColumnGroup(title = stringResource(R.string.settings_group_appearance)) {
                item {
                    SettingsNavigationItem(
                        icon = Icons.Rounded.Palette,
                        title = stringResource(R.string.settings_theme_title),
                        description = stringResource(R.string.settings_theme_desc),
                        onClick = onOpenTheme,
                    )
                }
                item {
                    SettingsNavigationItem(
                        icon = Icons.Rounded.AutoAwesomeMotion,
                        title = stringResource(R.string.settings_motion_title),
                        description = stringResource(R.string.settings_motion_desc),
                        onClick = onOpenMotion,
                    )
                }
            }
        }

        item {
            SplicedColumnGroup(title = stringResource(R.string.settings_group_interaction)) {
                item {
                    SettingsToggleItem(
                        icon = Icons.Rounded.TouchApp,
                        title = stringResource(R.string.settings_haptic_title),
                        description = stringResource(R.string.settings_haptic_desc),
                        checked = hapticFeedbackEnabled,
                        onCheckedChange = onHapticFeedbackEnabledChange,
                    )
                }
            }
        }

        item {
            SplicedColumnGroup(title = stringResource(id = R.string.settings_general_title)) {
                item {
                    SettingsNavigationItem(
                        icon = Icons.Rounded.Info,
                        title = stringResource(id = R.string.settings_about_title),
                        description = stringResource(id = R.string.settings_about_desc, appVersion),
                        onClick = {},
                    )
                }
                item {
                    SettingsNavigationItem(
                        icon = Icons.Rounded.Refresh,
                        title = stringResource(id = R.string.settings_refresh_title),
                        description = stringResource(id = R.string.settings_refresh_desc),
                        onClick = {},
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.size(6.dp)) }
    }

}

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

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
internal fun ThemeSettingsPageContent(
    modifier: Modifier = Modifier,
    themeConfig: ThemeConfig,
    customThemes: List<CustomThemeEntry>,
    hapticsEnabled: Boolean,
    onThemeConfigChange: (ThemeConfig) -> Unit,
    onCustomThemesChange: (List<CustomThemeEntry>) -> Unit,
) {
    val isDynamicSupported = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S
    val context = LocalContext.current
    val previewDark = themeConfig.appearance == ThemeAppearanceMode.Dark ||
        (themeConfig.appearance == ThemeAppearanceMode.System && isSystemInDarkTheme())
    val haptic = LocalHapticFeedback.current
    val dynamicPreviewScheme = if (isDynamicSupported) {
        if (previewDark) androidx.compose.material3.dynamicDarkColorScheme(context)
        else androidx.compose.material3.dynamicLightColorScheme(context)
    } else {
        colorSchemeFromSeed(Color(0xFF2E6CF6), previewDark)
    }
    val customPreviewScheme = if (previewDark) {
        androidx.compose.material3.darkColorScheme(
            primary = Color(0xFFB6B6B6),
            onPrimary = Color(0xFF111111),
            primaryContainer = Color(0xFF3F3F3F),
            onPrimaryContainer = Color(0xFFEDEDED),
            surface = Color(0xFF1C1C1C),
            onSurface = Color(0xFFEDEDED),
            surfaceContainer = Color(0xFF2A2A2A),
            onSurfaceVariant = Color(0xFFBDBDBD),
            outline = Color(0xFF3A3A3A),
        )
    } else {
        androidx.compose.material3.lightColorScheme(
            primary = Color(0xFF6E6E6E),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFE0E0E0),
            onPrimaryContainer = Color(0xFF2A2A2A),
            surface = Color(0xFFF3F3F3),
            onSurface = Color(0xFF2B2B2B),
            surfaceContainer = Color(0xFFE7E7E7),
            onSurfaceVariant = Color(0xFF6C6C6C),
            outline = Color(0xFFB5B5B5),
        )
    }
    val monoPreviewScheme = monochromeColorScheme(previewDark)

    var customHex by remember(themeConfig.customSeed) { mutableStateOf(themeConfig.customSeed?.toHex() ?: "#2E6CF6") }
    var customName by remember { mutableStateOf("") }
    var showCustomDialog by remember { mutableStateOf(false) }
    var pendingDeleteId by remember { mutableStateOf<String?>(null) }
    var confirmDeleteId by remember { mutableStateOf<String?>(null) }
    var fontScaleSlider by remember(themeConfig.fontScale) { mutableStateOf(themeConfig.fontScale) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(pendingDeleteId) {
                if (pendingDeleteId == null) return@pointerInput
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Final)
                        val anyConsumed = event.changes.any { it.isConsumed }
                        val allUp = event.changes.all { it.changedToUp() }
                        if (!anyConsumed && allUp) {
                            pendingDeleteId = null
                        }
                    }
                }
            },
        contentPadding = PaddingValues(
            bottom = 24.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
        ),
    ) {
        item { Spacer(modifier = Modifier.size(12.dp)) }

        item {
            SplicedColumnGroup(title = stringResource(R.string.settings_group_theme_mode)) {
                item {
                    ThemeSectionBlock(
                        title = stringResource(R.string.settings_appearance_title),
                        description = stringResource(R.string.settings_appearance_desc),
                    ) {
                        androidx.compose.foundation.layout.FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            ThemeAppearanceMode.values().forEach { appearance ->
                                androidx.compose.material3.FilterChip(
                                    selected = themeConfig.appearance == appearance,
                                    onClick = {
                                        if (hapticsEnabled) {
                                            haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                                        }
                                        onThemeConfigChange(themeConfig.copy(appearance = appearance))
                                    },
                                    label = {
                                        Text(
                                            when (appearance) {
                                                ThemeAppearanceMode.System -> stringResource(R.string.appearance_auto)
                                                ThemeAppearanceMode.Light -> stringResource(R.string.appearance_light)
                                                ThemeAppearanceMode.Dark -> stringResource(R.string.appearance_dark)
                                            },
                                        )
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            SplicedColumnGroup(title = stringResource(R.string.settings_group_theme_color)) {
                item {
                    ThemeSectionBlock(
                        title = stringResource(R.string.settings_color_scheme_title),
                        description = if (isDynamicSupported) {
                            stringResource(R.string.settings_color_scheme_dynamic_supported)
                        } else {
                            stringResource(R.string.settings_color_scheme_dynamic_unsupported)
                        },
                    ) {
                        val swatches = buildList<ThemeSwatchItem> {
                            ThemePresets.filter { it.mode != ThemeMode.Custom }.forEach { add(ThemeSwatchItem.Preset(it)) }
                            customThemes.forEach { add(ThemeSwatchItem.CustomTheme(it)) }
                            add(ThemeSwatchItem.CustomAdd)
                        }
                        val columns = 4
                        val itemSize = 84.dp
                        val itemSpacing = 2.dp
                        val rows = (swatches.size + columns - 1) / columns
                        androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                            columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(columns),
                            userScrollEnabled = false,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(itemSize * rows + itemSpacing * (rows - 1)),
                            verticalArrangement = Arrangement.spacedBy(itemSpacing),
                            horizontalArrangement = Arrangement.spacedBy(itemSpacing),
                        ) {
                            items(swatches.size) { index ->
                                val swatch = swatches[index]
                                val preset = (swatch as? ThemeSwatchItem.Preset)?.preset
                                val disabled = preset?.mode == ThemeMode.Dynamic && !isDynamicSupported
                                val shape = gridItemShape(index = index, total = swatches.size, columns = columns, outerCorner = 16.dp, innerCorner = 6.dp)
                                when (swatch) {
                                    is ThemeSwatchItem.Preset -> {
                                        ThemeSwatch(
                                            label = stringResource(swatch.preset.nameResId),
                                            scheme = when (swatch.preset.mode) {
                                                ThemeMode.Dynamic -> dynamicPreviewScheme
                                                ThemeMode.Monochrome -> monoPreviewScheme
                                                else -> swatch.preset.seed?.let { remember(it, previewDark) { colorSchemeFromSeed(it, previewDark) } } ?: dynamicPreviewScheme
                                            },
                                            selected = themeConfig.mode == swatch.preset.mode,
                                            disabled = disabled,
                                            shape = shape,
                                            size = itemSize,
                                            hapticsEnabled = hapticsEnabled,
                                            showDynamicIcon = swatch.preset.mode == ThemeMode.Dynamic,
                                            onClick = {
                                                pendingDeleteId = null
                                                if (disabled) return@ThemeSwatch
                                                onThemeConfigChange(
                                                    themeConfig.copy(
                                                        mode = swatch.preset.mode,
                                                        customSeed = swatch.preset.seed,
                                                        customThemeId = null,
                                                        customMonochrome = false,
                                                    ),
                                                )
                                            },
                                        )
                                    }
                                    is ThemeSwatchItem.CustomAdd -> {
                                        ThemeSwatch(
                                            label = stringResource(R.string.theme_preset_custom),
                                            scheme = customPreviewScheme,
                                            selected = themeConfig.mode == ThemeMode.Custom && themeConfig.customThemeId == null,
                                            disabled = false,
                                            shape = shape,
                                            size = itemSize,
                                            hapticsEnabled = hapticsEnabled,
                                            icon = Icons.Outlined.Add,
                                            onClick = {
                                                pendingDeleteId = null
                                                customName = ""
                                                showCustomDialog = true
                                            },
                                        )
                                    }
                                    is ThemeSwatchItem.CustomTheme -> {
                                        ThemeSwatch(
                                            label = swatch.theme.name,
                                            scheme = remember(swatch.theme.seed, previewDark) { colorSchemeFromSeed(swatch.theme.seed, previewDark) },
                                            selected = themeConfig.mode == ThemeMode.Custom && themeConfig.customThemeId == swatch.theme.id,
                                            disabled = false,
                                            shape = shape,
                                            size = itemSize,
                                            hapticsEnabled = hapticsEnabled,
                                            showDelete = pendingDeleteId == swatch.theme.id,
                                            onClick = {
                                                if (pendingDeleteId == swatch.theme.id) {
                                                    confirmDeleteId = swatch.theme.id
                                                } else {
                                                    pendingDeleteId = null
                                                    onThemeConfigChange(
                                                        themeConfig.copy(
                                                            mode = ThemeMode.Custom,
                                                            customThemeId = swatch.theme.id,
                                                            customSeed = swatch.theme.seed,
                                                            customMonochrome = swatch.theme.monochrome,
                                                        ),
                                                    )
                                                }
                                            },
                                            onLongPress = { pendingDeleteId = swatch.theme.id },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            SplicedColumnGroup(title = stringResource(R.string.settings_group_display)) {
                item {
                    SettingsToggleItem(
                        title = stringResource(R.string.settings_pure_black_title),
                        description = stringResource(R.string.settings_pure_black_desc),
                        checked = themeConfig.pureBlack,
                        onCheckedChange = { onThemeConfigChange(themeConfig.copy(pureBlack = it)) },
                    )
                }
                item {
                    SettingsToggleItem(
                        title = stringResource(R.string.settings_font_scale_title),
                        description = stringResource(R.string.settings_font_scale_desc),
                        checked = themeConfig.fontScaleEnabled,
                        onCheckedChange = { enabled ->
                            onThemeConfigChange(themeConfig.copy(fontScaleEnabled = enabled))
                        },
                    )
                }
                item {
                    ThemeSectionBlock(
                        title = stringResource(R.string.settings_font_scale_ratio_title),
                        description = stringResource(R.string.settings_font_scale_ratio_desc, (fontScaleSlider * 100).toInt()),
                    ) {
                        Slider(
                            value = fontScaleSlider,
                            onValueChange = {
                                fontScaleSlider = it
                                onThemeConfigChange(
                                    themeConfig.copy(
                                        fontScale = it,
                                        fontScaleEnabled = true,
                                    ),
                                )
                            },
                            valueRange = 0.85f..1.3f,
                            enabled = themeConfig.fontScaleEnabled,
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(6.dp)) }
    }

    if (showCustomDialog) {
        val defaultThemeName = stringResource(R.string.custom_theme_default_name, customThemes.size + 1)
        CustomThemeSheet(
            initialHex = customHex,
            initialName = customName,
            previewDark = previewDark,
            hapticsEnabled = hapticsEnabled,
            onDismiss = { showCustomDialog = false },
            onSave = { name, seed ->
                val safeName = name.ifBlank { defaultThemeName }
                val entry = CustomThemeEntry(
                    id = java.util.UUID.randomUUID().toString(),
                    name = safeName,
                    seedArgb = seed.toArgb(),
                    monochrome = false,
                )
                customHex = seed.toHex()
                onCustomThemesChange(customThemes + entry)
                onThemeConfigChange(
                    themeConfig.copy(
                        mode = ThemeMode.Custom,
                        customThemeId = entry.id,
                        customSeed = entry.seed,
                        customMonochrome = false,
                    ),
                )
                showCustomDialog = false
            },
        )
    }

    if (confirmDeleteId != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = {
                confirmDeleteId = null
                pendingDeleteId = null
            },
            title = { Text(stringResource(R.string.dialog_delete_custom_theme_title)) },
            text = { Text(stringResource(R.string.dialog_delete_custom_theme_message)) },
            confirmButton = {
                androidx.compose.material3.FilledTonalButton(
                    onClick = {
                        val id = confirmDeleteId ?: return@FilledTonalButton
                        val updated = customThemes.filterNot { it.id == id }
                        onCustomThemesChange(updated)
                        if (themeConfig.customThemeId == id) {
                            onThemeConfigChange(
                                themeConfig.copy(
                                    mode = ThemeMode.Monochrome,
                                    customThemeId = null,
                                    customSeed = null,
                                    customMonochrome = false,
                                ),
                            )
                        }
                        confirmDeleteId = null
                        pendingDeleteId = null
                    },
                ) {
                    Text(stringResource(R.string.dialog_delete))
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        confirmDeleteId = null
                        pendingDeleteId = null
                    },
                ) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            },
        )
    }
}

@Composable
internal fun MotionSettingsPageContent(
    modifier: Modifier = Modifier,
    navDurationMs: Int,
    onNavDurationChange: (Int) -> Unit,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            bottom = 24.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
        ),
    ) {
        item { Spacer(modifier = Modifier.size(12.dp)) }
        item {
            SplicedColumnGroup(title = stringResource(R.string.settings_group_motion)) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                    ) {
                        val minMs = 0
                        val maxMs = 600
                        val stepMs = 10
                        val sliderValue = navDurationMs.coerceIn(minMs, maxMs).toFloat()
                        Text(
                            text = stringResource(R.string.motion_transition_duration_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = stringResource(R.string.motion_transition_duration_desc, sliderValue.toInt()),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
                        )
                        Slider(
                            value = sliderValue,
                            valueRange = minMs.toFloat()..maxMs.toFloat(),
                            steps = ((maxMs - minMs) / stepMs) - 1,
                            onValueChange = {
                                val stepped = ((it / stepMs).toInt() * stepMs).coerceIn(minMs, maxMs)
                                onNavDurationChange(stepped)
                            },
                        )
                        Text(
                            text = stringResource(R.string.motion_transition_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.size(6.dp)) }
    }
}
