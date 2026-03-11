package com.asuka.player.data

import com.asuka.player.contract.QueueHistoryStore

class RoomQueueHistoryStore(
    private val queueHistoryDao: QueueHistoryDao,
    private val maxSize: Int = 50,
) : QueueHistoryStore {
    private val lock = Any()

    override fun push(mediaId: String) {
        synchronized(lock) {
            if (queueHistoryDao.latestMediaId() == mediaId) return
            queueHistoryDao.insert(QueueHistoryEntity(mediaId = mediaId))
            val overflow = queueHistoryDao.count() - maxSize
            if (overflow > 0) {
                queueHistoryDao.deleteOldest(overflow)
            }
        }
    }

    override fun items(): List<String> {
        synchronized(lock) {
            return queueHistoryDao.items()
        }
    }
}
