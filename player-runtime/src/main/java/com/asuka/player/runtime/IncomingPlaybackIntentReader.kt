package com.asuka.player.runtime

import android.content.Intent
import com.asuka.player.contract.PlaybackSessionRequest
import com.asuka.player.platform.PlaybackSessionRequestCodec

data class IncomingPlaybackIntent(
    val request: PlaybackSessionRequest,
)

object IncomingPlaybackIntentReader {
    fun read(intent: Intent?): IncomingPlaybackIntent? {
        return PlaybackSessionRequestCodec.fromExternalIntent(intent)?.let(::IncomingPlaybackIntent)
    }
}
