package com.asuka.player.contract

interface PlaybackPreviewFrameProvider {
    suspend fun loadPreviewFrame(
        playbackUri: String,
        positionMs: Long,
        maxWidthPx: Int,
        maxHeightPx: Int,
    ): ByteArray?
}
