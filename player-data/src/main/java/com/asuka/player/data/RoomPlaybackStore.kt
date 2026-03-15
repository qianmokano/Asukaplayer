package com.asuka.player.data

import com.asuka.player.contract.PlaybackStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class RoomPlaybackStore(
    private val playbackStateDao: PlaybackStateDao,
    private val maxEntries: Int = 200,
    private val nowMs: () -> Long = System::currentTimeMillis,
) : PlaybackStore {
    private val mutex = Mutex()

    override suspend fun recentMediaIds(limit: Int): List<String> {
        return withContext(Dispatchers.IO) {
            mutex.withLock {
                val safeLimit = limit.coerceAtLeast(0)
                if (safeLimit == 0) return@withLock emptyList()
                playbackStateDao.recentMediaIds(safeLimit)
            }
        }
    }

    override suspend fun loadPosition(mediaId: String): Long? = withContext(Dispatchers.IO) {
        mutex.withLock {
            playbackStateDao.findByMediaId(mediaId)?.positionMs
        }
    }

    override suspend fun savePosition(mediaId: String, positionMs: Long) {
        update(mediaId) { current ->
            current.copy(positionMs = positionMs)
        }
    }

    override suspend fun loadPlaybackSpeed(mediaId: String): Float? = withContext(Dispatchers.IO) {
        mutex.withLock {
            playbackStateDao.findByMediaId(mediaId)?.playbackSpeed
        }
    }

    override suspend fun savePlaybackSpeed(mediaId: String, speed: Float) {
        update(mediaId) { current ->
            current.copy(playbackSpeed = speed)
        }
    }

    override suspend fun loadAudioTrackId(mediaId: String): String? = withContext(Dispatchers.IO) {
        mutex.withLock {
            playbackStateDao.findByMediaId(mediaId)?.audioTrackId
        }
    }

    override suspend fun saveAudioTrackId(mediaId: String, trackId: String) {
        update(mediaId) { current ->
            current.copy(audioTrackId = trackId)
        }
    }

    override suspend fun loadSubtitleTrackId(mediaId: String): String? = withContext(Dispatchers.IO) {
        mutex.withLock {
            playbackStateDao.findByMediaId(mediaId)?.subtitleTrackId
        }
    }

    override suspend fun saveSubtitleTrackId(mediaId: String, trackId: String) {
        update(mediaId) { current ->
            current.copy(subtitleTrackId = trackId)
        }
    }

    override suspend fun loadZoom(mediaId: String): Float? = withContext(Dispatchers.IO) {
        mutex.withLock {
            playbackStateDao.findByMediaId(mediaId)?.zoom
        }
    }

    override suspend fun saveZoom(mediaId: String, zoom: Float) {
        update(mediaId) { current ->
            current.copy(zoom = zoom)
        }
    }

    private suspend fun update(
        mediaId: String,
        change: (PlaybackStateEntity) -> PlaybackStateEntity,
    ) {
        withContext(Dispatchers.IO) {
            mutex.withLock {
                val current = playbackStateDao.findByMediaId(mediaId)
                val isNewEntry = current == null
                val base = current ?: PlaybackStateEntity(mediaId = mediaId)
                playbackStateDao.upsert(
                    change(base).copy(
                        mediaId = mediaId,
                        lastTouchedAt = nowMs(),
                    ),
                )
                if (isNewEntry) {
                    playbackStateDao.pruneToMaxEntries(maxEntries)
                }
            }
        }
    }
}
