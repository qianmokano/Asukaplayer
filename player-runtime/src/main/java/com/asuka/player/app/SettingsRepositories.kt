package com.asuka.player.app
import com.asuka.player.core.PlaybackRuntimeSettings
import com.asuka.player.core.PlaybackRuntimeSettingsSource
import com.asuka.player.core.PlayerSettings
import com.asuka.player.data.AppSettingsStore
import com.asuka.player.data.CustomThemeRecord
import com.asuka.player.data.PlaybackBehaviorRecord
import com.asuka.player.data.PlayerSettingsRecord
import com.asuka.player.data.UiSettingsRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class UiSettingsState(
    val themeConfig: ThemeConfig,
    val customThemes: List<CustomThemeEntry>,
    val navDurationMs: Int,
    val hapticFeedbackEnabled: Boolean,
)

class UiSettingsRepository(
    private val store: AppSettingsStore,
) {
    private val _settings = MutableStateFlow(store.loadUiSettings().toUiSettingsState())

    val settings: StateFlow<UiSettingsState> = _settings.asStateFlow()

    var themeConfig: ThemeConfig
        get() = settings.value.themeConfig
        set(value) {
            if (settings.value.themeConfig == value) return
            persist(settings.value.copy(themeConfig = value))
        }

    var customThemes: List<CustomThemeEntry>
        get() = settings.value.customThemes
        set(value) {
            if (settings.value.customThemes == value) return
            persist(settings.value.copy(customThemes = value))
        }

    var navDurationMs: Int
        get() = settings.value.navDurationMs
        set(value) {
            if (settings.value.navDurationMs == value) return
            persist(settings.value.copy(navDurationMs = value))
        }

    var hapticFeedbackEnabled: Boolean
        get() = settings.value.hapticFeedbackEnabled
        set(value) {
            if (settings.value.hapticFeedbackEnabled == value) return
            persist(settings.value.copy(hapticFeedbackEnabled = value))
        }

    private fun persist(value: UiSettingsState) {
        _settings.value = value
        store.saveUiSettings(value.toUiSettingsRecord())
    }
}

class PlayerSettingsRepository(
    private val store: AppSettingsStore,
) {
    private val _settings = MutableStateFlow(store.loadPlayerSettings().toPlayerSettings())

    val settings: StateFlow<PlayerSettings> = _settings.asStateFlow()

    var playerSettings: PlayerSettings
        get() = settings.value
        set(value) {
            if (settings.value == value) return
            _settings.value = value
            store.savePlayerSettings(value.toPlayerSettingsRecord())
        }
}

class PlaybackBehaviorRepository(
    private val store: AppSettingsStore,
) {
    private val _settings = MutableStateFlow(store.loadPlaybackBehavior())

    val settings: StateFlow<PlaybackBehaviorRecord> = _settings.asStateFlow()

    var keepConnectionInBackground: Boolean
        get() = settings.value.keepConnectionInBackground
        set(value) {
            if (settings.value.keepConnectionInBackground == value) return
            persist(settings.value.copy(keepConnectionInBackground = value))
        }

    var rememberedBrightness: Float?
        get() = settings.value.rememberedBrightness
        set(value) {
            val clamped = value?.coerceIn(0f, 1f)
            if (settings.value.rememberedBrightness == clamped) return
            persist(settings.value.copy(rememberedBrightness = clamped))
        }

    private fun persist(value: PlaybackBehaviorRecord) {
        _settings.value = value
        store.savePlaybackBehavior(value)
    }
}

class AppPlaybackRuntimeSettingsSource(
    private val playerSettingsRepository: PlayerSettingsRepository,
    private val playbackBehaviorRepository: PlaybackBehaviorRepository,
    scope: CoroutineScope,
) : PlaybackRuntimeSettingsSource {
    override fun current(): PlaybackRuntimeSettings {
        return playerSettingsRepository.settings.value.toRuntimeSettings(
            keepConnectionInBackground = playbackBehaviorRepository.settings.value.keepConnectionInBackground,
        )
    }

    override val settings: StateFlow<PlaybackRuntimeSettings> =
        combine(
            playerSettingsRepository.settings,
            playbackBehaviorRepository.settings,
        ) { playerSettings, behavior ->
            playerSettings.toRuntimeSettings(
                keepConnectionInBackground = behavior.keepConnectionInBackground,
            )
        }.stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = current(),
        )
}

private fun UiSettingsRecord.toUiSettingsState(): UiSettingsState {
    return UiSettingsState(
        themeConfig = toThemeConfig(),
        customThemes = customThemes.toCustomThemeEntries(),
        navDurationMs = navDurationMs,
        hapticFeedbackEnabled = hapticFeedbackEnabled,
    )
}

private fun UiSettingsState.toUiSettingsRecord(): UiSettingsRecord {
    return UiSettingsRecord(
        themeMode = themeConfig.mode.name,
        themeAppearance = themeConfig.appearance.name,
        customSeedArgb = themeConfig.customSeedArgb,
        customThemeId = themeConfig.customThemeId,
        customMonochrome = themeConfig.customMonochrome,
        pureBlack = themeConfig.pureBlack,
        fontScale = themeConfig.fontScale,
        fontScaleEnabled = themeConfig.fontScaleEnabled,
        customThemes = customThemes.toCustomThemeRecords(),
        navDurationMs = navDurationMs,
        hapticFeedbackEnabled = hapticFeedbackEnabled,
    )
}

private fun UiSettingsRecord.toThemeConfig(): ThemeConfig {
    return ThemeConfig(
        mode = runCatching { ThemeMode.valueOf(themeMode) }.getOrDefault(ThemeMode.Monochrome),
        customSeedArgb = customSeedArgb,
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

private fun PlayerSettingsRecord.toPlayerSettings(): PlayerSettings {
    return PlayerSettings(
        seekGestureEnabled = seekGestureEnabled,
        brightnessGestureEnabled = brightnessGestureEnabled,
        volumeGestureEnabled = volumeGestureEnabled,
        zoomGestureEnabled = zoomGestureEnabled,
        panGestureEnabled = panGestureEnabled,
        doubleTapGestureEnabled = doubleTapGestureEnabled,
        doubleTapAction = doubleTapAction.toDoubleTapAction(),
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

private fun PlayerSettings.toPlayerSettingsRecord(): PlayerSettingsRecord {
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

fun PlayerSettings.toRuntimeSettings(
    keepConnectionInBackground: Boolean,
): PlaybackRuntimeSettings {
    return PlaybackRuntimeSettings(
        playerSettings = this,
        keepSessionConnectionInBackground = keepConnectionInBackground,
    )
}

private fun String?.toDoubleTapAction(): PlayerSettings.DoubleTapAction {
    return when (this) {
        "toggle_play_pause" -> PlayerSettings.DoubleTapAction.TogglePlayPause
        "both" -> PlayerSettings.DoubleTapAction.Both
        else -> PlayerSettings.DoubleTapAction.Seek
    }
}

private val PlayerSettings.DoubleTapAction.value: String
    get() = when (this) {
        PlayerSettings.DoubleTapAction.Seek -> "seek"
        PlayerSettings.DoubleTapAction.TogglePlayPause -> "toggle_play_pause"
        PlayerSettings.DoubleTapAction.Both -> "both"
    }
