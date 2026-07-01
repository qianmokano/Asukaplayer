package com.asuka.player.app

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.asuka.player.R
import com.asuka.player.runtime.CustomThemeEntry
import com.asuka.player.runtime.ThemeAppearanceMode
import com.asuka.player.runtime.ThemeConfig
import com.asuka.player.runtime.ThemeMode

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

    var customHex by remember(themeConfig.customSeedArgb) {
        mutableStateOf(themeConfig.customSeedColor?.toHex() ?: "#2E6CF6")
    }
    var customName by remember { mutableStateOf("") }
    var showCustomDialog by remember { mutableStateOf(false) }
    var pendingDeleteId by remember { mutableStateOf<String?>(null) }
    var confirmDeleteId by remember { mutableStateOf<String?>(null) }
    var fontScaleSlider by remember(themeConfig.fontScale) { mutableFloatStateOf(themeConfig.fontScale) }

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
            ThemeAppearanceSection(
                themeConfig = themeConfig,
                hapticsEnabled = hapticsEnabled,
                onThemeConfigChange = onThemeConfigChange,
            )
        }

        item {
            ThemeColorSection(
                themeConfig = themeConfig,
                customThemes = customThemes,
                isDynamicSupported = isDynamicSupported,
                previewDark = previewDark,
                dynamicPreviewScheme = dynamicPreviewScheme,
                customPreviewScheme = customPreviewScheme,
                monoPreviewScheme = monoPreviewScheme,
                hapticsEnabled = hapticsEnabled,
                pendingDeleteId = pendingDeleteId,
                onPendingDeleteIdChange = { pendingDeleteId = it },
                onConfirmDeleteIdChange = { confirmDeleteId = it },
                onThemeConfigChange = onThemeConfigChange,
                onOpenCustomDialog = {
                    customName = ""
                    showCustomDialog = true
                },
            )
        }

        item {
            ThemeDisplaySection(
                themeConfig = themeConfig,
                fontScaleSlider = fontScaleSlider,
                onFontScaleSliderChange = { fontScaleSlider = it },
                onThemeConfigChange = onThemeConfigChange,
            )
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
                        customSeedArgb = entry.seedArgb,
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
                                        customSeedArgb = null,
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
