package com.asuka.player.app

import android.net.Uri
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import com.asuka.player.R

internal enum class ThemeMode {
    Dynamic,
    Custom,
    Violet,
    Teal,
    Red,
    Sandstone,
    Neutral,
    Monochrome,
}

internal enum class ThemeAppearanceMode {
    System,
    Light,
    Dark,
}

internal data class ThemeConfig(
    val mode: ThemeMode = ThemeMode.Monochrome,
    val customSeed: Color? = null,
    val customThemeId: String? = null,
    val customMonochrome: Boolean = false,
    val appearance: ThemeAppearanceMode = ThemeAppearanceMode.System,
    val pureBlack: Boolean = true,
    val fontScale: Float = 1.0f,
    val fontScaleEnabled: Boolean = false,
)

/**
 * Allowed values for double-tap gesture action, used in [PlayerSettingsConfig].
 * Persisted as [value] strings in SharedPreferences.
 */
internal enum class DoubleTapActionSetting(val value: String) {
    Seek("seek"),
    TogglePlayPause("toggle_play_pause"),
    Both("both");

    companion object {
        fun fromValue(raw: String?): DoubleTapActionSetting =
            entries.firstOrNull { it.value == raw } ?: Seek
    }
}

internal data class PlayerSettingsConfig(
    val seekGestureEnabled: Boolean = true,
    val brightnessGestureEnabled: Boolean = true,
    val volumeGestureEnabled: Boolean = true,
    val zoomGestureEnabled: Boolean = true,
    val panGestureEnabled: Boolean = true,
    val doubleTapGestureEnabled: Boolean = true,
    val doubleTapAction: DoubleTapActionSetting = DoubleTapActionSetting.Seek,
    val longPressGestureEnabled: Boolean = true,
    val seekIncrementSec: Int = 10,
    val seekSensitivity: Float = 1.0f,
    val longPressSpeed: Float = 2.0f,
    val controllerTimeoutSec: Int = 3,
    val hideButtonsBackground: Boolean = false,
    val resumePlayback: Boolean = true,
    val defaultPlaybackSpeed: Float = 1.0f,
    val autoplay: Boolean = true,
    val autoPip: Boolean = true,
    val autoBackgroundPlay: Boolean = false,
    val rememberBrightness: Boolean = false,
    val rememberSelections: Boolean = true,
)

internal data class CustomThemeEntry(
    val id: String,
    val name: String,
    val seedArgb: Int,
    val monochrome: Boolean,
) {
    val seed: Color
        get() = Color(seedArgb)
}

internal data class ThemePreset(
    val mode: ThemeMode,
    @StringRes val nameResId: Int,
    val description: String,
    val seed: Color?,
)

internal val ThemePresets = listOf(
    ThemePreset(ThemeMode.Dynamic, R.string.theme_preset_dynamic, "Follow system wallpaper/theme", null),
    ThemePreset(ThemeMode.Violet, R.string.theme_preset_violet, "Material Baseline Violet", Color(0xFF6750A4)),
    ThemePreset(ThemeMode.Teal, R.string.theme_preset_teal, "Material Baseline Teal", Color(0xFF006E6A)),
    ThemePreset(ThemeMode.Red, R.string.theme_preset_red, "Material Baseline Red", Color(0xFFB3261E)),
    ThemePreset(ThemeMode.Sandstone, R.string.theme_preset_sandstone, "Material Baseline Sand", Color(0xFF8B6B4A)),
    ThemePreset(ThemeMode.Neutral, R.string.theme_preset_neutral, "Material Baseline Neutral", Color(0xFF6B7280)),
    ThemePreset(ThemeMode.Monochrome, R.string.theme_preset_monochrome, "Monochrome", null),
    ThemePreset(ThemeMode.Custom, R.string.theme_preset_custom, "Enter or pick any color", null),
)

internal sealed interface ThemeSwatchItem {
    data class Preset(val preset: ThemePreset) : ThemeSwatchItem
    data class CustomTheme(val theme: CustomThemeEntry) : ThemeSwatchItem
    data object CustomAdd : ThemeSwatchItem
}

internal enum class CustomPaletteMode(@StringRes val labelResId: Int) {
    Grid(R.string.palette_mode_grid),
    Spectrum(R.string.palette_mode_spectrum),
    Sliders(R.string.palette_mode_sliders),
}

internal data class LocalVideoItem(
    val id: Long,
    val uri: Uri,
    val title: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val folderName: String,
    val folderPath: String,
    val folderId: Long,
    val dateAddedSec: Long,
) {
    val durationLabel: String
        get() = formatDuration(durationMs)

    val sizeLabel: String
        get() = formatSize(sizeBytes)
}

internal data class LocalVideoFolder(
    val id: Long,
    val name: String,
    val videos: List<LocalVideoItem>,
) {
    val videoCount: Int
        get() = videos.size

    private val totalDurationMs: Long
        get() = videos.sumOf { it.durationMs }.coerceAtLeast(0L)

    private val totalSizeBytes: Long
        get() = videos.sumOf { it.sizeBytes }.coerceAtLeast(0L)

    val totalDurationLabel: String
        get() = formatDuration(totalDurationMs)

    val totalSizeLabel: String
        get() = formatSize(totalSizeBytes)
}

internal fun buildFolderGroups(items: List<LocalVideoItem>): List<LocalVideoFolder> {
    return items
        .groupBy { it.folderId }
        .map { (folderId, videos) ->
            LocalVideoFolder(
                id = folderId,
                name = videos.firstOrNull()?.folderName.orEmpty(),
                videos = videos.sortedByDescending { it.dateAddedSec },
            )
        }
        .sortedBy { it.name.lowercase() }
}

internal fun formatDuration(durationMs: Long): String {
    val totalSec = (durationMs / 1000L).coerceAtLeast(0L)
    val h = totalSec / 3600L
    val m = (totalSec % 3600L) / 60L
    val s = totalSec % 60L
    return if (h > 0) String.format("%d:%02d:%02d", h, m, s) else String.format("%02d:%02d", m, s)
}

internal fun formatSize(sizeBytes: Long): String {
    val mb = sizeBytes / (1024f * 1024f)
    return String.format("%.1f MB", mb)
}
