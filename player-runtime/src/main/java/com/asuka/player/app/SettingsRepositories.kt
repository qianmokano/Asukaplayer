package com.asuka.player.runtime

import com.asuka.player.contract.PlaybackRuntimeSettings
import com.asuka.player.contract.PlaybackRuntimeSettingsSource
import com.asuka.player.contract.PlayerSettings
import com.asuka.player.data.AppSettingsStore
import com.asuka.player.data.AppSettingsSnapshot
import com.asuka.player.data.CustomThemeRecord
import com.asuka.player.data.PlaybackBehaviorRecord
import com.asuka.player.data.PlayerSettingsRecord
import com.asuka.player.data.UiSettingsRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.runBlocking

data class UiSettingsState(
    val themeConfig: ThemeConfig,
    val customThemes: List<CustomThemeEntry>,
    val navDurationMs: Int,
    val hapticFeedbackEnabled: Boolean,
)

class UiSettingsRepository(
    private val store: AppSettingsStore,
    scope: CoroutineScope,
) {
    val settings: StateFlow<UiSettingsState> = store.snapshots
        .map { it.uiSettings.toUiSettingsState() }
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = store.loadedSnapshot().uiSettings.toUiSettingsState(),
        )

    suspend fun setThemeConfig(value: ThemeConfig) {
        if (settings.value.themeConfig == value) return
        store.saveUiSettings(settings.value.copy(themeConfig = value).toUiSettingsRecord())
    }

    suspend fun setCustomThemes(value: List<CustomThemeEntry>) {
        if (settings.value.customThemes == value) return
        store.saveUiSettings(settings.value.copy(customThemes = value).toUiSettingsRecord())
    }

    suspend fun setNavDurationMs(value: Int) {
        if (settings.value.navDurationMs == value) return
        store.saveUiSettings(settings.value.copy(navDurationMs = value).toUiSettingsRecord())
    }

    suspend fun setHapticFeedbackEnabled(value: Boolean) {
        if (settings.value.hapticFeedbackEnabled == value) return
        store.saveUiSettings(settings.value.copy(hapticFeedbackEnabled = value).toUiSettingsRecord())
    }
}

class PlayerSettingsRepository(
    private val store: AppSettingsStore,
    scope: CoroutineScope,
) {
    val settings: StateFlow<PlayerSettings> = store.snapshots
        .map { it.playerSettings.toPlayerSettings() }
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = store.loadedSnapshot().playerSettings.toPlayerSettings(),
        )

    suspend fun setPlayerSettings(value: PlayerSettings) {
        if (settings.value == value) return
        store.savePlayerSettings(value.toPlayerSettingsRecord())
    }
}

class PlaybackBehaviorRepository(
    private val store: AppSettingsStore,
    scope: CoroutineScope,
) {
    val settings: StateFlow<PlaybackBehaviorRecord> = store.snapshots
        .map { it.playbackBehavior }
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = store.loadedSnapshot().playbackBehavior,
        )

    val keepConnectionInBackground: Boolean
        get() = settings.value.keepConnectionInBackground

    val rememberedBrightness: Float?
        get() = settings.value.rememberedBrightness

    suspend fun setKeepConnectionInBackground(value: Boolean) {
        if (settings.value.keepConnectionInBackground == value) return
        store.savePlaybackBehavior(settings.value.copy(keepConnectionInBackground = value))
    }

    suspend fun setRememberedBrightness(value: Float?) {
        val clamped = value?.coerceIn(0f, 1f)
        if (settings.value.rememberedBrightness == clamped) return
        store.savePlaybackBehavior(settings.value.copy(rememberedBrightness = clamped))
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

private fun AppSettingsStore.loadedSnapshot(): AppSettingsSnapshot {
    return runBlocking {
        awaitLoaded()
        loadSnapshot()
    }
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
        else -> PlayerSettings.DoubleTapAction.TogglePlayPause
    }
}

private val PlayerSettings.DoubleTapAction.value: String
    get() = when (this) {
        PlayerSettings.DoubleTapAction.Seek -> "seek"
        PlayerSettings.DoubleTapAction.TogglePlayPause -> "toggle_play_pause"
        PlayerSettings.DoubleTapAction.Both -> "both"
    }
