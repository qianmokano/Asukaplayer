package com.asuka.player.core

import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import androidx.annotation.MainThread
import com.asuka.player.data.PlaybackStore
import java.util.concurrent.ConcurrentHashMap

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
 * **Thread safety:** All public methods must be called on the main thread.
 * Writes are coalesced into a single [SharedPreferences.Editor.apply] per main-loop
 * pass to reduce disk I/O and avoid ANRs during `Activity.onPause()` flushes.
 *
 * A [pendingValues] overlay ensures that values written via [editor] are immediately
 * visible to subsequent reads, even before the editor is flushed.
 */
class SharedPreferencesPlaybackStore(context: Context) : PlaybackStore {
    private val prefs = context.getSharedPreferences("asuka_playback_state", Context.MODE_PRIVATE)

    // In-memory cache of the mediaId list. Loaded lazily on first write.
    private var cachedIds: MutableList<String>? = null

    // Pending editor that accumulates writes until the next main-loop drain.
    private var pendingEditor: SharedPreferences.Editor? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private val flushRunnable = Runnable { flush() }

    // Overlay for values written but not yet flushed, so reads see fresh data.
    private val pendingValues = ConcurrentHashMap<String, Any>()

    companion object {
        private const val KEY_MEDIA_IDS = "__recent_media_ids__"
        private const val MAX_ENTRIES = 200
        // Newline is not valid in URIs (RFC 3986), so it is safe as a separator.
        private const val SEP = "\n"
    }

    /**
     * Returns the shared editor, creating one if needed and scheduling a flush
     * on the next main-loop iteration. Multiple save calls within the same frame
     * will batch into a single [SharedPreferences.Editor.apply].
     */
    @MainThread
    private fun editor(): SharedPreferences.Editor {
        pendingEditor?.let { return it }
        val ed = prefs.edit()
        pendingEditor = ed
        mainHandler.post(flushRunnable)
        return ed
    }

    @MainThread
    private fun flush() {
        pendingEditor?.apply()
        pendingEditor = null
        pendingValues.clear()
    }

    @MainThread
    private fun getOrLoadIds(): MutableList<String> {
        cachedIds?.let { return it }
        val raw = prefs.getString(KEY_MEDIA_IDS, "") ?: ""
        return (if (raw.isEmpty()) mutableListOf() else raw.split(SEP).toMutableList())
            .also { cachedIds = it }
    }

    /**
     * Records [mediaId] as the most-recently-used entry.
     * Evicts the oldest entry (and deletes its keys) if the list exceeds [MAX_ENTRIES].
     * All mutations are written to [editor] so callers can batch them in one apply().
     */
    @MainThread
    private fun touchMediaId(mediaId: String, editor: SharedPreferences.Editor) {
        val ids = getOrLoadIds()
        ids.remove(mediaId)
        ids.add(mediaId)
        while (ids.size > MAX_ENTRIES) {
            val evicted = ids.removeFirst()
            editor.remove("pos:$evicted")
                  .remove("spd:$evicted")
                  .remove("aud:$evicted")
                  .remove("sub:$evicted")
                  .remove("zoom:$evicted")
            pendingValues.remove("pos:$evicted")
            pendingValues.remove("spd:$evicted")
            pendingValues.remove("aud:$evicted")
            pendingValues.remove("sub:$evicted")
            pendingValues.remove("zoom:$evicted")
        }
        editor.putString(KEY_MEDIA_IDS, ids.joinToString(SEP))
    }

    override fun loadPosition(mediaId: String): Long? {
        val key = "pos:$mediaId"
        pendingValues[key]?.let { return it as Long }
        return if (prefs.contains(key)) prefs.getLong(key, 0L) else null
    }

    @MainThread
    override fun savePosition(mediaId: String, positionMs: Long) {
        val ed = editor()
        touchMediaId(mediaId, ed)
        ed.putLong("pos:$mediaId", positionMs)
        pendingValues["pos:$mediaId"] = positionMs
    }

    override fun loadPlaybackSpeed(mediaId: String): Float? {
        val key = "spd:$mediaId"
        pendingValues[key]?.let { return it as Float }
        return if (prefs.contains(key)) prefs.getFloat(key, 1f) else null
    }

    @MainThread
    override fun savePlaybackSpeed(mediaId: String, speed: Float) {
        val ed = editor()
        touchMediaId(mediaId, ed)
        ed.putFloat("spd:$mediaId", speed)
        pendingValues["spd:$mediaId"] = speed
    }

    override fun loadAudioTrack(mediaId: String): Int? {
        val key = "aud:$mediaId"
        pendingValues[key]?.let { return it as Int }
        return if (prefs.contains(key)) prefs.getInt(key, 0) else null
    }

    @MainThread
    override fun saveAudioTrack(mediaId: String, trackIndex: Int) {
        val ed = editor()
        touchMediaId(mediaId, ed)
        ed.putInt("aud:$mediaId", trackIndex)
        pendingValues["aud:$mediaId"] = trackIndex
    }

    override fun loadSubtitleTrack(mediaId: String): Int? {
        val key = "sub:$mediaId"
        pendingValues[key]?.let { return it as Int }
        return if (prefs.contains(key)) prefs.getInt(key, 0) else null
    }

    @MainThread
    override fun saveSubtitleTrack(mediaId: String, trackIndex: Int) {
        val ed = editor()
        touchMediaId(mediaId, ed)
        ed.putInt("sub:$mediaId", trackIndex)
        pendingValues["sub:$mediaId"] = trackIndex
    }

    override fun loadZoom(mediaId: String): Float? {
        val key = "zoom:$mediaId"
        pendingValues[key]?.let { return it as Float }
        return if (prefs.contains(key)) prefs.getFloat(key, 1f) else null
    }

    @MainThread
    override fun saveZoom(mediaId: String, zoom: Float) {
        val ed = editor()
        touchMediaId(mediaId, ed)
        ed.putFloat("zoom:$mediaId", zoom)
        pendingValues["zoom:$mediaId"] = zoom
    }
}
