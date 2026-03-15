package com.asuka.player.engine

import androidx.media3.common.C
import androidx.media3.common.Player
import com.asuka.player.contract.LoopMode
import com.asuka.player.contract.VideoScaleMode
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class Media3PlaybackControllerTest {

    // -- seekByTarget --

    @Test
    fun seekByTarget_forwardWithinDuration() {
        assertEquals(
            15_000L,
            Media3PlaybackController.seekByTarget(10_000L, 5_000L, 100_000L),
        )
    }

    @Test
    fun seekByTarget_backwardWithinDuration() {
        assertEquals(
            5_000L,
            Media3PlaybackController.seekByTarget(10_000L, -5_000L, 100_000L),
        )
    }

    @Test
    fun seekByTarget_clampsToZeroWhenNegative() {
        assertEquals(
            0L,
            Media3PlaybackController.seekByTarget(3_000L, -10_000L, 100_000L),
        )
    }

    @Test
    fun seekByTarget_clampsToDurationWhenOvershoot() {
        assertEquals(
            100_000L,
            Media3PlaybackController.seekByTarget(95_000L, 10_000L, 100_000L),
        )
    }

    @Test
    fun seekByTarget_unknownDuration_clampsToZeroOnly() {
        assertEquals(
            0L,
            Media3PlaybackController.seekByTarget(3_000L, -10_000L, C.TIME_UNSET),
        )
    }

    @Test
    fun seekByTarget_unknownDuration_doesNotClampUpper() {
        assertEquals(
            1_000_000L,
            Media3PlaybackController.seekByTarget(500_000L, 500_000L, C.TIME_UNSET),
        )
    }

    @Test
    fun seekByTarget_zeroDuration_treatedAsUnknown() {
        assertEquals(
            10_000L,
            Media3PlaybackController.seekByTarget(5_000L, 5_000L, 0L),
        )
    }

    @Test
    fun seekByTarget_negativeDuration_treatedAsUnknown() {
        assertEquals(
            0L,
            Media3PlaybackController.seekByTarget(5_000L, -10_000L, -1L),
        )
    }

    // -- subtitleMimeTypeForExtension --

    @Test
    fun subtitleMimeType_srt() {
        assertEquals("application/x-subrip", Media3PlaybackController.subtitleMimeTypeForExtension("srt"))
    }

    @Test
    fun subtitleMimeType_vtt() {
        assertEquals("text/vtt", Media3PlaybackController.subtitleMimeTypeForExtension("vtt"))
    }

    @Test
    fun subtitleMimeType_webvtt() {
        assertEquals("text/vtt", Media3PlaybackController.subtitleMimeTypeForExtension("webvtt"))
    }

    @Test
    fun subtitleMimeType_ass() {
        assertEquals("text/x-ssa", Media3PlaybackController.subtitleMimeTypeForExtension("ass"))
    }

    @Test
    fun subtitleMimeType_ssa() {
        assertEquals("text/x-ssa", Media3PlaybackController.subtitleMimeTypeForExtension("ssa"))
    }

    @Test
    fun subtitleMimeType_ttml() {
        assertEquals("application/ttml+xml", Media3PlaybackController.subtitleMimeTypeForExtension("ttml"))
    }

    @Test
    fun subtitleMimeType_dfxp() {
        assertEquals("application/ttml+xml", Media3PlaybackController.subtitleMimeTypeForExtension("dfxp"))
    }

    @Test
    fun subtitleMimeType_unknownDefaultsToSubrip() {
        assertEquals("application/x-subrip", Media3PlaybackController.subtitleMimeTypeForExtension("xyz"))
    }

    @Test
    fun subtitleMimeType_nullDefaultsToSubrip() {
        assertEquals("application/x-subrip", Media3PlaybackController.subtitleMimeTypeForExtension(null))
    }

    @Test
    fun subtitleMimeType_caseInsensitive() {
        assertEquals("text/vtt", Media3PlaybackController.subtitleMimeTypeForExtension("VTT"))
        assertEquals("text/x-ssa", Media3PlaybackController.subtitleMimeTypeForExtension("ASS"))
    }

    // -- loopModeToRepeatMode --

    @Test
    fun loopModeToRepeatMode_off() {
        assertEquals(Player.REPEAT_MODE_OFF, Media3PlaybackController.loopModeToRepeatMode(LoopMode.OFF))
    }

    @Test
    fun loopModeToRepeatMode_one() {
        assertEquals(Player.REPEAT_MODE_ONE, Media3PlaybackController.loopModeToRepeatMode(LoopMode.ONE))
    }

    @Test
    fun loopModeToRepeatMode_all() {
        assertEquals(Player.REPEAT_MODE_ALL, Media3PlaybackController.loopModeToRepeatMode(LoopMode.ALL))
    }

    // -- repeatModeToLoopMode --

    @Test
    fun repeatModeToLoopMode_off() {
        assertEquals(LoopMode.OFF, Media3PlaybackController.repeatModeToLoopMode(Player.REPEAT_MODE_OFF))
    }

    @Test
    fun repeatModeToLoopMode_one() {
        assertEquals(LoopMode.ONE, Media3PlaybackController.repeatModeToLoopMode(Player.REPEAT_MODE_ONE))
    }

    @Test
    fun repeatModeToLoopMode_all() {
        assertEquals(LoopMode.ALL, Media3PlaybackController.repeatModeToLoopMode(Player.REPEAT_MODE_ALL))
    }

    @Test
    fun repeatModeToLoopMode_unknownValueFallsToAll() {
        assertEquals(LoopMode.ALL, Media3PlaybackController.repeatModeToLoopMode(99))
    }

    // -- loopMode round-trip --

    @Test
    fun loopMode_roundTripPreservesAllValues() {
        for (mode in LoopMode.entries) {
            assertEquals(
                mode,
                Media3PlaybackController.repeatModeToLoopMode(
                    Media3PlaybackController.loopModeToRepeatMode(mode),
                ),
            )
        }
    }

    // -- videoScaleModeToScaleType --

    @Test
    fun videoScaleMode_fitMapsToScaleToFit() {
        assertEquals(
            C.VIDEO_SCALING_MODE_SCALE_TO_FIT,
            Media3PlaybackController.videoScaleModeToScaleType(VideoScaleMode.FIT),
        )
    }

    @Test
    fun videoScaleMode_fillMapsToScaleToFitWithCropping() {
        assertEquals(
            C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING,
            Media3PlaybackController.videoScaleModeToScaleType(VideoScaleMode.FILL),
        )
    }

    @Test
    fun videoScaleMode_cropMapsToScaleToFitWithCropping() {
        assertEquals(
            C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING,
            Media3PlaybackController.videoScaleModeToScaleType(VideoScaleMode.CROP),
        )
    }

    @Test
    fun videoScaleMode_stretchMapsToScaleToFitWithCropping() {
        assertEquals(
            C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING,
            Media3PlaybackController.videoScaleModeToScaleType(VideoScaleMode.STRETCH),
        )
    }
}
