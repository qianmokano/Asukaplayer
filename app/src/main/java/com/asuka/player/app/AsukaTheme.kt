package com.asuka.player.app

import android.app.Activity
import android.graphics.drawable.ColorDrawable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.core.view.WindowCompat
import com.asuka.player.runtime.ThemeAppearanceMode
import com.asuka.player.runtime.ThemeConfig
import com.asuka.player.runtime.ThemeMode

@Composable
internal fun AsukaTheme(
    themeConfig: ThemeConfig,
    content: @Composable () -> Unit,
) {
    val isSystemDark = isSystemInDarkTheme()
    val dark = when (themeConfig.appearance) {
        ThemeAppearanceMode.System -> isSystemDark
        ThemeAppearanceMode.Light -> false
        ThemeAppearanceMode.Dark -> true
    }
    val presetSeed = ThemePresets.firstOrNull { it.mode == themeConfig.mode }?.seed
    val seed = when (themeConfig.mode) {
        ThemeMode.Custom -> themeConfig.customSeedColor ?: Color(0xFF2E6CF6)
        ThemeMode.Dynamic -> null
        ThemeMode.Monochrome -> null
        else -> presetSeed ?: Color(0xFF2E6CF6)
    }
    val baseScheme = when {
        themeConfig.mode == ThemeMode.Dynamic &&
            android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S ->
            if (dark) androidx.compose.material3.dynamicDarkColorScheme(LocalContext.current)
            else androidx.compose.material3.dynamicLightColorScheme(LocalContext.current)

        themeConfig.mode == ThemeMode.Monochrome ||
            (themeConfig.mode == ThemeMode.Custom && themeConfig.customMonochrome) ->
            monochromeColorScheme(dark)

        seed != null -> colorSchemeFromSeed(seed = seed, darkTheme = dark)
        else -> if (dark) darkColorScheme() else lightColorScheme()
    }
    val scheme = if (themeConfig.pureBlack && dark) applyPureBlackScheme(baseScheme) else baseScheme
    val scaledTypography = MaterialTheme.typography.scaled(
        if (themeConfig.fontScaleEnabled) themeConfig.fontScale else 1f,
    )
    val view = LocalView.current
    val window = (view.context as? Activity)?.window
    if (window != null && !view.isInEditMode) {
        SideEffect {
            @Suppress("DEPRECATION")
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            @Suppress("DEPRECATION")
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            window.setBackgroundDrawable(ColorDrawable(scheme.surfaceContainerLowest.toArgb()))
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
    }

    MaterialTheme(
        colorScheme = scheme,
        typography = scaledTypography,
        content = content,
    )
}

private fun Typography.scaled(factor: Float): Typography {
    val safeFactor = factor.coerceIn(0.85f, 1.3f)
    fun TextStyle.scaled() = copy(
        fontSize = fontSize * safeFactor,
        lineHeight = lineHeight * safeFactor,
        letterSpacing = letterSpacing * safeFactor,
    )
    return Typography(
        displayLarge = displayLarge.scaled(),
        displayMedium = displayMedium.scaled(),
        displaySmall = displaySmall.scaled(),
        headlineLarge = headlineLarge.scaled(),
        headlineMedium = headlineMedium.scaled(),
        headlineSmall = headlineSmall.scaled(),
        titleLarge = titleLarge.scaled(),
        titleMedium = titleMedium.scaled(),
        titleSmall = titleSmall.scaled(),
        bodyLarge = bodyLarge.scaled(),
        bodyMedium = bodyMedium.scaled(),
        bodySmall = bodySmall.scaled(),
        labelLarge = labelLarge.scaled(),
        labelMedium = labelMedium.scaled(),
        labelSmall = labelSmall.scaled(),
    )
}
