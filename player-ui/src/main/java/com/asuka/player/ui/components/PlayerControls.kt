package com.asuka.player.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.asuka.player.contract.PlaybackController
import com.asuka.player.contract.SeekIntervalPolicy
import com.asuka.player.ui.R

@Composable
fun PlayerControls(
    modifier: Modifier = Modifier,
    controller: PlaybackController,
    locked: Boolean,
    onLockToggle: () -> Unit,
    seekBackwardSec: Int = SeekIntervalPolicy.DEFAULT_SECONDS,
    seekForwardSec: Int = SeekIntervalPolicy.DEFAULT_SECONDS,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        SimpleButton(
            label = if (locked) stringResource(id = R.string.unlock) else stringResource(id = R.string.lock),
            onClick = onLockToggle,
            tag = "btn_lock",
        )
        SimpleButton(
            label = stringResource(id = R.string.seek_backward),
            onClick = { controller.seekBy(-SeekIntervalPolicy.toMilliseconds(seekBackwardSec)) },
            tag = "btn_prev",
        )
        SimpleButton(label = stringResource(id = R.string.play_pause), onClick = { controller.togglePlayPause() }, tag = "btn_play_pause")
        SimpleButton(
            label = stringResource(id = R.string.seek_forward),
            onClick = { controller.seekBy(SeekIntervalPolicy.toMilliseconds(seekForwardSec)) },
            tag = "btn_next",
        )
    }
}
