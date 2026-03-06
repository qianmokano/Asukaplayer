package com.asuka.player.core

import android.content.ClipData
import android.content.Intent
import android.net.Uri

object IntentQueueReader {
    fun read(intent: Intent): List<Uri> {
        val clip: ClipData? = intent.clipData
        val clipUris = buildList {
            if (clip != null) {
                for (i in 0 until clip.itemCount) {
                    clip.getItemAt(i).uri?.let { add(it) }
                }
            }
        }
        if (clipUris.isNotEmpty()) {
            val queue = clipUris.distinct().toMutableList()
            val dataUri = intent.data
            if (dataUri != null && dataUri !in queue) {
                queue.add(0, dataUri)
            }
            return queue
        }
        return listOfNotNull(intent.data)
    }
}
