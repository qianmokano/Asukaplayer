package com.asuka.player.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.math.round

private val SPEED_PRESETS = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f, 3.0f)
private const val SPEED_MIN = 0.25f
private const val SPEED_MAX = 3.0f
private const val SPEED_STEP = 0.25f

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SpeedSelectorPanel(
    selectedSpeed: Float,
    onSpeed: (Float) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilledTonalIconButton(
                onClick = {
                    onSpeed((selectedSpeed - SPEED_STEP).coerceAtLeast(SPEED_MIN).roundToStep())
                },
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = Color.White.copy(alpha = 0.12f),
                    contentColor = Color.White,
                ),
            ) {
                Icon(imageVector = Icons.Rounded.Remove, contentDescription = null)
            }
            Text(
                text = selectedSpeed.toSpeedLabel(),
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            FilledTonalIconButton(
                onClick = {
                    onSpeed((selectedSpeed + SPEED_STEP).coerceAtMost(SPEED_MAX).roundToStep())
                },
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = Color.White.copy(alpha = 0.12f),
                    contentColor = Color.White,
                ),
            ) {
                Icon(imageVector = Icons.Rounded.Add, contentDescription = null)
            }
        }

        var draggingSpeed by remember { mutableFloatStateOf(selectedSpeed) }
        var isDragging by remember { mutableStateOf(false) }
        val displaySpeed = if (isDragging) draggingSpeed else selectedSpeed.coerceIn(SPEED_MIN, SPEED_MAX)
        Slider(
            value = displaySpeed,
            onValueChange = {
                isDragging = true
                draggingSpeed = it
            },
            onValueChangeFinished = {
                isDragging = false
                onSpeed(draggingSpeed)
            },
            valueRange = SPEED_MIN..SPEED_MAX,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.White,
                inactiveTrackColor = Color.White.copy(alpha = 0.3f),
            ),
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SPEED_PRESETS.forEach { speed ->
                SpeedChip(
                    label = speed.toSpeedLabel(),
                    selected = kotlin.math.abs(speed - selectedSpeed) < 0.01f,
                    onClick = { onSpeed(speed) },
                )
            }
        }
    }
}

@Composable
private fun SpeedChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .border(
                width = 1.dp,
                color = if (selected) Color.White else Color.White.copy(alpha = 0.35f),
                shape = RoundedCornerShape(20.dp),
            )
            .background(if (selected) Color.White.copy(alpha = 0.15f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (selected) Color.White else Color.White.copy(alpha = 0.7f),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

private fun Float.toSpeedLabel(): String =
    "%.2f".format(this).trimEnd('0').trimEnd('.') + "x"

private fun Float.roundToStep(): Float =
    round(this / SPEED_STEP) * SPEED_STEP
