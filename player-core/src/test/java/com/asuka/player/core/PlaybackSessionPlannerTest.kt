package com.asuka.player.core

import android.net.Uri
import com.asuka.player.data.InMemoryPlaybackStore
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@RunWith(RobolectricTestRunner::class)
class PlaybackSessionPlannerTest {

    private val target = Uri.parse("file:///video/target.mp4")
    private val extra = Uri.parse("file:///video/extra.mp4")
    private val history = Uri.parse("file:///video/history.mp4")

    @Test
    fun plan_buildsQueueAndRestoresPositionIndependentlyFromTrackSelections() {
        val store = InMemoryPlaybackStore().apply {
            savePosition(target.toString(), 42_000L)
            savePlaybackSpeed(target.toString(), 1.75f)
            saveAudioTrack(target.toString(), TrackIndexCodec.encode(1, 0))
            saveSubtitleTrack(target.toString(), TrackIndexCodec.SUBTITLE_DISABLED)
        }
        val historyStore = InMemoryQueueHistoryStore().apply { push(history) }
        val planner = PlaybackSessionPlanner(
            playbackStateRepository = PlaybackStateRepository(store),
            queueHistoryRepository = QueueHistoryRepository(historyStore),
        )

        val plan = planner.plan(
            targetUri = target,
            launchNeighbors = listOf(extra),
            resolvedTitles = mapOf(target to "Target Title"),
            policy = PlaybackStartupPolicy(
                resumePlayback = true,
                defaultPlaybackSpeed = 1.0f,
                rememberTrackSelections = false,
            ),
        )

        assertEquals(listOf(target, extra), plan.queue.items.map { it.localConfiguration?.uri })
        assertEquals(0, plan.queue.startIndex)
        assertEquals(42_000L, plan.resumePositionMs)
        assertEquals(1.75f, plan.playbackSpeed)
        assertNull(plan.trackSelectionRestoreRequest)
    }

    @Test
    fun plan_includesTrackRestoreOnlyWhenEnabled() {
        val store = InMemoryPlaybackStore().apply {
            saveAudioTrack(target.toString(), TrackIndexCodec.encode(2, 1))
            saveSubtitleTrack(target.toString(), TrackIndexCodec.encode(3, 0))
        }
        val planner = PlaybackSessionPlanner(
            playbackStateRepository = PlaybackStateRepository(store),
            queueHistoryRepository = QueueHistoryRepository(InMemoryQueueHistoryStore()),
        )

        val plan = planner.plan(
            targetUri = target,
            launchNeighbors = emptyList(),
            policy = PlaybackStartupPolicy(
                resumePlayback = false,
                defaultPlaybackSpeed = 1.0f,
                rememberTrackSelections = true,
            ),
        )

        assertEquals(0L, plan.resumePositionMs)
        assertEquals(1.0f, plan.playbackSpeed)
        assertEquals(
            TrackSelectionRestoreRequest(
                mediaId = target.toString(),
                audioTrackIndex = TrackIndexCodec.encode(2, 1),
                subtitleTrackIndex = TrackIndexCodec.encode(3, 0),
            ),
            plan.trackSelectionRestoreRequest,
        )
    }

    @Test
    fun plan_usesResolvedTitlesForExplicitQueue() {
        val historyStore = InMemoryQueueHistoryStore().apply { push(history) }
        val planner = PlaybackSessionPlanner(
            playbackStateRepository = PlaybackStateRepository(InMemoryPlaybackStore()),
            queueHistoryRepository = QueueHistoryRepository(historyStore),
        )

        val plan = planner.plan(
            targetUri = target,
            launchNeighbors = listOf(extra),
            resolvedTitles = mapOf(
                target to "Target Title",
                extra to "Extra Title",
            ),
            policy = PlaybackStartupPolicy(
                resumePlayback = false,
                defaultPlaybackSpeed = 1.0f,
                rememberTrackSelections = false,
            ),
        )

        assertEquals(listOf(target, extra), plan.queue.items.map { it.localConfiguration?.uri })
        assertEquals(
            listOf("Target Title", "Extra Title"),
            plan.queue.items.map { it.mediaMetadata.title?.toString() },
        )
    }
}
