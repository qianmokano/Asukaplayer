package com.asuka.player.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.asuka.player.R
import com.asuka.player.runtime.CustomThemeEntry
import com.asuka.player.runtime.ThemeAppearanceMode
import com.asuka.player.runtime.ThemeConfig
import com.asuka.player.runtime.ThemeMode

@Composable
internal fun ThemeAppearanceSection(
    themeConfig: ThemeConfig,
    hapticsEnabled: Boolean,
    onThemeConfigChange: (ThemeConfig) -> Unit,
) {
    val haptic = LocalHapticFeedback.current

    SplicedColumnGroup(title = stringResource(R.string.settings_group_theme_mode)) {
        item {
            ThemeSectionBlock(
                title = stringResource(R.string.settings_appearance_title),
                description = stringResource(R.string.settings_appearance_desc),
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ThemeAppearanceMode.values().forEach { appearance ->
                        FilterChip(
                            selected = themeConfig.appearance == appearance,
                            onClick = {
                                if (hapticsEnabled) {
                                    haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                                }
                                onThemeConfigChange(themeConfig.copy(appearance = appearance))
                            },
                            label = {
                                Text(
                                    when (appearance) {
                                        ThemeAppearanceMode.System -> stringResource(R.string.appearance_auto)
                                        ThemeAppearanceMode.Light -> stringResource(R.string.appearance_light)
                                        ThemeAppearanceMode.Dark -> stringResource(R.string.appearance_dark)
                                    },
                                )
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun ThemeColorSection(
    themeConfig: ThemeConfig,
    customThemes: List<CustomThemeEntry>,
    isDynamicSupported: Boolean,
    previewDark: Boolean,
    dynamicPreviewScheme: ColorScheme,
    customPreviewScheme: ColorScheme,
    monoPreviewScheme: ColorScheme,
    hapticsEnabled: Boolean,
    pendingDeleteId: String?,
    onPendingDeleteIdChange: (String?) -> Unit,
    onConfirmDeleteIdChange: (String?) -> Unit,
    onThemeConfigChange: (ThemeConfig) -> Unit,
    onOpenCustomDialog: () -> Unit,
) {
    SplicedColumnGroup(title = stringResource(R.string.settings_group_theme_color)) {
        item {
            ThemeSectionBlock(
                title = stringResource(R.string.settings_color_scheme_title),
                description = if (isDynamicSupported) {
                    stringResource(R.string.settings_color_scheme_dynamic_supported)
                } else {
                    stringResource(R.string.settings_color_scheme_dynamic_unsupported)
                },
            ) {
                val swatches = buildList<ThemeSwatchItem> {
                    ThemePresets.filter { it.mode != ThemeMode.Custom }.forEach { add(ThemeSwatchItem.Preset(it)) }
                    customThemes.forEach { add(ThemeSwatchItem.CustomTheme(it)) }
                    add(ThemeSwatchItem.CustomAdd)
                }
                val columns = 4
                val itemSize = 84.dp
                val itemSpacing = 2.dp
                val rows = (swatches.size + columns - 1) / columns
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columns),
                    userScrollEnabled = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemSize * rows + itemSpacing * (rows - 1)),
                    verticalArrangement = Arrangement.spacedBy(itemSpacing),
                    horizontalArrangement = Arrangement.spacedBy(itemSpacing),
                ) {
                    items(swatches.size) { index ->
                        val swatch = swatches[index]
                        val preset = (swatch as? ThemeSwatchItem.Preset)?.preset
                        val disabled = preset?.mode == ThemeMode.Dynamic && !isDynamicSupported
                        val shape = gridItemShape(
                            index = index,
                            total = swatches.size,
                            columns = columns,
                            outerCorner = 16.dp,
                            innerCorner = 6.dp,
                        )
                        when (swatch) {
                            is ThemeSwatchItem.Preset -> {
                                ThemeSwatch(
                                    label = stringResource(swatch.preset.nameResId),
                                    scheme = when (swatch.preset.mode) {
                                        ThemeMode.Dynamic -> dynamicPreviewScheme
                                        ThemeMode.Monochrome -> monoPreviewScheme
                                        else -> swatch.preset.seed?.let {
                                            remember(it, previewDark) { colorSchemeFromSeed(it, previewDark) }
                                        } ?: dynamicPreviewScheme
                                    },
                                    selected = themeConfig.mode == swatch.preset.mode,
                                    disabled = disabled,
                                    shape = shape,
                                    size = itemSize,
                                    hapticsEnabled = hapticsEnabled,
                                    showDynamicIcon = swatch.preset.mode == ThemeMode.Dynamic,
                                    onClick = {
                                        onPendingDeleteIdChange(null)
                                        if (disabled) return@ThemeSwatch
                                        onThemeConfigChange(
                                            themeConfig.copy(
                                                mode = swatch.preset.mode,
                                                customSeedArgb = swatch.preset.seed?.toArgb(),
                                                customThemeId = null,
                                                customMonochrome = false,
                                            ),
                                        )
                                    },
                                )
                            }

                            is ThemeSwatchItem.CustomAdd -> {
                                ThemeSwatch(
                                    label = stringResource(R.string.theme_preset_custom),
                                    scheme = customPreviewScheme,
                                    selected = themeConfig.mode == ThemeMode.Custom && themeConfig.customThemeId == null,
                                    disabled = false,
                                    shape = shape,
                                    size = itemSize,
                                    hapticsEnabled = hapticsEnabled,
                                    icon = Icons.Outlined.Add,
                                    onClick = {
                                        onPendingDeleteIdChange(null)
                                        onOpenCustomDialog()
                                    },
                                )
                            }

                            is ThemeSwatchItem.CustomTheme -> {
                                ThemeSwatch(
                                    label = swatch.theme.name,
                                    scheme = remember(swatch.theme.seedArgb, previewDark) {
                                        colorSchemeFromSeed(swatch.theme.seedColor, previewDark)
                                    },
                                    selected = themeConfig.mode == ThemeMode.Custom && themeConfig.customThemeId == swatch.theme.id,
                                    disabled = false,
                                    shape = shape,
                                    size = itemSize,
                                    hapticsEnabled = hapticsEnabled,
                                    showDelete = pendingDeleteId == swatch.theme.id,
                                    onClick = {
                                        if (pendingDeleteId == swatch.theme.id) {
                                            onConfirmDeleteIdChange(swatch.theme.id)
                                        } else {
                                            onPendingDeleteIdChange(null)
                                            onThemeConfigChange(
                                                themeConfig.copy(
                                                    mode = ThemeMode.Custom,
                                                    customThemeId = swatch.theme.id,
                                                    customSeedArgb = swatch.theme.seedArgb,
                                                    customMonochrome = swatch.theme.monochrome,
                                                ),
                                            )
                                        }
                                    },
                                    onLongPress = { onPendingDeleteIdChange(swatch.theme.id) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun ThemeDisplaySection(
    themeConfig: ThemeConfig,
    fontScaleSlider: Float,
    onFontScaleSliderChange: (Float) -> Unit,
    onThemeConfigChange: (ThemeConfig) -> Unit,
) {
    SplicedColumnGroup(title = stringResource(R.string.settings_group_display)) {
        item {
            SettingsToggleItem(
                title = stringResource(R.string.settings_pure_black_title),
                description = stringResource(R.string.settings_pure_black_desc),
                checked = themeConfig.pureBlack,
                onCheckedChange = { onThemeConfigChange(themeConfig.copy(pureBlack = it)) },
            )
        }
        item {
            SettingsToggleItem(
                title = stringResource(R.string.settings_font_scale_title),
                description = stringResource(R.string.settings_font_scale_desc),
                checked = themeConfig.fontScaleEnabled,
                onCheckedChange = { enabled ->
                    onThemeConfigChange(themeConfig.copy(fontScaleEnabled = enabled))
                },
            )
        }
        item {
            ThemeSectionBlock(
                title = stringResource(R.string.settings_font_scale_ratio_title),
                description = stringResource(R.string.settings_font_scale_ratio_desc, (fontScaleSlider * 100).toInt()),
            ) {
                Slider(
                    value = fontScaleSlider,
                    onValueChange = {
                        onFontScaleSliderChange(it)
                        onThemeConfigChange(
                            themeConfig.copy(
                                fontScale = it,
                                fontScaleEnabled = true,
                            ),
                        )
                    },
                    valueRange = 0.85f..1.3f,
                    enabled = themeConfig.fontScaleEnabled,
                )
            }
        }
    }
}
