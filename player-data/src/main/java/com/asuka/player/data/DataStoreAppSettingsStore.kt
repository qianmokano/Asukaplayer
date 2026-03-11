package com.asuka.player.data

import android.content.Context
import androidx.datastore.core.DataMigration
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.Serializer
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import kotlin.text.Charsets.UTF_8
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class DataStoreAppSettingsStore(
    context: Context,
    scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
    legacyStore: SharedPreferencesAppSettingsStore = SharedPreferencesAppSettingsStore(context),
) : AppSettingsStore {
    private val writeMutex = Mutex()
    private val stateLock = Any()
    private val dataStore: DataStore<AppSettingsSnapshot> = DataStoreFactory.create(
        serializer = AppSettingsSnapshotSerializer,
        migrations = listOf(SharedPreferencesAppSettingsMigration(legacyStore)),
        scope = scope,
        produceFile = { appSettingsFile(context) },
    )
    private var snapshotCache: AppSettingsSnapshot = runBlocking {
        dataStore.data.first().normalized()
    }

    init {
        scope.launch {
            dataStore.data.collect { snapshot ->
                synchronized(stateLock) {
                    snapshotCache = snapshot.normalized()
                }
            }
        }
    }

    override fun loadSnapshot(): AppSettingsSnapshot {
        synchronized(stateLock) {
            return snapshotCache
        }
    }

    override fun saveSnapshot(snapshot: AppSettingsSnapshot) {
        val normalized = snapshot.normalized()
        synchronized(stateLock) {
            snapshotCache = normalized
        }
        persistAsync(normalized)
    }

    internal suspend fun awaitPersistence() {
        writeMutex.withLock { }
    }

    private fun persistAsync(snapshot: AppSettingsSnapshot) {
        dataStoreScope.launch {
            writeMutex.withLock {
                dataStore.updateData { snapshot }
            }
        }
    }

    private val dataStoreScope = scope

    private class SharedPreferencesAppSettingsMigration(
        private val legacyStore: SharedPreferencesAppSettingsStore,
    ) : DataMigration<AppSettingsSnapshot> {
        override suspend fun shouldMigrate(currentData: AppSettingsSnapshot): Boolean {
            return currentData == AppSettingsSnapshot() && legacyStore.hasLegacyData()
        }

        override suspend fun migrate(currentData: AppSettingsSnapshot): AppSettingsSnapshot {
            if (!shouldMigrate(currentData)) return currentData
            return legacyStore.loadSnapshot()
        }

        override suspend fun cleanUp() = Unit
    }

    private object AppSettingsSnapshotSerializer : Serializer<AppSettingsSnapshot> {
        override val defaultValue: AppSettingsSnapshot = AppSettingsSnapshot()

        override suspend fun readFrom(input: InputStream): AppSettingsSnapshot {
            val raw = input.readBytes().toString(UTF_8)
            return runCatching { AppSettingsSnapshotJsonCodec.decode(raw) }
                .getOrDefault(defaultValue)
        }

        override suspend fun writeTo(
            t: AppSettingsSnapshot,
            output: OutputStream,
        ) {
            output.write(AppSettingsSnapshotJsonCodec.encode(t).toByteArray(UTF_8))
        }
    }
}

private fun appSettingsFile(context: Context): File {
    return File(context.filesDir, "datastore/app_settings.json").apply {
        parentFile?.mkdirs()
    }
}
