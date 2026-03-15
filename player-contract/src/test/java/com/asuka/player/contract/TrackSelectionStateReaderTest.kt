package com.asuka.player.contract

import com.asuka.player.contract.TrackIndexCodec
import kotlin.test.Test
import kotlin.test.assertEquals

class TrackSelectionStateReaderTest {

    @Test
    fun decodeEncode_roundTrip() {
        val encoded = TrackIndexCodec.encode(2, 5)
        val (g, t) = TrackIndexCodec.decode(encoded)
        assertEquals(2, g)
        assertEquals(5, t)
    }
}
