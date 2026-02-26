package com.asuka.player.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.asuka.player.ui.state.ZoomState

@Composable
fun ZoomIndicator(
    modifier: Modifier = Modifier,
    zoomState: ZoomState,
) {
    if (!zoomState.isZooming) return
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val percent = (zoomState.zoom * 100).toInt()
        Text(text = "$percent%", color = Color.White, style = MaterialTheme.typography.headlineMedium)
    }
}
