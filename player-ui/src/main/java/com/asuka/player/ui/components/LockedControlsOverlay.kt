package com.asuka.player.ui.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.asuka.player.ui.R

@Composable
fun LockedControlsOverlay(
    visible: Boolean,
    unlockHintVisible: Boolean,
    onTap: () -> Unit,
    onUnlock: () -> Unit,
) {
    if (!visible) return
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(unlockHintVisible) {
                detectTapGestures(onTap = { onTap() })
            }
            .padding(16.dp),
    ) {
        if (unlockHintVisible) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(id = R.string.locked),
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.testTag("locked_label"),
                )
                SimpleButton(
                    label = stringResource(id = R.string.unlock),
                    onClick = onUnlock,
                    tag = "btn_unlock_controls",
                )
            }
        }
    }
}
