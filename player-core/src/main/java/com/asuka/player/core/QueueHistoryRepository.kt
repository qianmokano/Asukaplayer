package com.asuka.player.core

import android.net.Uri
import com.asuka.player.data.QueueHistoryStore

class QueueHistoryRepository(
    private val store: QueueHistoryStore,
) {
    fun items(): List<Uri> = store.items()
}
