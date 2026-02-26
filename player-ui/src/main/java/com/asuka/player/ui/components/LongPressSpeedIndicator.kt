package com.asuka.player.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.asuka.player.ui.state.LongPressSpeedState

@Composable
fun LongPressSpeedIndicator(
    modifier: Modifier = Modifier,
    state: LongPressSpeedState,
) {
    if (!state.active) return
    Box(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(18.dp))
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("long_press_speed_indicator"),
    ) {
        Text(
            text = "${state.speed}x",
            color = Color.White,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}
