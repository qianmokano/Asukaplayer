package com.asuka.player.runtime

import com.asuka.player.data.APP_LANGUAGE_SYSTEM

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

// Native display names stay untranslated so each option reads in its own language.
enum class AppLanguagePreference(val languageTag: String?, val nativeDisplayName: String) {
    System(languageTag = null, nativeDisplayName = ""),
    English(languageTag = "en", nativeDisplayName = "English"),
    Chinese(languageTag = "zh-CN", nativeDisplayName = "简体中文"),
    Spanish(languageTag = "es", nativeDisplayName = "Español"),
    ;

    val storageKey: String
        get() = languageTag ?: APP_LANGUAGE_SYSTEM

    companion object {
        fun fromStorageKey(value: String): AppLanguagePreference {
            return entries.firstOrNull { it.storageKey == value } ?: System
        }
    }
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
