package com.asuka.player.contract

interface PlaybackUiPersistence {
    suspend fun readZoom(mediaId: String): Float?
    suspend fun saveZoom(mediaId: String, zoom: Float)
    fun readRememberedBrightness(): Float?
    fun saveRememberedBrightness(brightness: Float)
    suspend fun readRememberedVolumePercent(): Int?
    fun saveRememberedVolumePercent(volumePercent: Int)
}
