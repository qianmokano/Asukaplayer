package com.asuka.player.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.asuka.player.contract.PlaybackController
import com.asuka.player.ui.LandscapeCutoutPadding
import com.asuka.player.ui.R
import com.asuka.player.ui.theme.PlayerUiTokens
import com.asuka.player.ui.utils.formatTimeMs

private val bottomBarButtonSize = 44.dp
private val bottomBarButtonIconSize = 22.dp
private val bottomBarPlayPauseIconSize = 26.dp
private val bottomBarLoadingRingSize = 52.dp
private val bottomBarLoadingRingStrokeWidth = 3.dp
private val bottomBarVerticalPadding = 4.dp
private val bottomBarRowSpacing = 2.dp
private val bottomBarTimeRowHeight = 20.dp
private val bottomBarSliderHeight = 18.dp
private val bottomBarSliderTrackHeight = 6.dp
private val bottomBarSliderThumbSize = DpSize(4.dp, 14.dp)
private val bottomBarActionRowHeight = bottomBarButtonSize + 4.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BottomBar(
    modifier: Modifier = Modifier,
    showButtonBackground: Boolean = true,
    controller: PlaybackController,
    landscapeCutoutPadding: LandscapeCutoutPadding = LandscapeCutoutPadding.None,
    positionMs: Long,
    durationMs: Long,
    isPlaying: Boolean,
    isBuffering: Boolean,
    onSeekBarDragChange: (Boolean) -> Unit = {},
    onNext: () -> Unit,
    onSpeed: () -> Unit,
    onSubtitle: () -> Unit,
    onRotate: () -> Unit,
    showTimeRow: Boolean = true,
    showActionRow: Boolean = true,
) {
    val layoutDirection = LocalLayoutDirection.current
    var showRemainingTime by remember { mutableStateOf(false) }
    var sliderDragging by remember { mutableStateOf(false) }
    var sliderValue by remember { mutableFloatStateOf(0f) }
    val sliderInteractionSource = remember { MutableInteractionSource() }
    val sliderColors = SliderDefaults.colors(
        thumbColor = Color.White,
        activeTrackColor = Color.White,
        inactiveTrackColor = Color.White.copy(alpha = 0.30f),
    )
    val displayPositionMs = if (sliderDragging) sliderValue.toLong() else positionMs.coerceAtLeast(0L)
    LaunchedEffect(positionMs) {
        if (!sliderDragging) sliderValue = positionMs.toFloat()
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.Black.copy(alpha = PlayerUiTokens.Alpha.topGradientStart),
                    ),
                ),
            )
            .navigationBarsPadding()
            .padding(
                start = PlayerUiTokens.Spacing.md + landscapeCutoutPadding.start(layoutDirection),
                top = bottomBarVerticalPadding,
                end = PlayerUiTokens.Spacing.md + landscapeCutoutPadding.end(layoutDirection),
                bottom = bottomBarVerticalPadding,
            ),
        verticalArrangement = Arrangement.spacedBy(bottomBarRowSpacing),
    ) {
        // ── Time row: "00:35 / 03:26" ─────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(bottomBarTimeRowHeight),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (showTimeRow) {
                val currentText = if (showRemainingTime && durationMs > 0L) {
                    "-${formatTimeMs((durationMs - displayPositionMs).coerceAtLeast(0L))}"
                } else {
                    formatTimeMs(displayPositionMs)
                }
                Text(
                    text = "$currentText / ${formatTimeMs(durationMs)}",
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .clickable { showRemainingTime = !showRemainingTime }
                        .padding(start = 2.dp),
                )
            }
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
                .height(bottomBarSliderHeight)
                .testTag("bottom_seek_bar"),
            colors = sliderColors,
            interactionSource = sliderInteractionSource,
            thumb = {
                SliderDefaults.Thumb(
                    interactionSource = sliderInteractionSource,
                    modifier = Modifier,
                    colors = sliderColors,
                    enabled = true,
                    thumbSize = bottomBarSliderThumbSize,
                )
            },
            track = { sliderState ->
                SliderDefaults.Track(
                    sliderState = sliderState,
                    modifier = Modifier.height(bottomBarSliderTrackHeight),
                    enabled = true,
                    colors = sliderColors,
                )
            },
        )

        // ── Action button row ──────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(bottomBarActionRowHeight),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (showActionRow) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier.size(bottomBarButtonSize + 4.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isBuffering) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .size(bottomBarLoadingRingSize)
                                    .testTag("play_pause_loading_ring"),
                                color = PlayerUiTokens.loadingIndicatorColor(),
                                strokeWidth = bottomBarLoadingRingStrokeWidth,
                            )
                        }
                        SimpleButton(
                            label = stringResource(id = R.string.play_pause),
                            icon = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            onClick = controller::togglePlayPause,
                            tag = "btn_play_pause",
                            showBackground = showButtonBackground,
                            size = bottomBarButtonSize,
                            iconSize = bottomBarPlayPauseIconSize,
                        )
                    }
                    SimpleButton(
                        label = stringResource(id = R.string.next),
                        icon = Icons.Rounded.SkipNext,
                        onClick = onNext,
                        tag = "btn_next",
                        showBackground = showButtonBackground,
                        size = bottomBarButtonSize,
                        iconSize = bottomBarButtonIconSize,
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SimpleButton(
                        label = stringResource(id = R.string.subs),
                        icon = Icons.Rounded.Subtitles,
                        onClick = onSubtitle,
                        tag = "btn_subs",
                        showBackground = showButtonBackground,
                        size = bottomBarButtonSize,
                        iconSize = bottomBarButtonIconSize,
                    )
                    SimpleButton(
                        label = stringResource(id = R.string.speed),
                        icon = Icons.Rounded.Speed,
                        onClick = onSpeed,
                        tag = "btn_speed",
                        showBackground = showButtonBackground,
                        size = bottomBarButtonSize,
                        iconSize = bottomBarButtonIconSize,
                    )
                    SimpleButton(
                        label = stringResource(id = R.string.rotate),
                        icon = Icons.Rounded.ScreenRotation,
                        onClick = onRotate,
                        tag = "btn_rotate",
                        showBackground = showButtonBackground,
                        size = bottomBarButtonSize,
                        iconSize = bottomBarButtonIconSize,
                    )
                }
            }
        }
    }
}
