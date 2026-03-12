package com.asuka.player.platform

import androidx.media3.common.Format
import androidx.media3.common.util.UnstableApi
import java.security.MessageDigest

object TrackSelectionIdentity {
    @UnstableApi
    fun create(
        type: Int,
        format: Format,
    ): String {
        val raw = buildString {
            append("v1|")
            append(type)
            append('|')
            append(format.id.orEmpty())
            append('|')
            append(format.label.orEmpty())
            append('|')
            append(format.language.orEmpty())
            append('|')
            append(format.sampleMimeType.orEmpty())
            append('|')
            append(format.containerMimeType.orEmpty())
            append('|')
            append(format.codecs.orEmpty())
            append('|')
            append(format.roleFlags)
            append('|')
            append(format.selectionFlags)
            append('|')
            append(format.bitrate)
            append('|')
            append(format.channelCount)
            append('|')
            append(format.sampleRate)
            append('|')
            append(format.width)
            append('x')
            append(format.height)
        }
        val digest = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray(Charsets.UTF_8))
        return buildString(digest.size * 2 + 3) {
            append("ts:")
            digest.forEach { byte ->
                append(((byte.toInt() ushr 4) and 0xF).toString(16))
                append((byte.toInt() and 0xF).toString(16))
            }
        }
    }
}
