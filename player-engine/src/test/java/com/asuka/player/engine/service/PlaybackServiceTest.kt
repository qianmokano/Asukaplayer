package com.asuka.player.engine.service

import android.app.Application
import android.app.NotificationManager
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.asuka.player.contract.PlaybackStore
import com.asuka.player.contract.QueueHistoryStore
import com.asuka.player.platform.PlaybackActivityDependencies
import com.asuka.player.platform.PlaybackDependenciesProvider
import com.asuka.player.platform.PlaybackServiceDependencies
import com.asuka.player.platform.PlaybackStateWriter
import com.asuka.player.platform.QueueHistoryWriter
import com.asuka.player.engine.R
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.delay
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(sdk = [34], application = PlaybackServiceTestApplication::class)
@RunWith(RobolectricTestRunner::class)
class PlaybackServiceTest {

    @Test
    fun onCreate_initializesSessionPlayerAndNotificationChannel() {
        val playbackStore = RecordingPlaybackStore()
        val queueHistoryStore = RecordingQueueHistoryStore()
        PlaybackServiceTestApplication.configure(
            playbackStore = playbackStore,
            queueHistoryStore = queueHistoryStore,
        )

        val serviceController = Robolectric.buildService(PlaybackService::class.java).create()
        val service = serviceController.get()

        val notificationManager = service.getSystemService(NotificationManager::class.java)
        assertNotNull(notificationManager.getNotificationChannel("asuka_playback"))
        assertNotNull(service.readPrivateField<Any>("player"))
        assertNotNull(service.readPrivateField<Any>("session"))
        assertNotNull(service.readPrivateField<PlaybackStateWriter>("writer"))
        assertNotNull(service.readPrivateField<QueueHistoryWriter>("historyWriter"))

        serviceController.destroy()
    }

    @Test
    fun onDestroy_closesFieldsAndDrainsNonBlocking() {
        val playbackStore = RecordingPlaybackStore()
        val queueHistoryStore = RecordingQueueHistoryStore()
        PlaybackServiceTestApplication.configure(
            playbackStore = playbackStore,
            queueHistoryStore = queueHistoryStore,
        )

        val serviceController = Robolectric.buildService(PlaybackService::class.java).create()
        val service = serviceController.get()
        val writer = service.readPrivateField<PlaybackStateWriter>("writer") ?: error("writer missing")
        val historyWriter = service.readPrivateField<QueueHistoryWriter>("historyWriter") ?: error("historyWriter missing")
        val mediaItem = MediaItem.Builder()
            .setMediaId("media-store:42")
            .setUri(Uri.parse("content://videos/current.mp4"))
            .build()

        writer.onMediaItemTransition(mediaItem, Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED)
        historyWriter.onMediaItemTransition(mediaItem, Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED)

        serviceController.destroy()

        assertNull(service.readPrivateField<Any>("player"))
        assertNull(service.readPrivateField<Any>("session"))
        assertNull(service.readPrivateField<Any>("writer"))
        assertNull(service.readPrivateField<Any>("historyWriter"))

        waitForCondition {
            playbackStore.savedPositions.any { it.first == "media-store:42" } &&
                queueHistoryStore.pushedMediaIds.contains("media-store:42")
        }
    }
}

class PlaybackServiceTestApplication : Application(), PlaybackDependenciesProvider {
    override val playbackActivityDependencies: PlaybackActivityDependencies
        get() = error("PlaybackActivityDependencies should not be used in PlaybackService tests")

    override val playbackServiceDependencies: PlaybackServiceDependencies
        get() = configuredServiceDependencies

    companion object {
        private lateinit var configuredServiceDependencies: PlaybackServiceDependencies

        fun configure(
            playbackStore: PlaybackStore,
            queueHistoryStore: QueueHistoryStore,
        ) {
            configuredServiceDependencies = object : PlaybackServiceDependencies {
                override val playbackStore: PlaybackStore = playbackStore
                override val queueHistoryStore: QueueHistoryStore = queueHistoryStore
                override val sessionActivityClass: Class<*>? = null
                override val notificationSmallIconResId: Int = R.drawable.ic_stat_playback
            }
        }
    }
}

private class RecordingPlaybackStore(
    private val delayMs: Long = 0L,
) : PlaybackStore {
    val savedPositions = CopyOnWriteArrayList<Pair<String, Long>>()

    override suspend fun recentMediaIds(limit: Int): List<String> = emptyList()

    override suspend fun loadPosition(mediaId: String): Long? = null

    override suspend fun savePosition(mediaId: String, positionMs: Long) {
        if (delayMs > 0L) delay(delayMs)
        savedPositions += mediaId to positionMs
    }

    override suspend fun loadPlaybackSpeed(mediaId: String): Float? = null

    override suspend fun savePlaybackSpeed(mediaId: String, speed: Float) = Unit

    override suspend fun loadAudioTrackId(mediaId: String): String? = null

    override suspend fun saveAudioTrackId(mediaId: String, trackId: String) = Unit

    override suspend fun loadSubtitleTrackId(mediaId: String): String? = null

    override suspend fun saveSubtitleTrackId(mediaId: String, trackId: String) = Unit

    override suspend fun loadZoom(mediaId: String): Float? = null

    override suspend fun saveZoom(mediaId: String, zoom: Float) = Unit
}

private class RecordingQueueHistoryStore(
    private val delayMs: Long = 0L,
) : QueueHistoryStore {
    val pushedMediaIds = CopyOnWriteArrayList<String>()

    override suspend fun push(mediaId: String) {
        if (delayMs > 0L) delay(delayMs)
        pushedMediaIds += mediaId
    }

    override suspend fun items(): List<String> = pushedMediaIds.toList()
}

private inline fun <reified T> PlaybackService.readPrivateField(name: String): T? {
    val field = PlaybackService::class.java.getDeclaredField(name)
    field.isAccessible = true
    @Suppress("UNCHECKED_CAST")
    return field.get(this) as T?
}

private fun waitForCondition(
    timeoutMs: Long = 3_000L,
    predicate: () -> Boolean,
) {
    val deadline = System.currentTimeMillis() + timeoutMs
    while (System.currentTimeMillis() < deadline) {
        if (predicate()) return
        Thread.sleep(20L)
    }
    error("Condition not met within ${timeoutMs}ms")
}
