package com.asuka.player.data

import android.content.Context
import android.content.SharedPreferences
import com.asuka.player.contract.PlaybackStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * SharedPreferences-backed PlaybackStore. Survives process restarts.
 *
 * Maintains an LRU list of up to [MAX_ENTRIES] distinct mediaIds under the key
 * [KEY_MEDIA_IDS]. When the limit is exceeded the oldest entry and all seven of
 * its associated keys are removed in the same [SharedPreferences.Editor.apply] call.
 *
 * The list of mediaIds is cached in memory after first access so that repeated
 * save operations (e.g. periodic position writes during playback) do not incur
 * string-parsing overhead on every call.
 *
 * **Thread safety:** All operations are serialized on [lock], so callers can
 * read/write safely from the main thread, Media3 callbacks, or background work
 * without depending on looper affinity.
 */
class SharedPreferencesPlaybackStore(context: Context) : PlaybackStore {
    private val prefs = context.getSharedPreferences("asuka_playback_state", Context.MODE_PRIVATE)
    private val lock = Any()

    // In-memory cache of the mediaId list. Loaded lazily on first write.
    private var cachedIds: MutableList<String>? = null

    companion object {
        private const val KEY_MEDIA_IDS = "__recent_media_ids__"
        private const val MAX_ENTRIES = 200
        // Legacy separator for media id lists stored as a simple newline-joined string.
        private const val LEGACY_SEP = "\n"
        private const val LIST_PREFIX = "lp:"
    }

    object MediaIdListCodec {
        // Reasonable upper bound for a single media ID / URI entry (e.g. long content:// URI).
        // Prevents integer overflow in (colonIdx + 1 + len) and limits per-entry allocations.
        private const val MAX_ENTRY_LEN = 4096

        fun encode(ids: List<String>): String {
            val builder = StringBuilder(LIST_PREFIX)
            ids.forEach { id ->
                builder.append(id.length).append(':').append(id)
            }
            return builder.toString()
        }

        fun decode(raw: String): MutableList<String> {
            if (!raw.startsWith(LIST_PREFIX)) return decodeLegacy(raw)
            return runCatching {
                val out = mutableListOf<String>()
                var idx = LIST_PREFIX.length
                while (idx < raw.length) {
                    val colonIdx = raw.indexOf(':', startIndex = idx)
                    require(colonIdx > idx) { "missing length delimiter" }
                    val len = raw.substring(idx, colonIdx).toInt()
                    require(len in 0..MAX_ENTRY_LEN) { "entry length out of bounds: $len" }
                    val start = colonIdx + 1
                    val end = start + len
                    require(end <= raw.length) { "truncated entry" }
                    out += raw.substring(start, end)
                    idx = end
                }
                out
            }.getOrElse { decodeLegacy(raw) }
        }

        private fun decodeLegacy(raw: String): MutableList<String> {
            if (raw.isEmpty()) return mutableListOf()
            return raw.split(LEGACY_SEP)
                .filter { it.isNotEmpty() }
                .toMutableList()
        }
    }

    private fun getOrLoadIds(): MutableList<String> {
        cachedIds?.let { return it }
        val raw = prefs.getString(KEY_MEDIA_IDS, "") ?: ""
        return MediaIdListCodec.decode(raw).also { cachedIds = it }
    }

    /**
     * Records [mediaId] as the most-recently-used entry.
     * Evicts the oldest entry (and deletes its keys) if the list exceeds [MAX_ENTRIES].
     * All mutations are written into the same [editor] so the media id list and
     * associated state keys stay in sync for a single store operation.
     */
    private fun touchMediaId(mediaId: String, editor: SharedPreferences.Editor) {
        val ids = getOrLoadIds()
        ids.remove(mediaId)
        ids.add(mediaId)
        while (ids.size > MAX_ENTRIES) {
            val evicted = ids.removeAt(0)
            editor.remove("pos:$evicted")
                .remove("spd:$evicted")
                .remove("aud:$evicted")
                .remove("sub:$evicted")
                .remove("audid:$evicted")
                .remove("subid:$evicted")
                .remove("zoom:$evicted")
        }
        editor.putString(KEY_MEDIA_IDS, MediaIdListCodec.encode(ids))
    }

    override suspend fun recentMediaIds(limit: Int): List<String> {
        return withContext(Dispatchers.IO) {
            synchronized(lock) {
                val safeLimit = limit.coerceAtLeast(0)
                if (safeLimit == 0) return@synchronized emptyList()
                val ids = getOrLoadIds()
                ids.takeLast(safeLimit).asReversed()
            }
        }
    }

    override suspend fun loadPosition(mediaId: String): Long? {
        return withContext(Dispatchers.IO) {
            synchronized(lock) {
                val key = "pos:$mediaId"
                if (prefs.contains(key)) prefs.getLong(key, 0L) else null
            }
        }
    }

    override suspend fun savePosition(mediaId: String, positionMs: Long) {
        withContext(Dispatchers.IO) {
            synchronized(lock) {
                prefs.edit()
                    .also { touchMediaId(mediaId, it) }
                    .putLong("pos:$mediaId", positionMs)
                    .apply()
            }
        }
    }

    override suspend fun loadPlaybackSpeed(mediaId: String): Float? {
        return withContext(Dispatchers.IO) {
            synchronized(lock) {
                val key = "spd:$mediaId"
                if (prefs.contains(key)) prefs.getFloat(key, 1f) else null
            }
        }
    }

    override suspend fun savePlaybackSpeed(mediaId: String, speed: Float) {
        withContext(Dispatchers.IO) {
            synchronized(lock) {
                prefs.edit()
                    .also { touchMediaId(mediaId, it) }
                    .putFloat("spd:$mediaId", speed)
                    .apply()
            }
        }
    }

    override suspend fun loadAudioTrackId(mediaId: String): String? {
        return withContext(Dispatchers.IO) {
            synchronized(lock) {
                val key = "audid:$mediaId"
                if (prefs.contains(key)) prefs.getString(key, null) else null
            }
        }
    }

    override suspend fun saveAudioTrackId(mediaId: String, trackId: String) {
        withContext(Dispatchers.IO) {
            synchronized(lock) {
                prefs.edit()
                    .also { touchMediaId(mediaId, it) }
                    .putString("audid:$mediaId", trackId)
                    .apply()
            }
        }
    }

    override suspend fun loadSubtitleTrackId(mediaId: String): String? {
        return withContext(Dispatchers.IO) {
            synchronized(lock) {
                val key = "subid:$mediaId"
                if (prefs.contains(key)) prefs.getString(key, null) else null
            }
        }
    }

    override suspend fun saveSubtitleTrackId(mediaId: String, trackId: String) {
        withContext(Dispatchers.IO) {
            synchronized(lock) {
                prefs.edit()
                    .also { touchMediaId(mediaId, it) }
                    .putString("subid:$mediaId", trackId)
                    .apply()
            }
        }
    }

    override suspend fun loadZoom(mediaId: String): Float? {
        return withContext(Dispatchers.IO) {
            synchronized(lock) {
                val key = "zoom:$mediaId"
                if (prefs.contains(key)) prefs.getFloat(key, 1f) else null
            }
        }
    }

    override suspend fun saveZoom(mediaId: String, zoom: Float) {
        withContext(Dispatchers.IO) {
            synchronized(lock) {
                prefs.edit()
                    .also { touchMediaId(mediaId, it) }
                    .putFloat("zoom:$mediaId", zoom)
                    .apply()
            }
        }
    }
}
