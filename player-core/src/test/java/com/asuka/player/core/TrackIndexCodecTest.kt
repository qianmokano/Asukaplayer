package com.asuka.player.core

import kotlin.test.Test
import kotlin.test.assertEquals

class TrackIndexCodecTest {

    @Test
    fun encodeDecode_roundTrip() {
        val encoded = TrackIndexCodec.encode(12, 34)
        val (g, t) = TrackIndexCodec.decode(encoded)
        assertEquals(12, g)
        assertEquals(34, t)
    }
}
