package com.asuka.player.data

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@Config(sdk = [34])
@RunWith(RobolectricTestRunner::class)
class AsukaMediaLibraryIndexDatabaseMigrationTest {
    @Test
    fun migrate1To2_preservesRowsAndBackfillsGenerationColumns() {
        val context = RuntimeEnvironment.getApplication()
        context.deleteDatabase(TEST_DB)
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(TEST_DB)
                .callback(object : SupportSQLiteOpenHelper.Callback(1) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        createVersion1Schema(db)
                    }

                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                })
                .build(),
        )

        try {
            val db = helper.writableDatabase
            insertVersion1Row(db)
            AsukaMediaLibraryIndexDatabase.MIGRATION_1_2.migrate(db)
            db.query("SELECT mediaStoreId, generationAdded, generationModified FROM media_library_video")
                .use { cursor ->
                    assertEquals(true, cursor.moveToFirst())
                    assertEquals(42L, cursor.getLong(0))
                    assertEquals(0L, cursor.getLong(1))
                    assertEquals(0L, cursor.getLong(2))
                }
        } finally {
            helper.close()
            context.deleteDatabase(TEST_DB)
        }
    }

    private fun createVersion1Schema(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS media_library_video (
                mediaStoreId INTEGER NOT NULL,
                uri TEXT NOT NULL,
                title TEXT NOT NULL,
                durationMs INTEGER NOT NULL,
                sizeBytes INTEGER NOT NULL,
                folderName TEXT NOT NULL,
                folderPath TEXT NOT NULL,
                folderId INTEGER NOT NULL,
                dateAddedSec INTEGER NOT NULL,
                dateModifiedSec INTEGER NOT NULL,
                PRIMARY KEY(mediaStoreId)
            )
            """.trimIndent(),
        )
    }

    private fun insertVersion1Row(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            INSERT INTO media_library_video (
                mediaStoreId, uri, title, durationMs, sizeBytes,
                folderName, folderPath, folderId, dateAddedSec, dateModifiedSec
            ) VALUES (
                42, 'content://media/external/video/media/42', 'clip.mp4', 1000, 2000,
                'Movies', '/storage/emulated/0/Movies', 7, 10, 20
            )
            """.trimIndent(),
        )
    }

    private companion object {
        const val TEST_DB = "media-library-migration-test"
    }
}
