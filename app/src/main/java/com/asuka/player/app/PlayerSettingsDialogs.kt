package com.asuka.player.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.asuka.player.contract.PlayerSettings
import com.asuka.player.R
import java.util.Locale

@Composable
internal fun DoubleTapActionOptionRow(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton,
            )
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
internal fun DoubleTapActionDialog(
    playerSettings: PlayerSettings,
    onPlayerSettingsChange: (PlayerSettings) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.dialog_double_tap_action_title)) },
        text = {
            Column {
                HorizontalDivider()
                LazyColumn(
                    modifier = Modifier.selectableGroup(),
                    contentPadding = PaddingValues(vertical = 8.dp),
                ) {
                    item {
                        DoubleTapActionOptionRow(
                            text = stringResource(R.string.action_seek),
                            selected = playerSettings.doubleTapAction == PlayerSettings.DoubleTapAction.Seek,
                            onClick = {
                                onPlayerSettingsChange(
                                    playerSettings.copy(
                                        doubleTapGestureEnabled = true,
                                        doubleTapAction = PlayerSettings.DoubleTapAction.Seek,
                                    ),
                                )
                                onDismiss()
                            },
                        )
                    }
                    item {
                        DoubleTapActionOptionRow(
                            text = stringResource(R.string.action_play_pause),
                            selected = playerSettings.doubleTapAction == PlayerSettings.DoubleTapAction.TogglePlayPause,
                            onClick = {
                                onPlayerSettingsChange(
                                    playerSettings.copy(
                                        doubleTapGestureEnabled = true,
                                        doubleTapAction = PlayerSettings.DoubleTapAction.TogglePlayPause,
                                    ),
                                )
                                onDismiss()
                            },
                        )
                    }
                    item {
                        DoubleTapActionOptionRow(
                            text = stringResource(R.string.action_seek_and_play_pause),
                            selected = playerSettings.doubleTapAction == PlayerSettings.DoubleTapAction.Both,
                            onClick = {
                                onPlayerSettingsChange(
                                    playerSettings.copy(
                                        doubleTapGestureEnabled = true,
                                        doubleTapAction = PlayerSettings.DoubleTapAction.Both,
                                    ),
                                )
                                onDismiss()
                            },
                        )
                    }
                }
                HorizontalDivider()
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.dialog_cancel))
            }
        },
        confirmButton = {},
    )
}

@Composable
internal fun LongPressSpeedDialog(
    editingLongPressSpeed: Float,
    onEditingLongPressSpeedChange: (Float) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.dialog_long_press_title)) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = String.format(Locale.US, "%.1fx", editingLongPressSpeed),
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
                Slider(
                    value = editingLongPressSpeed,
                    onValueChange = { value ->
                        onEditingLongPressSpeedChange(((value * 10).toInt() / 10f).coerceIn(0.2f, 4.0f))
                    },
                    valueRange = 0.2f..4.0f,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.dialog_cancel))
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = stringResource(R.string.dialog_done))
            }
        },
    )
}
