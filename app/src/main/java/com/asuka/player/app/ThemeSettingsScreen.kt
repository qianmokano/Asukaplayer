package com.asuka.player.app

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.asuka.player.R

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ThemeSettingsPageContent(
    modifier: Modifier = Modifier,
    themeConfig: ThemeConfig,
    customThemes: List<CustomThemeEntry>,
    hapticsEnabled: Boolean,
    onThemeConfigChange: (ThemeConfig) -> Unit,
    onCustomThemesChange: (List<CustomThemeEntry>) -> Unit,
) {
    val isDynamicSupported = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S
    val context = LocalContext.current
    val previewDark = themeConfig.appearance == ThemeAppearanceMode.Dark ||
        (themeConfig.appearance == ThemeAppearanceMode.System && isSystemInDarkTheme())
    val haptic = LocalHapticFeedback.current
    val dynamicPreviewScheme = if (isDynamicSupported) {
        if (previewDark) androidx.compose.material3.dynamicDarkColorScheme(context)
        else androidx.compose.material3.dynamicLightColorScheme(context)
    } else {
        colorSchemeFromSeed(Color(0xFF2E6CF6), previewDark)
    }
    val customPreviewScheme = if (previewDark) {
        darkColorScheme(
            primary = Color(0xFFB6B6B6),
            onPrimary = Color(0xFF111111),
            primaryContainer = Color(0xFF3F3F3F),
            onPrimaryContainer = Color(0xFFEDEDED),
            surface = Color(0xFF1C1C1C),
            onSurface = Color(0xFFEDEDED),
            surfaceContainer = Color(0xFF2A2A2A),
            onSurfaceVariant = Color(0xFFBDBDBD),
            outline = Color(0xFF3A3A3A),
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF6E6E6E),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFE0E0E0),
            onPrimaryContainer = Color(0xFF2A2A2A),
            surface = Color(0xFFF3F3F3),
            onSurface = Color(0xFF2B2B2B),
            surfaceContainer = Color(0xFFE7E7E7),
            onSurfaceVariant = Color(0xFF6C6C6C),
            outline = Color(0xFFB5B5B5),
        )
    }
    val monoPreviewScheme = monochromeColorScheme(previewDark)

    var customHex by remember(themeConfig.customSeed) { mutableStateOf(themeConfig.customSeed?.toHex() ?: "#2E6CF6") }
    var customName by remember { mutableStateOf("") }
    var showCustomDialog by remember { mutableStateOf(false) }
    var pendingDeleteId by remember { mutableStateOf<String?>(null) }
    var confirmDeleteId by remember { mutableStateOf<String?>(null) }
    var fontScaleSlider by remember(themeConfig.fontScale) { mutableStateOf(themeConfig.fontScale) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(pendingDeleteId) {
                if (pendingDeleteId == null) return@pointerInput
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Final)
                        val anyConsumed = event.changes.any { it.isConsumed }
                        val allUp = event.changes.all { it.changedToUp() }
                        if (!anyConsumed && allUp) {
                            pendingDeleteId = null
                        }
                    }
                }
            },
        contentPadding = PaddingValues(
            bottom = 24.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
        ),
    ) {
        item { Spacer(modifier = Modifier.height(12.dp)) }

        item {
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

        item {
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
                                                else -> swatch.preset.seed?.let { remember(it, previewDark) { colorSchemeFromSeed(it, previewDark) } }
                                                    ?: dynamicPreviewScheme
                                            },
                                            selected = themeConfig.mode == swatch.preset.mode,
                                            disabled = disabled,
                                            shape = shape,
                                            size = itemSize,
                                            hapticsEnabled = hapticsEnabled,
                                            showDynamicIcon = swatch.preset.mode == ThemeMode.Dynamic,
                                            onClick = {
                                                pendingDeleteId = null
                                                if (disabled) return@ThemeSwatch
                                                onThemeConfigChange(
                                                    themeConfig.copy(
                                                        mode = swatch.preset.mode,
                                                        customSeed = swatch.preset.seed,
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
                                                pendingDeleteId = null
                                                customName = ""
                                                showCustomDialog = true
                                            },
                                        )
                                    }

                                    is ThemeSwatchItem.CustomTheme -> {
                                        ThemeSwatch(
                                            label = swatch.theme.name,
                                            scheme = remember(swatch.theme.seed, previewDark) { colorSchemeFromSeed(swatch.theme.seed, previewDark) },
                                            selected = themeConfig.mode == ThemeMode.Custom && themeConfig.customThemeId == swatch.theme.id,
                                            disabled = false,
                                            shape = shape,
                                            size = itemSize,
                                            hapticsEnabled = hapticsEnabled,
                                            showDelete = pendingDeleteId == swatch.theme.id,
                                            onClick = {
                                                if (pendingDeleteId == swatch.theme.id) {
                                                    confirmDeleteId = swatch.theme.id
                                                } else {
                                                    pendingDeleteId = null
                                                    onThemeConfigChange(
                                                        themeConfig.copy(
                                                            mode = ThemeMode.Custom,
                                                            customThemeId = swatch.theme.id,
                                                            customSeed = swatch.theme.seed,
                                                            customMonochrome = swatch.theme.monochrome,
                                                        ),
                                                    )
                                                }
                                            },
                                            onLongPress = { pendingDeleteId = swatch.theme.id },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
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
                                fontScaleSlider = it
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

        item { Spacer(modifier = Modifier.height(6.dp)) }
    }

    if (showCustomDialog) {
        val defaultThemeName = stringResource(R.string.custom_theme_default_name, customThemes.size + 1)
        CustomThemeSheet(
            initialHex = customHex,
            initialName = customName,
            previewDark = previewDark,
            hapticsEnabled = hapticsEnabled,
            onDismiss = { showCustomDialog = false },
            onSave = { name, seed ->
                val safeName = name.ifBlank { defaultThemeName }
                val entry = CustomThemeEntry(
                    id = java.util.UUID.randomUUID().toString(),
                    name = safeName,
                    seedArgb = seed.toArgb(),
                    monochrome = false,
                )
                customHex = seed.toHex()
                onCustomThemesChange(customThemes + entry)
                onThemeConfigChange(
                    themeConfig.copy(
                        mode = ThemeMode.Custom,
                        customThemeId = entry.id,
                        customSeed = entry.seed,
                        customMonochrome = false,
                    ),
                )
                showCustomDialog = false
            },
        )
    }

    if (confirmDeleteId != null) {
        AlertDialog(
            onDismissRequest = {
                confirmDeleteId = null
                pendingDeleteId = null
            },
            title = { Text(stringResource(R.string.dialog_delete_custom_theme_title)) },
            text = { Text(stringResource(R.string.dialog_delete_custom_theme_message)) },
            confirmButton = {
                FilledTonalButton(
                    onClick = {
                        val id = confirmDeleteId ?: return@FilledTonalButton
                        val updated = customThemes.filterNot { it.id == id }
                        onCustomThemesChange(updated)
                        if (themeConfig.customThemeId == id) {
                            onThemeConfigChange(
                                themeConfig.copy(
                                    mode = ThemeMode.Monochrome,
                                    customThemeId = null,
                                    customSeed = null,
                                    customMonochrome = false,
                                ),
                            )
                        }
                        confirmDeleteId = null
                        pendingDeleteId = null
                    },
                ) {
                    Text(stringResource(R.string.dialog_delete))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        confirmDeleteId = null
                        pendingDeleteId = null
                    },
                ) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            },
        )
    }
}
