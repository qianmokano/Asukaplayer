package com.asuka.player.core

import androidx.media3.common.Format
import java.security.MessageDigest

data class PersistedTrackSelection(
    val stableId: String,
) {
    val isDisabledSubtitle: Boolean
        get() = stableId == DISABLED_SUBTITLE_ID

    companion object {
        const val DISABLED_SUBTITLE_ID = "track-selection:v1:subtitle-disabled"

        fun disabledSubtitle(): PersistedTrackSelection = PersistedTrackSelection(DISABLED_SUBTITLE_ID)
    }
}

object TrackSelectionIdentity {
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
