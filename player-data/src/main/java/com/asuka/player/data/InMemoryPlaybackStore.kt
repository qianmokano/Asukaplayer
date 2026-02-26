package com.asuka.player.data

import java.util.concurrent.ConcurrentHashMap

class InMemoryPlaybackStore : PlaybackStore {
    private val positions = ConcurrentHashMap<String, Long>()
    private val speeds = ConcurrentHashMap<String, Float>()
    private val audioTracks = ConcurrentHashMap<String, Int>()
    private val subtitleTracks = ConcurrentHashMap<String, Int>()
    private val zooms = ConcurrentHashMap<String, Float>()

    override fun loadPosition(mediaId: String): Long? = positions[mediaId]
    override fun savePosition(mediaId: String, positionMs: Long) {
        positions[mediaId] = positionMs
    }

    override fun loadPlaybackSpeed(mediaId: String): Float? = speeds[mediaId]
    override fun savePlaybackSpeed(mediaId: String, speed: Float) {
        speeds[mediaId] = speed
    }

    override fun loadAudioTrack(mediaId: String): Int? = audioTracks[mediaId]
    override fun saveAudioTrack(mediaId: String, trackIndex: Int) {
        audioTracks[mediaId] = trackIndex
    }

    override fun loadSubtitleTrack(mediaId: String): Int? = subtitleTracks[mediaId]
    override fun saveSubtitleTrack(mediaId: String, trackIndex: Int) {
        subtitleTracks[mediaId] = trackIndex
    }

    override fun loadZoom(mediaId: String): Float? = zooms[mediaId]
    override fun saveZoom(mediaId: String, zoom: Float) {
        zooms[mediaId] = zoom
    }
}
