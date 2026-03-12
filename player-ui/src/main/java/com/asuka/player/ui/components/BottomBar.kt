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
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.Headset
import androidx.compose.material.icons.rounded.PictureInPictureAlt
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.material.icons.rounded.Tune
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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.asuka.player.contract.PlaybackController
import com.asuka.player.ui.LandscapeCutoutPadding
import com.asuka.player.ui.R
import com.asuka.player.ui.theme.PlayerUiTokens
import com.asuka.player.ui.utils.formatTimeMs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BottomBar(
    modifier: Modifier = Modifier,
    showBackground: Boolean = true,
    controller: PlaybackController,
    landscapeCutoutPadding: LandscapeCutoutPadding = LandscapeCutoutPadding.None,
    positionMs: Long,
    durationMs: Long,
    onSeekBarDragChange: (Boolean) -> Unit = {},
    onScale: () -> Unit,
    onRotate: () -> Unit,
    onPip: () -> Unit,
    onBackground: () -> Unit,
    onPlaybackMode: () -> Unit,
) {
    val layoutDirection = LocalLayoutDirection.current
    var showRemainingTime by remember { mutableStateOf(false) }
    var sliderDragging by remember { mutableStateOf(false) }
    var sliderValue by remember { mutableFloatStateOf(0f) }
    val displayPositionMs = if (sliderDragging) sliderValue.toLong() else positionMs.coerceAtLeast(0L)
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
            .padding(
                start = PlayerUiTokens.Spacing.md + landscapeCutoutPadding.start(layoutDirection),
                top = PlayerUiTokens.Spacing.sm,
                end = PlayerUiTokens.Spacing.md + landscapeCutoutPadding.end(layoutDirection),
                bottom = PlayerUiTokens.Spacing.sm,
            ),
    ) {
        // ── Time row: "00:35 / 03:26" on left, scale button on right ──────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val currentText = if (showRemainingTime && durationMs > 0L) {
                "-${formatTimeMs((durationMs - displayPositionMs).coerceAtLeast(0L))}"
            } else {
                formatTimeMs(displayPositionMs)
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
                icon = Icons.Rounded.ScreenRotation,
                onClick = onRotate,
                tag = "btn_rotate",
                size = 40.dp,
                iconSize = 22.dp,
            )
        }

        // ── Seek bar ───────────────────────────────────────────────────────────
        Slider(
            value = if (durationMs > 0L) displayPositionMs.toFloat() else 0f,
            valueRange = 0f..(durationMs.takeIf { it > 0 } ?: 1L).toFloat(),
            onValueChange = {
                if (!sliderDragging) {
                    sliderDragging = true
                    onSeekBarDragChange(true)
                }
                sliderValue = it
            },
            onValueChangeFinished = {
                controller.seekTo(sliderValue.toLong())
                if (sliderDragging) {
                    sliderDragging = false
                    onSeekBarDragChange(false)
                }
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
                label = stringResource(id = R.string.scale),
                icon = Icons.Rounded.AspectRatio,
                onClick = onScale,
                tag = "btn_scale",
            )
            SimpleButton(
                label = stringResource(id = R.string.pip),
                icon = Icons.Rounded.PictureInPictureAlt,
                onClick = onPip,
                tag = "btn_pip",
            )
            SimpleButton(
                label = stringResource(id = R.string.background_short),
                icon = Icons.Rounded.Headset,
                onClick = onBackground,
                tag = "btn_bg",
            )
            SimpleButton(
                label = stringResource(id = R.string.playback_mode_short),
                icon = Icons.Rounded.Tune,
                onClick = onPlaybackMode,
                tag = "btn_playback_mode",
            )
        }
    }
}
