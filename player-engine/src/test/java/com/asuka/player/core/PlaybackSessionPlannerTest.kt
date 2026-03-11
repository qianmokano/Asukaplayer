package com.asuka.player.core

import android.net.Uri
import com.asuka.player.contract.PersistedTrackSelection
import com.asuka.player.contract.PlaybackQueueEntry
import com.asuka.player.contract.PlaybackSessionPlanner
import com.asuka.player.contract.PlaybackStartupPolicy
import com.asuka.player.contract.PlaybackStateRepository
import com.asuka.player.contract.TrackSelectionRestoreRequest
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
            targetUri = target.toString(),
            launchNeighbors = listOf(extra.toString()),
            resolvedTitles = mapOf(target.toString() to "Target Title"),
            policy = PlaybackStartupPolicy(
                resumePlayback = true,
                defaultPlaybackSpeed = 1.0f,
                rememberTrackSelections = false,
            ),
        )

        assertEquals(listOf(target.toString(), extra.toString()), plan.queue.items.map { it.uri })
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
            targetUri = target.toString(),
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
            targetUri = target.toString(),
            launchNeighbors = emptyList(),
            policy = PlaybackStartupPolicy(
                resumePlayback = false,
                defaultPlaybackSpeed = 1.0f,
                rememberTrackSelections = false,
            ),
        )

        assertEquals(listOf(target.toString()), plan.queue.items.map { it.uri })
        assertEquals(0, plan.queue.startIndex)
    }

    @Test
    fun plan_usesResolvedTitlesForExplicitQueue() {
        val planner = PlaybackSessionPlanner(
            playbackStateRepository = PlaybackStateRepository(InMemoryPlaybackStore()),
        )

        val plan = planner.plan(
            targetUri = target.toString(),
            launchNeighbors = listOf(extra.toString()),
            resolvedTitles = mapOf(
                target.toString() to "Target Title",
                extra.toString() to "Extra Title",
            ),
            policy = PlaybackStartupPolicy(
                resumePlayback = false,
                defaultPlaybackSpeed = 1.0f,
                rememberTrackSelections = false,
            ),
        )

        assertEquals(listOf(target.toString(), extra.toString()), plan.queue.items.map { it.uri })
        assertEquals(
            listOf("Target Title", "Extra Title"),
            plan.queue.items.map { it.title },
        )
    }

    @Test
    fun plan_preservesExplicitQueueOrderWhenTargetIsNotFirstItem() {
        val planner = PlaybackSessionPlanner(
            playbackStateRepository = PlaybackStateRepository(InMemoryPlaybackStore()),
        )

        val plan = planner.plan(
            targetUri = target.toString(),
            launchNeighbors = listOf(previous.toString(), target.toString(), extra.toString()),
            resolvedTitles = mapOf(
                previous.toString() to "Previous Title",
                target.toString() to "Target Title",
                extra.toString() to "Extra Title",
            ),
            policy = PlaybackStartupPolicy(
                resumePlayback = false,
                defaultPlaybackSpeed = 1.0f,
                rememberTrackSelections = false,
            ),
        )

        assertEquals(listOf(previous.toString(), target.toString(), extra.toString()), plan.queue.items.map { it.uri })
        assertEquals(1, plan.queue.startIndex)
        assertEquals(
            listOf("Previous Title", "Target Title", "Extra Title"),
            plan.queue.items.map { it.title },
        )
    }

    @Test
    fun plan_usesStableMediaId_whenPlaybackUriIsFallbackCopy() {
        val original = Uri.parse("content://videos/original.mp4")
        val fallback = Uri.parse("file:///cache/fallback.mp4")
        val store = InMemoryPlaybackStore().apply {
            savePosition(original.toString(), 12_345L)
            savePlaybackSpeed(original.toString(), 1.25f)
            saveAudioTrackId(original.toString(), "audio-stable")
        }
        val planner = PlaybackSessionPlanner(
            playbackStateRepository = PlaybackStateRepository(store),
        )

        val plan = planner.plan(
            target = PlaybackQueueEntry(
                mediaId = original.toString(),
                uri = fallback.toString(),
            ),
            launchNeighbors = emptyList(),
            policy = PlaybackStartupPolicy(
                resumePlayback = true,
                defaultPlaybackSpeed = 1.0f,
                rememberTrackSelections = true,
            ),
        )

        assertEquals(12_345L, plan.resumePositionMs)
        assertEquals(1.25f, plan.playbackSpeed)
        assertEquals(original.toString(), plan.queue.items.single().mediaId)
        assertEquals(fallback.toString(), plan.queue.items.single().uri)
        assertEquals(
            TrackSelectionRestoreRequest(
                mediaId = original.toString(),
                audioTrackSelection = PersistedTrackSelection("audio-stable"),
                subtitleTrackSelection = null,
            ),
            plan.trackSelectionRestoreRequest,
        )
    }
}
