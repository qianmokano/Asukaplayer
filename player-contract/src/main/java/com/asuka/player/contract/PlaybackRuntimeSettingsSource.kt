package com.asuka.player.contract

import kotlinx.coroutines.flow.StateFlow

interface PlaybackRuntimeSettingsSource {
    val settings: StateFlow<PlayerSettings>

    fun current(): PlayerSettings = settings.value
}
