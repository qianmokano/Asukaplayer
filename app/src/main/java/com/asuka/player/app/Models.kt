package com.asuka.player.app

import android.net.Uri
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import com.asuka.player.R
import com.asuka.player.contract.PlaybackQueueEntry
import com.asuka.player.runtime.CustomThemeEntry
import com.asuka.player.runtime.ThemeMode
import java.util.Locale

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
    val resumePositionMs: Long = 0L,
) {
    val playbackMediaId: String
        get() = "media-store:$id"

    val durationLabel: String
        get() = formatDuration(durationMs)

    val resumeProgressFraction: Float?
        get() {
            if (durationMs <= 0L || resumePositionMs <= 0L) return null
            return (resumePositionMs.toFloat() / durationMs.toFloat())
                .coerceIn(0f, 1f)
                .takeIf { it > 0f }
        }

    val sizeLabel: String
        get() = formatSize(sizeBytes)

    fun toPlaybackQueueEntry(
        mediaIdOverride: String = playbackMediaId,
    ): PlaybackQueueEntry {
        return PlaybackQueueEntry(
            mediaId = mediaIdOverride,
            uri = uri.toString(),
        )
    }
}

internal data class PlaybackSelection(
    val targetEntry: PlaybackQueueEntry,
    val queueEntries: List<PlaybackQueueEntry>,
) {
    init {
        require(queueEntries.isNotEmpty()) { "queueEntries must not be empty" }
    }
}

internal fun singlePlaybackSelection(uri: String): PlaybackSelection {
    val entry = PlaybackQueueEntry(
        mediaId = uri,
        uri = uri,
    )
    return PlaybackSelection(
        targetEntry = entry,
        queueEntries = listOf(entry),
    )
}

internal data class LocalVideoFolder(
    val id: Long,
    val name: String,
    val videoCount: Int,
    val totalDurationMs: Long,
    val totalSizeBytes: Long,
) {
    val totalDurationLabel: String
        get() = formatDuration(totalDurationMs)

    val totalSizeLabel: String
        get() = formatSize(totalSizeBytes)
}

internal fun formatDuration(durationMs: Long): String {
    val totalSec = (durationMs / 1000L).coerceAtLeast(0L)
    val h = totalSec / 3600L
    val m = (totalSec % 3600L) / 60L
    val s = totalSec % 60L
    return if (h > 0) {
        String.format(Locale.ROOT, "%d:%02d:%02d", h, m, s)
    } else {
        String.format(Locale.ROOT, "%02d:%02d", m, s)
    }
}

internal fun formatSize(sizeBytes: Long): String {
    val mb = sizeBytes / (1024f * 1024f)
    return String.format(Locale.ROOT, "%.1f MB", mb)
}
