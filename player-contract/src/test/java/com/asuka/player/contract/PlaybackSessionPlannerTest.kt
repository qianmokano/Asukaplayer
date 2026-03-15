package com.asuka.player.contract

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.runBlocking

class PlaybackSessionPlannerTest {

    private val target = "file:///video/target.mp4"
    private val extra = "file:///video/extra.mp4"
    private val previous = "file:///video/previous.mp4"

    private fun testStore() = MapPlaybackStore()

    @Test
    fun plan_buildsQueueAndRestoresPositionIndependentlyFromTrackSelections() = runBlocking {
        val store = testStore().apply {
            savePosition(target, 42_000L)
            savePlaybackSpeed(target, 1.75f)
            saveAudioTrackId(target, "audio-1")
            saveSubtitleTrackId(target, PersistedTrackSelection.DISABLED_SUBTITLE_ID)
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

        assertEquals(listOf(target, extra), plan.queue.items.map { it.uri })
        assertEquals(0, plan.queue.startIndex)
        assertEquals(42_000L, plan.resumePositionMs)
        assertEquals(1.75f, plan.playbackSpeed)
        assertNull(plan.trackSelectionRestoreRequest)
    }

    @Test
    fun plan_includesTrackRestoreOnlyWhenEnabled() = runBlocking {
        val store = testStore().apply {
            saveAudioTrackId(target, "audio-2")
            saveSubtitleTrackId(target, "subtitle-3")
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
                mediaId = target,
                audioTrackSelection = PersistedTrackSelection("audio-2"),
                subtitleTrackSelection = PersistedTrackSelection("subtitle-3"),
            ),
            plan.trackSelectionRestoreRequest,
        )
    }

    @Test
    fun plan_withoutExplicitNeighborsKeepsQueueScopedToCurrentItem() = runBlocking {
        val planner = PlaybackSessionPlanner(
            playbackStateRepository = PlaybackStateRepository(testStore()),
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

        assertEquals(listOf(target), plan.queue.items.map { it.uri })
        assertEquals(0, plan.queue.startIndex)
    }

    @Test
    fun plan_usesResolvedTitlesForExplicitQueue() = runBlocking {
        val planner = PlaybackSessionPlanner(
            playbackStateRepository = PlaybackStateRepository(testStore()),
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

        assertEquals(listOf(target, extra), plan.queue.items.map { it.uri })
        assertEquals(
            listOf("Target Title", "Extra Title"),
            plan.queue.items.map { it.title },
        )
    }

    @Test
    fun plan_preservesExplicitQueueOrderWhenTargetIsNotFirstItem() = runBlocking {
        val planner = PlaybackSessionPlanner(
            playbackStateRepository = PlaybackStateRepository(testStore()),
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

        assertEquals(listOf(previous, target, extra), plan.queue.items.map { it.uri })
        assertEquals(1, plan.queue.startIndex)
        assertEquals(
            listOf("Previous Title", "Target Title", "Extra Title"),
            plan.queue.items.map { it.title },
        )
    }

    @Test
    fun plan_usesStableMediaId_whenPlaybackUriIsFallbackCopy() = runBlocking {
        val original = "content://videos/original.mp4"
        val fallback = "file:///cache/fallback.mp4"
        val store = testStore().apply {
            savePosition(original, 12_345L)
            savePlaybackSpeed(original, 1.25f)
            saveAudioTrackId(original, "audio-stable")
        }
        val planner = PlaybackSessionPlanner(
            playbackStateRepository = PlaybackStateRepository(store),
        )

        val plan = planner.plan(
            target = PlaybackQueueEntry(
                mediaId = original,
                uri = fallback,
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
        assertEquals(original, plan.queue.items.single().mediaId)
        assertEquals(fallback, plan.queue.items.single().uri)
        assertEquals(
            TrackSelectionRestoreRequest(
                mediaId = original,
                audioTrackSelection = PersistedTrackSelection("audio-stable"),
                subtitleTrackSelection = null,
            ),
            plan.trackSelectionRestoreRequest,
        )
    }
}

private class MapPlaybackStore : PlaybackStore {
    private val positions = mutableMapOf<String, Long>()
    private val speeds = mutableMapOf<String, Float>()
    private val audioTrackIds = mutableMapOf<String, String>()
    private val subtitleTrackIds = mutableMapOf<String, String>()
    private val zooms = mutableMapOf<String, Float>()

    override suspend fun loadPosition(mediaId: String) = positions[mediaId]
    override suspend fun savePosition(mediaId: String, positionMs: Long) { positions[mediaId] = positionMs }
    override suspend fun loadPlaybackSpeed(mediaId: String) = speeds[mediaId]
    override suspend fun savePlaybackSpeed(mediaId: String, speed: Float) { speeds[mediaId] = speed }
    override suspend fun loadAudioTrackId(mediaId: String) = audioTrackIds[mediaId]
    override suspend fun saveAudioTrackId(mediaId: String, trackId: String) { audioTrackIds[mediaId] = trackId }
    override suspend fun loadSubtitleTrackId(mediaId: String) = subtitleTrackIds[mediaId]
    override suspend fun saveSubtitleTrackId(mediaId: String, trackId: String) { subtitleTrackIds[mediaId] = trackId }
    override suspend fun loadZoom(mediaId: String) = zooms[mediaId]
    override suspend fun saveZoom(mediaId: String, zoom: Float) { zooms[mediaId] = zoom }
}