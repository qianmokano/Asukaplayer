package com.asuka.player.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.asuka.player.contract.PlaybackController
import com.asuka.player.ui.R
import com.asuka.player.ui.theme.PlayerUiTokens

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
            icon = Icons.Rounded.SkipPrevious,
            onClick = onPrevious,
            tag = "btn_prev",
            size = 52.dp,
            iconSize = 26.dp,
        )
        SimpleButton(
            label = stringResource(id = R.string.play_pause),
            icon = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
            onClick = { controller.togglePlayPause() },
            tag = "btn_play_pause",
            size = PlayerUiTokens.ButtonSize.playbackPrimary,
            iconSize = PlayerUiTokens.ButtonSize.playbackPrimaryIcon,
        )
        SimpleButton(
            label = stringResource(id = R.string.next),
            icon = Icons.Rounded.SkipNext,
            onClick = onNext,
            tag = "btn_next",
            size = 52.dp,
            iconSize = 26.dp,
        )
    }
}
