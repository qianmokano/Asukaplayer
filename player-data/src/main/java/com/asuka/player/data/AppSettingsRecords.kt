package com.asuka.player.data

data class CustomThemeRecord(
    val id: String,
    val name: String,
    val seedArgb: Int,
    val monochrome: Boolean,
)

data class UiSettingsRecord(
    val themeMode: String = "Monochrome",
    val themeAppearance: String = "System",
    val customSeedArgb: Int? = null,
    val customThemeId: String? = null,
    val customMonochrome: Boolean = false,
    val pureBlack: Boolean = true,
    val fontScale: Float = 1.0f,
    val fontScaleEnabled: Boolean = false,
    val customThemes: List<CustomThemeRecord> = emptyList(),
    val navDurationMs: Int = 300,
    val hapticFeedbackEnabled: Boolean = true,
)

data class PlayerSettingsRecord(
    val seekGestureEnabled: Boolean = true,
    val brightnessGestureEnabled: Boolean = true,
    val volumeGestureEnabled: Boolean = true,
    val zoomGestureEnabled: Boolean = true,
    val panGestureEnabled: Boolean = true,
    val doubleTapGestureEnabled: Boolean = true,
    val doubleTapAction: String = "toggle_play_pause",
    val longPressGestureEnabled: Boolean = true,
    val seekIncrementSec: Int = 10,
    val seekSensitivity: Float = 1.0f,
    val longPressSpeed: Float = 2.0f,
    val controllerTimeoutSec: Int = 3,
    val hideButtonsBackground: Boolean = false,
    val resumePlayback: Boolean = true,
    val defaultPlaybackSpeed: Float = 1.0f,
    val autoplay: Boolean = true,
    val autoPip: Boolean = true,
    val autoBackgroundPlay: Boolean = false,
    val rememberBrightness: Boolean = false,
    val rememberVolume: Boolean = false,
    val rememberSelections: Boolean = true,
)

data class PlaybackBehaviorRecord(
    val keepConnectionInBackground: Boolean = true,
    val rememberedBrightness: Float? = null,
    val rememberedVolumePercent: Int? = null,
)

data class AppSettingsSnapshot(
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val uiSettings: UiSettingsRecord = UiSettingsRecord(),
    val playerSettings: PlayerSettingsRecord = PlayerSettingsRecord(),
    val playbackBehavior: PlaybackBehaviorRecord = PlaybackBehaviorRecord(),
) {
    fun normalized(): AppSettingsSnapshot {
        return copy(
            schemaVersion = CURRENT_SCHEMA_VERSION,
            uiSettings = uiSettings.normalized(),
            playerSettings = playerSettings.normalized(),
            playbackBehavior = playbackBehavior.normalized(),
        )
    }

    companion object {
        const val CURRENT_SCHEMA_VERSION = 1
    }
}

internal fun UiSettingsRecord.normalized(): UiSettingsRecord {
    return copy(
        fontScale = fontScale.coerceIn(0.85f, 1.3f),
        customThemes = customThemes.normalized(),
        navDurationMs = navDurationMs.coerceIn(0, 2000),
    )
}

internal fun PlayerSettingsRecord.normalized(): PlayerSettingsRecord {
    return copy(
        seekIncrementSec = seekIncrementSec.coerceIn(1, 60),
        seekSensitivity = seekSensitivity.coerceIn(0.1f, 2.0f),
        longPressSpeed = longPressSpeed.coerceIn(0.2f, 4.0f),
        controllerTimeoutSec = controllerTimeoutSec.coerceIn(1, 60),
        defaultPlaybackSpeed = defaultPlaybackSpeed.coerceIn(0.2f, 4.0f),
    )
}

internal fun PlaybackBehaviorRecord.normalized(): PlaybackBehaviorRecord {
    return copy(
        rememberedBrightness = rememberedBrightness?.coerceIn(0f, 1f),
        rememberedVolumePercent = rememberedVolumePercent?.coerceIn(0, 100),
    )
}

internal fun List<CustomThemeRecord>.normalized(): List<CustomThemeRecord> {
    return filter { it.id.isNotBlank() && it.name.isNotBlank() }
}
