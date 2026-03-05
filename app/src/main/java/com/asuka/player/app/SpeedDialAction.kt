package com.asuka.player.app

import androidx.compose.ui.graphics.vector.ImageVector

internal data class SpeedDialAction(
    val icon: ImageVector,
    val label: String,
    val onClick: () -> Unit,
)

