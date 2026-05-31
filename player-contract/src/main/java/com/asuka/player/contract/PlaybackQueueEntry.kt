package com.asuka.player.contract

data class PlaybackQueueEntry(
    val mediaId: String,
    val uri: String,
    val persistable: Boolean = true,
)
