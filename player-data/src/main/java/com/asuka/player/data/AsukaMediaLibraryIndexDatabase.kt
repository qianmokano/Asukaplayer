package com.asuka.player.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase

@Entity(
    tableName = "media_library_video",
    indices = [
        Index("folderId"),
        Index("dateAddedSec"),
        Index("dateModifiedSec"),
    ],
)
data class IndexedVideoEntity(
    @PrimaryKey
    val mediaStoreId: Long,
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

    @Query("DELETE FROM media_library_video WHERE mediaStoreId IN (:ids)")
    fun deleteByIds(ids: List<Long>)

    @Query("SELECT MAX(dateModifiedSec) FROM media_library_video")
    fun maxDateModifiedSec(): Long?

    @Query("SELECT MAX(generationModified) FROM media_library_video")
    fun maxGenerationModified(): Long?

    @Query("SELECT COUNT(*) FROM media_library_video")
    fun count(): Int

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
        WHERE mediaStoreId IN (:ids)
        """,
    )
    fun findByIds(ids: List<Long>): List<IndexedVideoEntity>
}

@Database(
    entities = [IndexedVideoEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class AsukaMediaLibraryIndexDatabase : RoomDatabase() {
    abstract fun indexedVideoDao(): IndexedVideoDao

    companion object {
        const val DB_NAME = "asuka_media_library_index.db"

        fun open(context: Context): AsukaMediaLibraryIndexDatabase {
            return Room.databaseBuilder(
                context,
                AsukaMediaLibraryIndexDatabase::class.java,
                DB_NAME,
            ).fallbackToDestructiveMigration()
                .build()
        }

        fun inMemory(context: Context): AsukaMediaLibraryIndexDatabase {
            return Room.inMemoryDatabaseBuilder(
                context,
                AsukaMediaLibraryIndexDatabase::class.java,
            ).allowMainThreadQueries()
                .build()
        }
    }
}
