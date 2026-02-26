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
 */
class SharedPreferencesPlaybackStore(context: Context) : PlaybackStore {
    private val prefs = context.getSharedPreferences("asuka_playback_state", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_MEDIA_IDS = "__recent_media_ids__"
        private const val MAX_ENTRIES = 200
        // Newline is not valid in URIs (RFC 3986), so it is safe as a separator.
        private const val SEP = "\n"
    }

    /**
     * Records [mediaId] as the most-recently-used entry.
     * Evicts the oldest entry (and deletes its keys) if the list exceeds [MAX_ENTRIES].
     * All mutations are written to [editor] so callers can batch them in one apply().
     */
    private fun touchMediaId(mediaId: String, editor: SharedPreferences.Editor) {
        val raw = prefs.getString(KEY_MEDIA_IDS, "") ?: ""
        val ids: MutableList<String> = if (raw.isEmpty()) mutableListOf()
                                       else raw.split(SEP).toMutableList()
        ids.remove(mediaId)
        ids.add(mediaId)
        while (ids.size > MAX_ENTRIES) {
            val evicted = ids.removeFirst()
            editor.remove("pos:$evicted")
                  .remove("spd:$evicted")
                  .remove("aud:$evicted")
                  .remove("sub:$evicted")
                  .remove("zoom:$evicted")
        }
        editor.putString(KEY_MEDIA_IDS, ids.joinToString(SEP))
    }

    override fun loadPosition(mediaId: String): Long? =
        if (prefs.contains("pos:$mediaId")) prefs.getLong("pos:$mediaId", 0L) else null

    override fun savePosition(mediaId: String, positionMs: Long) {
        val editor = prefs.edit()
        touchMediaId(mediaId, editor)
        editor.putLong("pos:$mediaId", positionMs).apply()
    }

    override fun loadPlaybackSpeed(mediaId: String): Float? =
        if (prefs.contains("spd:$mediaId")) prefs.getFloat("spd:$mediaId", 1f) else null

    override fun savePlaybackSpeed(mediaId: String, speed: Float) {
        val editor = prefs.edit()
        touchMediaId(mediaId, editor)
        editor.putFloat("spd:$mediaId", speed).apply()
    }

    override fun loadAudioTrack(mediaId: String): Int? =
        if (prefs.contains("aud:$mediaId")) prefs.getInt("aud:$mediaId", 0) else null

    override fun saveAudioTrack(mediaId: String, trackIndex: Int) {
        val editor = prefs.edit()
        touchMediaId(mediaId, editor)
        editor.putInt("aud:$mediaId", trackIndex).apply()
    }

    override fun loadSubtitleTrack(mediaId: String): Int? =
        if (prefs.contains("sub:$mediaId")) prefs.getInt("sub:$mediaId", 0) else null

    override fun saveSubtitleTrack(mediaId: String, trackIndex: Int) {
        val editor = prefs.edit()
        touchMediaId(mediaId, editor)
        editor.putInt("sub:$mediaId", trackIndex).apply()
    }

    override fun loadZoom(mediaId: String): Float? =
        if (prefs.contains("zoom:$mediaId")) prefs.getFloat("zoom:$mediaId", 1f) else null

    override fun saveZoom(mediaId: String, zoom: Float) {
        val editor = prefs.edit()
        touchMediaId(mediaId, editor)
        editor.putFloat("zoom:$mediaId", zoom).apply()
    }
}
