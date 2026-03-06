package com.asuka.player.core

import android.net.Uri

class QueueHistoryRepository(
    private val store: QueueHistoryStore,
) {
    fun items(): List<Uri> = store.items()
}

