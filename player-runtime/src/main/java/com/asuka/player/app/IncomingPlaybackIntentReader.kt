package com.asuka.player.runtime

import android.content.Intent
import com.asuka.player.platform.PlaybackIntentPayload
import com.asuka.player.platform.PlaybackIntentPayloadCodec

data class IncomingPlaybackIntent(
    val payload: PlaybackIntentPayload,
)

object IncomingPlaybackIntentReader {
    fun read(intent: Intent?): IncomingPlaybackIntent? {
        return PlaybackIntentPayloadCodec.fromExternalIntent(intent)?.let(::IncomingPlaybackIntent)
    }
}
