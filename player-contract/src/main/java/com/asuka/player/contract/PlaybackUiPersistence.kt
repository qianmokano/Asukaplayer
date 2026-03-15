package com.asuka.player.contract

interface PlaybackUiPersistence {
    suspend fun readZoom(mediaId: String): Float?
    suspend fun saveZoom(mediaId: String, zoom: Float)
    fun readRememberedBrightness(): Float?
    fun saveRememberedBrightness(brightness: Float)
}
