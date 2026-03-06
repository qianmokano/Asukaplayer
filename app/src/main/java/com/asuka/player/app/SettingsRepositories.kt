package com.asuka.player.app

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.asuka.player.data.AppSettingsStore
import com.asuka.player.data.CustomThemeRecord
import com.asuka.player.data.PlaybackBehaviorRecord
import com.asuka.player.data.PlayerSettingsRecord
import com.asuka.player.data.UiSettingsRecord

internal class UiSettingsRepository(
    private val store: AppSettingsStore,
) {
    var themeConfig: ThemeConfig
        get() = store.loadUiSettings().toThemeConfig()
        set(value) {
            store.updateUiSettings { current ->
                current.copy(
                    themeMode = value.mode.name,
                    themeAppearance = value.appearance.name,
                    customSeedArgb = value.customSeed?.toArgb(),
                    customThemeId = value.customThemeId,
                    customMonochrome = value.customMonochrome,
                    pureBlack = value.pureBlack,
                    fontScale = value.fontScale,
                    fontScaleEnabled = value.fontScaleEnabled,
                )
            }
        }

    var customThemes: List<CustomThemeEntry>
        get() = store.loadUiSettings().customThemes.toCustomThemeEntries()
        set(value) {
            store.updateUiSettings { current ->
                current.copy(customThemes = value.toCustomThemeRecords())
            }
        }

    var navDurationMs: Int
        get() = store.loadUiSettings().navDurationMs
        set(value) {
            store.updateUiSettings { current ->
                current.copy(navDurationMs = value)
            }
        }

    var hapticFeedbackEnabled: Boolean
        get() = store.loadUiSettings().hapticFeedbackEnabled
        set(value) {
            store.updateUiSettings { current ->
                current.copy(hapticFeedbackEnabled = value)
            }
        }
}

internal class PlayerSettingsRepository(
    private val store: AppSettingsStore,
) {
    var playerSettings: PlayerSettingsConfig
        get() = store.loadPlayerSettings().toPlayerSettingsConfig()
        set(value) {
            store.savePlayerSettings(value.toPlayerSettingsRecord())
        }
}

internal class PlaybackBehaviorRepository(
    private val store: AppSettingsStore,
) {
    var keepConnectionInBackground: Boolean
        get() = store.loadPlaybackBehavior().keepConnectionInBackground
        set(value) {
            store.savePlaybackBehavior(PlaybackBehaviorRecord(keepConnectionInBackground = value))
        }
}

private fun AppSettingsStore.updateUiSettings(transform: (UiSettingsRecord) -> UiSettingsRecord) {
    saveUiSettings(transform(loadUiSettings()))
}

private fun UiSettingsRecord.toThemeConfig(): ThemeConfig {
    return ThemeConfig(
        mode = runCatching { ThemeMode.valueOf(themeMode) }.getOrDefault(ThemeMode.Monochrome),
        customSeed = customSeedArgb?.let(::Color),
        customThemeId = customThemeId,
        customMonochrome = customMonochrome,
        appearance = runCatching { ThemeAppearanceMode.valueOf(themeAppearance) }
            .getOrDefault(ThemeAppearanceMode.System),
        pureBlack = pureBlack,
        fontScale = fontScale,
        fontScaleEnabled = fontScaleEnabled,
    )
}

private fun List<CustomThemeRecord>.toCustomThemeEntries(): List<CustomThemeEntry> {
    return map { record ->
        CustomThemeEntry(
            id = record.id,
            name = record.name,
            seedArgb = record.seedArgb,
            monochrome = record.monochrome,
        )
    }
}

private fun List<CustomThemeEntry>.toCustomThemeRecords(): List<CustomThemeRecord> {
    return map { theme ->
        CustomThemeRecord(
            id = theme.id,
            name = theme.name,
            seedArgb = theme.seedArgb,
            monochrome = theme.monochrome,
        )
    }
}

private fun PlayerSettingsRecord.toPlayerSettingsConfig(): PlayerSettingsConfig {
    return PlayerSettingsConfig(
        seekGestureEnabled = seekGestureEnabled,
        brightnessGestureEnabled = brightnessGestureEnabled,
        volumeGestureEnabled = volumeGestureEnabled,
        zoomGestureEnabled = zoomGestureEnabled,
        panGestureEnabled = panGestureEnabled,
        doubleTapGestureEnabled = doubleTapGestureEnabled,
        doubleTapAction = DoubleTapActionSetting.fromValue(doubleTapAction),
        longPressGestureEnabled = longPressGestureEnabled,
        seekIncrementSec = seekIncrementSec,
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
        rememberSelections = rememberSelections,
    )
}

private fun PlayerSettingsConfig.toPlayerSettingsRecord(): PlayerSettingsRecord {
    return PlayerSettingsRecord(
        seekGestureEnabled = seekGestureEnabled,
        brightnessGestureEnabled = brightnessGestureEnabled,
        volumeGestureEnabled = volumeGestureEnabled,
        zoomGestureEnabled = zoomGestureEnabled,
        panGestureEnabled = panGestureEnabled,
        doubleTapGestureEnabled = doubleTapGestureEnabled,
        doubleTapAction = doubleTapAction.value,
        longPressGestureEnabled = longPressGestureEnabled,
        seekIncrementSec = seekIncrementSec,
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
        rememberSelections = rememberSelections,
    )
}
