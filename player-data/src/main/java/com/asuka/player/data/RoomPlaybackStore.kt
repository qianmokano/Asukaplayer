package com.asuka.player.data

import com.asuka.player.contract.PlaybackStore

class RoomPlaybackStore(
    private val playbackStateDao: PlaybackStateDao,
    private val maxEntries: Int = 200,
    private val nowMs: () -> Long = System::currentTimeMillis,
) : PlaybackStore {
    private val lock = Any()

    override fun recentMediaIds(limit: Int): List<String> {
        synchronized(lock) {
            val safeLimit = limit.coerceAtLeast(0)
            if (safeLimit == 0) return emptyList()
            return playbackStateDao.recentMediaIds(safeLimit)
        }
    }

    override fun loadPosition(mediaId: String): Long? = synchronized(lock) {
        playbackStateDao.findByMediaId(mediaId)?.positionMs
    }

    override fun savePosition(mediaId: String, positionMs: Long) {
        update(mediaId) { current ->
            current.copy(positionMs = positionMs)
        }
    }

    override fun loadPlaybackSpeed(mediaId: String): Float? = synchronized(lock) {
        playbackStateDao.findByMediaId(mediaId)?.playbackSpeed
    }

    override fun savePlaybackSpeed(mediaId: String, speed: Float) {
        update(mediaId) { current ->
            current.copy(playbackSpeed = speed)
        }
    }

    override fun loadAudioTrackId(mediaId: String): String? = synchronized(lock) {
        playbackStateDao.findByMediaId(mediaId)?.audioTrackId
    }

    override fun saveAudioTrackId(mediaId: String, trackId: String) {
        update(mediaId) { current ->
            current.copy(audioTrackId = trackId)
        }
    }

    override fun loadSubtitleTrackId(mediaId: String): String? = synchronized(lock) {
        playbackStateDao.findByMediaId(mediaId)?.subtitleTrackId
    }

    override fun saveSubtitleTrackId(mediaId: String, trackId: String) {
        update(mediaId) { current ->
            current.copy(subtitleTrackId = trackId)
        }
    }

    override fun loadZoom(mediaId: String): Float? = synchronized(lock) {
        playbackStateDao.findByMediaId(mediaId)?.zoom
    }

    override fun saveZoom(mediaId: String, zoom: Float) {
        update(mediaId) { current ->
            current.copy(zoom = zoom)
        }
    }

    private fun update(
        mediaId: String,
        change: (PlaybackStateEntity) -> PlaybackStateEntity,
    ) {
        synchronized(lock) {
            val current = playbackStateDao.findByMediaId(mediaId) ?: PlaybackStateEntity(mediaId = mediaId)
            playbackStateDao.upsert(
                change(current).copy(
                    mediaId = mediaId,
                    lastTouchedAt = nowMs(),
                ),
            )
            playbackStateDao.pruneToMaxEntries(maxEntries)
        }
    }
}
