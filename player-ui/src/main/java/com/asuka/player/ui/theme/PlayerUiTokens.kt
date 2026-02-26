package com.asuka.player.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

object PlayerUiTokens {
    object Alpha {
        const val topGradientStart = 0.45f
        const val bottomGradientEnd = 0.35f
        const val overlayBackdrop = 0.5f
        const val feedbackSurface = 0.65f
    }

    object Spacing {
        val xs = 8.dp
        val sm = 10.dp
        val md = 12.dp
        val lg = 18.dp
        val xl = 20.dp
    }

    object Motion {
        const val fastMs = 140
        const val normalMs = 200
        const val slowMs = 260
        const val feedbackMs = 700L
    }

    // Dark navy background used by all control buttons.
    val buttonBackground = Color(0xFF2D3047)

    val panelBackground = Color(0xFF1A1A1A)
}
