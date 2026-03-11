package com.asuka.player.contract

class QueueHistoryRepository(
    private val store: QueueHistoryStore,
) {
    suspend fun items(): List<String> = store.items()
}
