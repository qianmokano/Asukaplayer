package com.asuka.player.data

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class InMemoryPlaybackStoreTest {

    private fun store() = InMemoryPlaybackStore()

    // --- Position ---

    @Test
    fun loadPosition_returnsNullForUnknownMediaId() {
        assertNull(store().loadPosition("unknown"))
    }

    @Test
    fun saveAndLoadPosition_roundTrip() {
        val s = store()
        s.savePosition("v1", 12345L)
        assertEquals(12345L, s.loadPosition("v1"))
    }

    @Test
    fun savePosition_overwritesPreviousValue() {
        val s = store()
        s.savePosition("v1", 100L)
        s.savePosition("v1", 200L)
        assertEquals(200L, s.loadPosition("v1"))
    }

    // --- PlaybackSpeed ---

    @Test
    fun loadPlaybackSpeed_returnsNullForUnknownMediaId() {
        assertNull(store().loadPlaybackSpeed("unknown"))
    }

    @Test
    fun saveAndLoadPlaybackSpeed_roundTrip() {
        val s = store()
        s.savePlaybackSpeed("v1", 1.5f)
        assertEquals(1.5f, s.loadPlaybackSpeed("v1"))
    }

    @Test
    fun savePlaybackSpeed_overwritesPreviousValue() {
        val s = store()
        s.savePlaybackSpeed("v1", 1.0f)
        s.savePlaybackSpeed("v1", 2.0f)
        assertEquals(2.0f, s.loadPlaybackSpeed("v1"))
    }

    // --- AudioTrack ---

    @Test
    fun loadAudioTrackId_returnsNullForUnknownMediaId() {
        assertNull(store().loadAudioTrackId("unknown"))
    }

    @Test
    fun saveAndLoadAudioTrackId_roundTrip() {
        val s = store()
        s.saveAudioTrackId("v1", "audio-3")
        assertEquals("audio-3", s.loadAudioTrackId("v1"))
    }

    @Test
    fun saveAudioTrackId_overwritesPreviousValue() {
        val s = store()
        s.saveAudioTrackId("v1", "audio-1")
        s.saveAudioTrackId("v1", "audio-5")
        assertEquals("audio-5", s.loadAudioTrackId("v1"))
    }

    // --- SubtitleTrack ---

    @Test
    fun loadSubtitleTrackId_returnsNullForUnknownMediaId() {
        assertNull(store().loadSubtitleTrackId("unknown"))
    }

    @Test
    fun saveAndLoadSubtitleTrackId_roundTrip() {
        val s = store()
        s.saveSubtitleTrackId("v1", "subtitle-2")
        assertEquals("subtitle-2", s.loadSubtitleTrackId("v1"))
    }

    @Test
    fun saveSubtitleTrackId_overwritesPreviousValue() {
        val s = store()
        s.saveSubtitleTrackId("v1", "subtitle-0")
        s.saveSubtitleTrackId("v1", "subtitle-4")
        assertEquals("subtitle-4", s.loadSubtitleTrackId("v1"))
    }

    // --- Zoom ---

    @Test
    fun loadZoom_returnsNullForUnknownMediaId() {
        assertNull(store().loadZoom("unknown"))
    }

    @Test
    fun saveAndLoadZoom_roundTrip() {
        val s = store()
        s.saveZoom("v1", 2.5f)
        assertEquals(2.5f, s.loadZoom("v1"))
    }

    @Test
    fun saveZoom_overwritesPreviousValue() {
        val s = store()
        s.saveZoom("v1", 1.0f)
        s.saveZoom("v1", 3.0f)
        assertEquals(3.0f, s.loadZoom("v1"))
    }

    // --- Independence ---

    @Test
    fun differentMediaIds_doNotInterfere() {
        val s = store()
        s.savePosition("v1", 100L)
        s.savePosition("v2", 200L)
        s.savePlaybackSpeed("v1", 1.0f)
        s.savePlaybackSpeed("v2", 2.0f)
        s.saveAudioTrackId("v1", "audio-0")
        s.saveAudioTrackId("v2", "audio-1")
        s.saveSubtitleTrackId("v1", "subtitle-10")
        s.saveSubtitleTrackId("v2", "subtitle-20")
        s.saveZoom("v1", 1.5f)
        s.saveZoom("v2", 2.5f)

        assertEquals(100L, s.loadPosition("v1"))
        assertEquals(200L, s.loadPosition("v2"))
        assertEquals(1.0f, s.loadPlaybackSpeed("v1"))
        assertEquals(2.0f, s.loadPlaybackSpeed("v2"))
        assertEquals("audio-0", s.loadAudioTrackId("v1"))
        assertEquals("audio-1", s.loadAudioTrackId("v2"))
        assertEquals("subtitle-10", s.loadSubtitleTrackId("v1"))
        assertEquals("subtitle-20", s.loadSubtitleTrackId("v2"))
        assertEquals(1.5f, s.loadZoom("v1"))
        assertEquals(2.5f, s.loadZoom("v2"))

        assertNull(s.loadPosition("v3"))
    }
}
