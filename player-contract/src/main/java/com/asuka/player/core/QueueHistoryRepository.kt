package com.asuka.player.contract

class QueueHistoryRepository(
    private val store: QueueHistoryStore,
) {
    fun items(): List<String> = store.items()
}
