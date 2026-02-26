package com.asuka.player.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.asuka.player.ui.state.TapFeedbackState
import kotlin.math.abs

@Composable
fun DoubleTapIndicator(
    modifier: Modifier = Modifier,
    state: TapFeedbackState,
) {
    if (!state.visible || state.deltaMs == 0L) return
    val isForward = state.deltaMs > 0L
    val seconds = abs(state.deltaMs / 1000L)
    val text = if (isForward) "+${seconds}s" else "-${seconds}s"
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = if (state.direction == TapFeedbackState.Direction.LEFT) {
            Alignment.CenterStart
        } else {
            Alignment.CenterEnd
        },
    ) {
        Box(
            modifier = Modifier
                .padding(horizontal = 28.dp)
                .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(12.dp))
                .padding(horizontal = 18.dp, vertical = 10.dp)
                .testTag("double_tap_indicator"),
        ) {
            Text(
                text = text,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}
