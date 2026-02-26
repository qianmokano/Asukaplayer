package com.asuka.player.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.asuka.player.ui.state.VolumeBrightnessState

@Composable
fun VerticalAdjustIndicator(
    modifier: Modifier = Modifier,
    state: VolumeBrightnessState,
) {
    val mode = state.activeMode ?: return
    val value = when (mode) {
        VolumeBrightnessState.Mode.VOLUME -> state.volumePercent
        VolumeBrightnessState.Mode.BRIGHTNESS -> state.brightnessPercent
    }
    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(48.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "$value%",
            color = Color.White,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}
