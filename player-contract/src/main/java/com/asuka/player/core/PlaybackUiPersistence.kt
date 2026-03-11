package com.asuka.player.contract

interface PlaybackUiPersistence {
    fun readZoom(mediaId: String): Float?
    fun saveZoom(mediaId: String, zoom: Float)
    fun readRememberedBrightness(): Float?
    fun saveRememberedBrightness(brightness: Float)
}
