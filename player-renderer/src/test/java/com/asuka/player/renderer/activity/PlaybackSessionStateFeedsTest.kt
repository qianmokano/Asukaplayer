package com.asuka.player.renderer.activity

import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.Tracks
import com.asuka.player.contract.LoopMode
import com.asuka.player.contract.PlaybackController
import com.asuka.player.contract.PlaybackTrackSelectionController
import com.asuka.player.contract.VideoScaleMode
import com.asuka.player.render.api.PlaybackSurfaceState
import java.lang.reflect.Proxy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(sdk = [34])
@RunWith(RobolectricTestRunner::class)
class PlaybackSessionStateFeedsTest {

    @Test
    fun bindResolvedSession_recreatesFeedsWhenControllerIdentityChanges() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val state = MutableStateFlow(PlaybackHostState())
            val feeds = PlaybackSessionStateFeeds(
                scope = scope,
                state = state,
            )

            val firstPlayerState = FakePlayerState(
                title = "First Title",
                mediaId = "first-media",
                speed = 1.0f,
            )
            val firstController = FakePlaybackController()
            val firstTrackController = FakeTrackSelectionController()
            val firstSurface = FakeSurfaceState("first")
            feeds.bindResolvedSession(
                player = firstPlayerState.asPlayer(),
                controllerIdentity = "first-controller",
                playbackController = firstController,
                trackSelectionController = firstTrackController,
                surfaceState = firstSurface,
            )
            yield()

            assertEquals("First Title", feeds.uiState.value.title)
            assertEquals("first-media", state.value.trackUiState.currentMediaId)
            assertEquals(1.0f, state.value.trackUiState.currentSpeed)
            assertSame(firstController, state.value.controller)
            assertSame(firstTrackController, state.value.trackSelectionController)
            assertSame(firstSurface, state.value.surfaceState)

            val secondPlayerState = FakePlayerState(
                title = "Second Title",
                mediaId = "second-media",
                speed = 1.5f,
            )
            val secondController = FakePlaybackController()
            val secondTrackController = FakeTrackSelectionController()
            val secondSurface = FakeSurfaceState("second")
            feeds.bindResolvedSession(
                player = secondPlayerState.asPlayer(),
                controllerIdentity = "second-controller",
                playbackController = secondController,
                trackSelectionController = secondTrackController,
                surfaceState = secondSurface,
            )
            yield()

            assertEquals("Second Title", feeds.uiState.value.title)
            assertEquals("second-media", state.value.trackUiState.currentMediaId)
            assertEquals(1.5f, state.value.trackUiState.currentSpeed)
            assertSame(secondController, state.value.controller)
            assertSame(secondTrackController, state.value.trackSelectionController)
            assertSame(secondSurface, state.value.surfaceState)

            firstPlayerState.setMedia(
                title = "Stale Title",
                mediaId = "stale-media",
            )
            firstPlayerState.dispatchMediaItemTransition()
            yield()

            assertEquals("Second Title", feeds.uiState.value.title)
            assertEquals("second-media", state.value.trackUiState.currentMediaId)

            secondPlayerState.setMedia(
                title = "Updated Second Title",
                mediaId = "updated-second-media",
            )
            secondPlayerState.dispatchMediaItemTransition()
            yield()

            assertEquals("Updated Second Title", feeds.uiState.value.title)
            assertEquals("updated-second-media", state.value.trackUiState.currentMediaId)
        } finally {
            scope.cancel()
        }
    }
}

private data class FakeSurfaceState(
    val id: String,
) : PlaybackSurfaceState

private class FakePlaybackController : PlaybackController {
    private val connectedState = MutableStateFlow(true)
    override val isConnected: StateFlow<Boolean> = connectedState.asStateFlow()

    override fun prepare() = Unit
    override fun play() = Unit
    override fun pause() = Unit
    override fun togglePlayPause() = Unit
    override fun seekTo(positionMs: Long) = Unit
    override fun seekBy(deltaMs: Long) = Unit
    override fun setPlaybackSpeed(speed: Float) = Unit
    override fun setSubtitleEnabled(enabled: Boolean, preferredGroupIndex: Int, preferredTrackIndex: Int) = Unit
    override fun addExternalSubtitle(uri: String, label: String?) = Unit
    override fun setVideoScaleMode(mode: VideoScaleMode) = Unit
    override fun setLoopMode(mode: LoopMode) = Unit
    override fun setShuffleEnabled(enabled: Boolean) = Unit
    override fun skipToNext() = Unit
    override fun skipToPrevious() = Unit
    override fun getRepeatMode(): LoopMode = LoopMode.OFF
    override fun isShuffleEnabled(): Boolean = false
}

private class FakeTrackSelectionController : PlaybackTrackSelectionController {
    override fun setAudioTrack(groupIndex: Int, trackIndex: Int) = Unit
    override fun setSubtitleTrack(groupIndex: Int, trackIndex: Int) = Unit
    override fun disableSubtitles() = Unit
}

private class FakePlayerState(
    title: String,
    mediaId: String,
    speed: Float,
) {
    private val listeners = linkedSetOf<Player.Listener>()
    private var currentMediaItem: MediaItem? = mediaItem(
        title = title,
        mediaId = mediaId,
    )
    private var playbackParameters: PlaybackParameters = PlaybackParameters(speed)

    private val player = Proxy.newProxyInstance(
        Player::class.java.classLoader,
        arrayOf(Player::class.java),
    ) { _, method, args ->
        when (method.name) {
            "addListener" -> {
                listeners += args?.firstOrNull() as Player.Listener
                null
            }

            "removeListener" -> {
                listeners -= args?.firstOrNull() as Player.Listener
                null
            }

            "getCurrentMediaItem" -> currentMediaItem
            "getMediaMetadata" -> currentMediaItem?.mediaMetadata ?: MediaMetadata.EMPTY
            "getCurrentPosition" -> 0L
            "getDuration" -> C.TIME_UNSET
            "getPlaybackState" -> Player.STATE_READY
            "getRepeatMode" -> Player.REPEAT_MODE_OFF
            "getShuffleModeEnabled" -> false
            "isPlaying" -> false
            "getPlaybackParameters" -> playbackParameters
            "getCurrentTracks" -> Tracks.EMPTY
            "getTrackSelectionParameters" -> TrackSelectionParameters.DEFAULT
            else -> defaultValue(method.returnType)
        }
    } as Player

    fun asPlayer(): Player = player

    fun setMedia(
        title: String,
        mediaId: String,
    ) {
        currentMediaItem = mediaItem(
            title = title,
            mediaId = mediaId,
        )
    }

    fun dispatchMediaItemTransition() {
        listeners.toList().forEach { listener ->
            listener.onMediaItemTransition(
                currentMediaItem,
                Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED,
            )
        }
    }

    private fun mediaItem(
        title: String,
        mediaId: String,
    ): MediaItem {
        return MediaItem.Builder()
            .setMediaId(mediaId)
            .setUri("content://videos/$mediaId.mp4")
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .build(),
            )
            .build()
    }
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
