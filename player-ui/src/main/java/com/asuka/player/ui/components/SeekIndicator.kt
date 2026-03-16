package com.asuka.player.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.asuka.player.contract.PlaybackPreviewFrameProvider
import com.asuka.player.ui.state.SeekState
import com.asuka.player.ui.utils.formatTimeMs

@Composable
fun SeekIndicator(
    modifier: Modifier = Modifier,
    seekState: SeekState,
    playbackUri: String?,
    durationMs: Long,
    previewFrameProvider: PlaybackPreviewFrameProvider?,
) {
    if (!seekState.seeking) return
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val previewPositionMs = if (durationMs > 0L) {
        seekState.previewPositionMs.coerceIn(0L, durationMs)
    } else {
        seekState.previewPositionMs.coerceAtLeast(0L)
    }
    val previewBucketMs = ((previewPositionMs + PREVIEW_BUCKET_MS / 2L) / PREVIEW_BUCKET_MS) * PREVIEW_BUCKET_MS
    val previewWidth = if (configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) 176.dp else 192.dp
    val previewHeight = if (configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) 99.dp else 108.dp
    val timeLabel = "${formatTimeMs(previewPositionMs)} / ${formatTimeMs(durationMs)}"
    val timeLabelMeasureText = seekTimeHudMeasureText(durationMs)
    val timeLabelStyle = MaterialTheme.typography.labelLarge.copy(fontFamily = FontFamily.Monospace)
    val compactHudWidth = rememberSeekTimeHudWidth(
        text = timeLabelMeasureText,
        style = timeLabelStyle,
    )
    val previewBitmap = produceState<Bitmap?>(initialValue = null, playbackUri, previewBucketMs, previewFrameProvider) {
        value = if (playbackUri == null || previewFrameProvider == null) {
            null
        } else {
            previewFrameProvider.loadPreviewFrame(
                playbackUri = playbackUri,
                positionMs = previewBucketMs,
                maxWidthPx = with(density) { previewWidth.roundToPx() },
                maxHeightPx = with(density) { previewHeight.roundToPx() },
            )?.let { bytes ->
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }
        }
    }.value
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        if (previewBitmap == null) {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = gestureHudSurfaceColor(),
                contentColor = gestureHudContentColor(),
                tonalElevation = 6.dp,
                modifier = Modifier
                    .width(compactHudWidth)
                    .testTag("seek_indicator_hud"),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                ) {
                    Text(
                        text = timeLabel,
                        color = gestureHudContentColor(),
                        style = timeLabelStyle,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        softWrap = false,
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier.testTag("seek_indicator_hud"),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .width(previewWidth)
                        .aspectRatio(previewWidth / previewHeight)
                        .clip(RoundedCornerShape(14.dp))
                        .border(
                            width = 1.5.dp,
                            color = Color.White.copy(alpha = 0.92f),
                            shape = RoundedCornerShape(14.dp),
                        )
                        .testTag("seek_indicator_preview"),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        bitmap = previewBitmap.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize(),
                    )
                }
                Text(
                    text = timeLabel,
                    color = gestureHudContentColor(),
                    style = timeLabelStyle,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    softWrap = false,
                )
            }
        }
    }
}

private const val PREVIEW_BUCKET_MS = 500L
private val SeekIndicatorHudMinWidth = 116.dp
private val SeekIndicatorHudHorizontalPadding = 16.dp
private const val SeekIndicatorUnknownDurationTemplate = "888:88:88 / 888:88:88"

internal fun seekTimeHudMeasureText(durationMs: Long): String {
    if (durationMs <= 0L) return SeekIndicatorUnknownDurationTemplate
    val durationText = formatTimeMs(durationMs)
    return buildString((durationText.length * 2) + 3) {
        durationText.forEach { char ->
            append(if (char.isDigit()) '8' else char)
        }
        append(" / ")
        durationText.forEach { char ->
            append(if (char.isDigit()) '8' else char)
        }
    }
}

internal fun seekTimeHudDigitTemplate(text: String): String {
    return buildString(text.length) {
        text.forEach { char ->
            append(if (char.isDigit()) '8' else char)
        }
    }
}

@Composable
private fun rememberSeekTimeHudWidth(
    text: String,
    style: TextStyle,
): Dp {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    return remember(text, style, textMeasurer, density) {
        with(density) {
            (
                textMeasurer.measure(
                    text = AnnotatedString(text),
                    style = style,
                    maxLines = 1,
                    softWrap = false,
                ).size.width.toDp() + (SeekIndicatorHudHorizontalPadding * 2)
            ).coerceAtLeast(SeekIndicatorHudMinWidth)
        }
    }
}
