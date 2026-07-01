package com.asuka.player.app

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt

internal fun parseHexColor(value: String): Color? =
    runCatching { Color(value.trim().toColorInt()) }.getOrNull()

internal fun Color.toHex(): String = String.format("#%06X", 0xFFFFFF and toArgb())

internal fun Color.toRgb(): Triple<Int, Int, Int> {
    val argb = toArgb()
    return Triple((argb shr 16) and 0xFF, (argb shr 8) and 0xFF, argb and 0xFF)
}

internal fun Color.toHsv(): FloatArray =
    FloatArray(3).also { android.graphics.Color.colorToHSV(toArgb(), it) }

private fun hsvColor(hue: Float, saturation: Float, value: Float): Color {
    return Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, value)))
}

internal fun spectrumColorFor(offset: Offset, size: IntSize): Color {
    val x = (offset.x / size.width).coerceIn(0f, 1f)
    val y = (offset.y / size.height).coerceIn(0f, 1f)
    return hsvColor(y * 360f, x, 1f - x)
}

private const val GRID_COLUMNS = 12
private const val GRID_ROWS = 9
internal val GridCellSize = 24.dp
internal val GridPaletteWidth = GridCellSize * GRID_COLUMNS
internal val GridPaletteHeight = GridCellSize * GRID_ROWS

private val spectrumHueColors = listOf(
    Color(0xFFFF0000),
    Color(0xFFFFFF00),
    Color(0xFF00FF00),
    Color(0xFF00FFFF),
    Color(0xFF0000FF),
    Color(0xFFFF00FF),
    Color(0xFFFF0000),
)

internal val SpectrumHueColors: List<Color>
    get() = spectrumHueColors

internal val CustomGridPalette = buildGridPalette()

private fun buildGridPalette(): List<List<Color>> {
    val grid = mutableListOf<List<Color>>()
    val grayRow = (0 until GRID_COLUMNS).map { i ->
        val t = i / (GRID_COLUMNS - 1f)
        val v = 1f - t * 0.9f
        Color(v, v, v)
    }
    grid.add(grayRow)
    for (r in 0 until GRID_ROWS - 1) {
        val value = 0.95f - r * 0.08f
        val saturation = 0.85f - r * 0.06f
        val row = (0 until GRID_COLUMNS).map { c ->
            val hue = (c / (GRID_COLUMNS - 1f)) * 330f
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
