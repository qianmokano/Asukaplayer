package com.asuka.player.platform

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import java.lang.reflect.Proxy
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(sdk = [34])
@RunWith(RobolectricTestRunner::class)
class PlaybackArtworkBridgeTest {

    @Test
    fun attach_loadsArtworkForCurrentMediaItem() {
        val probe = PlayerProbe(
            currentMediaItem = mediaItem("media-1", "content://videos/1.mp4"),
            currentMediaItemIndex = 2,
        )
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)

        try {
            val bridge = bridge(scope) { "cover-1".encodeToByteArray() }

            bridge.attach(probe.player)

            assertEquals(1, probe.replacedItems.size)
            assertEquals(2, probe.replacedItems.single().first)
            assertContentEquals(
                "cover-1".encodeToByteArray(),
                probe.replacedItems.single().second.mediaMetadata.artworkData,
            )
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun failedArtworkLoad_canRetryForSameMediaId() {
        val probe = PlayerProbe(
            currentMediaItem = mediaItem("media-1", "content://videos/1.mp4"),
            currentMediaItemIndex = 0,
        )
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        var attempts = 0

        try {
            val bridge = bridge(scope) {
                attempts += 1
                if (attempts == 1) {
                    null
                } else {
                    "cover-1".encodeToByteArray()
                }
            }

            bridge.attach(probe.player)
            assertEquals(1, attempts)
            assertEquals(0, probe.replacedItems.size)

            probe.dispatchTransition(Player.MEDIA_ITEM_TRANSITION_REASON_SEEK)

            assertEquals(2, attempts)
            assertEquals(1, probe.replacedItems.size)
            assertContentEquals(
                "cover-1".encodeToByteArray(),
                probe.replacedItems.single().second.mediaMetadata.artworkData,
            )
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun mediaItemTransition_loadsArtworkForNewMediaId() {
        val probe = PlayerProbe(
            currentMediaItem = mediaItem("media-1", "content://videos/1.mp4"),
            currentMediaItemIndex = 0,
        )
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)

        try {
            val bridge = bridge(scope) { uri ->
                uri.lastPathSegment?.substringBefore('.')?.encodeToByteArray()
            }

            bridge.attach(probe.player)

            probe.currentMediaItem = mediaItem("media-2", "content://videos/2.mp4")
            probe.currentMediaItemIndex = 1
            probe.dispatchTransition(Player.MEDIA_ITEM_TRANSITION_REASON_SEEK)

            assertEquals(2, probe.replacedItems.size)
            assertEquals(0, probe.replacedItems[0].first)
            assertEquals(1, probe.replacedItems[1].first)
            assertContentEquals(
                "1".encodeToByteArray(),
                probe.replacedItems[0].second.mediaMetadata.artworkData,
            )
            assertContentEquals(
                "2".encodeToByteArray(),
                probe.replacedItems[1].second.mediaMetadata.artworkData,
            )
        } finally {
            scope.cancel()
        }
    }

    private fun bridge(
        scope: CoroutineScope,
        artworkLoader: suspend (Uri) -> ByteArray?,
    ): PlaybackArtworkBridge {
        return PlaybackArtworkBridge(
            scope = scope,
            backgroundDispatcher = Dispatchers.Unconfined,
            runOnUiThread = { it.run() },
            artworkLoader = artworkLoader,
        )
    }

    private fun mediaItem(mediaId: String, uri: String): MediaItem {
        return MediaItem.Builder()
            .setMediaId(mediaId)
            .setUri(uri)
            .build()
    }
}

private class PlayerProbe(
    var currentMediaItem: MediaItem?,
    var currentMediaItemIndex: Int,
) {
    private val listeners = CopyOnWriteArrayList<Player.Listener>()
    val replacedItems = mutableListOf<Pair<Int, MediaItem>>()

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

            "getCurrentMediaItem" -> currentMediaItem
            "getCurrentMediaItemIndex" -> currentMediaItemIndex
            "replaceMediaItem" -> {
                val actualArgs = args ?: error("replaceMediaItem args missing")
                val index = actualArgs[0] as Int
                val item = actualArgs[1] as MediaItem
                replacedItems += index to item
                if (index == currentMediaItemIndex) {
                    currentMediaItem = item
                }
                null
            }

            "hashCode" -> System.identityHashCode(this)
            "equals" -> args?.get(0) === this
            "toString" -> "PlaybackArtworkBridgeTest.PlayerProbe"
            else -> defaultValue(method.returnType)
        }
    } as Player

    fun dispatchTransition(reason: Int) {
        listeners.forEach { it.onMediaItemTransition(currentMediaItem, reason) }
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
