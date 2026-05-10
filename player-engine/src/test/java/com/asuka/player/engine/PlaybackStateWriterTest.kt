package com.asuka.player.engine

import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.TrackGroup
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.Tracks
import com.asuka.player.platform.PlaybackStateWriter
import com.asuka.player.platform.TrackSelectionIdentity
import com.asuka.player.data.InMemoryPlaybackStore
import java.lang.reflect.Proxy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PlaybackStateWriterTest {

    @Test
    fun shouldSavePositionOnPause_skipsBufferingIdleEnded() {
        assertFalse(PlaybackStateWriter.shouldSavePositionOnPause(isPlaying = false, playbackState = Player.STATE_BUFFERING))
        assertFalse(PlaybackStateWriter.shouldSavePositionOnPause(isPlaying = false, playbackState = Player.STATE_IDLE))
        assertFalse(PlaybackStateWriter.shouldSavePositionOnPause(isPlaying = false, playbackState = Player.STATE_ENDED))
    }

    @Test
    fun shouldSavePositionOnPause_savesWhenReadyAndNotPlaying() {
        assertTrue(PlaybackStateWriter.shouldSavePositionOnPause(isPlaying = false, playbackState = Player.STATE_READY))
    }

    @Test
    fun shouldSavePositionOnPause_neverSavesWhenPlaying() {
        assertFalse(PlaybackStateWriter.shouldSavePositionOnPause(isPlaying = true, playbackState = Player.STATE_READY))
    }

    @Test
    fun onPositionDiscontinuity_savesNewPositionForSeek() = runBlocking {
        val store = InMemoryPlaybackStore()
        val mediaItem = mediaItem("media://seek")
        val playerState = fakePlayer(currentMediaItem = mediaItem)
        val writer = PlaybackStateWriter(store)

        writer.attach(playerState.asPlayer())
        writer.onPositionDiscontinuity(
            oldPosition = positionInfo(mediaItem = mediaItem, positionMs = 1_000L),
            newPosition = positionInfo(mediaItem = mediaItem, positionMs = 25_000L),
            reason = Player.DISCONTINUITY_REASON_SEEK,
        )
        writer.awaitIdle()

        assertEquals(25_000L, store.loadPosition(mediaItem.mediaId))
    }

    @Test
    fun onPositionDiscontinuity_whenWithinTenSecondsOfEnd_resetsResumePosition() = runBlocking {
        val store = InMemoryPlaybackStore()
        val mediaItem = mediaItem("media://seek-near-end")
        val playerState = fakePlayer(
            currentMediaItem = mediaItem,
            duration = 120_000L,
        )
        val writer = PlaybackStateWriter(store)

        writer.attach(playerState.asPlayer())
        writer.onPositionDiscontinuity(
            oldPosition = positionInfo(mediaItem = mediaItem, positionMs = 1_000L),
            newPosition = positionInfo(mediaItem = mediaItem, positionMs = 110_001L),
            reason = Player.DISCONTINUITY_REASON_SEEK,
        )
        writer.awaitIdle()

        assertEquals(0L, store.loadPosition(mediaItem.mediaId))
    }

    @Test
    fun onPlaybackParametersChanged_afterMediaItemTransition_targetsNewItem() = runBlocking {
        val store = InMemoryPlaybackStore()
        val first = mediaItem("media://first")
        val second = mediaItem("media://second")
        val playerState = fakePlayer(currentMediaItem = first)
        val writer = PlaybackStateWriter(store)

        writer.attach(playerState.asPlayer())
        writer.onMediaItemTransition(second, Player.MEDIA_ITEM_TRANSITION_REASON_SEEK)
        writer.onPlaybackParametersChanged(PlaybackParameters(1.5f))
        writer.awaitIdle()

        assertNull(store.loadPlaybackSpeed(first.mediaId))
        assertEquals(1.5f, store.loadPlaybackSpeed(second.mediaId))
    }

    @Test
    fun onPlaybackStateChanged_whenEnded_resetsCurrentItemPosition() = runBlocking {
        val store = InMemoryPlaybackStore()
        val mediaItem = mediaItem("media://ended")
        val playerState = fakePlayer(currentMediaItem = mediaItem)
        val writer = PlaybackStateWriter(store)

        store.savePosition(mediaItem.mediaId, 99_000L)
        writer.attach(playerState.asPlayer())
        writer.onPlaybackStateChanged(Player.STATE_ENDED)
        writer.awaitIdle()

        assertEquals(0L, store.loadPosition(mediaItem.mediaId))
    }

    @Test
    fun checkpoint_savesPlayingPositionAfterInterval() = runBlocking {
        val store = InMemoryPlaybackStore()
        val mediaItem = mediaItem("media://checkpoint")
        val playerState = fakePlayer(
            currentMediaItem = mediaItem,
            currentPosition = 15_000L,
            playbackState = Player.STATE_READY,
            isPlaying = true,
        )
        val writer = PlaybackStateWriter(store)

        writer.attach(playerState.asPlayer())

        assertTrue(writer.checkpoint(nowMs = 10_000L, minIntervalMs = 5_000L))
        writer.awaitIdle()
        assertEquals(15_000L, store.loadPosition(mediaItem.mediaId))

        playerState.currentPosition = 17_500L
        assertFalse(writer.checkpoint(nowMs = 12_000L, minIntervalMs = 5_000L))
        assertEquals(15_000L, store.loadPosition(mediaItem.mediaId))

        assertTrue(writer.checkpoint(nowMs = 16_000L, minIntervalMs = 5_000L))
        writer.awaitIdle()
        assertEquals(17_500L, store.loadPosition(mediaItem.mediaId))
    }

    @Test
    fun flushCurrentPosition_savesLatestKnownPosition() = runBlocking {
        val store = InMemoryPlaybackStore()
        val mediaItem = mediaItem("media://flush")
        val playerState = fakePlayer(
            currentMediaItem = mediaItem,
            currentPosition = 42_000L,
            playbackState = Player.STATE_READY,
            isPlaying = true,
        )
        val writer = PlaybackStateWriter(store)

        writer.attach(playerState.asPlayer())

        assertTrue(writer.flushCurrentPositionAndAwait())
        assertEquals(42_000L, store.loadPosition(mediaItem.mediaId))
    }

    @Test
    fun flushCurrentPosition_whenEnded_preservesResetToZero() = runBlocking {
        val store = InMemoryPlaybackStore()
        val mediaItem = mediaItem("media://ended-flush")
        val playerState = fakePlayer(
            currentMediaItem = mediaItem,
            currentPosition = 99_000L,
            playbackState = Player.STATE_ENDED,
        )
        val writer = PlaybackStateWriter(store)

        store.savePosition(mediaItem.mediaId, 15_000L)
        writer.attach(playerState.asPlayer())

        assertTrue(writer.flushCurrentPositionAndAwait())
        assertEquals(0L, store.loadPosition(mediaItem.mediaId))
    }

    @Test
    fun flushCurrentPosition_whenExactlyTenSecondsRemain_keepsResumePosition() = runBlocking {
        val store = InMemoryPlaybackStore()
        val mediaItem = mediaItem("media://threshold-flush")
        val playerState = fakePlayer(
            currentMediaItem = mediaItem,
            currentPosition = 110_000L,
            duration = 120_000L,
            playbackState = Player.STATE_READY,
        )
        val writer = PlaybackStateWriter(store)

        writer.attach(playerState.asPlayer())

        assertTrue(writer.flushCurrentPositionAndAwait())
        assertEquals(110_000L, store.loadPosition(mediaItem.mediaId))
    }

    @Test
    fun onTracksChanged_savesSelectionsForActualCurrentItem() = runBlocking {
        val store = InMemoryPlaybackStore()
        val first = mediaItem("media://first")
        val second = mediaItem("media://second")
        val audioFormat = Format.Builder()
            .setId("audio-main")
            .setLabel("Main Audio")
            .setLanguage("ja")
            .setSampleMimeType(MimeTypes.AUDIO_AAC)
            .setChannelCount(2)
            .setSampleRate(48_000)
            .build()
        val subtitleFormat = Format.Builder()
            .setId("subtitle-main")
            .setLabel("English")
            .setLanguage("en")
            .setSampleMimeType(MimeTypes.TEXT_VTT)
            .build()
        val audioGroup = TrackGroup("audio", audioFormat)
        val subtitleGroup = TrackGroup("subtitle", subtitleFormat)
        val tracks = Tracks(
            listOf(
                Tracks.Group(
                    audioGroup,
                    false,
                    intArrayOf(C.FORMAT_HANDLED),
                    booleanArrayOf(true),
                ),
                Tracks.Group(
                    subtitleGroup,
                    false,
                    intArrayOf(C.FORMAT_HANDLED),
                    booleanArrayOf(true),
                ),
            ),
        )
        val playerState = fakePlayer(
            currentMediaItem = first,
            currentTracks = tracks,
            trackSelectionParameters = TrackSelectionParameters.DEFAULT
                .buildUpon()
                .addOverride(TrackSelectionOverride(audioGroup, 0))
                .addOverride(TrackSelectionOverride(subtitleGroup, 0))
                .build(),
        )
        val writer = PlaybackStateWriter(store)

        writer.attach(playerState.asPlayer())
        playerState.currentMediaItem = second
        playerState.currentTracks = tracks
        writer.onTracksChanged(tracks)
        writer.awaitIdle()

        assertNull(store.loadAudioTrackId(first.mediaId))
        assertEquals(
            TrackSelectionIdentity.create(C.TRACK_TYPE_AUDIO, audioFormat),
            store.loadAudioTrackId(second.mediaId),
        )
        assertEquals(
            TrackSelectionIdentity.create(C.TRACK_TYPE_TEXT, subtitleFormat),
            store.loadSubtitleTrackId(second.mediaId),
        )
    }

    @Test
    fun onPositionDiscontinuity_enqueuesSlowStoreWriteWithoutRunningItInline() = runTest {
        val mediaItem = mediaItem("media://slow")
        val playerState = fakePlayer(currentMediaItem = mediaItem)
        val dispatcher = StandardTestDispatcher(testScheduler)
        val store = SlowPlaybackStore()
        val writer = PlaybackStateWriter(store, writeDispatcher = dispatcher)

        writer.attach(playerState.asPlayer())
        writer.onPositionDiscontinuity(
            oldPosition = positionInfo(mediaItem = mediaItem, positionMs = 0L),
            newPosition = positionInfo(mediaItem = mediaItem, positionMs = 10_000L),
            reason = Player.DISCONTINUITY_REASON_SEEK,
        )

        assertEquals(0, store.savePositionCalls)
        dispatcher.scheduler.runCurrent()
        assertEquals(1, store.savePositionCalls)
        writer.awaitIdle()
    }

    private class FakePlayerState(
        var currentMediaItem: MediaItem? = null,
        var currentPosition: Long = 0L,
        var duration: Long = C.TIME_UNSET,
        var playbackState: Int = Player.STATE_IDLE,
        var currentTracks: Tracks = Tracks.EMPTY,
        var trackSelectionParameters: TrackSelectionParameters = TrackSelectionParameters.DEFAULT,
        var isPlaying: Boolean = false,
    ) {
        fun asPlayer(): Player {
            return Proxy.newProxyInstance(
                Player::class.java.classLoader,
                arrayOf(Player::class.java),
            ) { _, method, args ->
                when (method.name) {
                    "addListener", "removeListener" -> null
                    "getCurrentMediaItem" -> currentMediaItem
                    "getCurrentPosition" -> currentPosition
                    "getDuration" -> duration
                    "getPlaybackState" -> playbackState
                    "getCurrentTracks" -> currentTracks
                    "getTrackSelectionParameters" -> trackSelectionParameters
                    "setTrackSelectionParameters" -> {
                        trackSelectionParameters = args?.firstOrNull() as? TrackSelectionParameters
                            ?: TrackSelectionParameters.DEFAULT
                        null
                    }
                    "isPlaying" -> isPlaying
                    else -> defaultValue(method.returnType)
                }
            } as Player
        }
    }

    private fun fakePlayer(
        currentMediaItem: MediaItem? = null,
        currentPosition: Long = 0L,
        duration: Long = C.TIME_UNSET,
        playbackState: Int = Player.STATE_IDLE,
        currentTracks: Tracks = Tracks.EMPTY,
        trackSelectionParameters: TrackSelectionParameters = TrackSelectionParameters.DEFAULT,
        isPlaying: Boolean = false,
    ): FakePlayerState {
        return FakePlayerState(
            currentMediaItem = currentMediaItem,
            currentPosition = currentPosition,
            duration = duration,
            playbackState = playbackState,
            currentTracks = currentTracks,
            trackSelectionParameters = trackSelectionParameters,
            isPlaying = isPlaying,
        )
    }

    private fun mediaItem(mediaId: String): MediaItem {
        return MediaItem.Builder()
            .setMediaId(mediaId)
            .build()
    }

    private fun positionInfo(mediaItem: MediaItem, positionMs: Long): Player.PositionInfo {
        return Player.PositionInfo(
            null,
            0,
            mediaItem,
            null,
            0,
            positionMs,
            positionMs,
            C.INDEX_UNSET,
            C.INDEX_UNSET,
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

private class SlowPlaybackStore : com.asuka.player.contract.PlaybackStore {
    var savePositionCalls = 0
        private set

    override suspend fun loadPosition(mediaId: String): Long? = null
    override suspend fun savePosition(mediaId: String, positionMs: Long) {
        savePositionCalls += 1
        delay(200)
    }
    override suspend fun loadPlaybackSpeed(mediaId: String): Float? = null
    override suspend fun savePlaybackSpeed(mediaId: String, speed: Float) {
        delay(200)
    }
    override suspend fun loadAudioTrackId(mediaId: String): String? = null
    override suspend fun saveAudioTrackId(mediaId: String, trackId: String) {
        delay(200)
    }
    override suspend fun loadSubtitleTrackId(mediaId: String): String? = null
    override suspend fun saveSubtitleTrackId(mediaId: String, trackId: String) {
        delay(200)
    }
    override suspend fun loadZoom(mediaId: String): Float? = null
    override suspend fun saveZoom(mediaId: String, zoom: Float) {
        delay(200)
    }
}
