package com.asuka.player.contract

/**
 * History of played media used to seed new playback sessions.
 *
 * **Thread safety:** Implementations must be safe to call from any thread.
 */
interface QueueHistoryStore {
    suspend fun push(mediaId: String)
    suspend fun items(): List<String>
}
