package com.asuka.player.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Launch
import androidx.compose.material.icons.outlined.AspectRatio
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PictureInPictureAlt
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.ScreenRotation
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.asuka.player.contract.PlaybackController
import com.asuka.player.ui.R
import com.asuka.player.ui.theme.PlayerUiTokens
import com.asuka.player.ui.utils.formatTimeMs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomBar(
    modifier: Modifier = Modifier,
    showBackground: Boolean = true,
    controller: PlaybackController,
    positionMs: Long,
    durationMs: Long,
    onLockToggle: () -> Unit,
    onScale: () -> Unit,
    onRotate: () -> Unit,
    onPip: () -> Unit,
    onBackground: () -> Unit,
    onLoop: () -> Unit,
    onShuffle: () -> Unit,
) {
    var showRemainingTime by remember { mutableStateOf(false) }
    var sliderDragging by remember { mutableStateOf(false) }
    var sliderValue by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(positionMs) {
        if (!sliderDragging) sliderValue = positionMs.toFloat()
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = if (showBackground) {
                        listOf(Color.Transparent, Color.Black.copy(alpha = PlayerUiTokens.Alpha.topGradientStart))
                    } else {
                        listOf(Color.Transparent, Color.Transparent)
                    },
                ),
            )
            .navigationBarsPadding()
            .padding(horizontal = PlayerUiTokens.Spacing.md, vertical = PlayerUiTokens.Spacing.sm),
    ) {
        // ── Time row: "00:35 / 03:26" on left, scale button on right ──────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val currentText = if (showRemainingTime && durationMs > 0L) {
                "-${formatTimeMs((durationMs - positionMs).coerceAtLeast(0L))}"
            } else {
                formatTimeMs(positionMs)
            }
            Text(
                text = "$currentText / ${formatTimeMs(durationMs)}",
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .clickable { showRemainingTime = !showRemainingTime }
                    .padding(start = 2.dp, top = 4.dp, bottom = 4.dp),
            )
            SimpleButton(
                label = stringResource(id = R.string.rotate),
                icon = Icons.Outlined.ScreenRotation,
                onClick = onRotate,
                tag = "btn_rotate",
                size = 40.dp,
                iconSize = 22.dp,
            )
        }

        // ── Seek bar ───────────────────────────────────────────────────────────
        Slider(
            value = if (sliderDragging) sliderValue else if (durationMs > 0L) positionMs.toFloat() else 0f,
            valueRange = 0f..(durationMs.takeIf { it > 0 } ?: 1L).toFloat(),
            onValueChange = {
                sliderDragging = true
                sliderValue = it
            },
            onValueChangeFinished = {
                controller.seekTo(sliderValue.toLong())
                sliderDragging = false
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 0.dp),
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.White,
                inactiveTrackColor = Color.White.copy(alpha = 0.30f),
            ),
        )

        // ── Action button row ──────────────────────────────────────────────────
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState()),
        ) {
            SimpleButton(
                label = stringResource(id = R.string.lock),
                icon = Icons.Outlined.Lock,
                onClick = onLockToggle,
                tag = "btn_lock",
            )
            SimpleButton(
                label = stringResource(id = R.string.scale),
                icon = Icons.Outlined.AspectRatio,
                onClick = onScale,
                tag = "btn_scale",
            )
            SimpleButton(
                label = stringResource(id = R.string.pip),
                icon = Icons.Outlined.PictureInPictureAlt,
                onClick = onPip,
                tag = "btn_pip",
            )
            SimpleButton(
                label = stringResource(id = R.string.background_short),
                icon = Icons.AutoMirrored.Outlined.Launch,
                onClick = onBackground,
                tag = "btn_bg",
            )
            SimpleButton(
                label = stringResource(id = R.string.loop),
                icon = Icons.Outlined.Repeat,
                onClick = onLoop,
                tag = "btn_loop",
            )
            SimpleButton(
                label = stringResource(id = R.string.shuffle),
                icon = Icons.Outlined.Shuffle,
                onClick = onShuffle,
                tag = "btn_shuffle",
            )
        }
    }
}
