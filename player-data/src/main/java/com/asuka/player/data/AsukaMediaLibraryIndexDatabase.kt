package com.asuka.player.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.migration.Migration
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Entity(
    tableName = "media_library_video",
    indices = [
        Index(value = ["volumeName", "mediaStoreId"], unique = true),
        Index("folderId"),
        Index("dateAddedSec"),
        Index("dateModifiedSec"),
    ],
)
data class IndexedVideoEntity(
    val volumeName: String = "external_primary",
    val mediaStoreId: Long,
    @PrimaryKey
    val stableId: String = "$volumeName:$mediaStoreId",
    val uri: String,
    val title: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val folderName: String,
    val folderPath: String,
    val folderId: Long,
    val dateAddedSec: Long,
    val dateModifiedSec: Long,
    val generationAdded: Long = 0L,
    val generationModified: Long = 0L,
)

@Entity(tableName = "media_library_sync_state")
data class MediaLibrarySyncStateEntity(
    @PrimaryKey
    val volumeName: String = "external_primary",
    val lastReconciledGeneration: Long,
    val mediaStoreVersion: String? = null,
)

data class IndexedFolderSummaryRow(
    val folderId: Long,
    val folderName: String,
    val videoCount: Int,
    val totalDurationMs: Long,
    val totalSizeBytes: Long,
)

@Dao
interface IndexedVideoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertAll(entities: List<IndexedVideoEntity>)

    @Query("SELECT mediaStoreId FROM media_library_video")
    fun allIds(): List<Long>

    @Query("SELECT mediaStoreId FROM media_library_video WHERE volumeName = :volumeName")
    fun allMediaStoreIds(volumeName: String): List<Long>

    @Query("DELETE FROM media_library_video WHERE mediaStoreId IN (:ids)")
    fun deleteByIds(ids: List<Long>)

    @Query("DELETE FROM media_library_video WHERE volumeName = :volumeName AND mediaStoreId IN (:ids)")
    fun deleteByMediaStoreIds(volumeName: String, ids: List<Long>)

    @Query("DELETE FROM media_library_video WHERE volumeName NOT IN (:volumeNames)")
    fun deleteByMissingVolumes(volumeNames: List<String>): Int

    @Query("DELETE FROM media_library_video")
    fun deleteAll(): Int

    @Query("SELECT MAX(dateModifiedSec) FROM media_library_video")
    fun maxDateModifiedSec(): Long?

    @Query("SELECT MAX(dateModifiedSec) FROM media_library_video WHERE volumeName = :volumeName")
    fun maxDateModifiedSec(volumeName: String): Long?

    @Query("SELECT MAX(generationModified) FROM media_library_video")
    fun maxGenerationModified(): Long?

    @Query("SELECT MAX(generationModified) FROM media_library_video WHERE volumeName = :volumeName")
    fun maxGenerationModified(volumeName: String): Long?

    @Query("SELECT MAX(generationAdded) FROM media_library_video")
    fun maxGenerationAdded(): Long?

    @Query("SELECT MAX(generationAdded) FROM media_library_video WHERE volumeName = :volumeName")
    fun maxGenerationAdded(volumeName: String): Long?

    @Query("SELECT COUNT(*) FROM media_library_video")
    fun count(): Int

    @Query("SELECT COUNT(*) FROM media_library_video WHERE volumeName = :volumeName")
    fun count(volumeName: String): Int

    @Query("SELECT COUNT(*) FROM (SELECT 1 FROM media_library_video GROUP BY folderId)")
    fun folderCount(): Int

    @Query("SELECT COUNT(*) FROM media_library_video WHERE folderId = :folderId")
    fun countByFolder(folderId: Long): Int

    @Query(
        """
        SELECT
            folderId,
            folderName,
            COUNT(*) AS videoCount,
            COALESCE(SUM(durationMs), 0) AS totalDurationMs,
            COALESCE(SUM(sizeBytes), 0) AS totalSizeBytes
        FROM media_library_video
        GROUP BY folderId, folderName
        ORDER BY folderName COLLATE NOCASE ASC, folderId ASC
        LIMIT :limit OFFSET :offset
        """,
    )
    fun pagedFolders(limit: Int, offset: Int): List<IndexedFolderSummaryRow>

    @Query(
        """
        SELECT * FROM media_library_video
        ORDER BY dateAddedSec DESC, mediaStoreId DESC
        LIMIT :limit OFFSET :offset
        """,
    )
    fun pagedVideos(limit: Int, offset: Int): List<IndexedVideoEntity>

    @Query(
        """
        SELECT * FROM media_library_video
        WHERE folderId = :folderId
        ORDER BY dateAddedSec DESC, mediaStoreId DESC
        LIMIT :limit OFFSET :offset
        """,
    )
    fun pagedVideosByFolder(folderId: Long, limit: Int, offset: Int): List<IndexedVideoEntity>

    @Query(
        """
        SELECT * FROM media_library_video
        WHERE stableId IN (:ids)
        """,
    )
    fun findByStableIds(ids: List<String>): List<IndexedVideoEntity>

    @Query(
        """
        SELECT * FROM media_library_video
        WHERE mediaStoreId IN (:ids)
        """,
    )
    fun findByIds(ids: List<Long>): List<IndexedVideoEntity>
}

@Dao
interface MediaLibrarySyncStateDao {
    @Query("SELECT * FROM media_library_sync_state WHERE volumeName = :volumeName")
    fun state(volumeName: String = "external_primary"): MediaLibrarySyncStateEntity?

    @Query("DELETE FROM media_library_sync_state WHERE volumeName NOT IN (:volumeNames)")
    fun deleteByMissingVolumes(volumeNames: List<String>): Int

    @Query("DELETE FROM media_library_sync_state")
    fun deleteAll(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(entity: MediaLibrarySyncStateEntity)
}

@Database(
    entities = [
        IndexedVideoEntity::class,
        MediaLibrarySyncStateEntity::class,
    ],
    version = 4,
    exportSchema = true,
)
abstract class AsukaMediaLibraryIndexDatabase : RoomDatabase() {
    abstract fun indexedVideoDao(): IndexedVideoDao
    abstract fun mediaLibrarySyncStateDao(): MediaLibrarySyncStateDao

    companion object {
        const val DB_NAME = "asuka_media_library_index.db"

        fun open(context: Context): AsukaMediaLibraryIndexDatabase {
            return Room.databaseBuilder(
                context,
                AsukaMediaLibraryIndexDatabase::class.java,
                DB_NAME,
            ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                .build()
        }

        fun inMemory(context: Context): AsukaMediaLibraryIndexDatabase {
            return Room.inMemoryDatabaseBuilder(
                context,
                AsukaMediaLibraryIndexDatabase::class.java,
            ).allowMainThreadQueries()
                .build()
        }

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE media_library_video ADD COLUMN generationAdded INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE media_library_video ADD COLUMN generationModified INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS media_library_sync_state (
                        id INTEGER NOT NULL,
                        lastReconciledGeneration INTEGER NOT NULL,
                        mediaStoreVersion TEXT,
                        PRIMARY KEY(id)
                    )
                    """.trimIndent(),
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Rows from the merged external volume have no trustworthy source-volume key.
                // Rebuild this cache so every row can be keyed by its specific MediaStore volume.
                db.execSQL("DROP TABLE IF EXISTS media_library_video")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS media_library_video (
                        volumeName TEXT NOT NULL,
                        mediaStoreId INTEGER NOT NULL,
                        stableId TEXT NOT NULL,
                        uri TEXT NOT NULL,
                        title TEXT NOT NULL,
                        durationMs INTEGER NOT NULL,
                        sizeBytes INTEGER NOT NULL,
                        folderName TEXT NOT NULL,
                        folderPath TEXT NOT NULL,
                        folderId INTEGER NOT NULL,
                        dateAddedSec INTEGER NOT NULL,
                        dateModifiedSec INTEGER NOT NULL,
                        generationAdded INTEGER NOT NULL,
                        generationModified INTEGER NOT NULL,
                        PRIMARY KEY(stableId)
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_media_library_video_volumeName_mediaStoreId " +
                        "ON media_library_video(volumeName, mediaStoreId)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_media_library_video_folderId " +
                        "ON media_library_video(folderId)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_media_library_video_dateAddedSec " +
                        "ON media_library_video(dateAddedSec)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_media_library_video_dateModifiedSec " +
                        "ON media_library_video(dateModifiedSec)",
                )
                db.execSQL("DROP TABLE IF EXISTS media_library_sync_state")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS media_library_sync_state (
                        volumeName TEXT NOT NULL,
                        lastReconciledGeneration INTEGER NOT NULL,
                        mediaStoreVersion TEXT,
                        PRIMARY KEY(volumeName)
                    )
                    """.trimIndent(),
                )
            }
        }
    }
}
