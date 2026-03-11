package com.asuka.player.data

import com.asuka.player.contract.QueueHistoryStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RoomQueueHistoryStore(
    private val queueHistoryDao: QueueHistoryDao,
    private val maxSize: Int = 50,
) : QueueHistoryStore {
    private val lock = Any()

    override suspend fun push(mediaId: String) {
        withContext(Dispatchers.IO) {
            synchronized(lock) {
                if (queueHistoryDao.latestMediaId() == mediaId) return@synchronized
                queueHistoryDao.insert(QueueHistoryEntity(mediaId = mediaId))
                val overflow = queueHistoryDao.count() - maxSize
                if (overflow > 0) {
                    queueHistoryDao.deleteOldest(overflow)
                }
            }
        }
    }

    override suspend fun items(): List<String> {
        return withContext(Dispatchers.IO) {
            synchronized(lock) {
                queueHistoryDao.items()
            }
        }
    }
}
