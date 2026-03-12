package com.asuka.player.renderer.activity

import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import java.lang.reflect.Proxy
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(sdk = [34])
@RunWith(RobolectricTestRunner::class)
class PlaybackPictureInPictureControllerTest {

    @Test
    fun resolveAspectRatio_returnsFallbackForTallVideo() {
        val ratio = PlaybackPictureInPictureController.resolveAspectRatio(100, 400)

        assertEquals(PictureInPictureAspectRatio(1, 2), ratio)
    }

    @Test
    fun resolveAspectRatio_returnsFallbackForWideVideo() {
        val ratio = PlaybackPictureInPictureController.resolveAspectRatio(400, 100)

        assertEquals(PictureInPictureAspectRatio(239, 100), ratio)
    }

    @Test
    fun resolveAspectRatio_returnsNullForInvalidSize() {
        val ratio = PlaybackPictureInPictureController.resolveAspectRatio(0, 200)

        assertNull(ratio)
    }

    @Test
    fun resolveAspectRatioPreservingPrevious_keepsStableRatioForInvalidSize() {
        val previous = PictureInPictureAspectRatio(16, 9)

        val ratio = PlaybackPictureInPictureController.resolveAspectRatioPreservingPrevious(
            previous = previous,
            width = 0,
            height = 0,
        )

        assertEquals(previous, ratio)
    }

    @Test
    fun switchingMedia_keepsPreviousShapeUntilNewVideoSizeArrives() {
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        val player = PlayerProbe(
            isPlaying = true,
            videoSize = VideoSize(1920, 1080),
        )
        val appliedRatios = mutableListOf<PictureInPictureAspectRatio?>()
        val controller = PlaybackPictureInPictureController(
            activity = activity,
            currentPlayerProvider = { player.player },
            currentControllerProvider = { null },
            setPictureInPictureParams = { params ->
                appliedRatios += params.aspectRatio.toAspectRatio()
            },
        )

        controller.onPictureInPictureModeChanged(
            PictureInPictureTransition(
                isInPictureInPicture = true,
                shouldRegisterReceiver = false,
                shouldAttachPlayStateListener = true,
            ),
        )
        controller.updatePictureInPictureParamsIfSupported()

        player.videoSize = VideoSize.UNKNOWN
        player.dispatchIsPlayingChanged(false)

        player.videoSize = VideoSize(1080, 1920)
        player.dispatchVideoSizeChanged()
        player.dispatchIsPlayingChanged(true)

        val landscapeRatio = PlaybackPictureInPictureController.resolveAspectRatio(1920, 1080)
            ?.toRational()
            .toAspectRatio()
        val portraitRatio = PlaybackPictureInPictureController.resolveAspectRatio(1080, 1920)
            ?.toRational()
            .toAspectRatio()
        assertEquals(
            listOf<PictureInPictureAspectRatio?>(
                landscapeRatio,
                landscapeRatio,
                portraitRatio,
                portraitRatio,
            ),
            appliedRatios,
        )
    }
}

private fun Rational?.toAspectRatio(): PictureInPictureAspectRatio? {
    return this?.let { PictureInPictureAspectRatio(it.numerator, it.denominator) }
}

private class PlayerProbe(
    var isPlaying: Boolean,
    var videoSize: VideoSize,
) {
    private val listeners = CopyOnWriteArrayList<Player.Listener>()

    val player: Player = Proxy.newProxyInstance(
        Player::class.java.classLoader,
        arrayOf(Player::class.java),
    ) { _, method, args ->
        when (method.name) {
            "addListener" -> {
                listeners += args?.get(0) as Player.Listener
                null
            }

            "removeListener" -> {
                listeners -= args?.get(0) as Player.Listener
                null
            }

            "isPlaying" -> isPlaying
            "getVideoSize" -> videoSize
            "hashCode" -> System.identityHashCode(this)
            "equals" -> args?.get(0) === this
            "toString" -> "PlaybackPictureInPictureControllerTest.PlayerProbe"
            else -> defaultValue(method.returnType)
        }
    } as Player

    fun dispatchIsPlayingChanged(value: Boolean) {
        isPlaying = value
        listeners.forEach { it.onIsPlayingChanged(value) }
    }

    fun dispatchVideoSizeChanged() {
        listeners.forEach { it.onVideoSizeChanged(videoSize) }
    }

    private fun defaultValue(returnType: Class<*>): Any? {
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
