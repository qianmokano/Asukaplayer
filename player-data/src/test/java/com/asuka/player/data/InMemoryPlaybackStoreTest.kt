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
    fun loadAudioTrack_returnsNullForUnknownMediaId() {
        assertNull(store().loadAudioTrack("unknown"))
    }

    @Test
    fun saveAndLoadAudioTrack_roundTrip() {
        val s = store()
        s.saveAudioTrack("v1", 3)
        assertEquals(3, s.loadAudioTrack("v1"))
    }

    @Test
    fun saveAudioTrack_overwritesPreviousValue() {
        val s = store()
        s.saveAudioTrack("v1", 1)
        s.saveAudioTrack("v1", 5)
        assertEquals(5, s.loadAudioTrack("v1"))
    }

    // --- SubtitleTrack ---

    @Test
    fun loadSubtitleTrack_returnsNullForUnknownMediaId() {
        assertNull(store().loadSubtitleTrack("unknown"))
    }

    @Test
    fun saveAndLoadSubtitleTrack_roundTrip() {
        val s = store()
        s.saveSubtitleTrack("v1", 2)
        assertEquals(2, s.loadSubtitleTrack("v1"))
    }

    @Test
    fun saveSubtitleTrack_overwritesPreviousValue() {
        val s = store()
        s.saveSubtitleTrack("v1", 0)
        s.saveSubtitleTrack("v1", 4)
        assertEquals(4, s.loadSubtitleTrack("v1"))
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
        s.saveAudioTrack("v1", 0)
        s.saveAudioTrack("v2", 1)
        s.saveSubtitleTrack("v1", 10)
        s.saveSubtitleTrack("v2", 20)
        s.saveZoom("v1", 1.5f)
        s.saveZoom("v2", 2.5f)

        assertEquals(100L, s.loadPosition("v1"))
        assertEquals(200L, s.loadPosition("v2"))
        assertEquals(1.0f, s.loadPlaybackSpeed("v1"))
        assertEquals(2.0f, s.loadPlaybackSpeed("v2"))
        assertEquals(0, s.loadAudioTrack("v1"))
        assertEquals(1, s.loadAudioTrack("v2"))
        assertEquals(10, s.loadSubtitleTrack("v1"))
        assertEquals(20, s.loadSubtitleTrack("v2"))
        assertEquals(1.5f, s.loadZoom("v1"))
        assertEquals(2.5f, s.loadZoom("v2"))

        assertNull(s.loadPosition("v3"))
    }
}
