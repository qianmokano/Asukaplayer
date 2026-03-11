package com.asuka.player.app

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.asuka.player.contract.PlayerSettings

@Composable
internal fun PlayerSettingsPlaceholderPageContent(
    modifier: Modifier = Modifier,
    playerSettings: PlayerSettings,
    onPlayerSettingsChange: (PlayerSettings) -> Unit,
) {
    val longPressSpeed = playerSettings.longPressSpeed
    var showDoubleTapActionDialog by remember { mutableStateOf(false) }
    var showLongPressSpeedDialog by remember { mutableStateOf(false) }
    var editingLongPressSpeed by remember { mutableFloatStateOf(longPressSpeed) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            bottom = 24.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
        ),
        ) {
        item {
            Spacer(modifier = Modifier.size(12.dp))
        }

        item {
            PlayerGestureSettingsGroup(
                playerSettings = playerSettings,
                onPlayerSettingsChange = onPlayerSettingsChange,
                onOpenDoubleTapAction = { showDoubleTapActionDialog = true },
                onOpenLongPressSpeed = {
                    editingLongPressSpeed = playerSettings.longPressSpeed
                    showLongPressSpeedDialog = true
                },
            )
        }

        item {
            PlayerUiSettingsGroup(
                playerSettings = playerSettings,
                onPlayerSettingsChange = onPlayerSettingsChange,
            )
        }

        item {
            PlayerPlaybackSettingsGroup(
                playerSettings = playerSettings,
                onPlayerSettingsChange = onPlayerSettingsChange,
            )
        }

        item { PlayerSettingsListFooter() }
    }

    if (showDoubleTapActionDialog) {
        DoubleTapActionDialog(
            playerSettings = playerSettings,
            onPlayerSettingsChange = onPlayerSettingsChange,
            onDismiss = { showDoubleTapActionDialog = false },
        )
    }

    if (showLongPressSpeedDialog) {
        LongPressSpeedDialog(
            editingLongPressSpeed = editingLongPressSpeed,
            onEditingLongPressSpeedChange = { editingLongPressSpeed = it },
            onConfirm = {
                onPlayerSettingsChange(
                    playerSettings.copy(longPressSpeed = editingLongPressSpeed.coerceIn(0.2f, 4.0f)),
                )
                showLongPressSpeedDialog = false
            },
            onDismiss = { showLongPressSpeedDialog = false },
        )
    }
}
