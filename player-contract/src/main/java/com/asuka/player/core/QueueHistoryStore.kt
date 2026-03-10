package com.asuka.player.core

import android.net.Uri

/**
 * History of played media used to seed new playback sessions.
 *
 * **Thread safety:** Implementations must be safe to call from any thread.
 */
interface QueueHistoryStore {
    fun push(uri: Uri)
    fun items(): List<Uri>
}
