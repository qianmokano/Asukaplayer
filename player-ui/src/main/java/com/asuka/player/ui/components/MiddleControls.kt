package com.asuka.player.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.asuka.player.core.PlaybackController
import com.asuka.player.ui.R

@Composable
fun MiddleControls(
    modifier: Modifier = Modifier,
    controller: PlaybackController,
    isPlaying: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SimpleButton(
            label = stringResource(id = R.string.prev),
            icon = Icons.Outlined.SkipPrevious,
            onClick = onPrevious,
            tag = "btn_prev",
            size = 52.dp,
            iconSize = 26.dp,
        )
        SimpleButton(
            label = stringResource(id = R.string.play_pause),
            icon = if (isPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
            onClick = { controller.togglePlayPause() },
            tag = "btn_play_pause",
            size = 68.dp,
            iconSize = 32.dp,
        )
        SimpleButton(
            label = stringResource(id = R.string.next),
            icon = Icons.Outlined.SkipNext,
            onClick = onNext,
            tag = "btn_next",
            size = 52.dp,
            iconSize = 26.dp,
        )
    }
}
