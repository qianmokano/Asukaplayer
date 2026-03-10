package com.asuka.player.ui.controller

import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Proxy
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

class PlayerUiStateHolderTest {

    @Test
    fun progressTicker_runsOnlyWhilePlayerIsPlaying() {
        val probe = PlayerProbe()
        val holder = PlayerUiStateHolder(probe.player)
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        try {
            holder.attach()
            holder.startProgressTicker(scope, intervalMs = 20L)

            Thread.sleep(70L)
            val readsWhilePaused = probe.currentPositionReads.get()

            probe.setPlaybackState(Player.STATE_READY)
            probe.setPlaying(true)
            Thread.sleep(90L)
            val readsWhilePlaying = probe.currentPositionReads.get()

            probe.setPlaying(false)
            val readsAtPause = probe.currentPositionReads.get()
            Thread.sleep(90L)
            val readsAfterPause = probe.currentPositionReads.get()

            assertEquals(1, readsWhilePaused)
            assertTrue(readsWhilePlaying > readsWhilePaused)
            assertTrue(readsAfterPause - readsAtPause <= 1)
        } finally {
            holder.detach()
            scope.cancel()
        }
    }
}

private class PlayerProbe {
    private val listeners = CopyOnWriteArrayList<Player.Listener>()
    private var isPlaying: Boolean = false
    private var playbackState: Int = Player.STATE_IDLE
    val currentPositionReads = AtomicInteger(0)

    val player: Player = Proxy.newProxyInstance(
        Player::class.java.classLoader,
        arrayOf(Player::class.java),
        InvocationHandler { _, method, args ->
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
                "getPlaybackState" -> playbackState
                "getCurrentPosition" -> currentPositionReads.incrementAndGet().toLong() * 1000L
                "getDuration" -> 120_000L
                "getMediaMetadata" -> MediaMetadata.EMPTY
                "hashCode" -> System.identityHashCode(this)
                "equals" -> args?.get(0) === this
                "toString" -> "PlayerProbe"
                else -> defaultValue(method.returnType)
            }
        },
    ) as Player

    fun setPlaying(value: Boolean) {
        isPlaying = value
        listeners.forEach { it.onIsPlayingChanged(value) }
    }

    fun setPlaybackState(value: Int) {
        playbackState = value
        listeners.forEach { it.onPlaybackStateChanged(value) }
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
