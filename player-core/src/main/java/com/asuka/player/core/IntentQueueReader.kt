package com.asuka.player.core

import android.content.ClipData
import android.content.Intent
import android.net.Uri

object IntentQueueReader {
    fun read(intent: Intent): List<Uri> {
        val result = mutableListOf<Uri>()
        intent.data?.let { result += it }
        val clip: ClipData? = intent.clipData
        if (clip != null) {
            for (i in 0 until clip.itemCount) {
                clip.getItemAt(i).uri?.let { result += it }
            }
        }
        return result.distinct()
    }
}
