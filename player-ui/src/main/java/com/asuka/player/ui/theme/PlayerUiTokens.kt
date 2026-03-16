package com.asuka.player.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
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

    object ButtonSize {
        val playbackPrimary = 68.dp
        val playbackPrimaryIcon = 32.dp
    }

    @Composable
    fun buttonBackground(showBackground: Boolean = true): Color {
        return if (showBackground) MaterialTheme.colorScheme.onSecondaryContainer else Color.Transparent
    }

    @Composable
    fun buttonContent(showBackground: Boolean = true): Color {
        return if (showBackground) {
            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.92f)
        } else {
            Color.White
        }
    }

    @Composable
    fun loadingIndicatorColor(): Color {
        val colorScheme = MaterialTheme.colorScheme
        val base = listOf(
            colorScheme.primary,
            colorScheme.primaryContainer,
            colorScheme.inversePrimary,
        ).maxBy { it.luminance() }
        return lerp(base, Color.White, 0.18f)
    }

    val panelBackground = Color(0xFF1A1A1A)
}
