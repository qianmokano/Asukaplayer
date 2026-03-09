package com.asuka.player.core

object TrackIndexCodec {
    // Bit layout of the encoded Int:
    //   bits 30..16  (15 bits) → groupIndex, range [0, 32767]
    //   bits 15..0   (16 bits) → trackIndex, range [0, 65535]
    //   bit  31      (sign bit) is always 0 so the value is non-negative.
    private const val MAX_GROUP_INDEX = 0x7FFF
    private const val MAX_TRACK_INDEX = 0xFFFF

    /**
     * Sentinel value used by UI selection state to indicate that subtitles were
     * explicitly disabled by the user. Consumers must check for this value before
     * calling [decode], as decoding it produces invalid indices.
     */
    const val SUBTITLE_DISABLED = -1

    fun encode(groupIndex: Int, trackIndex: Int): Int {
        require(groupIndex in 0..MAX_GROUP_INDEX) {
            "groupIndex $groupIndex out of encodable range [0, $MAX_GROUP_INDEX]"
        }
        require(trackIndex in 0..MAX_TRACK_INDEX) {
            "trackIndex $trackIndex out of encodable range [0, $MAX_TRACK_INDEX]"
        }
        return (groupIndex shl 16) or trackIndex
    }

    /**
     * Decodes a value previously produced by [encode].
     * **Callers must check [SUBTITLE_DISABLED] before calling this** — decoding -1 produces
     * invalid indices because the sentinel was never passed through [encode].
     */
    fun decode(value: Int): Pair<Int, Int> {
        require(value >= 0) {
            "decode() expects a non-negative value from encode(). " +
                "Check for SUBTITLE_DISABLED (-1) before calling decode()."
        }
        val groupIndex = value ushr 16          // unsigned shift avoids sign extension
        val trackIndex = value and MAX_TRACK_INDEX
        return groupIndex to trackIndex
    }
}
