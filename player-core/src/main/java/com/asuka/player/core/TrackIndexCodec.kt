package com.asuka.player.core

object TrackIndexCodec {
    // groupIndex occupies the upper 15 bits (0..32767), trackIndex the lower 16 bits (0..65535).
    private const val MAX_GROUP_INDEX = 0x7FFF
    private const val MAX_TRACK_INDEX = 0xFFFF

    fun encode(groupIndex: Int, trackIndex: Int): Int {
        require(groupIndex in 0..MAX_GROUP_INDEX) {
            "groupIndex $groupIndex out of encodable range [0, $MAX_GROUP_INDEX]"
        }
        require(trackIndex in 0..MAX_TRACK_INDEX) {
            "trackIndex $trackIndex out of encodable range [0, $MAX_TRACK_INDEX]"
        }
        return (groupIndex shl 16) or trackIndex
    }

    fun decode(value: Int): Pair<Int, Int> {
        val groupIndex = value ushr 16          // unsigned shift avoids sign extension
        val trackIndex = value and MAX_TRACK_INDEX
        return groupIndex to trackIndex
    }
}
