package com.asuka.player.data

import java.util.concurrent.ConcurrentHashMap

class InMemoryPlaybackStore : PlaybackStore {
    private val positions = ConcurrentHashMap<String, Long>()
    private val speeds = ConcurrentHashMap<String, Float>()
    private val audioTrackIds = ConcurrentHashMap<String, String>()
    private val subtitleTrackIds = ConcurrentHashMap<String, String>()
    private val zooms = ConcurrentHashMap<String, Float>()

    override fun loadPosition(mediaId: String): Long? = positions[mediaId]
    override fun savePosition(mediaId: String, positionMs: Long) {
        positions[mediaId] = positionMs
    }

    override fun loadPlaybackSpeed(mediaId: String): Float? = speeds[mediaId]
    override fun savePlaybackSpeed(mediaId: String, speed: Float) {
        speeds[mediaId] = speed
    }

    override fun loadAudioTrackId(mediaId: String): String? = audioTrackIds[mediaId]
    override fun saveAudioTrackId(mediaId: String, trackId: String) {
        audioTrackIds[mediaId] = trackId
    }

    override fun loadSubtitleTrackId(mediaId: String): String? = subtitleTrackIds[mediaId]
    override fun saveSubtitleTrackId(mediaId: String, trackId: String) {
        subtitleTrackIds[mediaId] = trackId
    }

    override fun loadZoom(mediaId: String): Float? = zooms[mediaId]
    override fun saveZoom(mediaId: String, zoom: Float) {
        zooms[mediaId] = zoom
    }
}
