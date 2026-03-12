package com.asuka.player.renderer.activity

import androidx.compose.runtime.Composable

interface PlaybackThemeProvider {
    @Composable
    fun ProvidePlaybackTheme(content: @Composable () -> Unit)
}
