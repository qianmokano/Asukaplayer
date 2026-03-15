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
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class DataStoreAppSettingsStore(
    context: Context,
    scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
    private val legacyStore: SharedPreferencesAppSettingsStore = SharedPreferencesAppSettingsStore(context),
) : AppSettingsStore {
    private val writeMutex = Mutex()
    private val initialLoad = CompletableDeferred<Unit>()
    private val dataStore: DataStore<AppSettingsSnapshot> = DataStoreFactory.create(
        serializer = AppSettingsSnapshotSerializer,
        migrations = listOf(SharedPreferencesAppSettingsMigration(legacyStore)),
        scope = scope,
        produceFile = { appSettingsFile(context) },
    )
    private val _snapshots = MutableStateFlow(AppSettingsSnapshot())

    override val snapshots: StateFlow<AppSettingsSnapshot> = _snapshots.asStateFlow()

    init {
        scope.launch {
            dataStore.data.collect { snapshot ->
                _snapshots.value = snapshot.normalized()
                initialLoad.complete(Unit)
            }
        }
    }

    override suspend fun saveSnapshot(snapshot: AppSettingsSnapshot) {
        val normalized = snapshot.normalized()
        writeMutex.withLock {
            val persisted = dataStore.updateData { normalized }.normalized()
            _snapshots.value = persisted
        }
    }

    override suspend fun updateSnapshot(transform: (AppSettingsSnapshot) -> AppSettingsSnapshot) {
        writeMutex.withLock {
            val current = _snapshots.value
            val transformed = transform(current).normalized()
            val persisted = dataStore.updateData { transformed }.normalized()
            _snapshots.value = persisted
        }
    }

    override suspend fun awaitLoaded() {
        initialLoad.await()
    }

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
