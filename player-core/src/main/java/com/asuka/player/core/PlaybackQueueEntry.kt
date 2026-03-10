package com.asuka.player.core

import android.net.Uri

data class PlaybackQueueEntry(
    val mediaId: String,
    val uri: Uri,
)
