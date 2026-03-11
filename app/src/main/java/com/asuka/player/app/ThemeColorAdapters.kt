package com.asuka.player.app

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.asuka.player.runtime.CustomThemeEntry
import com.asuka.player.runtime.ThemeConfig

internal val ThemeConfig.customSeedColor: Color?
    get() = customSeedArgb?.let(::Color)

internal val CustomThemeEntry.seedColor: Color
    get() = Color(seedArgb)

internal fun ThemeConfig.withCustomSeed(color: Color?): ThemeConfig {
    return copy(customSeedArgb = color?.toArgb())
}
