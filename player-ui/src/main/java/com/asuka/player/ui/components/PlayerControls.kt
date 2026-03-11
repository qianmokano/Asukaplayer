package com.asuka.player.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.asuka.player.contract.PlaybackController
import com.asuka.player.ui.R

@Composable
fun PlayerControls(
    modifier: Modifier = Modifier,
    controller: PlaybackController,
    locked: Boolean,
    onLockToggle: () -> Unit,
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
        SimpleButton(label = stringResource(id = R.string.prev), onClick = { controller.seekBy(-10_000) }, tag = "btn_prev")
        SimpleButton(label = stringResource(id = R.string.play_pause), onClick = { controller.togglePlayPause() }, tag = "btn_play_pause")
        SimpleButton(label = stringResource(id = R.string.next), onClick = { controller.seekBy(10_000) }, tag = "btn_next")
    }
}
