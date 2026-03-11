package com.asuka.player.contract

import kotlinx.coroutines.flow.StateFlow

interface PlaybackRuntimeSettingsSource {
    val settings: StateFlow<PlaybackRuntimeSettings>

    fun current(): PlaybackRuntimeSettings = settings.value
}
