package com.asuka.player.contract

interface PlaybackPreviewFrameProvider {
    suspend fun loadPreviewFrame(
        mediaId: String,
        positionMs: Long,
        maxWidthPx: Int,
        maxHeightPx: Int,
    ): ByteArray?
}
