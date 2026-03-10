package com.asuka.player.core

import android.content.Context
import android.view.Window

interface PlaybackDeviceController {
    fun currentVolumePercent(): Int
    fun setVolumePercent(percent: Int)
    fun currentBrightnessPercent(): Int
    fun setBrightnessPercent(percent: Int)
}

fun interface PlaybackDeviceControllerFactory {
    fun create(
        context: Context,
        window: Window,
    ): PlaybackDeviceController
}

interface PlaybackUiPersistence {
    fun readZoom(mediaId: String): Float?
    fun savePlaybackSpeed(mediaId: String, speed: Float)
    fun saveAudioTrack(mediaId: String, trackId: String)
    fun saveSubtitleTrack(mediaId: String, trackId: String)
    fun disableSubtitles(mediaId: String)
    fun saveZoom(mediaId: String, zoom: Float)
    fun readRememberedBrightness(): Float?
    fun saveRememberedBrightness(brightness: Float)
}
