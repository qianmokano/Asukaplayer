package com.asuka.player.app

import android.app.Activity
import android.graphics.drawable.ColorDrawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.asuka.player.R
import com.materialkolor.dynamiccolor.DynamicColor
import com.materialkolor.dynamiccolor.MaterialDynamicColors
import com.materialkolor.hct.Hct
import com.materialkolor.scheme.DynamicScheme
import com.materialkolor.scheme.SchemeMonochrome
import com.materialkolor.scheme.SchemeTonalSpot

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
        ThemeMode.Custom -> themeConfig.customSeed ?: Color(0xFF2E6CF6)
        ThemeMode.Dynamic -> null
        ThemeMode.Monochrome -> null
        else -> presetSeed ?: Color(0xFF2E6CF6)
    }
    val baseScheme = when {
        themeConfig.mode == ThemeMode.Dynamic && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S ->
            if (dark) androidx.compose.material3.dynamicDarkColorScheme(LocalContext.current)
            else androidx.compose.material3.dynamicLightColorScheme(LocalContext.current)
        themeConfig.mode == ThemeMode.Monochrome || (themeConfig.mode == ThemeMode.Custom && themeConfig.customMonochrome) ->
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

internal fun parseHexColor(value: String): Color? = runCatching { Color(android.graphics.Color.parseColor(value.trim())) }.getOrNull()
internal fun Color.toHex(): String = String.format("#%06X", 0xFFFFFF and toArgb())
internal fun Color.toRgb(): Triple<Int, Int, Int> {
    val argb = toArgb()
    return Triple((argb shr 16) and 0xFF, (argb shr 8) and 0xFF, argb and 0xFF)
}
internal fun Color.toHsv(): FloatArray = FloatArray(3).also { android.graphics.Color.colorToHSV(toArgb(), it) }
private fun hsvColor(hue: Float, saturation: Float, value: Float): Color = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, value)))
private fun spectrumColorFor(offset: Offset, size: IntSize): Color {
    val x = (offset.x / size.width).coerceIn(0f, 1f)
    val y = (offset.y / size.height).coerceIn(0f, 1f)
    return hsvColor(y * 360f, x, 1f - x)
}

private const val GridColumns = 12
private const val GridRows = 9
private val GridCellSize = 24.dp
private val GridPaletteWidth = GridCellSize * GridColumns
private val GridPaletteHeight = GridCellSize * GridRows
private val SpectrumHueColors = listOf(
    Color(0xFFFF0000),
    Color(0xFFFFFF00),
    Color(0xFF00FF00),
    Color(0xFF00FFFF),
    Color(0xFF0000FF),
    Color(0xFFFF00FF),
    Color(0xFFFF0000),
)
private val CustomGridPalette = buildGridPalette()

private fun buildGridPalette(): List<List<Color>> {
    val grid = mutableListOf<List<Color>>()
    val grayRow = (0 until GridColumns).map { i ->
        val t = i / (GridColumns - 1f)
        val v = 1f - t * 0.9f
        Color(v, v, v)
    }
    grid.add(grayRow)
    for (r in 0 until GridRows - 1) {
        val value = 0.95f - r * 0.08f
        val saturation = 0.85f - r * 0.06f
        val row = (0 until GridColumns).map { c ->
            val hue = (c / (GridColumns - 1f)) * 330f
            hsvColor(hue, saturation.coerceIn(0.2f, 1f), value.coerceIn(0.25f, 1f))
        }
        grid.add(row)
    }
    return grid
}

internal fun gridItemShape(
    index: Int,
    total: Int,
    columns: Int,
    outerCorner: Dp,
    innerCorner: Dp,
): RoundedCornerShape {
    val rows = (total + columns - 1) / columns
    val row = index / columns
    val col = index % columns
    val isTop = row == 0
    val isBottom = row == rows - 1
    val isLeft = col == 0
    val isRight = col == columns - 1 || index == total - 1
    return RoundedCornerShape(
        topStart = if (isTop && isLeft) outerCorner else innerCorner,
        topEnd = if (isTop && isRight) outerCorner else innerCorner,
        bottomStart = if (isBottom && isLeft) outerCorner else innerCorner,
        bottomEnd = if (isBottom && isRight) outerCorner else innerCorner,
    )
}

@Composable
internal fun ThemeSwatch(
    label: String,
    scheme: ColorScheme,
    selected: Boolean,
    disabled: Boolean,
    shape: RoundedCornerShape,
    size: Dp,
    hapticsEnabled: Boolean,
    icon: ImageVector? = null,
    showDynamicIcon: Boolean = false,
    showDelete: Boolean = false,
    onLongPress: (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    Surface(
        shape = shape,
        tonalElevation = if (selected) 4.dp else 1.dp,
        modifier = Modifier
            .size(size)
            .clip(shape)
            .combinedClickable(
                enabled = !disabled,
                onClick = {
                    if (hapticsEnabled) {
                        haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                    }
                    onClick()
                },
                onLongClick = {
                    if (hapticsEnabled) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                    onLongPress?.invoke()
                },
            ),
        color = scheme.surfaceContainer,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
        ) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box {
                    if (showDelete) {
                        DeleteSwatch(borderColor = scheme.primary)
                    } else {
                        ColorSwatch(
                            color = scheme.primary,
                            selected = selected,
                            icon = icon,
                            iconTint = if (icon != null) scheme.onPrimaryContainer else null,
                            borderColor = scheme.primary,
                        )
                        if (showDynamicIcon) {
                            Icon(
                                imageVector = Icons.Rounded.Palette,
                                contentDescription = null,
                                tint = scheme.onSurfaceVariant,
                                modifier = Modifier
                                    .size(16.dp)
                                    .align(Alignment.BottomEnd)
                                    .offset(x = 3.dp, y = 3.dp),
                            )
                        }
                    }
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (disabled) {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
internal fun ColorSwatch(
    color: Color,
    selected: Boolean = false,
    icon: ImageVector? = null,
    iconTint: Color? = null,
    borderColor: Color = MaterialTheme.colorScheme.primary,
) {
    val shape = CircleShape
    val borderWidth = if (selected) 2.dp else 1.dp
    val strokeColor = if (selected) borderColor else borderColor.copy(alpha = 0.45f)
    val innerPadding = if (selected) 2.dp else 0.dp
    Box(
        modifier = Modifier
            .size(40.dp)
            .border(borderWidth, strokeColor, shape)
            .padding(innerPadding)
            .clip(shape),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.weight(1f)) {
                Box(modifier = Modifier.weight(1f).fillMaxSize().background(color))
                Box(modifier = Modifier.weight(1f).fillMaxSize().background(color.copy(alpha = 0.85f)))
            }
            Row(modifier = Modifier.weight(1f)) {
                Box(modifier = Modifier.weight(1f).fillMaxSize().background(color.copy(alpha = 0.65f)))
                Box(modifier = Modifier.weight(1f).fillMaxSize().background(color.copy(alpha = 0.5f)))
            }
        }
        if (icon != null) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(imageVector = icon, contentDescription = null, tint = iconTint ?: MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(18.dp))
            }
        } else if (selected) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(imageVector = Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
internal fun DeleteSwatch(borderColor: Color) {
    val shape = CircleShape
    Box(
        modifier = Modifier
            .size(40.dp)
            .border(2.dp, borderColor, shape)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.Delete,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
internal fun ThemeSectionBlock(
    title: String,
    description: String,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 10.dp),
        )
        content()
    }
}

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
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.custom_theme_title), style = MaterialTheme.typography.titleLarge)
            androidx.compose.foundation.layout.FlowRow(
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
                CustomPaletteMode.Grid -> GridPalette(colors = CustomGridPalette, selected = currentColor, cellSize = GridCellSize) {
                    currentColor = it
                    hex = it.toHex()
                    isError = false
                }
                CustomPaletteMode.Spectrum -> SpectrumPalette(color = currentColor, width = GridPaletteWidth, height = GridPaletteHeight) {
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
                        if (isError) stringResource(R.string.custom_theme_hex_error) else stringResource(R.string.custom_theme_hex_hint),
                        color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PreviewChip(stringResource(R.string.custom_theme_preview_primary), previewScheme.primary, previewScheme.onPrimary, Modifier.weight(1f))
                PreviewChip(stringResource(R.string.custom_theme_preview_container), previewScheme.primaryContainer, previewScheme.onPrimaryContainer, Modifier.weight(1f))
                PreviewChip(stringResource(R.string.custom_theme_preview_accent), previewScheme.tertiaryContainer, previewScheme.onTertiaryContainer, Modifier.weight(1f))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.dialog_cancel)) }
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
internal fun PreviewChip(label: String, background: Color, foreground: Color, modifier: Modifier = Modifier) {
    Surface(shape = MaterialTheme.shapes.large, color = background, tonalElevation = 1.dp, modifier = modifier) {
        Box(modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp), contentAlignment = Alignment.Center) {
            Text(text = label, style = MaterialTheme.typography.labelLarge, color = foreground)
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
                                    if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary)
                                    else Modifier,
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
                val offsetX = with(density) { (indicatorX * (boxSize.width - 24.dp.toPx()).coerceAtLeast(0f)).toDp() }
                val offsetY = with(density) { (indicatorY * (boxSize.height - 24.dp.toPx()).coerceAtLeast(0f)).toDp() }
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
private fun RgbSliderRow(label: String, value: Int, onValueChange: (Int) -> Unit) {
    var textValue by remember(value) { mutableStateOf(value.toString()) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
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
