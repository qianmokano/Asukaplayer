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

@Composable
internal fun RecentPageContent(
    modifier: Modifier = Modifier,
    recentMediaIds: List<String>,
    knownVideos: Map<String, LocalVideoItem>,
    onPlay: (String, List<String>) -> Unit,
) {
    val context = LocalContext.current
    val queueMediaIds = remember(recentMediaIds) { recentMediaIds.distinct() }
    LazyColumn(
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
                items = recentMediaIds,
                key = { _, mediaId -> mediaId },
            ) { index, mediaId ->
                val known = knownVideos[mediaId]
                val uri = remember(mediaId) {
                    try {
                        Uri.parse(mediaId)
                    } catch (_: Throwable) {
                        null
                    }
                }
                val fallbackTitle = uri?.lastPathSegment ?: mediaId
                val title by produceState(initialValue = fallbackTitle, mediaId) {
                    if (uri == null) return@produceState
                    val resolved = runCatching { resolveDisplayName(context, uri) }.getOrNull()
                    value = resolved?.ifBlank { fallbackTitle } ?: fallbackTitle
                }

                GroupedListRow(
                    index = index,
                    totalCount = recentMediaIds.size,
                    horizontalPadding = VIDEO_GROUP_HORIZONTAL_PADDING,
                ) {
                    if (known != null) {
                        SettingsNavigationItem(
                            icon = Icons.Outlined.VideoLibrary,
                            thumbnailUri = known.uri,
                            thumbnailId = known.id,
                            durationLabel = known.durationLabel,
                            title = known.title,
                            description = known.folderPath,
                            onClick = { onPlay(known.uri.toString(), queueMediaIds) },
                        )
                    } else {
                        SettingsNavigationItem(
                            icon = Icons.Outlined.VideoLibrary,
                            title = title,
                            description = uri?.toString() ?: mediaId,
                            onClick = { onPlay(mediaId, queueMediaIds) },
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.size(6.dp)) }
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
