package com.asuka.player.platform

import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.TrackGroup
import androidx.media3.common.Tracks
import java.lang.reflect.Proxy
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(sdk = [34])
@RunWith(RobolectricTestRunner::class)
class TrackSelectionStateReaderTest {

    @Test
    fun read_returnsCurrentlySelectedTracksWithoutOverrides() {
        val audioGroup = TrackGroup(
            "audio",
            Format.Builder()
                .setId("audio-main")
                .setSampleMimeType(MimeTypes.AUDIO_AAC)
                .build(),
            Format.Builder()
                .setId("audio-alt")
                .setSampleMimeType(MimeTypes.AUDIO_AAC)
                .build(),
        )
        val subtitleGroup = TrackGroup(
            "subtitle",
            Format.Builder()
                .setId("subtitle-off")
                .setSampleMimeType(MimeTypes.TEXT_VTT)
                .build(),
            Format.Builder()
                .setId("subtitle-en")
                .setSampleMimeType(MimeTypes.TEXT_VTT)
                .build(),
        )
        val tracks = Tracks(
            listOf(
                Tracks.Group(
                    audioGroup,
                    false,
                    intArrayOf(C.FORMAT_HANDLED, C.FORMAT_HANDLED),
                    booleanArrayOf(false, true),
                ),
                Tracks.Group(
                    subtitleGroup,
                    false,
                    intArrayOf(C.FORMAT_HANDLED, C.FORMAT_HANDLED),
                    booleanArrayOf(false, true),
                ),
            ),
        )
        val player = Proxy.newProxyInstance(
            Player::class.java.classLoader,
            arrayOf(Player::class.java),
        ) { _, method, _ ->
            when (method.name) {
                "getCurrentTracks" -> tracks
                else -> defaultValue(method.returnType)
            }
        } as Player

        val selected = TrackSelectionStateReader(player).read()

        assertEquals(
            listOf(
                TrackSelectionStateReader.Selected(
                    type = C.TRACK_TYPE_AUDIO,
                    groupIndex = 0,
                    trackIndex = 1,
                ),
                TrackSelectionStateReader.Selected(
                    type = C.TRACK_TYPE_TEXT,
                    groupIndex = 1,
                    trackIndex = 1,
                ),
            ),
            selected,
        )
    }

    private companion object {
        fun defaultValue(returnType: Class<*>): Any? {
            return when {
                returnType == java.lang.Boolean.TYPE -> false
                returnType == java.lang.Integer.TYPE -> 0
                returnType == java.lang.Long.TYPE -> 0L
                returnType == java.lang.Float.TYPE -> 0f
                returnType == java.lang.Double.TYPE -> 0.0
                returnType == java.lang.Short.TYPE -> 0.toShort()
                returnType == java.lang.Byte.TYPE -> 0.toByte()
                returnType == java.lang.Character.TYPE -> 0.toChar()
                else -> null
            }
        }
    }
}
