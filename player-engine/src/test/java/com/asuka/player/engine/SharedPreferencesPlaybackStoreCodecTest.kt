package com.asuka.player.engine

import com.asuka.player.data.SharedPreferencesPlaybackStore
import kotlin.test.Test
import kotlin.test.assertEquals

class SharedPreferencesPlaybackStoreCodecTest {

    @Test
    fun mediaIdListCodec_roundTrip_allowsNewlines() {
        val ids = listOf(
            "content://media/external/video/media/42",
            "line1\nline2",
            "contains:colon",
            "emoji-\uD83C\uDF7F",
            "",
        )
        val encoded = SharedPreferencesPlaybackStore.MediaIdListCodec.encode(ids)
        val decoded = SharedPreferencesPlaybackStore.MediaIdListCodec.decode(encoded)
        assertEquals(ids, decoded)
    }

    @Test
    fun mediaIdListCodec_decode_legacySeparator() {
        val raw = "a\nb\nc"
        val decoded = SharedPreferencesPlaybackStore.MediaIdListCodec.decode(raw)
        assertEquals(listOf("a", "b", "c"), decoded)
    }
}
