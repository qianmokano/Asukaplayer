package com.asuka.player.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.asuka.player.ui.state.SeekState

@Composable
fun SeekIndicator(
    modifier: Modifier = Modifier,
    seekState: SeekState,
) {
    if (!seekState.seeking) return
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val sign = if (seekState.deltaMs > 0) "+" else "-"
        val absDelta = kotlin.math.abs(seekState.deltaMs)
        val secs = absDelta / 1000
        val tenths = (absDelta % 1000) / 100
        val amount = if (tenths == 0L) "${secs}s" else "${secs}.${tenths}s"
        Text(
            text = "$sign$amount",
            color = Color.White,
            style = MaterialTheme.typography.headlineMedium,
        )
    }
}
