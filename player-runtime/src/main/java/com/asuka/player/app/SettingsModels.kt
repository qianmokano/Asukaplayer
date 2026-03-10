package com.asuka.player.app

enum class ThemeMode {
    Dynamic,
    Custom,
    Violet,
    Teal,
    Red,
    Sandstone,
    Neutral,
    Monochrome,
}

enum class ThemeAppearanceMode {
    System,
    Light,
    Dark,
}

data class ThemeConfig(
    val mode: ThemeMode = ThemeMode.Monochrome,
    val customSeedArgb: Int? = null,
    val customThemeId: String? = null,
    val customMonochrome: Boolean = false,
    val appearance: ThemeAppearanceMode = ThemeAppearanceMode.System,
    val pureBlack: Boolean = true,
    val fontScale: Float = 1.0f,
    val fontScaleEnabled: Boolean = false,
)

enum class DoubleTapActionSetting(val value: String) {
    Seek("seek"),
    TogglePlayPause("toggle_play_pause"),
    Both("both");

    companion object {
        fun fromValue(raw: String?): DoubleTapActionSetting =
            entries.firstOrNull { it.value == raw } ?: Seek
    }
}

data class PlayerSettingsConfig(
    val seekGestureEnabled: Boolean = true,
    val brightnessGestureEnabled: Boolean = true,
    val volumeGestureEnabled: Boolean = true,
    val zoomGestureEnabled: Boolean = true,
    val panGestureEnabled: Boolean = true,
    val doubleTapGestureEnabled: Boolean = true,
    val doubleTapAction: DoubleTapActionSetting = DoubleTapActionSetting.Seek,
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
    val rememberSelections: Boolean = true,
)

data class CustomThemeEntry(
    val id: String,
    val name: String,
    val seedArgb: Int,
    val monochrome: Boolean,
)
