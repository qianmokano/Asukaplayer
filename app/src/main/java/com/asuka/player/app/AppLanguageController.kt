package com.asuka.player.app

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.asuka.player.runtime.AppLanguagePreference
import com.asuka.player.runtime.UiSettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Applies the persisted app-language preference via AppCompatDelegate and keeps it in
 * sync when the user changes it in-app. Cold-start restore and in-app switching share
 * the same diff-based path. On API 33+ the system persists per-app locales itself, so
 * a locale override that differs from the stored preference means the user changed it
 * from system settings; that value is adopted back into the settings store.
 */
internal class AppLanguageController(
    private val uiSettingsRepository: UiSettingsRepository,
    private val scope: CoroutineScope,
    private val applier: AppLanguageApplier = AppCompatDelegateApplier,
) {
    fun start() {
        scope.launch {
            adoptSystemManagedOverride()
            var lastApplied: AppLanguagePreference? = null
            uiSettingsRepository.settings.collect { state ->
                if (state.appLanguage == lastApplied) return@collect
                lastApplied = state.appLanguage
                applier.apply(state.appLanguage)
            }
        }
    }

    private suspend fun adoptSystemManagedOverride() {
        val systemLocales = applier.currentApplicationLocales()
        val systemPreference = systemLocales.toAppLanguagePreference() ?: return
        if (systemPreference != uiSettingsRepository.settings.value.appLanguage) {
            uiSettingsRepository.setAppLanguage(systemPreference)
        }
    }
}

internal interface AppLanguageApplier {
    fun apply(preference: AppLanguagePreference)

    fun currentApplicationLocales(): LocaleListCompat
}

internal object AppCompatDelegateApplier : AppLanguageApplier {
    override fun apply(preference: AppLanguagePreference) {
        AppCompatDelegate.setApplicationLocales(preference.toLocaleList())
    }

    override fun currentApplicationLocales(): LocaleListCompat {
        return AppCompatDelegate.getApplicationLocales()
    }
}

internal fun AppLanguagePreference.toLocaleList(): LocaleListCompat {
    val tag = languageTag ?: return LocaleListCompat.getEmptyLocaleList()
    return LocaleListCompat.forLanguageTags(tag)
}

// Returns null when the override is empty or does not map to a supported language.
internal fun LocaleListCompat.toAppLanguagePreference(): AppLanguagePreference? {
    if (isEmpty) return null
    val tag = toLanguageTags()
    return AppLanguagePreference.entries.firstOrNull { preference ->
        preference.languageTag == tag || tag.startsWith("${preference.languageTag}-")
    }
}
