package com.asuka.player.app

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.asuka.player.R

@Composable
internal fun MotionSettingsPageContent(
    modifier: Modifier = Modifier,
    navDurationMs: Int,
    onNavDurationChange: (Int) -> Unit,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            bottom = 24.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
        ),
    ) {
        item { Spacer(modifier = Modifier.size(12.dp)) }
        item {
            SplicedColumnGroup(title = stringResource(R.string.settings_group_motion)) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                    ) {
                        val minMs = 0
                        val maxMs = 600
                        val stepMs = 10
                        val sliderValue = navDurationMs.coerceIn(minMs, maxMs).toFloat()
                        Text(
                            text = stringResource(R.string.motion_transition_duration_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = stringResource(R.string.motion_transition_duration_desc, sliderValue.toInt()),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
                        )
                        Slider(
                            value = sliderValue,
                            valueRange = minMs.toFloat()..maxMs.toFloat(),
                            steps = ((maxMs - minMs) / stepMs) - 1,
                            onValueChange = {
                                val stepped = ((it / stepMs).toInt() * stepMs).coerceIn(minMs, maxMs)
                                onNavDurationChange(stepped)
                            },
                        )
                        Text(
                            text = stringResource(R.string.motion_transition_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.size(6.dp)) }
    }
}
