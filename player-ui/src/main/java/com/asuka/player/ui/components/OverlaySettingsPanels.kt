package com.asuka.player.ui.components

import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.asuka.player.contract.LoopMode
import com.asuka.player.contract.VideoScaleMode
import com.asuka.player.ui.R

@Composable
fun SettingsMenuPanel(
    audioSummary: String,
    scaleSummary: String,
    loopSummary: String,
    shuffleSummary: String,
    onAudio: () -> Unit,
    onScale: () -> Unit,
    onLoopMode: () -> Unit,
    onShuffleMode: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        SettingsNavigationRow(
            tag = "settings_menu_audio",
            title = stringResource(id = R.string.audio_track),
            summary = audioSummary,
            onClick = onAudio,
        )
        SettingsNavigationRow(
            tag = "settings_menu_scale",
            title = stringResource(id = R.string.content_scale),
            summary = scaleSummary,
            onClick = onScale,
        )
        SettingsNavigationRow(
            tag = "settings_menu_loop_mode",
            title = stringResource(id = R.string.loop),
            summary = loopSummary,
            onClick = onLoopMode,
        )
        SettingsNavigationRow(
            tag = "settings_menu_shuffle_mode",
            title = stringResource(id = R.string.shuffle),
            summary = shuffleSummary,
            onClick = onShuffleMode,
        )
    }
}

@Composable
fun ScaleSelectorPanel(
    selectedMode: VideoScaleMode,
    onScale: (VideoScaleMode) -> Unit,
) {
    val options = listOf(
        stringResource(id = R.string.fit) to VideoScaleMode.FIT,
        stringResource(id = R.string.fill) to VideoScaleMode.FILL,
        stringResource(id = R.string.crop) to VideoScaleMode.CROP,
        stringResource(id = R.string.stretch) to VideoScaleMode.STRETCH,
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .selectableGroup(),
    ) {
        options.forEach { (label, mode) ->
            TrackOptionRow(
                label = label,
                selected = mode == selectedMode,
                onClick = { onScale(mode) },
            )
        }
    }
}

@Composable
fun LoopModePanel(
    currentRepeatMode: LoopMode,
    onLoopMode: (LoopMode) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .selectableGroup(),
    ) {
        TrackOptionRow(
            label = stringResource(id = R.string.playback_mode_loop_off),
            selected = currentRepeatMode == LoopMode.OFF,
            onClick = { onLoopMode(LoopMode.OFF) },
        )
        TrackOptionRow(
            label = stringResource(id = R.string.playback_mode_loop_one),
            selected = currentRepeatMode == LoopMode.ONE,
            onClick = { onLoopMode(LoopMode.ONE) },
        )
        TrackOptionRow(
            label = stringResource(id = R.string.playback_mode_loop_all),
            selected = currentRepeatMode == LoopMode.ALL,
            onClick = { onLoopMode(LoopMode.ALL) },
        )
    }
}

@Composable
fun ShuffleModePanel(
    shuffleEnabled: Boolean,
    onShuffleEnabled: (Boolean) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .selectableGroup(),
    ) {
        TrackOptionRow(
            label = stringResource(id = R.string.playback_mode_shuffle_off),
            selected = !shuffleEnabled,
            onClick = { onShuffleEnabled(false) },
        )
        TrackOptionRow(
            label = stringResource(id = R.string.playback_mode_shuffle_on),
            selected = shuffleEnabled,
            onClick = { onShuffleEnabled(true) },
        )
    }
}

@Composable
internal fun VideoScaleMode.toOverlayLabel(): String =
    when (this) {
        VideoScaleMode.FIT -> stringResource(id = R.string.fit)
        VideoScaleMode.FILL -> stringResource(id = R.string.fill)
        VideoScaleMode.CROP -> stringResource(id = R.string.crop)
        VideoScaleMode.STRETCH -> stringResource(id = R.string.stretch)
    }

@Composable
internal fun LoopMode.toLoopModeLabel(): String =
    when (this) {
        LoopMode.OFF -> stringResource(id = R.string.playback_mode_loop_off)
        LoopMode.ONE -> stringResource(id = R.string.playback_mode_loop_one)
        LoopMode.ALL -> stringResource(id = R.string.playback_mode_loop_all)
    }

@Composable
private fun SettingsNavigationRow(
    tag: String,
    title: String,
    summary: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 12.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = title,
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = summary,
                color = Color.White.copy(alpha = 0.65f),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            imageVector = Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.7f),
        )
    }
}
