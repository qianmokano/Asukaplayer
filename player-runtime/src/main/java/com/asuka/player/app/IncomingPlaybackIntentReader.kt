package com.asuka.player.app

import android.content.Intent
import android.net.Uri
import android.os.Build

data class IncomingPlaybackIntent(
    val mediaId: String,
    val sourceIntent: Intent? = null,
    val queueMediaIds: List<String> = emptyList(),
)

object IncomingPlaybackIntentReader {
    fun read(intent: Intent?): IncomingPlaybackIntent? {
        val sourceIntent = intent ?: return null
        val dataUri = sourceIntent.data
        val clipUris = buildList {
            val clipData = sourceIntent.clipData ?: return@buildList
            for (index in 0 until clipData.itemCount) {
                clipData.getItemAt(index).uri?.let(::add)
            }
        }.distinct()
        val extraStreamUris = readExtraStreamUris(sourceIntent).distinct()
        val primaryUri = dataUri ?: clipUris.firstOrNull() ?: extraStreamUris.firstOrNull() ?: return null
        val queueUris = when {
            clipUris.isNotEmpty() -> normalizedQueue(primaryUri, clipUris)
            extraStreamUris.isNotEmpty() -> normalizedQueue(primaryUri, extraStreamUris)
            else -> listOf(primaryUri)
        }
        return IncomingPlaybackIntent(
            mediaId = primaryUri.toString(),
            sourceIntent = sourceIntent.takeIf { clipUris.isNotEmpty() },
            queueMediaIds = queueUris.map(Uri::toString),
        )
    }

    private fun normalizedQueue(primaryUri: Uri, queueUris: List<Uri>): List<Uri> {
        return if (primaryUri in queueUris) {
            queueUris
        } else {
            listOf(primaryUri) + queueUris
        }
    }

    private fun readExtraStreamUris(intent: Intent): List<Uri> {
        val multiple = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)
        }.orEmpty()
        if (multiple.isNotEmpty()) return multiple

        val single = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
        }
        return listOfNotNull(single)
    }
}
