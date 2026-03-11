package com.asuka.player.app

import android.net.Uri
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.asuka.player.R
import com.asuka.player.contract.PlaybackQueueEntry

@Composable
internal fun RecentPageContent(
    modifier: Modifier = Modifier,
    recentMediaIds: List<String>,
    knownVideos: Map<String, LocalVideoItem>,
    onPlay: (PlaybackSelection) -> Unit,
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val queueEntries = remember(recentMediaIds, knownVideos) {
        recentMediaIds
            .distinct()
            .map { mediaId ->
                val known = knownVideos[mediaId]
                if (known != null) {
                    known.toPlaybackQueueEntry(mediaIdOverride = mediaId)
                } else {
                    PlaybackQueueEntry(
                        mediaId = mediaId,
                        uri = mediaId,
                    )
                }
            }
    }
    val visibleRecentMediaIds = rememberIncrementalItems(
        items = recentMediaIds,
        listState = listState,
        pageSize = 60,
        loadMoreThreshold = 20,
    )
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            bottom = 92.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
        ),
    ) {
        item { Spacer(modifier = Modifier.size(12.dp)) }

        if (recentMediaIds.isEmpty()) {
            item { EmptyBlock(text = stringResource(id = R.string.empty_recent_list)) }
        } else {
            item {
                SectionTitle(text = stringResource(id = R.string.recent_group_title, recentMediaIds.size))
            }
            itemsIndexed(
                items = visibleRecentMediaIds,
                key = { _, mediaId -> mediaId },
            ) { index, mediaId ->
                val descriptor = remember(mediaId, knownVideos[mediaId]) {
                    RecentPlaybackDescriptor.from(
                        mediaId = mediaId,
                        knownVideo = knownVideos[mediaId],
                        unavailableLabel = context.getString(R.string.recent_unknown_source),
                    )
                }
                val title by produceState(initialValue = descriptor.fallbackTitle, descriptor) {
                    val uri = descriptor.uri ?: return@produceState
                    if (!descriptor.shouldResolveDisplayName) return@produceState
                    val resolved = runCatching { resolveDisplayName(context, uri) }.getOrNull()
                    value = resolved?.ifBlank { descriptor.fallbackTitle } ?: descriptor.fallbackTitle
                }

                GroupedListRow(
                    index = index,
                    totalCount = visibleRecentMediaIds.size,
                    horizontalPadding = VIDEO_GROUP_HORIZONTAL_PADDING,
                ) {
                    if (descriptor.thumbnailUri != null || descriptor.thumbnailId != null) {
                        SettingsNavigationItem(
                            icon = Icons.Outlined.VideoLibrary,
                            thumbnailUri = descriptor.thumbnailUri,
                            thumbnailId = descriptor.thumbnailId,
                            durationLabel = descriptor.durationLabel,
                            title = title,
                            description = descriptor.description,
                            onClick = {
                                onPlay(
                                    PlaybackSelection(
                                        targetEntry = descriptor.targetEntry,
                                        queueEntries = queueEntries,
                                    ),
                                )
                            },
                        )
                    } else {
                        SettingsNavigationItem(
                            icon = Icons.Outlined.VideoLibrary,
                            title = title,
                            description = descriptor.description,
                            onClick = {
                                onPlay(
                                    PlaybackSelection(
                                        targetEntry = descriptor.targetEntry,
                                        queueEntries = queueEntries,
                                    ),
                                )
                            },
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.size(6.dp)) }
    }
}

internal data class RecentPlaybackDescriptor(
    val targetEntry: PlaybackQueueEntry,
    val uri: Uri?,
    val fallbackTitle: String,
    val description: String,
    val thumbnailUri: Uri?,
    val thumbnailId: Long?,
    val durationLabel: String?,
    val shouldResolveDisplayName: Boolean,
) {
    companion object {
        fun from(
            mediaId: String,
            knownVideo: LocalVideoItem?,
            unavailableLabel: String,
        ): RecentPlaybackDescriptor {
            if (knownVideo != null) {
                return RecentPlaybackDescriptor(
                    targetEntry = knownVideo.toPlaybackQueueEntry(mediaIdOverride = mediaId),
                    uri = knownVideo.uri,
                    fallbackTitle = knownVideo.title,
                    description = knownVideo.folderPath,
                    thumbnailUri = knownVideo.uri,
                    thumbnailId = knownVideo.id,
                    durationLabel = knownVideo.durationLabel,
                    shouldResolveDisplayName = false,
                )
            }

            val uri = runCatching { Uri.parse(mediaId) }.getOrNull()
            val fallbackTitle = uri?.lastPathSegment?.takeIf { it.isNotBlank() }
                ?: mediaId.substringAfterLast('/').ifBlank { unavailableLabel }
            val scheme = uri?.scheme?.lowercase()
            val description = when (scheme) {
                "content", "file" -> uri.toString()
                "http", "https", "rtsp" -> uri.toString()
                null -> unavailableLabel
                else -> uri.toString()
            }
            val thumbnailUri = when (scheme) {
                "content", "file" -> uri
                else -> null
            }
            return RecentPlaybackDescriptor(
                targetEntry = PlaybackQueueEntry(
                    mediaId = mediaId,
                    uri = mediaId,
                ),
                uri = uri,
                fallbackTitle = fallbackTitle,
                description = description,
                thumbnailUri = thumbnailUri,
                thumbnailId = null,
                durationLabel = null,
                shouldResolveDisplayName = scheme == "content",
            )
        }
    }
}

private fun resolveDisplayName(context: android.content.Context, uri: Uri): String? {
    val resolver = context.contentResolver
    val cursor = runCatching {
        resolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)
    }.getOrNull() ?: return null
    cursor.use {
        if (!it.moveToFirst()) return null
        val idx = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
        if (idx < 0) return null
        return it.getString(idx)
    }
}
