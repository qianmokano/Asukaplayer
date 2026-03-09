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
    private val previous = Uri.parse("file:///video/previous.mp4")

    @Test
    fun plan_buildsQueueAndRestoresPositionIndependentlyFromTrackSelections() {
        val store = InMemoryPlaybackStore().apply {
            savePosition(target.toString(), 42_000L)
            savePlaybackSpeed(target.toString(), 1.75f)
            saveAudioTrackId(target.toString(), "audio-1")
            saveSubtitleTrackId(target.toString(), PersistedTrackSelection.DISABLED_SUBTITLE_ID)
        }
        val planner = PlaybackSessionPlanner(
            playbackStateRepository = PlaybackStateRepository(store),
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
            saveAudioTrackId(target.toString(), "audio-2")
            saveSubtitleTrackId(target.toString(), "subtitle-3")
        }
        val planner = PlaybackSessionPlanner(
            playbackStateRepository = PlaybackStateRepository(store),
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
                audioTrackSelection = PersistedTrackSelection("audio-2"),
                subtitleTrackSelection = PersistedTrackSelection("subtitle-3"),
            ),
            plan.trackSelectionRestoreRequest,
        )
    }

    @Test
    fun plan_withoutExplicitNeighborsKeepsQueueScopedToCurrentItem() {
        val planner = PlaybackSessionPlanner(
            playbackStateRepository = PlaybackStateRepository(InMemoryPlaybackStore()),
        )

        val plan = planner.plan(
            targetUri = target,
            launchNeighbors = emptyList(),
            policy = PlaybackStartupPolicy(
                resumePlayback = false,
                defaultPlaybackSpeed = 1.0f,
                rememberTrackSelections = false,
            ),
        )

        assertEquals(listOf(target), plan.queue.items.map { it.localConfiguration?.uri })
        assertEquals(0, plan.queue.startIndex)
    }

    @Test
    fun plan_usesResolvedTitlesForExplicitQueue() {
        val planner = PlaybackSessionPlanner(
            playbackStateRepository = PlaybackStateRepository(InMemoryPlaybackStore()),
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

    @Test
    fun plan_preservesExplicitQueueOrderWhenTargetIsNotFirstItem() {
        val planner = PlaybackSessionPlanner(
            playbackStateRepository = PlaybackStateRepository(InMemoryPlaybackStore()),
        )

        val plan = planner.plan(
            targetUri = target,
            launchNeighbors = listOf(previous, target, extra),
            resolvedTitles = mapOf(
                previous to "Previous Title",
                target to "Target Title",
                extra to "Extra Title",
            ),
            policy = PlaybackStartupPolicy(
                resumePlayback = false,
                defaultPlaybackSpeed = 1.0f,
                rememberTrackSelections = false,
            ),
        )

        assertEquals(listOf(previous, target, extra), plan.queue.items.map { it.localConfiguration?.uri })
        assertEquals(1, plan.queue.startIndex)
        assertEquals(
            listOf("Previous Title", "Target Title", "Extra Title"),
            plan.queue.items.map { it.mediaMetadata.title?.toString() },
        )
    }
}
