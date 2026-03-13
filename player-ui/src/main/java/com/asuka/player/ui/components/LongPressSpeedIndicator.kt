package com.asuka.player.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
            .fillMaxWidth()
            .offset(y = rememberGestureHudVerticalOffset()),
            contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.testTag("long_press_speed_indicator"),
            shape = MaterialTheme.shapes.large,
            color = gestureHudSurfaceColor(),
            contentColor = gestureHudContentColor(),
            tonalElevation = 6.dp,
        ) {
            Text(
                text = "${state.speed}x",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            )
        }
    }
}
