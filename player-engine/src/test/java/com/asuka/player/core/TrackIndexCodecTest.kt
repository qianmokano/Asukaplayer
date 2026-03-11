package com.asuka.player.core

import com.asuka.player.contract.TrackIndexCodec
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals

class TrackIndexCodecTest {

    @Test
    fun encodeDecode_roundTrip() {
        val encoded = TrackIndexCodec.encode(12, 34)
        val (g, t) = TrackIndexCodec.decode(encoded)
        assertEquals(12, g)
        assertEquals(34, t)
    }

    @Test
    fun encodeDecode_zeroIndices() {
        val encoded = TrackIndexCodec.encode(0, 0)
        val (g, t) = TrackIndexCodec.decode(encoded)
        assertEquals(0, g)
        assertEquals(0, t)
    }

    @Test
    fun encodeDecode_maxGroupIndex() {
        val encoded = TrackIndexCodec.encode(0x7FFF, 0)
        val (g, _) = TrackIndexCodec.decode(encoded)
        assertEquals(0x7FFF, g)
    }

    @Test
    fun encodeDecode_maxTrackIndex() {
        val encoded = TrackIndexCodec.encode(0, 0xFFFF)
        val (_, t) = TrackIndexCodec.decode(encoded)
        assertEquals(0xFFFF, t)
    }

    @Test
    fun encode_rejectsNegativeGroupIndex() {
        assertFailsWith<IllegalArgumentException> {
            TrackIndexCodec.encode(-1, 0)
        }
    }

    @Test
    fun encode_rejectsOverflowGroupIndex() {
        assertFailsWith<IllegalArgumentException> {
            TrackIndexCodec.encode(0x8000, 0)
        }
    }

    @Test
    fun encode_rejectsNegativeTrackIndex() {
        assertFailsWith<IllegalArgumentException> {
            TrackIndexCodec.encode(0, -1)
        }
    }

    @Test
    fun subtitleDisabledSentinel() {
        assertEquals(-1, TrackIndexCodec.SUBTITLE_DISABLED)
        // Sentinel should never equal any valid encoded value
        val valid = TrackIndexCodec.encode(0, 0)
        assertNotEquals(TrackIndexCodec.SUBTITLE_DISABLED, valid)
    }
}
