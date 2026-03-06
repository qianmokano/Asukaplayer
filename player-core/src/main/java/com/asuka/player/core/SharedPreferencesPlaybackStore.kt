package com.asuka.player.core

import android.content.Context
import android.content.SharedPreferences
import com.asuka.player.data.PlaybackStore

/**
 * SharedPreferences-backed PlaybackStore. Survives process restarts.
 *
 * Maintains an LRU list of up to [MAX_ENTRIES] distinct mediaIds under the key
 * [KEY_MEDIA_IDS]. When the limit is exceeded the oldest entry and all five of
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

    internal object MediaIdListCodec {
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
                    require(len >= 0) { "negative length" }
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
                  .remove("zoom:$evicted")
        }
        editor.putString(KEY_MEDIA_IDS, MediaIdListCodec.encode(ids))
    }

    override fun recentMediaIds(limit: Int): List<String> {
        synchronized(lock) {
            val safeLimit = limit.coerceAtLeast(0)
            if (safeLimit == 0) return emptyList()
            val ids = getOrLoadIds()
            return ids.takeLast(safeLimit).asReversed()
        }
    }

    override fun loadPosition(mediaId: String): Long? {
        synchronized(lock) {
            val key = "pos:$mediaId"
            return if (prefs.contains(key)) prefs.getLong(key, 0L) else null
        }
    }

    override fun savePosition(mediaId: String, positionMs: Long) {
        synchronized(lock) {
            prefs.edit()
                .also { touchMediaId(mediaId, it) }
                .putLong("pos:$mediaId", positionMs)
                .apply()
        }
    }

    override fun loadPlaybackSpeed(mediaId: String): Float? {
        synchronized(lock) {
            val key = "spd:$mediaId"
            return if (prefs.contains(key)) prefs.getFloat(key, 1f) else null
        }
    }

    override fun savePlaybackSpeed(mediaId: String, speed: Float) {
        synchronized(lock) {
            prefs.edit()
                .also { touchMediaId(mediaId, it) }
                .putFloat("spd:$mediaId", speed)
                .apply()
        }
    }

    override fun loadAudioTrack(mediaId: String): Int? {
        synchronized(lock) {
            val key = "aud:$mediaId"
            return if (prefs.contains(key)) prefs.getInt(key, 0) else null
        }
    }

    override fun saveAudioTrack(mediaId: String, trackIndex: Int) {
        synchronized(lock) {
            prefs.edit()
                .also { touchMediaId(mediaId, it) }
                .putInt("aud:$mediaId", trackIndex)
                .apply()
        }
    }

    override fun loadSubtitleTrack(mediaId: String): Int? {
        synchronized(lock) {
            val key = "sub:$mediaId"
            return if (prefs.contains(key)) prefs.getInt(key, 0) else null
        }
    }

    override fun saveSubtitleTrack(mediaId: String, trackIndex: Int) {
        synchronized(lock) {
            prefs.edit()
                .also { touchMediaId(mediaId, it) }
                .putInt("sub:$mediaId", trackIndex)
                .apply()
        }
    }

    override fun loadZoom(mediaId: String): Float? {
        synchronized(lock) {
            val key = "zoom:$mediaId"
            return if (prefs.contains(key)) prefs.getFloat(key, 1f) else null
        }
    }

    override fun saveZoom(mediaId: String, zoom: Float) {
        synchronized(lock) {
            prefs.edit()
                .also { touchMediaId(mediaId, it) }
                .putFloat("zoom:$mediaId", zoom)
                .apply()
        }
    }
}
