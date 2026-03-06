package com.asuka.player.app

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.materialkolor.dynamiccolor.DynamicColor
import com.materialkolor.dynamiccolor.MaterialDynamicColors
import com.materialkolor.hct.Hct
import com.materialkolor.scheme.DynamicScheme
import com.materialkolor.scheme.SchemeMonochrome
import com.materialkolor.scheme.SchemeTonalSpot

internal fun colorSchemeFromSeed(seed: Color, darkTheme: Boolean): ColorScheme {
    val scheme = SchemeTonalSpot(Hct.fromInt(seed.toArgb()), darkTheme, 0.0)
    return colorSchemeFromDynamic(scheme = scheme, darkTheme = darkTheme)
}

private fun colorSchemeFromDynamic(
    scheme: DynamicScheme,
    darkTheme: Boolean,
): ColorScheme {
    val materialColors = MaterialDynamicColors()
    fun DynamicColor.c(): Color = Color(getArgb(scheme))

    return if (darkTheme) {
        darkColorScheme(
            primary = materialColors.primary().c(),
            onPrimary = materialColors.onPrimary().c(),
            primaryContainer = materialColors.primaryContainer().c(),
            onPrimaryContainer = materialColors.onPrimaryContainer().c(),
            inversePrimary = materialColors.inversePrimary().c(),
            secondary = materialColors.secondary().c(),
            onSecondary = materialColors.onSecondary().c(),
            secondaryContainer = materialColors.secondaryContainer().c(),
            onSecondaryContainer = materialColors.onSecondaryContainer().c(),
            tertiary = materialColors.tertiary().c(),
            onTertiary = materialColors.onTertiary().c(),
            tertiaryContainer = materialColors.tertiaryContainer().c(),
            onTertiaryContainer = materialColors.onTertiaryContainer().c(),
            background = materialColors.background().c(),
            onBackground = materialColors.onBackground().c(),
            surface = materialColors.surface().c(),
            onSurface = materialColors.onSurface().c(),
            surfaceDim = materialColors.surfaceDim().c(),
            surfaceBright = materialColors.surfaceBright().c(),
            surfaceContainerLowest = materialColors.surfaceContainerLowest().c(),
            surfaceContainerLow = materialColors.surfaceContainerLow().c(),
            surfaceContainer = materialColors.surfaceContainer().c(),
            surfaceContainerHigh = materialColors.surfaceContainerHigh().c(),
            surfaceContainerHighest = materialColors.surfaceContainerHighest().c(),
            surfaceVariant = materialColors.surfaceVariant().c(),
            onSurfaceVariant = materialColors.onSurfaceVariant().c(),
            surfaceTint = materialColors.primary().c(),
            inverseSurface = materialColors.inverseSurface().c(),
            inverseOnSurface = materialColors.inverseOnSurface().c(),
            error = materialColors.error().c(),
            onError = materialColors.onError().c(),
            errorContainer = materialColors.errorContainer().c(),
            onErrorContainer = materialColors.onErrorContainer().c(),
            outline = materialColors.outline().c(),
            outlineVariant = materialColors.outlineVariant().c(),
            scrim = Color(0x66000000),
        )
    } else {
        lightColorScheme(
            primary = materialColors.primary().c(),
            onPrimary = materialColors.onPrimary().c(),
            primaryContainer = materialColors.primaryContainer().c(),
            onPrimaryContainer = materialColors.onPrimaryContainer().c(),
            inversePrimary = materialColors.inversePrimary().c(),
            secondary = materialColors.secondary().c(),
            onSecondary = materialColors.onSecondary().c(),
            secondaryContainer = materialColors.secondaryContainer().c(),
            onSecondaryContainer = materialColors.onSecondaryContainer().c(),
            tertiary = materialColors.tertiary().c(),
            onTertiary = materialColors.onTertiary().c(),
            tertiaryContainer = materialColors.tertiaryContainer().c(),
            onTertiaryContainer = materialColors.onTertiaryContainer().c(),
            background = materialColors.background().c(),
            onBackground = materialColors.onBackground().c(),
            surface = materialColors.surface().c(),
            onSurface = materialColors.onSurface().c(),
            surfaceDim = materialColors.surfaceDim().c(),
            surfaceBright = materialColors.surfaceBright().c(),
            surfaceContainerLowest = materialColors.surfaceContainerLowest().c(),
            surfaceContainerLow = materialColors.surfaceContainerLow().c(),
            surfaceContainer = materialColors.surfaceContainer().c(),
            surfaceContainerHigh = materialColors.surfaceContainerHigh().c(),
            surfaceContainerHighest = materialColors.surfaceContainerHighest().c(),
            surfaceVariant = materialColors.surfaceVariant().c(),
            onSurfaceVariant = materialColors.onSurfaceVariant().c(),
            surfaceTint = materialColors.primary().c(),
            inverseSurface = materialColors.inverseSurface().c(),
            inverseOnSurface = materialColors.inverseOnSurface().c(),
            error = materialColors.error().c(),
            onError = materialColors.onError().c(),
            errorContainer = materialColors.errorContainer().c(),
            onErrorContainer = materialColors.onErrorContainer().c(),
            outline = materialColors.outline().c(),
            outlineVariant = materialColors.outlineVariant().c(),
            scrim = Color(0x66000000),
        )
    }
}

internal fun monochromeColorScheme(darkTheme: Boolean): ColorScheme {
    val scheme = SchemeMonochrome(Hct.fromInt(Color(0xFF808080).toArgb()), darkTheme, 0.0)
    return colorSchemeFromDynamic(scheme = scheme, darkTheme = darkTheme)
}

internal fun applyPureBlackScheme(
    base: ColorScheme,
): ColorScheme {
    return base.copy(
        background = Color.Black,
        onBackground = Color.White,
        surface = Color.Black,
        onSurface = Color.White,
        surfaceDim = Color.Black,
        surfaceBright = Color(0xFF141414),
        surfaceContainerLowest = Color.Black,
        surfaceContainerLow = Color(0xFF050505),
        surfaceContainer = Color(0xFF0A0A0A),
        surfaceContainerHigh = Color(0xFF101010),
        surfaceContainerHighest = Color(0xFF161616),
        surfaceVariant = Color(0xFF121212),
        onSurfaceVariant = Color(0xFFD7D7D7),
        surfaceTint = base.primary.copy(alpha = 0.9f),
        inverseSurface = Color(0xFFE6E6E6),
        inverseOnSurface = Color(0xFF121212),
        outline = Color(0xFF4A4A4A),
        outlineVariant = Color(0xFF2A2A2A),
        scrim = Color(0xB3000000),
    )
}
