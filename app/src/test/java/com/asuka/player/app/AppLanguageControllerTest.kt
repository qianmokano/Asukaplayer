package com.asuka.player.app

import androidx.core.os.LocaleListCompat
import com.asuka.player.data.AppSettingsSnapshot
import com.asuka.player.data.AppSettingsStore
import com.asuka.player.runtime.AppLanguagePreference
import com.asuka.player.runtime.UiSettingsRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking

class AppLanguageControllerTest {

    @Test
    fun localeListMapping_coversAllPreferences() {
        assertEquals(0, AppLanguagePreference.System.toLocaleList().size())
        assertEquals("es", AppLanguagePreference.Spanish.toLocaleList().toLanguageTags())
        assertEquals("zh-CN", AppLanguagePreference.Chinese.toLocaleList().toLanguageTags())
        assertEquals("en", AppLanguagePreference.English.toLocaleList().toLanguageTags())
    }

    @Test
    fun localeListToPreference_matchesExactAndVariants() {
        assertEquals(AppLanguagePreference.Spanish, LocaleListCompat.forLanguageTags("es").toAppLanguagePreference())
        assertEquals(AppLanguagePreference.English, LocaleListCompat.forLanguageTags("en-US").toAppLanguagePreference())
        assertEquals(AppLanguagePreference.Chinese, LocaleListCompat.forLanguageTags("zh-CN").toAppLanguagePreference())
        assertEquals(null, LocaleListCompat.getEmptyLocaleList().toAppLanguagePreference())
        assertEquals(null, LocaleListCompat.forLanguageTags("fr").toAppLanguagePreference())
    }

    @Test
    fun controller_appliesPreference_andTracksInAppChanges() = runBlocking {
        val repository = UiSettingsRepository(FakeAppSettingsStore(), testScope())
        val applier = RecordingApplier()
        val controller = AppLanguageController(repository, testScope(), applier)

        controller.start()

        assertEquals(listOf(AppLanguagePreference.System), applier.applied)

        repository.setAppLanguage(AppLanguagePreference.Spanish)

        assertEquals(listOf(AppLanguagePreference.System, AppLanguagePreference.Spanish), applier.applied)
    }

    @Test
    fun controller_adoptsSystemManagedOverrideIntoStore() = runBlocking {
        val repository = UiSettingsRepository(FakeAppSettingsStore(), testScope())
        val applier = RecordingApplier(initialLocales = LocaleListCompat.forLanguageTags("es"))
        val controller = AppLanguageController(repository, testScope(), applier)

        controller.start()

        assertEquals(AppLanguagePreference.Spanish, repository.settings.value.appLanguage)
    }

    private fun testScope() = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)

    private class RecordingApplier(
        initialLocales: LocaleListCompat = LocaleListCompat.getEmptyLocaleList(),
    ) : AppLanguageApplier {
        private var current = initialLocales
        val applied = mutableListOf<AppLanguagePreference>()

        override fun apply(preference: AppLanguagePreference) {
            applied += preference
            current = preference.toLocaleList()
        }

        override fun currentApplicationLocales(): LocaleListCompat = current
    }

    private class FakeAppSettingsStore : AppSettingsStore {
        private val snapshotsFlow = MutableStateFlow(AppSettingsSnapshot())

        override val snapshots: StateFlow<AppSettingsSnapshot> = snapshotsFlow.asStateFlow()

        override suspend fun saveSnapshot(snapshot: AppSettingsSnapshot) {
            snapshotsFlow.value = snapshot
        }
    }
}
