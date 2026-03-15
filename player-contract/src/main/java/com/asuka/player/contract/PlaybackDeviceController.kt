package com.asuka.player.contract

interface PlaybackDeviceController {
    fun currentVolumePercent(): Int
    fun setVolumePercent(percent: Int)
    fun currentBrightnessPercent(): Int
    fun setBrightnessPercent(percent: Int)
}
