package com.asuka.player.data

import android.content.Context
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
@Entity(tableName = "playback_state")
data class PlaybackStateEntity(
    @PrimaryKey
    val mediaId: String,
    val positionMs: Long? = null,
    val playbackSpeed: Float? = null,
    val audioTrackId: String? = null,
    val subtitleTrackId: String? = null,
    val zoom: Float? = null,
    @ColumnInfo(index = true)
    val lastTouchedAt: Long = 0L,
)

@Entity(tableName = "queue_history")
data class QueueHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val entryId: Long = 0L,
    val mediaId: String,
)

@Dao
interface PlaybackStateDao {
    @Query("SELECT * FROM playback_state WHERE mediaId = :mediaId")
    fun findByMediaId(mediaId: String): PlaybackStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(entity: PlaybackStateEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertAll(entities: List<PlaybackStateEntity>)

    @Query("SELECT mediaId FROM playback_state ORDER BY lastTouchedAt DESC LIMIT :limit")
    fun recentMediaIds(limit: Int): List<String>

    @Query("SELECT COUNT(*) FROM playback_state")
    fun count(): Int

    @Query(
        """
        DELETE FROM playback_state
        WHERE mediaId NOT IN (
            SELECT mediaId FROM playback_state
            ORDER BY lastTouchedAt DESC
            LIMIT :maxEntries
        )
        """,
    )
    fun pruneToMaxEntries(maxEntries: Int)
}

@Dao
interface QueueHistoryDao {
    @Query("SELECT mediaId FROM queue_history ORDER BY entryId ASC")
    fun items(): List<String>

    @Query("SELECT mediaId FROM queue_history ORDER BY entryId DESC LIMIT 1")
    fun latestMediaId(): String?

    @Insert
    fun insert(entity: QueueHistoryEntity)

    @Insert
    fun insertAll(entities: List<QueueHistoryEntity>)

    @Query("SELECT COUNT(*) FROM queue_history")
    fun count(): Int

    @Query(
        """
        DELETE FROM queue_history
        WHERE entryId IN (
            SELECT entryId FROM queue_history
            ORDER BY entryId ASC
            LIMIT :count
        )
        """,
    )
    fun deleteOldest(count: Int)
}

@Database(
    entities = [
        PlaybackStateEntity::class,
        QueueHistoryEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class AsukaPlaybackRoomDatabase : RoomDatabase() {
    abstract fun playbackStateDao(): PlaybackStateDao
    abstract fun queueHistoryDao(): QueueHistoryDao

    companion object {
        const val DB_NAME = "asuka_playback.db"

        fun open(
            context: Context,
        ): AsukaPlaybackRoomDatabase {
            return Room.databaseBuilder(
                context,
                AsukaPlaybackRoomDatabase::class.java,
                DB_NAME,
            ).build()
        }

        fun inMemory(context: Context): AsukaPlaybackRoomDatabase {
            return Room.inMemoryDatabaseBuilder(
                context,
                AsukaPlaybackRoomDatabase::class.java,
            ).allowMainThreadQueries()
                .build()
        }
    }
}

suspend fun AsukaPlaybackRoomDatabase.importLegacyDataIfNeeded(
    legacyPlaybackStore: SharedPreferencesPlaybackStore,
    legacyQueueHistoryStore: SharedPreferencesQueueHistoryStore,
) {
    val playbackDao = playbackStateDao()
    val historyDao = queueHistoryDao()
    val playbackEntities = if (playbackDao.count() == 0) {
        legacyPlaybackStore.exportLegacyPlaybackStateEntities(limit = 10_000)
    } else {
        emptyList()
    }
    val historyEntities = if (historyDao.count() == 0) {
        legacyQueueHistoryStore.items().map { mediaId ->
            QueueHistoryEntity(mediaId = mediaId)
        }
    } else {
        emptyList()
    }
    if (playbackEntities.isEmpty() && historyEntities.isEmpty()) return

    runInTransaction {
        if (playbackEntities.isNotEmpty()) {
            playbackDao.upsertAll(playbackEntities)
        }
        if (historyEntities.isNotEmpty()) {
            historyDao.insertAll(historyEntities)
        }
    }
}

private suspend fun SharedPreferencesPlaybackStore.exportLegacyPlaybackStateEntities(
    limit: Int,
): List<PlaybackStateEntity> {
    val baseTime = System.currentTimeMillis()
    return recentMediaIds(limit = limit)
        .asReversed()
        .mapIndexed { index, mediaId ->
            PlaybackStateEntity(
                mediaId = mediaId,
                positionMs = loadPosition(mediaId),
                playbackSpeed = loadPlaybackSpeed(mediaId),
                audioTrackId = loadAudioTrackId(mediaId),
                subtitleTrackId = loadSubtitleTrackId(mediaId),
                zoom = loadZoom(mediaId),
                lastTouchedAt = baseTime + index,
            )
        }
}
