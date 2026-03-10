package com.asuka.player.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.asuka.player.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CustomThemeSheet(
    initialHex: String,
    initialName: String,
    previewDark: Boolean,
    hapticsEnabled: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, Color) -> Unit,
) {
    var hex by remember(initialHex) { mutableStateOf(initialHex) }
    var name by remember(initialName) { mutableStateOf(initialName) }
    var isError by remember { mutableStateOf(false) }
    val parsed = parseHexColor(hex)
    val defaultColor = MaterialTheme.colorScheme.primary
    var currentColor by remember { mutableStateOf(parsed ?: defaultColor) }
    val paletteMode = remember { mutableStateOf(CustomPaletteMode.Grid) }
    val haptic = LocalHapticFeedback.current

    if (parsed != null && parsed.toArgb() != currentColor.toArgb()) {
        currentColor = parsed
    }

    val previewScheme = colorSchemeFromSeed(currentColor, previewDark)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = androidx.compose.material3.rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
        ),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.custom_theme_title), style = MaterialTheme.typography.titleLarge)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CustomPaletteMode.values().forEach { mode ->
                    FilterChip(
                        selected = paletteMode.value == mode,
                        onClick = {
                            if (hapticsEnabled) {
                                haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                            }
                            paletteMode.value = mode
                        },
                        label = { Text(stringResource(mode.labelResId)) },
                    )
                }
            }

            when (paletteMode.value) {
                CustomPaletteMode.Grid -> GridPalette(
                    colors = CustomGridPalette,
                    selected = currentColor,
                    cellSize = GridCellSize,
                ) {
                    currentColor = it
                    hex = it.toHex()
                    isError = false
                }

                CustomPaletteMode.Spectrum -> SpectrumPalette(
                    color = currentColor,
                    width = GridPaletteWidth,
                    height = GridPaletteHeight,
                ) {
                    currentColor = it
                    hex = it.toHex()
                    isError = false
                }

                CustomPaletteMode.Sliders -> RgbSliders(color = currentColor) {
                    currentColor = it
                    hex = it.toHex()
                    isError = false
                }
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.custom_theme_name_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = hex,
                onValueChange = {
                    hex = it
                    isError = false
                },
                label = { Text(stringResource(R.string.custom_theme_hex_label)) },
                singleLine = true,
                isError = isError,
                trailingIcon = { ColorSwatch(color = parsed ?: currentColor) },
                supportingText = {
                    Text(
                        if (isError) {
                            stringResource(R.string.custom_theme_hex_error)
                        } else {
                            stringResource(R.string.custom_theme_hex_hint)
                        },
                        color = if (isError) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                PreviewChip(
                    stringResource(R.string.custom_theme_preview_primary),
                    previewScheme.primary,
                    previewScheme.onPrimary,
                    Modifier.weight(1f),
                )
                PreviewChip(
                    stringResource(R.string.custom_theme_preview_container),
                    previewScheme.primaryContainer,
                    previewScheme.onPrimaryContainer,
                    Modifier.weight(1f),
                )
                PreviewChip(
                    stringResource(R.string.custom_theme_preview_accent),
                    previewScheme.tertiaryContainer,
                    previewScheme.onTertiaryContainer,
                    Modifier.weight(1f),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.dialog_cancel))
                }
                FilledTonalButton(
                    onClick = {
                        val color = parsed
                        if (color != null) onSave(name, color) else isError = true
                    },
                    enabled = parsed != null,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.dialog_save_and_apply))
                }
            }
        }
    }
}

@Composable
internal fun GridPalette(
    colors: List<List<Color>>,
    selected: Color,
    cellSize: Dp,
    onPick: (Color) -> Unit,
) {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier
                .size(GridPaletteWidth, GridPaletteHeight)
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerLow),
        ) {
            colors.forEach { row ->
                Row {
                    row.forEach { color ->
                        val isSelected = color.toArgb() == selected.toArgb()
                        Box(
                            modifier = Modifier
                                .size(cellSize)
                                .background(color)
                                .then(
                                    if (isSelected) {
                                        Modifier.border(2.dp, MaterialTheme.colorScheme.primary)
                                    } else {
                                        Modifier
                                    },
                                )
                                .clickable { onPick(color) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun SpectrumPalette(
    color: Color,
    width: Dp,
    height: Dp,
    onPick: (Color) -> Unit,
) {
    val hsv = remember(color) { color.toHsv() }
    var boxSize by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(width, height)
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerLow),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Brush.verticalGradient(colors = SpectrumHueColors))
                    .background(Brush.horizontalGradient(colors = listOf(Color.White, Color.Transparent)))
                    .background(Brush.horizontalGradient(colors = listOf(Color.Transparent, Color.Black)))
                    .onSizeChanged { boxSize = it }
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset -> onPick(spectrumColorFor(offset, size)) },
                            onDrag = { change, _ -> onPick(spectrumColorFor(change.position, size)) },
                        )
                    },
            ) {
                val indicatorX = hsv[1].coerceIn(0f, 1f)
                val indicatorY = (hsv[0] / 360f).coerceIn(0f, 1f)
                val offsetX = with(density) {
                    (indicatorX * (boxSize.width - 24.dp.toPx()).coerceAtLeast(0f)).toDp()
                }
                val offsetY = with(density) {
                    (indicatorY * (boxSize.height - 24.dp.toPx()).coerceAtLeast(0f)).toDp()
                }
                Box(
                    modifier = Modifier
                        .offset(x = offsetX, y = offsetY)
                        .size(24.dp)
                        .border(2.dp, Color.White, CircleShape),
                )
            }
        }
    }
}

@Composable
internal fun RgbSliders(color: Color, onColorChange: (Color) -> Unit) {
    val rgb = color.toRgb()
    var r by remember(color) { mutableIntStateOf(rgb.first) }
    var g by remember(color) { mutableIntStateOf(rgb.second) }
    var b by remember(color) { mutableIntStateOf(rgb.third) }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        RgbSliderRow(stringResource(R.string.color_red), r) {
            r = it
            onColorChange(Color(r / 255f, g / 255f, b / 255f))
        }
        RgbSliderRow(stringResource(R.string.color_green), g) {
            g = it
            onColorChange(Color(r / 255f, g / 255f, b / 255f))
        }
        RgbSliderRow(stringResource(R.string.color_blue), b) {
            b = it
            onColorChange(Color(r / 255f, g / 255f, b / 255f))
        }
    }
}

@Composable
private fun RgbSliderRow(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
) {
    var textValue by remember(value) { mutableStateOf(value.toString()) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Slider(
                value = value.toFloat(),
                onValueChange = {
                    val next = it.toInt().coerceIn(0, 255)
                    textValue = next.toString()
                    onValueChange(next)
                },
                valueRange = 0f..255f,
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = textValue,
                onValueChange = { raw ->
                    val clean = raw.filter { it.isDigit() }.take(3)
                    textValue = clean
                    clean.toIntOrNull()?.let { onValueChange(it.coerceIn(0, 255)) }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                ),
                modifier = Modifier.width(72.dp),
            )
        }
    }
}
