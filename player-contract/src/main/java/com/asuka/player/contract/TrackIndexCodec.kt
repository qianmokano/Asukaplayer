package com.asuka.player.contract

object TrackIndexCodec {
    private const val MAX_GROUP_INDEX = 0x7FFF
    private const val MAX_TRACK_INDEX = 0xFFFF

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

    fun decode(value: Int): Pair<Int, Int> {
        require(value >= 0) {
            "decode() expects a non-negative value from encode(). " +
                "Check for SUBTITLE_DISABLED (-1) before calling decode()."
        }
        val groupIndex = value ushr 16
        val trackIndex = value and MAX_TRACK_INDEX
        return groupIndex to trackIndex
    }
}
