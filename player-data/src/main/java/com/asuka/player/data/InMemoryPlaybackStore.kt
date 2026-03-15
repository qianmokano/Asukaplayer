package com.asuka.player.data

import com.asuka.player.contract.PlaybackStore
import java.util.concurrent.ConcurrentHashMap

class InMemoryPlaybackStore : PlaybackStore {
    private val positions = ConcurrentHashMap<String, Long>()
    private val speeds = ConcurrentHashMap<String, Float>()
    private val audioTrackIds = ConcurrentHashMap<String, String>()
    private val subtitleTrackIds = ConcurrentHashMap<String, String>()
    private val zooms = ConcurrentHashMap<String, Float>()
    private val recentIds = LinkedHashSet<String>()

    override suspend fun recentMediaIds(limit: Int): List<String> {
        synchronized(recentIds) {
            return recentIds.toList().takeLast(limit).reversed()
        }
    }

    private fun touch(mediaId: String) {
        synchronized(recentIds) {
            recentIds.remove(mediaId)
            recentIds.add(mediaId)
        }
    }

    override suspend fun loadPosition(mediaId: String): Long? = positions[mediaId]
    override suspend fun savePosition(mediaId: String, positionMs: Long) {
        positions[mediaId] = positionMs
        touch(mediaId)
    }

    override suspend fun loadPlaybackSpeed(mediaId: String): Float? = speeds[mediaId]
    override suspend fun savePlaybackSpeed(mediaId: String, speed: Float) {
        speeds[mediaId] = speed
        touch(mediaId)
    }

    override suspend fun loadAudioTrackId(mediaId: String): String? = audioTrackIds[mediaId]
    override suspend fun saveAudioTrackId(mediaId: String, trackId: String) {
        audioTrackIds[mediaId] = trackId
        touch(mediaId)
    }

    override suspend fun loadSubtitleTrackId(mediaId: String): String? = subtitleTrackIds[mediaId]
    override suspend fun saveSubtitleTrackId(mediaId: String, trackId: String) {
        subtitleTrackIds[mediaId] = trackId
        touch(mediaId)
    }

    override suspend fun loadZoom(mediaId: String): Float? = zooms[mediaId]
    override suspend fun saveZoom(mediaId: String, zoom: Float) {
        zooms[mediaId] = zoom
        touch(mediaId)
    }
}
