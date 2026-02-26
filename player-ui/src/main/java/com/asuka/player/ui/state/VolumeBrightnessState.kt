package com.asuka.player.ui.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class VolumeBrightnessState {
    enum class Mode { VOLUME, BRIGHTNESS }

    var activeMode: Mode? by mutableStateOf(null)
        private set

    var volumePercent: Int by mutableIntStateOf(0)
        private set

    var brightnessPercent: Int by mutableIntStateOf(0)
        private set

    fun start(mode: Mode, currentVolume: Int, currentBrightness: Int) {
        activeMode = mode
        volumePercent = currentVolume
        brightnessPercent = currentBrightness
    }

    fun updateVolume(value: Int) {
        activeMode = Mode.VOLUME
        volumePercent = value
    }

    fun updateBrightness(value: Int) {
        activeMode = Mode.BRIGHTNESS
        brightnessPercent = value
    }

    fun end() {
        activeMode = null
    }
}
