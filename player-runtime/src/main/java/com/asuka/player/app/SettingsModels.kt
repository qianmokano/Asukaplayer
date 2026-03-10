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

data class CustomThemeEntry(
    val id: String,
    val name: String,
    val seedArgb: Int,
    val monochrome: Boolean,
)
