package com.asuka.player.ui.components

import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.asuka.player.contract.TrackIndexCodec
import com.asuka.player.ui.R
import com.asuka.player.ui.controller.TrackOption

@Composable
fun AudioSelectorPanel(
    tracks: List<TrackOption>,
    selected: Int?,
    onSelect: (TrackOption) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .selectableGroup(),
    ) {
        if (tracks.isEmpty()) {
            Text(
                text = stringResource(id = R.string.no_audio_track),
                color = Color.White.copy(alpha = 0.6f),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 12.dp),
            )
        } else {
            tracks.forEach { track ->
                val encoded = TrackIndexCodec.encode(track.groupIndex, track.trackIndex)
                TrackOptionRow(
                    label = track.label,
                    selected = encoded == selected,
                    onClick = { onSelect(track) },
                )
            }
        }
    }
}

@Composable
fun SubtitleSelectorPanel(
    tracks: List<TrackOption>,
    selected: Int?,
    onDisable: () -> Unit,
    onSelect: (TrackOption) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .selectableGroup(),
    ) {
        TrackOptionRow(
            label = stringResource(id = R.string.off),
            selected = selected == TrackIndexCodec.SUBTITLE_DISABLED,
            onClick = onDisable,
        )
        tracks.forEach { track ->
            val encoded = TrackIndexCodec.encode(track.groupIndex, track.trackIndex)
            TrackOptionRow(
                label = track.label,
                selected = encoded == selected,
                onClick = { onSelect(track) },
            )
        }
    }
}

@Composable
internal fun TrackOptionRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton,
            )
            .padding(horizontal = 4.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
            colors = RadioButtonDefaults.colors(
                selectedColor = Color.White,
                unselectedColor = Color.White.copy(alpha = 0.45f),
            ),
        )
        Text(
            text = label,
            color = Color.White,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
